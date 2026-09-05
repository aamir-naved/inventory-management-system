package com.inventory.desktop;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.inventory.auth.entity.UserAccount;
import com.inventory.support.AuthenticatedControllerTestSupport;

@SpringBootTest(properties = "app.desktop.enabled=true")
@AutoConfigureMockMvc
class DesktopBackupControllerTest extends AuthenticatedControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DesktopBackupOperations desktopBackupService;

    @Test
    void ownerCanReadDesktopInfoAndDownloadBackup() throws Exception {
        UserAccount owner = createUserAccount();
        createBusinessFor(owner);
        org.mockito.Mockito.when(desktopBackupService.dataDir()).thenReturn(Path.of("/tmp/inventory-desktop"));
        org.mockito.Mockito.when(desktopBackupService.pendingRestoreQueued()).thenReturn(false);
        doAnswer(invocation -> {
            java.io.OutputStream output = invocation.getArgument(0);
            output.write("gzip-bytes".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(desktopBackupService).writeGzipDump(any());

        mockMvc.perform(get("/desktop/info").header("Authorization", authorizationHeader(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.desktop").value(true))
            .andExpect(jsonPath("$.pendingRestore").value(false));

        mockMvc.perform(get("/desktop/backup").header("Authorization", authorizationHeader(owner)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".sql.gz")));
    }

    @Test
    void queuesRestoreForNextLaunch() throws Exception {
        UserAccount owner = createUserAccount();
        createBusinessFor(owner);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "inventory-backup.sql.gz",
            "application/gzip",
            new byte[] {0x1f, (byte) 0x8b, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}
        );

        mockMvc.perform(multipart("/desktop/restore")
                .file(file)
                .header("Authorization", authorizationHeader(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.restartRequired").value(true));

        verify(desktopBackupService).queueRestore(any(), org.mockito.Mockito.eq("inventory-backup.sql.gz"));
    }

    @Test
    void unauthenticatedBackupIsRejected() throws Exception {
        mockMvc.perform(get("/desktop/backup")).andExpect(status().isForbidden());
    }

    @Test
    void missingToolsSurfaceAsUnavailable() throws Exception {
        UserAccount owner = createUserAccount();
        createBusinessFor(owner);
        doThrow(new DesktopBackupException("PostgreSQL tools were not found."))
            .when(desktopBackupService).writeGzipDump(any());

        mockMvc.perform(get("/desktop/backup").header("Authorization", authorizationHeader(owner)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message").value("PostgreSQL tools were not found."));
    }
}
