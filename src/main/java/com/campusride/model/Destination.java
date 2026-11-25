package com.campusride.model;

import java.util.Objects;

/**
 * Represents a predefined hotspot/destination in the CampusRide network.
 */
public class Destination {
    private int destinationId;
    private String destinationName;
    private String category;
    private boolean active;

    public Destination() {}

    public Destination(int destinationId, String destinationName, String category, boolean active) {
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.category = category;
        this.active = active;
    }

    public Destination(int destinationId, String destinationName) {
        this(destinationId, destinationName, "General", true);
    }

    public int getDestinationId() { return destinationId; }
    public void setDestinationId(int destinationId) { this.destinationId = destinationId; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return destinationName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Destination that = (Destination) o;
        return destinationId == that.destinationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationId);
    }
}
