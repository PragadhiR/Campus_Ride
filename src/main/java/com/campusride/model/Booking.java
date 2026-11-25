package com.campusride.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a passenger's reservation on a ride.
 */
public class Booking {
    private int bookingId;
    private int rideId;
    private int passengerId;
    private String passengerName;
    private String passengerDepartment;
    private String passengerGender;
    private LocalDateTime bookingTime;
    private String bookingStatus; // 'CONFIRMED', 'CANCELLED'
    private BigDecimal fareShareSnapshot;

    // Joined Ride context fields for easy UI rendering
    private String driverName;
    private String originName;
    private String destinationName;
    private LocalDateTime departureTime;
    private String vehicleType;
    private BigDecimal totalFare;
    private String rideStatus;

    public Booking() {
        this.bookingStatus = "CONFIRMED";
        this.fareShareSnapshot = BigDecimal.ZERO;
    }

    public Booking(int bookingId, int rideId, int passengerId, LocalDateTime bookingTime,
                   String bookingStatus, BigDecimal fareShareSnapshot) {
        this.bookingId = bookingId;
        this.rideId = rideId;
        this.passengerId = passengerId;
        this.bookingTime = bookingTime;
        this.bookingStatus = bookingStatus;
        this.fareShareSnapshot = fareShareSnapshot != null ? fareShareSnapshot : BigDecimal.ZERO;
    }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public int getRideId() { return rideId; }
    public void setRideId(int rideId) { this.rideId = rideId; }

    public int getPassengerId() { return passengerId; }
    public void setPassengerId(int passengerId) { this.passengerId = passengerId; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getPassengerDepartment() { return passengerDepartment; }
    public void setPassengerDepartment(String passengerDepartment) { this.passengerDepartment = passengerDepartment; }

    public String getPassengerGender() { return passengerGender; }
    public void setPassengerGender(String passengerGender) { this.passengerGender = passengerGender; }

    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public BigDecimal getFareShareSnapshot() { return fareShareSnapshot; }
    public void setFareShareSnapshot(BigDecimal fareShareSnapshot) { this.fareShareSnapshot = fareShareSnapshot; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getOriginName() { return originName; }
    public void setOriginName(String originName) { this.originName = originName; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public BigDecimal getTotalFare() { return totalFare; }
    public void setTotalFare(BigDecimal totalFare) { this.totalFare = totalFare; }

    public String getRideStatus() { return rideStatus; }
    public void setRideStatus(String rideStatus) { this.rideStatus = rideStatus; }

    public boolean isConfirmed() {
        return "CONFIRMED".equalsIgnoreCase(bookingStatus);
    }
}
