package com.campusride;

import com.campusride.service.FareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FareService and Fare Calculation Tests")
public class FareServiceTest {
    private FareService fareService;

    @BeforeEach
    public void setUp() {
        fareService = new FareService();
    }

    @Test
    @DisplayName("Calculate fare share with 1 driver + 2 passengers = 3 participants")
    public void testCalculateFareShareThreeParticipants() {
        BigDecimal totalFare = new BigDecimal("600.00");
        int totalParticipants = 3;

        BigDecimal share = fareService.calculateShare(totalFare, totalParticipants);
        assertEquals(new BigDecimal("200.00"), share);
    }

    @Test
    @DisplayName("Calculate fare share with 1 driver + 1 passenger = 2 participants")
    public void testCalculateFareShareTwoParticipants() {
        BigDecimal totalFare = new BigDecimal("550.00");
        int totalParticipants = 2;

        BigDecimal share = fareService.calculateShare(totalFare, totalParticipants);
        assertEquals(new BigDecimal("275.00"), share);
    }

    @Test
    @DisplayName("Calculate individual savings: Solo baseline vs shared fare")
    public void testCalculateIndividualSavings() {
        BigDecimal soloBaseline = new BigDecimal("600.00");
        BigDecimal sharedFare = new BigDecimal("200.00");

        BigDecimal savings = fareService.calculateSavings(soloBaseline, sharedFare);
        assertEquals(new BigDecimal("400.00"), savings);
    }

    @Test
    @DisplayName("Savings should never be negative even if shared fare is unusually high")
    public void testSavingsNonNegative() {
        BigDecimal soloBaseline = new BigDecimal("100.00");
        BigDecimal sharedFare = new BigDecimal("120.00");

        BigDecimal savings = fareService.calculateSavings(soloBaseline, sharedFare);
        assertEquals(BigDecimal.ZERO, savings);
    }

    @Test
    @DisplayName("Handle edge cases for 0 participants or null fare")
    public void testEdgeCases() {
        assertEquals(BigDecimal.ZERO, fareService.calculateShare(null, 3));
        assertEquals(BigDecimal.ZERO, fareService.calculateShare(new BigDecimal("500.00"), 0));
        assertEquals("₹0.00", FareService.formatINR(null));
        assertEquals("₹250.50", FareService.formatINR(new BigDecimal("250.50")));
    }
}
