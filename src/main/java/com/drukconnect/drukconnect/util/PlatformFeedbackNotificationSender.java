package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.feedback.PlatformFeedback;

public interface PlatformFeedbackNotificationSender {

    /*
     * User submitted feedback.
     *
     * Send confirmation to submitting user.
     */
    void sendFeedbackSubmittedToUser(
            User user,
            PlatformFeedback feedback
    );


    /*
     * User submitted feedback.
     *
     * Send notification to admin/team inbox.
     */
    void sendFeedbackSubmittedToAdmin(
            PlatformFeedback feedback
    );


    /*
     * Admin changed NEW / IN_PROGRESS / RESOLVED.
     *
     * Send update to submitting user.
     */
    void sendStatusUpdatedToUser(
            User user,
            PlatformFeedback feedback
    );


    /*
     * Admin changed feedback status.
     *
     * Confirmation/track email to the acting admin.
     */
    void sendStatusUpdatedToAdmin(
            User admin,
            PlatformFeedback feedback
    );


    /*
     * Admin assigned or changed theme.
     *
     * User gets generic feedback-update notification.
     */
    void sendThemeUpdatedToUser(
            User user,
            PlatformFeedback feedback
    );


    /*
     * Admin gets detailed theme-change confirmation.
     */
    void sendThemeUpdatedToAdmin(
            User admin,
            PlatformFeedback feedback
    );
}