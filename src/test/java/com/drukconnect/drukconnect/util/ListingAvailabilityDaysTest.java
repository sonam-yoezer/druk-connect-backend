package com.drukconnect.drukconnect.util;
import com.drukconnect.drukconnect.common.ApiException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ListingAvailabilityDaysTest {
    @Test void normalizesSingleAndMultipleDays() {
        assertEquals("MONDAY", ListingAvailabilityDays.normalize(" monday "));
        assertEquals("MONDAY,WEDNESDAY,SUNDAY", ListingAvailabilityDays.normalize("SUNDAY, monday,WEDNESDAY,MONDAY"));
    }
    @Test void preservesLegacyMeaning() {
        assertEquals("SATURDAY,SUNDAY", ListingAvailabilityDays.normalize("WEEKENDS"));
        assertEquals("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY", ListingAvailabilityDays.normalize("WEEKDAYS"));
        assertEquals("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY", ListingAvailabilityDays.normalize("BOTH"));
    }
    @Test void rejectsEmptyAndUnknownDays() {
        for (String input : new String[]{"", " ", "MONDAY,", "MONDAY,,SUNDAY", "HOLIDAY", "MONDAY SUNDAY"})
            assertThrows(ApiException.class, () -> ListingAvailabilityDays.normalize(input));
        assertThrows(ApiException.class, () -> ListingAvailabilityDays.normalize(null));
    }
}
