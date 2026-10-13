package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.moderation.ListerScamAppeal;
import com.drukconnect.drukconnect.entity.moderation.ListerScamReport;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.stereotype.Component;

import org.springframework.web.util.HtmlUtils;

import java.util.Locale;

@Component
public class ListerScamEmailNotificationSender
        implements ListerScamNotificationSender {

    private final JavaMailSender mailSender;

    private final String fromEmail;


    public ListerScamEmailNotificationSender(

            JavaMailSender mailSender,

            @Value("${spring.mail.username}")
            String fromEmail

    ) {

        this.mailSender =
                mailSender;

        this.fromEmail =
                fromEmail;
    }


    /*
     * =========================================================
     * SCAM REPORT APPROVED
     *
     * LISTER IS NOW RESTRICTED FROM CREATING LISTINGS
     * =========================================================
     */
    @Override
    public void sendRestrictionApplied(

            User lister,

            ListerScamReport report

    ) {

        String listerName =
                fullName(
                        lister
                );


        String reference =
                reportReference(
                        report
                );


        String listingTitle =
                safe(
                        report.getSourceListingTitle()
                );


        String moderationReason =
                safe(
                        report.getModerationReason()
                );


        String subject =
                "Your DrukConnect listing access has been restricted";


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
                    font-family:Arial,Helvetica,sans-serif;
                    background:#F5F2EA;
                    color:#1C2541;
                ">

                    <div style="
                        max-width:600px;
                        margin:0 auto;
                        padding:32px 20px;
                    ">

                        <div style="
                            background:#FBF9F4;
                            border:1px solid #DDD7C8;
                            border-radius:8px;
                            padding:32px;
                        ">

                            <h2 style="
                                margin-top:0;
                                color:#7A2E33;
                            ">
                                Your listing access has been restricted
                            </h2>

                            <p>
                                Hello %s,
                            </p>

                            <p>
                                A scam report involving your DrukConnect account
                                was reviewed and approved by our moderation team.
                            </p>

                            <p>
                                <strong>
                                    Your account is still active and you can
                                    continue logging in normally.
                                </strong>
                            </p>

                            <p>
                                However, you are currently restricted from
                                creating new listings.
                            </p>

                            <div style="
                                background:#FFFFFF;
                                border:1px solid #DDD7C8;
                                border-radius:6px;
                                padding:16px;
                                margin:24px 0;
                            ">

                                <p>
                                    <strong>Reference:</strong>
                                    %s
                                </p>

                                <p>
                                    <strong>Reported from listing:</strong>
                                    %s
                                </p>

                                <p>
                                    <strong>Moderation reason:</strong>
                                    %s
                                </p>

                            </div>

                            <p>
                                If you believe this decision was incorrect,
                                log in to DrukConnect and submit an appeal.
                            </p>

                            <p>
                                Regards,<br>
                                DrukConnect Moderation Team
                            </p>

                        </div>

                    </div>

                </body>
                </html>
                """
                        .formatted(
                                safe(
                                        listerName
                                ),
                                safe(
                                        reference
                                ),
                                listingTitle,
                                moderationReason
                        );


        sendHtml(
                lister.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * LISTER SUBMITTED APPEAL
     * =========================================================
     */
    @Override
    public void sendAppealSubmitted(

            User lister,

            ListerScamAppeal appeal

    ) {

        String listerName =
                fullName(
                        lister
                );


        String subject =
                "We received your DrukConnect appeal";


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
                    font-family:Arial,Helvetica,sans-serif;
                    background:#F5F2EA;
                    color:#1C2541;
                ">

                    <div style="
                        max-width:600px;
                        margin:0 auto;
                        padding:32px 20px;
                    ">

                        <div style="
                            background:#FBF9F4;
                            border:1px solid #DDD7C8;
                            border-radius:8px;
                            padding:32px;
                        ">

                            <h2 style="
                                margin-top:0;
                            ">
                                Appeal received
                            </h2>

                            <p>
                                Hello %s,
                            </p>

                            <p>
                                We have received your appeal regarding your
                                DrukConnect listing restriction.
                            </p>

                            <div style="
                                background:#FFFFFF;
                                border:1px solid #DDD7C8;
                                border-radius:6px;
                                padding:16px;
                                margin:24px 0;
                            ">

                                <p>
                                    <strong>Appeal ID:</strong>
                                    %s
                                </p>

                                <p>
                                    <strong>Status:</strong>
                                    PENDING
                                </p>

                            </div>

                            <p>
                                Your current restriction remains active while
                                the appeal is being reviewed.
                            </p>

                            <p>
                                You can still log in and browse DrukConnect,
                                but you cannot create new listings until the
                                restriction is removed.
                            </p>

                            <p>
                                We will notify you when a decision has been made.
                            </p>

                            <p>
                                Regards,<br>
                                DrukConnect Moderation Team
                            </p>

                        </div>

                    </div>

                </body>
                </html>
                """
                        .formatted(

                                safe(
                                        listerName
                                ),

                                safe(
                                        appeal.getId()
                                                .toString()
                                )
                        );


        sendHtml(
                lister.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * APPEAL APPROVED
     *
     * RESTRICTION REMOVED
     * =========================================================
     */
    @Override
    public void sendAppealApproved(

            User lister,

            ListerScamAppeal appeal

    ) {

        String listerName =
                fullName(
                        lister
                );


        String moderationReason =
                safe(
                        appeal.getModerationReason()
                );


        String subject =
                "Your DrukConnect appeal has been approved";


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
                    font-family:Arial,Helvetica,sans-serif;
                    background:#F5F2EA;
                    color:#1C2541;
                ">

                    <div style="
                        max-width:600px;
                        margin:0 auto;
                        padding:32px 20px;
                    ">

                        <div style="
                            background:#FBF9F4;
                            border:1px solid #DDD7C8;
                            border-radius:8px;
                            padding:32px;
                        ">

                            <h2 style="
                                margin-top:0;
                                color:#2F4F3E;
                            ">
                                Your appeal has been approved
                            </h2>

                            <p>
                                Hello %s,
                            </p>

                            <p>
                                Your appeal has been reviewed and approved by
                                the DrukConnect moderation team.
                            </p>

                            <p>
                                <strong>
                                    Your listing creation restriction has now
                                    been removed.
                                </strong>
                            </p>

                            <p>
                                You can create new listings normally again.
                            </p>

                            <div style="
                                background:#FFFFFF;
                                border:1px solid #DDD7C8;
                                border-radius:6px;
                                padding:16px;
                                margin:24px 0;
                            ">

                                <p>
                                    <strong>Appeal ID:</strong>
                                    %s
                                </p>

                                <p>
                                    <strong>Decision:</strong>
                                    APPROVED
                                </p>

                                <p>
                                    <strong>Moderator note:</strong>
                                    %s
                                </p>

                            </div>

                            <p>
                                Regards,<br>
                                DrukConnect Moderation Team
                            </p>

                        </div>

                    </div>

                </body>
                </html>
                """
                        .formatted(

                                safe(
                                        listerName
                                ),

                                safe(
                                        appeal.getId()
                                                .toString()
                                ),

                                moderationReason
                        );


        sendHtml(
                lister.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * APPEAL REJECTED
     *
     * RESTRICTION REMAINS ACTIVE
     * =========================================================
     */
    @Override
    public void sendAppealRejected(

            User lister,

            ListerScamAppeal appeal

    ) {

        String listerName =
                fullName(
                        lister
                );


        String moderationReason =
                safe(
                        appeal.getModerationReason()
                );


        String subject =
                "Decision on your DrukConnect appeal";


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
                    font-family:Arial,Helvetica,sans-serif;
                    background:#F5F2EA;
                    color:#1C2541;
                ">

                    <div style="
                        max-width:600px;
                        margin:0 auto;
                        padding:32px 20px;
                    ">

                        <div style="
                            background:#FBF9F4;
                            border:1px solid #DDD7C8;
                            border-radius:8px;
                            padding:32px;
                        ">

                            <h2 style="
                                margin-top:0;
                                color:#7A2E33;
                            ">
                                Your appeal was not approved
                            </h2>

                            <p>
                                Hello %s,
                            </p>

                            <p>
                                Your appeal has been reviewed by the
                                DrukConnect moderation team.
                            </p>

                            <p>
                                The appeal was rejected, so your listing
                                creation restriction remains active.
                            </p>

                            <p>
                                You can still log in and browse DrukConnect,
                                but you cannot create new listings.
                            </p>

                            <div style="
                                background:#FFFFFF;
                                border:1px solid #DDD7C8;
                                border-radius:6px;
                                padding:16px;
                                margin:24px 0;
                            ">

                                <p>
                                    <strong>Appeal ID:</strong>
                                    %s
                                </p>

                                <p>
                                    <strong>Decision:</strong>
                                    REJECTED
                                </p>

                                <p>
                                    <strong>Moderator note:</strong>
                                    %s
                                </p>

                            </div>

                            <p>
                                Regards,<br>
                                DrukConnect Moderation Team
                            </p>

                        </div>

                    </div>

                </body>
                </html>
                """
                        .formatted(

                                safe(
                                        listerName
                                ),

                                safe(
                                        appeal.getId()
                                                .toString()
                                ),

                                moderationReason
                        );


        sendHtml(
                lister.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * SEND HTML EMAIL
     * =========================================================
     */
    private void sendHtml(

            String recipient,

            String subject,

            String html

    ) {

        if (
                recipient == null
                        ||
                        recipient.isBlank()
        ) {

            throw new IllegalStateException(
                    "Cannot send moderation email because the Lister has no email address"
            );
        }


        MimeMessage message =
                mailSender
                        .createMimeMessage();


        try {

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );


            helper.setFrom(
                    fromEmail
            );


            helper.setTo(
                    recipient
            );


            helper.setSubject(
                    subject
            );


            helper.setText(
                    html,
                    true
            );


            mailSender.send(
                    message
            );

        } catch (MessagingException ex) {

            throw new IllegalStateException(
                    "Could not create Lister scam notification email",
                    ex
            );
        }
    }


    /*
     * =========================================================
     * LISTER FULL NAME
     * =========================================================
     */
    private String fullName(
            User user
    ) {

        String firstName =
                user.getFirstName() == null
                        ? ""
                        : user.getFirstName()
                        .trim();


        String lastName =
                user.getLastName() == null
                        ? ""
                        : user.getLastName()
                        .trim();


        String result =
                (
                        firstName
                                + " "
                                + lastName
                ).trim();


        if (
                result.isBlank()
        ) {

            return "DrukConnect member";
        }


        return result;
    }


    /*
     * =========================================================
     * REPORT REFERENCE
     * =========================================================
     */
    private String reportReference(
            ListerScamReport report
    ) {

        return "RPT-"
                + report.getId()
                .toString()
                .substring(
                        0,
                        8
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }


    /*
     * =========================================================
     * ESCAPE HTML
     * =========================================================
     *
     * Some strings come from users/admins, therefore don't
     * inject them directly into HTML email.
     * =========================================================
     */
    private String safe(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return "Not provided";
        }


        return HtmlUtils.htmlEscape(
                value
        );
    }
}