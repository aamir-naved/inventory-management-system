package com.inventory.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.inventory.auth.mail.LoggingMailService;
import com.inventory.auth.mail.MailService;

@SpringBootTest
class DevMailConfigurationTest {

    @Autowired
    private MailService mailService;

    @Autowired
    private AuthProperties authProperties;

    @Test
    void usesLoggingMailAndHasJwtSecretWhenSmtpIsAbsent() {
        assertThat(mailService).isInstanceOf(LoggingMailService.class);
        assertThat(authProperties.getJwtSecret()).isNotBlank();
    }
}
