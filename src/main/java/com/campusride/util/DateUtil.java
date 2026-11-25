package com.campusride.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility functions for formatting and displaying dates and relative time windows.
 */
public class DateUtil {
    public static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    public static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    public static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatDateTime(LocalDateTime ldt) {
        if (ldt == null) return "N/A";
        return ldt.format(DISPLAY_FORMATTER);
    }

    public static String formatDateOnly(LocalDateTime ldt) {
        if (ldt == null) return "N/A";
        return ldt.format(DATE_ONLY_FORMATTER);
    }

    public static String formatTimeOnly(LocalDateTime ldt) {
        if (ldt == null) return "N/A";
        return ldt.format(TIME_ONLY_FORMATTER);
    }

    /**
     * Generates a concise reason why a ride matched a search time.
     * e.g., "Exact time match", "15 mins earlier", "20 mins later"
     */
    public static String getRelativeTimeMatchReason(LocalDateTime requestedTime, LocalDateTime actualDeparture) {
        if (requestedTime == null || actualDeparture == null) {
            return "Time window match";
        }

        Duration duration = Duration.between(requestedTime, actualDeparture);
        long minutesDiff = duration.toMinutes();

        if (minutesDiff == 0) {
            return "Exact time match";
        } else if (minutesDiff < 0) {
            return Math.abs(minutesDiff) + " mins earlier";
        } else {
            return minutesDiff + " mins later";
        }
    }
}
