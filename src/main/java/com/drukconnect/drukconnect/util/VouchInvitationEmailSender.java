package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.entity.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class VouchInvitationEmailSender {

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public VouchInvitationEmailSender(
            JavaMailSender mailSender,
            AppProperties properties
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(
            User requester,
            String email,
            String token
    ) {

        String link =
                properties.vouch().invitationUrl()
                        + "?token="
                        + token;

        SimpleMailMessage mail =
                new SimpleMailMessage();

        mail.setFrom(
                properties.mail().from()
        );

        mail.setTo(email);

        mail.setSubject(
                "DrukConnect Vouch Invitation"
        );

        mail.setText(
                requester.getFirstName()
                        + " "
                        + requester.getLastName()
                        + " has invited you to vouch for them on DrukConnect.\n\n"
                        + "Create your BUYER account using the link below:\n\n"
                        + link
                        + "\n\nThis invitation will expire automatically."
        );

        mailSender.send(mail);
    }
}