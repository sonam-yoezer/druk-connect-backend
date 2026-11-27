package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.entity.authentication.User;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailBrandingTest {
    @Test void senderDisplayNameReplacesOldPersonalName() throws Exception {
        var address = new InternetAddress(MailBranding.from("Druk Connect <sender@example.com>"));
        assertEquals("Bhutanese Marketplace", address.getPersonal());
        assertEquals("sender@example.com", address.getAddress());
    }
    @Test void otpAndInvitationUseNewBrandAndVouchWording() throws Exception {
        var mail = mock(JavaMailSender.class);
        var properties = new AppProperties(null, null, null,
            new AppProperties.Mail(true, "sender@example.com"), null,
            new AppProperties.Vouch("https://example.com/invite", null, null, null));
        new SmtpEmailOtpSender(mail, properties).send("recipient@example.com", "123456");
        var requester = new User(); requester.setFirstName("Sonam"); requester.setLastName("Dorji");
        new VouchInvitationEmailSender(mail, properties).send(requester, "recipient@example.com", "invitation-token");
        var messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(2)).send(messages.capture());
        for (var message : messages.getAllValues()) {
            assertEquals("Bhutanese Marketplace", new InternetAddress(message.getFrom()).getPersonal());
            assertTrue(message.getSubject().contains("Bhutanese Marketplace"));
            assertFalse(message.getText().contains("DrukConnect"));
        }
        String invitation = messages.getAllValues().get(1).getText();
        assertTrue(invitation.contains("Create an account to vouch using the link below:"));
        assertTrue(invitation.contains("https://example.com/invite?token=invitation-token"));
        assertFalse(invitation.contains("BUYER account"));
    }
}
