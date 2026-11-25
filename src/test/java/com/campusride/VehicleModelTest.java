package com.campusride;

import com.campusride.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vehicle OOP Model and Polymorphism Tests")
public class VehicleModelTest {

    @Test
    @DisplayName("AutoRide capacity, rate calculation, and polymorphic type")
    public void testAutoRide() {
        Ride auto = Ride.create("AUTO");
        assertInstanceOf(AutoRide.class, auto);
        assertEquals("AUTO", auto.getVehicleType());
        assertEquals(3, auto.getMaximumPassengerCapacity());

        // Distance: 10 km -> Base 40 + (10 * 15) = 190.00
        BigDecimal fare = auto.calculateEstimatedFare(10.0);
        assertEquals(new BigDecimal("190.00"), fare);
    }

    @Test
    @DisplayName("CabRide capacity, rate calculation, and polymorphic type")
    public void testCabRide() {
        Ride cab = Ride.create("CAB");
        assertInstanceOf(CabRide.class, cab);
        assertEquals("CAB", cab.getVehicleType());
        assertEquals(4, cab.getMaximumPassengerCapacity());

        // Distance: 15 km -> Base 100 + (15 * 22) = 430.00
        BigDecimal fare = cab.calculateEstimatedFare(15.0);
        assertEquals(new BigDecimal("430.00"), fare);
    }

    @Test
    @DisplayName("BikeRide capacity, rate calculation, and polymorphic type")
    public void testBikeRide() {
        Ride bike = Ride.create("BIKE");
        assertInstanceOf(BikeRide.class, bike);
        assertEquals("BIKE", bike.getVehicleType());
        assertEquals(1, bike.getMaximumPassengerCapacity());

        // Distance: 8 km -> Base 25 + (8 * 9) = 97.00
        BigDecimal fare = bike.calculateEstimatedFare(8.0);
        assertEquals(new BigDecimal("97.00"), fare);
    }

    @Test
    @DisplayName("Polymorphic factory fallback to CabRide on unknown or null type")
    public void testFactoryFallback() {
        Ride fallbackNull = Ride.create(null);
        assertInstanceOf(CabRide.class, fallbackNull);

        Ride fallbackUnknown = Ride.create("UNKNOWN");
        assertInstanceOf(CabRide.class, fallbackUnknown);
    }

    @Test
    @DisplayName("Total participants formula: 1 driver + confirmed passengers")
    public void testTotalParticipantsFormula() {
        Ride ride = new CabRide();
        assertEquals(1, ride.getTotalParticipants(0));
        assertEquals(3, ride.getTotalParticipants(2));
    }
}
