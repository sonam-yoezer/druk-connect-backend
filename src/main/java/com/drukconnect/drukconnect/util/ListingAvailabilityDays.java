package com.drukconnect.drukconnect.util;

import com.drukconnect.drukconnect.common.ApiException;
import org.springframework.http.HttpStatus;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

/** Canonical Monday-to-Sunday CSV, kept in the existing availability column. */
public final class ListingAvailabilityDays {
    private ListingAvailabilityDays() {}
    public static String normalize(String value) {
        if (value == null || value.isBlank()) throw invalid();
        String input = value.trim().toUpperCase(Locale.ROOT);
        input = switch (input) {
            case "WEEKDAYS" -> "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";
            case "WEEKENDS" -> "SATURDAY,SUNDAY";
            case "BOTH" -> "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";
            default -> input;
        };
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        try {
            for (String day : input.split(",", -1)) days.add(DayOfWeek.valueOf(day.trim()));
        } catch (IllegalArgumentException exception) { throw invalid(); }
        return days.stream().map(Enum::name).collect(Collectors.joining(","));
    }
    private static ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AVAILABILITY",
            "Select at least one valid day from MONDAY to SUNDAY, separated by commas.");
    }
}
