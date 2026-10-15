package com.drukconnect.drukconnect.dto.feedback;

import java.util.List;

public record FeedbackDashboardResponse(

        long newThisWeek,

        double fromListersPercent,

        double fromBrowsingUsersPercent,

        double easyMicroFeedbackPercent30Days,

        long unresolvedCount,

        List<FeedbackThemeStatResponse> recurringThemes

) {
}