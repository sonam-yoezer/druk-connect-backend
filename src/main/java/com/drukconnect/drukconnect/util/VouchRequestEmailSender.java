package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.entity.User;
import com.drukconnect.drukconnect.entity.VouchRequestEntity;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class VouchRequestEmailSender {

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public VouchRequestEmailSender(
            JavaMailSender mailSender,
            AppProperties properties
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(
            User requester,
            User target,
            VouchRequestEntity request,
            String rawToken
    ) {

        try {

            String encodedToken =
                    URLEncoder.encode(
                            rawToken,
                            StandardCharsets.UTF_8
                    );

            /*
             * BACKEND URL ONLY
             *
             * Example:
             * http://localhost:8080
             */
            String backendUrl =
                    properties.vouch()
                            .backendUrl();

            String acceptLink =
                    backendUrl
                            + "/api/v1/vouch-requests/email/"
                            + request.getId()
                            + "/respond"
                            + "?token="
                            + encodedToken
                            + "&action=accept";

            String declineLink =
                    backendUrl
                            + "/api/v1/vouch-requests/email/"
                            + request.getId()
                            + "/respond"
                            + "?token="
                            + encodedToken
                            + "&action=decline";

            MimeMessage mimeMessage =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mimeMessage,
                            true,
                            StandardCharsets.UTF_8.name()
                    );

            helper.setFrom(
                    properties.mail()
                            .from()
            );

            helper.setTo(
                    target.getEmail()
            );

            helper.setSubject(
                    "Vouch Request - DrukConnect"
            );

            String requesterName =
                    requester.getFirstName()
                            + " "
                            + requester.getLastName();

            String html =
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                    </head>

                    <body style="
                        margin:0;
                        padding:0;
                        background:#f5f7fa;
                        font-family:Arial,sans-serif;
                    ">

                        <div style="
                            max-width:600px;
                            margin:30px auto;
                            background:#ffffff;
                            border-radius:12px;
                            padding:35px;
                            box-shadow:0 2px 10px rgba(0,0,0,0.08);
                        ">

                            <h2 style="
                                color:#1f2937;
                                margin-bottom:20px;
                            ">
                                DrukConnect Vouch Request
                            </h2>

                            <p style="
                                color:#374151;
                                font-size:16px;
                            ">
                                Hello %s,
                            </p>

                            <p style="
                                color:#374151;
                                font-size:16px;
                                line-height:1.6;
                            ">
                                <strong>%s</strong>
                                has asked you to vouch for them on DrukConnect.
                            </p>

                            <div style="
                                background:#f3f4f6;
                                padding:15px;
                                border-radius:8px;
                                margin:20px 0;
                            ">

                                <strong>Message:</strong>

                                <p style="
                                    margin-bottom:0;
                                    color:#4b5563;
                                ">
                                    %s
                                </p>

                            </div>

                            <p style="
                                color:#374151;
                                font-size:15px;
                                line-height:1.6;
                            ">
                                Do you know and trust this person enough
                                to vouch for them?
                            </p>

                            <div style="
                                margin-top:30px;
                                margin-bottom:30px;
                            ">

                                <a
                                    href="%s"
                                    style="
                                        display:inline-block;
                                        background:#16a34a;
                                        color:#ffffff;
                                        text-decoration:none;
                                        padding:14px 24px;
                                        border-radius:8px;
                                        font-weight:bold;
                                        margin-right:10px;
                                    "
                                >
                                    ✓ Yes, I Accept
                                </a>

                                <a
                                    href="%s"
                                    style="
                                        display:inline-block;
                                        background:#dc2626;
                                        color:#ffffff;
                                        text-decoration:none;
                                        padding:14px 24px;
                                        border-radius:8px;
                                        font-weight:bold;
                                    "
                                >
                                    ✕ Decline
                                </a>

                            </div>

                            <hr style="
                                border:none;
                                border-top:1px solid #e5e7eb;
                                margin-top:30px;
                            ">

                            <p style="
                                color:#6b7280;
                                font-size:13px;
                            ">
                                You can also log in to DrukConnect
                                and respond from your account.
                            </p>

                            <p style="
                                color:#9ca3af;
                                font-size:12px;
                                margin-top:30px;
                            ">
                                DrukConnect
                            </p>

                        </div>

                    </body>
                    </html>
                    """
                            .formatted(
                                    target.getFirstName(),
                                    requesterName,
                                    request.getMessage(),
                                    acceptLink,
                                    declineLink
                            );

            helper.setText(
                    html,
                    true
            );

            mailSender.send(
                    mimeMessage
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to send vouch request email",
                    ex
            );
        }
    }
}