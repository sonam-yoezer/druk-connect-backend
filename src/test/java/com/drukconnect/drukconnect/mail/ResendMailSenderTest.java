package com.drukconnect.drukconnect.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ResendMailSenderTest {
    private MockRestServiceServer server;
    private ResendMailSender sender;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer test-key");
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new ResendMailSender(builder.build());
    }

    @Test
    void sendsPlainTextOtpOverHttps() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.from").value("onboarding@resend.dev"))
                .andExpect(jsonPath("$.to[0]").value("owner@example.com"))
                .andExpect(jsonPath("$.subject").value("Verification"))
                .andExpect(jsonPath("$.text").value("Your OTP is 1234"))
                .andRespond(withSuccess("{\"id\":\"email-1\"}", MediaType.APPLICATION_JSON));
        sender.send(plain());
        server.verify();
    }

    @Test
    void preservesNestedHtmlAndTextAndReplyTo() throws Exception {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(jsonPath("$.html").value("<p>Welcome འབྲུག</p>"))
                .andExpect(jsonPath("$.text").value("Welcome"))
                .andExpect(jsonPath("$.reply_to[0]").value("support@example.com"))
                .andRespond(withSuccess("{\"id\":\"email-2\"}", MediaType.APPLICATION_JSON));
        var message = sender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("onboarding@resend.dev");
        helper.setTo("owner@example.com");
        helper.setReplyTo("support@example.com");
        helper.setSubject("Welcome");
        helper.setText("Welcome", "<p>Welcome འབྲུག</p>");
        sender.send(message);
        server.verify();
    }

    @Test
    void providerRejectionFailsWithoutLeakingEmailContents() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body("secret-otp-and-provider-body").contentType(MediaType.APPLICATION_JSON));
        var failure = assertThrows(MailSendException.class, () -> sender.send(plain()));
        assertTrue(failure.getMessage().contains("403"));
        assertFalse(failure.getMessage().contains("secret-otp"));
        server.verify();
    }

    @Test
    void missingProviderMessageIdIsNotTreatedAsSuccess() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(MailSendException.class, () -> sender.send(plain()));
        server.verify();
    }

    private SimpleMailMessage plain() {
        var message = new SimpleMailMessage();
        message.setFrom("onboarding@resend.dev");
        message.setTo("owner@example.com");
        message.setSubject("Verification");
        message.setText("Your OTP is 1234");
        return message;
    }
}
