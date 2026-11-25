package com.campusride.model;

import com.campusride.util.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Abstract base class for all Ride types in CampusRide.
 * Demonstrates OOP principles:
 * - Abstraction: Abstract methods for vehicle-specific behavior and calculations
 * - Encapsulation: Private state with validated getters and setters
 * - Polymorphism: Vehicle subclasses override fare and capacity algorithms
 */
public abstract class Ride {
    private int rideId;
    private int driverId;
    private String driverName;
    private String driverDepartment;
    private String driverGender;
    private int originId;
    private String originName;
    private int destinationId;
    private String destinationName;
    private LocalDateTime departureTime;
    private BigDecimal totalFare;
    private int passengerCapacity;
    private int availableSeats;
    private boolean womenOnly;
    private boolean sameDepartmentOnly;
    private String notes;
    private String status; // 'OPEN', 'FULL', 'CANCELLED', 'COMPLETED'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Ride() {
        this.status = "OPEN";
        this.totalFare = BigDecimal.ZERO;
    }

    public Ride(int rideId, int driverId, int originId, int destinationId,
                LocalDateTime departureTime, BigDecimal totalFare,
                int passengerCapacity, int availableSeats, boolean womenOnly,
                boolean sameDepartmentOnly, String notes, String status) {
        this.rideId = rideId;
        this.driverId = driverId;
        this.originId = originId;
        this.destinationId = destinationId;
        this.departureTime = departureTime;
        this.totalFare = totalFare != null ? totalFare : BigDecimal.ZERO;
        this.passengerCapacity = passengerCapacity;
        this.availableSeats = availableSeats;
        this.womenOnly = womenOnly;
        this.sameDepartmentOnly = sameDepartmentOnly;
        this.notes = notes;
        this.status = status != null ? status : "OPEN";
    }

    // ==========================================
    // ABSTRACT METHODS (Polymorphism & Abstraction)
    // ==========================================

    /**
     * Returns the standardized uppercase vehicle type code ('AUTO', 'CAB', 'BIKE').
     */
    public abstract String getVehicleType();

    /**
     * Returns the maximum allowable passenger seating capacity for this vehicle type.
     */
    public abstract int getMaximumPassengerCapacity();

    /**
     * Calculates the system-suggested total estimated fare for a given travel distance.
     */
    public abstract BigDecimal calculateEstimatedFare(double distanceKm);

    /**
     * Calculates the estimated solo travel cost for a single passenger.
     * Used as a baseline for calculating individual savings.
     */
    public abstract BigDecimal calculateSoloFare(double distanceKm);

    // ==========================================
    // CONCRETE OOP BUSINESS METHODS
    // ==========================================

    /**
     * Calculates the total participants including the driver.
     * Formula: 1 (driver) + confirmed passenger bookings.
     */
    public int getTotalParticipants(int confirmedPassengers) {
        return 1 + Math.max(0, confirmedPassengers);
    }

    /**
     * Calculates the estimated fare share per participant.
     * Formula: Total Estimated Fare / Total Participants
     */
    public BigDecimal calculateFareShare(int confirmedPassengers) {
        int participants = getTotalParticipants(confirmedPassengers);
        if (totalFare == null || totalFare.compareTo(BigDecimal.ZERO) <= 0 || participants <= 0) {
            return BigDecimal.ZERO;
        }
        return totalFare.divide(BigDecimal.valueOf(participants), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates individual savings compared to solo travel.
     * Formula: Estimated Solo Travel Fare - Estimated Shared Fare
     */
    public BigDecimal calculateEstimatedSavings(double distanceKm, int confirmedPassengers) {
        BigDecimal solo = calculateSoloFare(distanceKm);
        BigDecimal shared = calculateFareShare(confirmedPassengers);
        BigDecimal savings = solo.subtract(shared);
        return savings.compareTo(BigDecimal.ZERO) > 0 ? savings : BigDecimal.ZERO;
    }

    /**
     * Validates ride attributes before insertion or update.
     */
    public void validate() throws ValidationException {
        if (originId <= 0) {
            throw new ValidationException("Please select a valid origin.");
        }
        if (destinationId <= 0) {
            throw new ValidationException("Please select a valid destination.");
        }
        if (originId == destinationId) {
            throw new ValidationException("Origin and destination must be different locations.");
        }
        if (departureTime == null) {
            throw new ValidationException("Departure time is required.");
        }
        if (departureTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Departure time must be set in the future.");
        }
        if (totalFare == null || totalFare.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Total estimated fare must be greater than ₹0.");
        }
        if (passengerCapacity <= 0) {
            throw new ValidationException("Passenger capacity must be at least 1.");
        }
        if (passengerCapacity > getMaximumPassengerCapacity()) {
            throw new ValidationException("Passenger capacity exceeds maximum limit of " +
                    getMaximumPassengerCapacity() + " for " + getVehicleType() + ".");
        }
        if (availableSeats < 0 || availableSeats > passengerCapacity) {
            throw new ValidationException("Available seats must be between 0 and passenger capacity.");
        }
    }

    /**
     * Factory method demonstrating polymorphism: Instantiates the appropriate subclass
     * based on vehicle type code.
     */
    public static Ride create(String vehicleType) {
        if (vehicleType == null) {
            return new CabRide();
        }
        return switch (vehicleType.trim().toUpperCase()) {
            case "AUTO" -> new AutoRide();
            case "BIKE" -> new BikeRide();
            default -> new CabRide();
        };
    }

    // ==========================================
    // GETTERS AND SETTERS (Encapsulation)
    // ==========================================

    public int getRideId() { return rideId; }
    public void setRideId(int rideId) { this.rideId = rideId; }

    public int getDriverId() { return driverId; }
    public void setDriverId(int driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverDepartment() { return driverDepartment; }
    public void setDriverDepartment(String driverDepartment) { this.driverDepartment = driverDepartment; }

    public String getDriverGender() { return driverGender; }
    public void setDriverGender(String driverGender) { this.driverGender = driverGender; }

    public int getOriginId() { return originId; }
    public void setOriginId(int originId) { this.originId = originId; }

    public String getOriginName() { return originName; }
    public void setOriginName(String originName) { this.originName = originName; }

    public int getDestinationId() { return destinationId; }
    public void setDestinationId(int destinationId) { this.destinationId = destinationId; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public BigDecimal getTotalFare() { return totalFare; }
    public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }

    public int getPassengerCapacity() { return passengerCapacity; }
    public void setPassengerCapacity(int passengerCapacity) { this.passengerCapacity = passengerCapacity; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public boolean isWomenOnly() { return womenOnly; }
    public void setWomenOnly(boolean womenOnly) { this.womenOnly = womenOnly; }

    public boolean isSameDepartmentOnly() { return sameDepartmentOnly; }
    public void setSameDepartmentOnly(boolean sameDepartmentOnly) { this.sameDepartmentOnly = sameDepartmentOnly; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getBookedSeats() {
        return Math.max(0, passengerCapacity - availableSeats);
    }
}
