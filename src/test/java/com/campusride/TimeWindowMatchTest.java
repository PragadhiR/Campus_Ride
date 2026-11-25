package com.campusride;

import com.campusride.util.DateUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Smart Time-Window 30-Minute Matching Tests")
public class TimeWindowMatchTest {

    private boolean isWithin30MinuteWindow(LocalDateTime requestedTime, LocalDateTime rideDepartureTime) {
        long minutesDiff = Math.abs(Duration.between(requestedTime, rideDepartureTime).toMinutes());
        return minutesDiff <= 30;
    }

    @Test
    @DisplayName("Rides departing at exact requested time match successfully")
    public void testExactTimeMatch() {
        LocalDateTime requested = LocalDateTime.of(2026, 11, 15, 17, 0);
        LocalDateTime departure = LocalDateTime.of(2026, 11, 15, 17, 0);

        assertTrue(isWithin30MinuteWindow(requested, departure));
        assertEquals("Exact time match", DateUtil.getRelativeTimeMatchReason(requested, departure));
    }

    @Test
    @DisplayName("Rides departing 20 minutes earlier match and report relative reason")
    public void testEarlierTimeMatch() {
        LocalDateTime requested = LocalDateTime.of(2026, 11, 15, 17, 0);
        LocalDateTime departure = LocalDateTime.of(2026, 11, 15, 16, 40);

        assertTrue(isWithin30MinuteWindow(requested, departure));
        assertEquals("20 mins earlier", DateUtil.getRelativeTimeMatchReason(requested, departure));
    }

    @Test
    @DisplayName("Rides departing 25 minutes later match and report relative reason")
    public void testLaterTimeMatch() {
        LocalDateTime requested = LocalDateTime.of(2026, 11, 15, 17, 0);
        LocalDateTime departure = LocalDateTime.of(2026, 11, 15, 17, 25);

        assertTrue(isWithin30MinuteWindow(requested, departure));
        assertEquals("25 mins later", DateUtil.getRelativeTimeMatchReason(requested, departure));
    }

    @Test
    @DisplayName("Rides departing exactly on the 30-minute boundary are included")
    public void testBoundaryInclusion() {
        LocalDateTime requested = LocalDateTime.of(2026, 11, 15, 17, 0);
        LocalDateTime departureEarlierBoundary = LocalDateTime.of(2026, 11, 15, 16, 30);
        LocalDateTime departureLaterBoundary = LocalDateTime.of(2026, 11, 15, 17, 30);

        assertTrue(isWithin30MinuteWindow(requested, departureEarlierBoundary));
        assertTrue(isWithin30MinuteWindow(requested, departureLaterBoundary));
        assertEquals("30 mins earlier", DateUtil.getRelativeTimeMatchReason(requested, departureEarlierBoundary));
        assertEquals("30 mins later", DateUtil.getRelativeTimeMatchReason(requested, departureLaterBoundary));
    }

    @Test
    @DisplayName("Rides departing outside the 30-minute window (31 mins or more) are excluded")
    public void testOutsideWindowExclusion() {
        LocalDateTime requested = LocalDateTime.of(2026, 11, 15, 17, 0);
        LocalDateTime departure31MinsLater = LocalDateTime.of(2026, 11, 15, 17, 31);
        LocalDateTime departure45MinsEarlier = LocalDateTime.of(2026, 11, 15, 16, 15);

        assertFalse(isWithin30MinuteWindow(requested, departure31MinsLater));
        assertFalse(isWithin30MinuteWindow(requested, departure45MinsEarlier));
    }
}
