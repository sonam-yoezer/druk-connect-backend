package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.entity.User;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class VouchSuccessEmailSender {

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public VouchSuccessEmailSender(
            JavaMailSender mailSender,
            AppProperties properties
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void send(
            User lister,
            User voucher,
            long activeVouchCount
    ) {

        try {

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
                    lister.getEmail()
            );

            helper.setSubject(
                    "Your DrukConnect Vouch Was Successful"
            );

            String voucherName =
                    voucher.getFirstName()
                            + " "
                            + voucher.getLastName();

            String requirementMessage;

            if (activeVouchCount >= 2) {

                requirementMessage =
                        """
                        <div style="
                            margin-top:20px;
                            background:#dcfce7;
                            color:#166534;
                            padding:15px;
                            border-radius:8px;
                        ">
                            ✓ You have now met the minimum requirement
                            of 2 active vouches.
                        </div>
                        """;

            } else {

                long remaining =
                        Math.max(
                                0,
                                2 - activeVouchCount
                        );

                requirementMessage =
                        """
                        <div style="
                            margin-top:20px;
                            background:#fef3c7;
                            color:#92400e;
                            padding:15px;
                            border-radius:8px;
                        ">
                            You need %d more active vouch%s
                            to meet the minimum requirement.
                        </div>
                        """
                                .formatted(
                                        remaining,
                                        remaining == 1 ? "" : "es"
                                );
            }

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
                                Vouch Successful
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
                                Good news! <strong>%s</strong>
                                has successfully vouched for you
                                on DrukConnect.
                            </p>

                            <div style="
                                background:#f3f4f6;
                                padding:20px;
                                border-radius:8px;
                                margin-top:20px;
                                text-align:center;
                            ">

                                <p style="
                                    margin:0;
                                    color:#6b7280;
                                    font-size:14px;
                                ">
                                    Your Active Vouches
                                </p>

                                <p style="
                                    margin:8px 0 0 0;
                                    color:#111827;
                                    font-size:36px;
                                    font-weight:bold;
                                ">
                                    %d
                                </p>

                            </div>

                            %s

                            <p style="
                                color:#6b7280;
                                font-size:13px;
                                line-height:1.5;
                                margin-top:25px;
                            ">
                                Active vouches help establish trust
                                within the DrukConnect community.
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
                                    lister.getFirstName(),
                                    voucherName,
                                    activeVouchCount,
                                    requirementMessage
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
                    "Unable to send successful vouch notification email",
                    ex
            );
        }
    }
}