package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.feedback.PlatformFeedback;

import com.drukconnect.drukconnect.enums.authentication.UserStatus;

import com.drukconnect.drukconnect.repository.authentication.UserRoleRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.stereotype.Component;

import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Locale;

@Component
public class PlatformFeedbackEmailNotificationSender
        implements PlatformFeedbackNotificationSender {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PlatformFeedbackEmailNotificationSender.class
            );


    private final JavaMailSender mailSender;

    private final UserRoleRepository userRoleRepository;

    private final String fromEmail;


    public PlatformFeedbackEmailNotificationSender(

            JavaMailSender mailSender,

            UserRoleRepository userRoleRepository,

            @Value("${spring.mail.username}")
            String fromEmail

    ) {

        this.mailSender =
                mailSender;

        this.userRoleRepository =
                userRoleRepository;

        this.fromEmail =
                fromEmail;
    }


    /*
     * =========================================================
     * FEEDBACK SUBMITTED -> USER
     * =========================================================
     */
    @Override
    public void sendFeedbackSubmittedToUser(

            User user,

            PlatformFeedback feedback

    ) {

        String subject =
                "We received your DrukConnect feedback";


        String html =
                """
                <!DOCTYPE html>
                <html>
                <body style="
                    font-family:Arial,sans-serif;
                    color:#1C2541;
                    line-height:1.6;
                ">

                    <h2>Thank you for your feedback</h2>

                    <p>
                        Hello %s,
                    </p>

                    <p>
                        Your feedback has been received by the
                        DrukConnect team.
                    </p>

                    <p>
                        <strong>Reference:</strong> %s
                    </p>

                    <p>
                        <strong>Type:</strong> %s
                    </p>

                    <p>
                        <strong>Status:</strong> %s
                    </p>

                    <p>
                        We will notify you when the status of your
                        feedback changes.
                    </p>

                    <p>
                        Regards,<br>
                        DrukConnect Team
                    </p>

                </body>
                </html>
                """
                        .formatted(

                                safe(
                                        fullName(
                                                user
                                        )
                                ),

                                safe(
                                        feedbackReference(
                                                feedback
                                        )
                                ),

                                safe(
                                        feedback.getFeedbackType()
                                                .name()
                                ),

                                safe(
                                        feedback.getStatus()
                                                .name()
                                )
                        );


        sendHtml(
                user.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * NEW FEEDBACK -> ALL ACTIVE ADMINS
     * =========================================================
     */
    @Override
    public void sendFeedbackSubmittedToAdmin(
            PlatformFeedback feedback
    ) {

        String submitterName =
                feedback.isAnonymous()

                        ? "Anonymous"

                        : feedback
                        .getSubmitterNameSnapshot();


        String subject =
                "New DrukConnect platform feedback";


        String html =
                """
                <!DOCTYPE html>
                <html>
                <body style="
                    font-family:Arial,sans-serif;
                    color:#1C2541;
                    line-height:1.6;
                ">

                    <h2>New platform feedback received</h2>

                    <p>
                        <strong>Reference:</strong> %s
                    </p>

                    <p>
                        <strong>Submitted by:</strong> %s
                    </p>

                    <p>
                        <strong>Access type:</strong> %s
                    </p>

                    <p>
                        <strong>Source:</strong> %s
                    </p>

                    <p>
                        <strong>Type:</strong> %s
                    </p>

                    <p>
                        <strong>Perspective:</strong> %s
                    </p>

                    <p>
                        <strong>Status:</strong> %s
                    </p>

                    <p>
                        <strong>Message:</strong>
                    </p>

                    <div style="
                        background:#F5F2EA;
                        padding:16px;
                        border-radius:6px;
                    ">
                        %s
                    </div>

                </body>
                </html>
                """
                        .formatted(

                                safe(
                                        feedbackReference(
                                                feedback
                                        )
                                ),

                                safe(
                                        submitterName
                                ),

                                safe(
                                        feedback.getSubmitterAccessType()
                                ),

                                safe(
                                        feedback.getSource()
                                                .name()
                                ),

                                safe(
                                        feedback.getFeedbackType()
                                                .name()
                                ),

                                safe(
                                        feedback.getPerspective()
                                                .name()
                                ),

                                safe(
                                        feedback.getStatus()
                                                .name()
                                ),

                                safe(
                                        feedback.getMessage()
                                )
                        );


        /*
         * THIS IS DYNAMIC.
         *
         * Every active user currently assigned ADMIN role
         * receives the notification.
         */
        sendToAllAdmins(
                subject,
                html
        );
    }


    /*
     * =========================================================
     * STATUS UPDATED -> ORIGINAL USER
     * =========================================================
     */
    @Override
    public void sendStatusUpdatedToUser(

            User user,

            PlatformFeedback feedback

    ) {

        String subject =
                "Your DrukConnect feedback status was updated";


        String html =
                """
                <!DOCTYPE html>
                <html>
                <body style="
                    font-family:Arial,sans-serif;
                    color:#1C2541;
                    line-height:1.6;
                ">

                    <h2>Your feedback has been updated</h2>

                    <p>Hello %s,</p>

                    <p>
                        The DrukConnect team has updated
                        your feedback.
                    </p>

                    <p>
                        <strong>Reference:</strong> %s
                    </p>

                    <p>
                        <strong>Current status:</strong> %s
                    </p>

                    <p>
                        Regards,<br>
                        DrukConnect Team
                    </p>

                </body>
                </html>
                """
                        .formatted(

                                safe(
                                        fullName(
                                                user
                                        )
                                ),

                                safe(
                                        feedbackReference(
                                                feedback
                                        )
                                ),

                                safe(
                                        feedback.getStatus()
                                                .name()
                                )
                        );


        sendHtml(
                user.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * STATUS UPDATED -> ACTING ADMIN
     * =========================================================
     *
     * This is the admin that actually performed the action.
     */
    @Override
    public void sendStatusUpdatedToAdmin(

            User admin,

            PlatformFeedback feedback

    ) {

        String subject =
                "DrukConnect feedback status updated";


        String html =
                """
                <!DOCTYPE html>
                <html>
                <body style="
                    font-family:Arial,sans-serif;
                    color:#1C2541;
                    line-height:1.6;
                ">

                    <h2>Feedback status changed</h2>

                    <p>Hello %s,</p>

                    <p>
                        You updated a platform feedback record.
                    </p>

                    <p>
                        <strong>Reference:</strong> %s
                    </p>

                    <p>
                        <strong>New status:</strong> %s
                    </p>

                    <p>
                        <strong>Feedback type:</strong> %s
                    </p>

                    <p>
                        This action has also been recorded in
                        the DrukConnect audit log.
                    </p>

                </body>
                </html>
                """
                        .formatted(

                                safe(
                                        fullName(
                                                admin
                                        )
                                ),

                                safe(
                                        feedbackReference(
                                                feedback
                                        )
                                ),

                                safe(
                                        feedback.getStatus()
                                                .name()
                                ),

                                safe(
                                        feedback.getFeedbackType()
                                                .name()
                                )
                        );


        sendHtml(
                admin.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * THEME UPDATED -> ORIGINAL USER
     * =========================================================
     */
    @Override
    public void sendThemeUpdatedToUser(

            User user,

            PlatformFeedback feedback

    ) {

        String subject =
                "Your DrukConnect feedback was reviewed";


        String html =
                """
                <!DOCTYPE html>
                <html>
                <body style="
                    font-family:Arial,sans-serif;
                    color:#1C2541;
                    line-height:1.6;
                ">

                    <h2>Your feedback has been reviewed</h2>

                    <p>Hello %s,</p>

                    <p>
                        The DrukConnect team has reviewed and
                        categorised your feedback.
                    </p>

                    <p>
                        <strong>Reference:</strong> %s
                    </p>

                    <p>
                        Thank you for helping us improve DrukConnect.
                    </p>

                    <p>
                        Regards,<br>
                        DrukConnect Team
                    </p>

                </body>
                </html>
                """
                        .formatted(

                                safe(
                                        fullName(
                                                user
                                        )
                                ),

                                safe(
                                        feedbackReference(
                                                feedback
                                        )
                                )
                        );


        sendHtml(
                user.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * THEME UPDATED -> ACTING ADMIN
     * =========================================================
     */
    @Override
    public void sendThemeUpdatedToAdmin(

            User admin,

            PlatformFeedback feedback

    ) {

        String subject =
                "DrukConnect feedback theme updated";


        String html =
                """
                <!DOCTYPE html>
                <html>
                <body style="
                    font-family:Arial,sans-serif;
                    color:#1C2541;
                    line-height:1.6;
                ">

                    <h2>Feedback theme updated</h2>

                    <p>Hello %s,</p>

                    <p>
                        You updated the recurring theme
                        for a platform feedback record.
                    </p>

                    <p>
                        <strong>Reference:</strong> %s
                    </p>

                    <p>
                        <strong>Theme key:</strong> %s
                    </p>

                    <p>
                        <strong>Theme:</strong> %s
                    </p>

                </body>
                </html>
                """
                        .formatted(

                                safe(
                                        fullName(
                                                admin
                                        )
                                ),

                                safe(
                                        feedbackReference(
                                                feedback
                                        )
                                ),

                                safe(
                                        feedback.getThemeKey()
                                ),

                                safe(
                                        feedback.getThemeLabel()
                                )
                        );


        sendHtml(
                admin.getEmail(),
                subject,
                html
        );
    }


    /*
     * =========================================================
     * FIND ALL ADMINS DYNAMICALLY
     * =========================================================
     */
    private List<User> activeAdmins() {

        return userRoleRepository
                .findUsersByRoleCodeAndStatus(
                        "ADMIN",
                        UserStatus.ACTIVE
                )
                .stream()

                /*
                 * Extra protection against invalid emails.
                 */
                .filter(user ->
                        user.getEmail() != null
                                &&
                                !user.getEmail()
                                        .isBlank()
                )

                /*
                 * Avoid duplicate recipients in case bad role
                 * data creates duplicate mappings.
                 */
                .distinct()

                .toList();
    }


    /*
     * =========================================================
     * SEND TO EVERY CURRENT ADMIN
     * =========================================================
     */
    private void sendToAllAdmins(

            String subject,

            String html

    ) {

        List<User> admins =
                activeAdmins();


        if (
                admins.isEmpty()
        ) {

            log.warn(
                    "No ACTIVE ADMIN users found. "
                            + "Admin feedback notification was not sent."
            );

            return;
        }


        /*
         * Send individually.
         *
         * This prevents administrators from seeing each
         * other's email addresses.
         *
         * It also allows one failed address to not stop
         * the remaining admin notifications.
         */
        for (
                User admin : admins
        ) {

            try {

                sendHtml(
                        admin.getEmail(),
                        subject,
                        html
                );

            } catch (Exception ex) {

                log.error(
                        "Failed to send platform feedback notification "
                                + "to admin. adminUserId={}, email={}",
                        admin.getId(),
                        admin.getEmail(),
                        ex
                );
            }
        }
    }


    /*
     * =========================================================
     * EMAIL
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
                    "Email recipient is missing"
            );
        }


        MimeMessage message =
                mailSender
                        .createMimeMessage();


        try {

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            false,
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
                    "Could not create feedback notification email",
                    ex
            );
        }
    }


    /*
     * =========================================================
     * FEEDBACK REFERENCE
     * =========================================================
     */
    private String feedbackReference(
            PlatformFeedback feedback
    ) {

        return "FDB-"
                + feedback.getId()
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
     * FULL NAME
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


        String name =
                (
                        firstName
                                + " "
                                + lastName
                ).trim();


        return name.isBlank()

                ? "DrukConnect member"

                : name;
    }


    /*
     * =========================================================
     * SAFE HTML
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