package com.campusride.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 2-Wheeler Motorcycle / Scooter ride subclass.
 * Distinct rules:
 * - Maximum capacity: 1 passenger (pillion rider)
 * - Base fare: ₹25.00
 * - Per-kilometer rate: ₹9.00/km
 */
public class BikeRide extends Ride {
    private static final BigDecimal BASE_FARE = new BigDecimal("25.00");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("9.00");
    private static final int MAX_CAPACITY = 1;

    public BikeRide() {
        super();
        setPassengerCapacity(1);
        setAvailableSeats(1);
    }

    public BikeRide(int rideId, int driverId, int originId, int destinationId,
                    LocalDateTime departureTime, BigDecimal totalFare,
                    int passengerCapacity, int availableSeats, boolean womenOnly,
                    boolean sameDepartmentOnly, String notes, String status) {
        super(rideId, driverId, originId, destinationId, departureTime, totalFare,
              passengerCapacity, availableSeats, womenOnly, sameDepartmentOnly, notes, status);
    }

    @Override
    public String getVehicleType() {
        return "BIKE";
    }

    @Override
    public int getMaximumPassengerCapacity() {
        return MAX_CAPACITY;
    }

    @Override
    public BigDecimal calculateEstimatedFare(double distanceKm) {
        if (distanceKm <= 0) return BASE_FARE;
        BigDecimal distanceCost = PER_KM_RATE.multiply(BigDecimal.valueOf(distanceKm));
        return BASE_FARE.add(distanceCost).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateSoloFare(double distanceKm) {
        // Commercial solo bike taxi estimate
        return calculateEstimatedFare(distanceKm);
    }
}
