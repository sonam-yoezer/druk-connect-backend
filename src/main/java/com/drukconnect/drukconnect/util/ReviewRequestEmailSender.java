package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.Listing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class ReviewRequestEmailSender {
    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public ReviewRequestEmailSender(JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.enabled:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    public void send(User buyer, User lister, Listing listing, String reviewUrl) {
        if (!enabled) throw new IllegalStateException("Review request email delivery is disabled");
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(buyer.getEmail());
            helper.setSubject("DrukConnect: please review " + listing.getListingTitle());
            helper.setText("""
                <div style="font-family:Arial,sans-serif;max-width:620px;margin:auto">
                  <h2>Share your experience</h2>
                  <p>Hello %s,</p>
                  <p>%s is requesting a review of <strong>%s</strong>.</p>
                  <p>Please leave an honest review only if you have experience with this service.</p>
                  <p><a href="%s">Review this service</a></p>
                  <p>Sign in with the DrukConnect account associated with this email.
                     This invitation does not sign you in automatically and expires in 30 days.</p>
                </div>
                """.formatted(escape(buyer.getFirstName()),
                    escape(lister.getFirstName() + " " + lister.getLastName()),
                    escape(listing.getListingTitle()), escape(reviewUrl)), true);
            mailSender.send(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to deliver review request email", exception);
        }
    }

    private String escape(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
}
