package com.campusride.service;

import com.campusride.dao.DestinationDAO;
import com.campusride.model.Ride;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Handles fare estimates, solo baseline calculations, and share allocations.
 */
public class FareService {
    private final DestinationDAO destinationDAO;

    public FareService() {
        this.destinationDAO = new DestinationDAO();
    }

    public FareService(DestinationDAO destinationDAO) {
        this.destinationDAO = destinationDAO;
    }

    /**
     * Retrieves distance between two destinations in km.
     */
    public double getDistanceKm(int originId, int destId) {
        return destinationDAO.getDistanceKm(originId, destId);
    }

    /**
     * Calculates the system suggested total fare based on vehicle type and distance.
     */
    public BigDecimal calculateSuggestedFare(String vehicleType, int originId, int destId) {
        double distanceKm = getDistanceKm(originId, destId);
        Ride template = Ride.create(vehicleType);
        return template.calculateEstimatedFare(distanceKm);
    }

    /**
     * Calculates baseline solo travel fare for the vehicle and route.
     */
    public BigDecimal calculateSoloBaseline(String vehicleType, int originId, int destId) {
        double distanceKm = getDistanceKm(originId, destId);
        Ride template = Ride.create(vehicleType);
        return template.calculateSoloFare(distanceKm);
    }

    /**
     * Calculates fare share per person for a given total fare and participant count.
     */
    public BigDecimal calculateShare(BigDecimal totalFare, int totalParticipants) {
        if (totalFare == null || totalParticipants <= 0) {
            return BigDecimal.ZERO;
        }
        return totalFare.divide(BigDecimal.valueOf(totalParticipants), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates individual savings per person against solo baseline.
     */
    public BigDecimal calculateSavings(BigDecimal soloBaseline, BigDecimal sharedFare) {
        if (soloBaseline == null || sharedFare == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal savings = soloBaseline.subtract(sharedFare);
        return savings.compareTo(BigDecimal.ZERO) > 0 ? savings : BigDecimal.ZERO;
    }

    /**
     * Formats monetary values with the Rupee symbol and 2 decimal places.
     */
    public static String formatINR(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return "₹" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
