package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.moderation.ListerScamAppeal;
import com.drukconnect.drukconnect.entity.moderation.ListerScamReport;

public interface ListerScamNotificationSender {

    void sendRestrictionApplied(
            User lister,
            ListerScamReport report
    );


    void sendAppealSubmitted(
            User lister,
            ListerScamAppeal appeal
    );


    void sendAppealApproved(
            User lister,
            ListerScamAppeal appeal
    );


    void sendAppealRejected(
            User lister,
            ListerScamAppeal appeal
    );
}