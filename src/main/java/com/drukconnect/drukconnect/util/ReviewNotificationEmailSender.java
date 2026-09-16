package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.Listing;
import com.drukconnect.drukconnect.entity.listing.ListingReview;
import com.drukconnect.drukconnect.enums.listing.ReviewStatus;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.stereotype.Component;

@Component
public class ReviewNotificationEmailSender {

    private final JavaMailSender mailSender;

    public ReviewNotificationEmailSender(
            JavaMailSender mailSender
    ) {
        this.mailSender =
                mailSender;
    }

    /*
     * =========================================================
     * REVIEWER EMAIL
     * =========================================================
     */
    public void sendReviewerModerationEmail(
            User reviewer,
            Listing listing,
            ListingReview review
    ) {

        String reviewerName =
                fullName(
                        reviewer
                );

        boolean approved =
                review.getStatus()
                        == ReviewStatus.APPROVED;

        String subject =
                approved
                        ? "Your DrukConnect review was approved"
                        : "Your DrukConnect review was rejected";

        String statusText =
                approved
                        ? "approved"
                        : "rejected";

        String statusMessage =
                approved
                        ? """
                        Your review has been approved by the DrukConnect administrator
                        and is now published on the listing.
                        """
                        : """
                        Your review was reviewed by the DrukConnect administrator
                        and was not approved for publication.
                        """;

        String rejectionSection =
                approved
                        ? ""
                        : """
                        <div style="margin-top:16px;padding:14px;background:#fff4f4;border-radius:8px;">
                            <strong>Reason:</strong>
                            <div style="margin-top:6px;">%s</div>
                        </div>
                        """.formatted(
                        escapeHtml(
                                review.getRejectionReason()
                                        == null
                                ? "No additional reason provided."
                                : review.getRejectionReason()
                        )
                );

        String html =
                """
                <div style="font-family:Arial,sans-serif;max-width:620px;margin:auto;color:#222;">
                    <h2>Review %s</h2>

                    <p>Hello %s,</p>

                    <p>%s</p>

                    <p>
                        <strong>Listing:</strong><br/>
                        %s
                    </p>

                    <div style="margin-top:16px;padding:14px;background:#f6f6f8;border-radius:8px;">
                        <strong>Your review:</strong>

                        <div style="margin-top:8px;">
                            Rating: %d / 5
                        </div>

                        <div style="margin-top:8px;">
                            "%s"
                        </div>
                    </div>

                    %s

                    <p style="margin-top:22px;">
                        Thank you for contributing to the DrukConnect community.
                    </p>
                </div>
                """.formatted(

                        statusText,

                        escapeHtml(
                                reviewerName
                        ),

                        statusMessage,

                        escapeHtml(
                                listing.getListingTitle()
                        ),

                        review.getRating(),

                        escapeHtml(
                                review.getComment()
                        ),

                        rejectionSection
                );

        sendHtmlEmail(
                reviewer.getEmail(),
                subject,
                html
        );
    }

    /*
     * =========================================================
     * LISTER EMAIL
     *
     * Only call this after APPROVAL.
     * =========================================================
     */
    public void sendListerNewReviewEmail(
            User lister,
            User reviewer,
            Listing listing,
            ListingReview review
    ) {

        String listerName =
                fullName(
                        lister
                );

        String reviewerName =
                fullName(
                        reviewer
                );

        String subject =
                "You have a new review on DrukConnect";

        String html =
                """
                <div style="font-family:Arial,sans-serif;max-width:620px;margin:auto;color:#222;">
                    <h2>You have a new review</h2>

                    <p>Hello %s,</p>

                    <p>
                        A new review has been approved and published
                        on your listing:
                    </p>

                    <p>
                        <strong>%s</strong>
                    </p>

                    <div style="margin-top:16px;padding:14px;background:#f6f6f8;border-radius:8px;">

                        <div>
                            <strong>Reviewer:</strong>
                            %s
                        </div>

                        <div style="margin-top:8px;">
                            <strong>Rating:</strong>
                            %d / 5
                        </div>

                        <div style="margin-top:8px;">
                            <strong>Review:</strong>
                        </div>

                        <div style="margin-top:6px;">
                            "%s"
                        </div>

                    </div>

                    <p style="margin-top:22px;">
                        You can view the review from your DrukConnect listing.
                    </p>
                </div>
                """.formatted(

                        escapeHtml(
                                listerName
                        ),

                        escapeHtml(
                                listing.getListingTitle()
                        ),

                        escapeHtml(
                                reviewerName
                        ),

                        review.getRating(),

                        escapeHtml(
                                review.getComment()
                        )
                );

        sendHtmlEmail(
                lister.getEmail(),
                subject,
                html
        );
    }

    /*
     * =========================================================
     * EMAIL SENDER
     * =========================================================
     */
    private void sendHtmlEmail(
            String destination,
            String subject,
            String html
    ) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setTo(
                    destination
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

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to send review notification email",
                    ex
            );
        }
    }

    private String fullName(
            User user
    ) {

        return (
                user.getFirstName()
                        + " "
                        + user.getLastName()
        ).trim();
    }

    /*
     * Prevent review comments from injecting HTML
     * into the email.
     */
    private String escapeHtml(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}