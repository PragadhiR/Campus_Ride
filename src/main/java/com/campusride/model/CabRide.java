package com.campusride.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 4-Wheeler Taxi / Cab ride subclass.
 * Distinct rules:
 * - Maximum capacity: 4 passengers (standard sedan/hatchback)
 * - Base fare: ₹100.00
 * - Per-kilometer rate: ₹22.00/km
 */
public class CabRide extends Ride {
    private static final BigDecimal BASE_FARE = new BigDecimal("100.00");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("22.00");
    private static final int MAX_CAPACITY = 4;

    public CabRide() {
        super();
        setPassengerCapacity(4);
        setAvailableSeats(4);
    }

    public CabRide(int rideId, int driverId, int originId, int destinationId,
                   LocalDateTime departureTime, BigDecimal totalFare,
                   int passengerCapacity, int availableSeats, boolean womenOnly,
                   boolean sameDepartmentOnly, String notes, String status) {
        super(rideId, driverId, originId, destinationId, departureTime, totalFare,
              passengerCapacity, availableSeats, womenOnly, sameDepartmentOnly, notes, status);
    }

    @Override
    public String getVehicleType() {
        return "CAB";
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
        // Solo travel in a private taxi incurs the full individual cab fare
        return calculateEstimatedFare(distanceKm);
    }
}
