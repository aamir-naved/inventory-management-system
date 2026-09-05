package com.inventory.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.inventory.auth.mail.LoggingMailService;
import com.inventory.auth.mail.MailMessage;
import com.inventory.auth.sms.SmsGateway;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoggingMailService loggingMailService;

    @Autowired
    private SmsGateway smsGateway;

    @BeforeEach
    void clearMail() {
        loggingMailService.clear();
        smsGateway.clear();
    }

    @Test
    void publicConfigShowsRegistrationOpenInTests() throws Exception {
        mockMvc.perform(get("/auth/public-config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openRegistration").value(true));
    }

    @Test
    void registersAndReturnsSession() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Business Owner",
                      "email": "owner@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andExpect(jsonPath("$.user.email").value("owner@example.com"))
            .andExpect(jsonPath("$.user.emailVerified").value(false))
            .andExpect(jsonPath("$.user.businessId").isEmpty());
    }

    @Test
    void registerAllowsComposeUiOrigin() throws Exception {
        mockMvc.perform(post("/auth/register")
                .header("Origin", "http://localhost:3001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Compose Owner",
                      "email": "compose-owner@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("compose-owner@example.com"));
    }

    @Test
    void logsInAndLoadsCurrentSession() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Business Owner",
                      "email": "owner-login@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk());

        String response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "owner-login@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("owner-login@example.com"))
            .andExpect(jsonPath("$.user.emailVerified").value(false))
            .andExpect(jsonPath("$.refreshToken").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String accessToken = JsonPath.read(response, "$.accessToken");

        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value("owner-login@example.com"))
            .andExpect(jsonPath("$.refreshToken").isEmpty());
    }

    @Test
    void verifiesEmailWithTokenFromMail() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Business Owner",
                      "email": "owner-verify@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.emailVerified").value(false));

        String token = extractTokenFromLatestMail("owner-verify@example.com", "/verify-email?token=");

        mockMvc.perform(post("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "%s"
                    }
                    """.formatted(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Email verified successfully."));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "owner-verify@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.emailVerified").value(true));
    }

    @Test
    void resetsPasswordViaForgotFlow() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Business Owner",
                      "email": "owner-reset@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "owner-reset@example.com"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(
                "If an account exists for that email, password reset instructions have been sent."
            ));

        String token = extractTokenFromLatestMail("owner-reset@example.com", "/reset-password?token=");

        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "token": "%s",
                      "password": "newpassword123"
                    }
                    """.formatted(token)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "owner-reset@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "owner-reset@example.com",
                      "password": "newpassword123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void refreshesAndLogoutRevokesRefreshToken() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Business Owner",
                      "email": "owner-refresh@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String registerBody = registerResult.getResponse().getContentAsString();
        String accessToken = JsonPath.read(registerBody, "$.accessToken");
        String refreshToken = JsonPath.read(registerBody, "$.refreshToken");

        String refreshedBody = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String nextRefreshToken = JsonPath.read(refreshedBody, "$.refreshToken");
        String nextAccessToken = JsonPath.read(refreshedBody, "$.accessToken");
        assertThat(nextRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(refreshToken)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer " + nextAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(nextRefreshToken)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "%s"
                    }
                    """.formatted(nextRefreshToken)))
            .andExpect(status().isBadRequest());

        // keep unused variable warning away for accessToken from register
        assertThat(accessToken).isNotBlank();
    }

    @Test
    void updatesProfileNameAndPassword() throws Exception {
        String registerBody = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Business Owner",
                      "email": "owner-profile@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String accessToken = JsonPath.read(registerBody, "$.accessToken");

        mockMvc.perform(patch("/auth/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Updated Owner"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.fullName").value("Updated Owner"))
            .andExpect(jsonPath("$.refreshToken").isEmpty());

        mockMvc.perform(patch("/auth/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Updated Owner",
                      "currentPassword": "password123",
                      "newPassword": "password456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").isString());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "owner-profile@example.com",
                      "password": "password456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.fullName").value("Updated Owner"));
    }

    @Test
    void signsInWithPhoneOtp() throws Exception {
        mockMvc.perform(post("/auth/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "phone": "9876500100"
                    }
                    """))
            .andExpect(status().isOk());

        String code = extractOtpFromLatestSms("+919876500100");

        mockMvc.perform(post("/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "phone": "9876500100",
                      "code": "%s"
                    }
                    """.formatted(code)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.user.phone").value("+919876500100"))
            .andExpect(jsonPath("$.user.emailVerified").value(true))
            .andExpect(jsonPath("$.user.businessId").isEmpty());
    }

    private String extractOtpFromLatestSms(String phone) {
        String body = smsGateway.findLatestTo(phone)
            .orElseThrow(() -> new IllegalStateException("No SMS found for " + phone))
            .body();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{6})").matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String extractTokenFromLatestMail(String email, String marker) {
        MailMessage message = loggingMailService.findLatestTo(email)
            .orElseThrow(() -> new IllegalStateException("No mail found for " + email));
        int index = message.body().indexOf(marker);
        assertThat(index).isGreaterThanOrEqualTo(0);
        String afterMarker = message.body().substring(index + marker.length());
        return afterMarker.split("\\s")[0].trim();
    }
}
