package com.campusride.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Auto-rickshaw ride subclass.
 * Distinct rules:
 * - Maximum capacity: 3 passengers
 * - Base fare: ₹40.00
 * - Per-kilometer rate: ₹15.00/km
 */
public class AutoRide extends Ride {
    private static final BigDecimal BASE_FARE = new BigDecimal("40.00");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("15.00");
    private static final int MAX_CAPACITY = 3;

    public AutoRide() {
        super();
        setPassengerCapacity(3);
        setAvailableSeats(3);
    }

    public AutoRide(int rideId, int driverId, int originId, int destinationId,
                    LocalDateTime departureTime, BigDecimal totalFare,
                    int passengerCapacity, int availableSeats, boolean womenOnly,
                    boolean sameDepartmentOnly, String notes, String status) {
        super(rideId, driverId, originId, destinationId, departureTime, totalFare,
              passengerCapacity, availableSeats, womenOnly, sameDepartmentOnly, notes, status);
    }

    @Override
    public String getVehicleType() {
        return "AUTO";
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
        // Solo travel in an auto incurs the full single-passenger auto fare
        return calculateEstimatedFare(distanceKm);
    }
}
