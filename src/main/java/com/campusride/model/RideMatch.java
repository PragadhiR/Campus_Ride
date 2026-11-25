package com.campusride.model;

import java.math.BigDecimal;

/**
 * Composite model encapsulating a Ride match with dynamic time-window context
 * and live fare-sharing breakdown.
 */
public class RideMatch {
    private Ride ride;
    private String matchReason;
    private double distanceKm;
    private BigDecimal soloFare;
    private BigDecimal estimatedShare;
    private BigDecimal potentialSavings;
    private int confirmedPassengers;

    public RideMatch(Ride ride, String matchReason, double distanceKm, int confirmedPassengers) {
        this.ride = ride;
        this.matchReason = matchReason;
        this.distanceKm = distanceKm;
        this.confirmedPassengers = confirmedPassengers;
        // Fare share when 1 new passenger joins = totalFare / (1 + confirmedPassengers + 1)
        this.estimatedShare = ride.calculateFareShare(confirmedPassengers + 1);
        this.soloFare = ride.calculateSoloFare(distanceKm);
        this.potentialSavings = soloFare.subtract(estimatedShare);
        if (this.potentialSavings.compareTo(BigDecimal.ZERO) < 0) {
            this.potentialSavings = BigDecimal.ZERO;
        }
    }

    public Ride getRide() { return ride; }
    public String getMatchReason() { return matchReason; }
    public double getDistanceKm() { return distanceKm; }
    public BigDecimal getSoloFare() { return soloFare; }
    public BigDecimal getEstimatedShare() { return estimatedShare; }
    public BigDecimal getPotentialSavings() { return potentialSavings; }
    public int getConfirmedPassengers() { return confirmedPassengers; }
}
