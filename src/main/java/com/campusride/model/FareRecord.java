package com.campusride.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Historical snapshot of cost allocation and savings for completed rides.
 */
public class FareRecord {
    private int recordId;
    private int rideId;
    private int studentId;
    private String studentRole; // 'DRIVER' or 'PASSENGER'
    private BigDecimal soloBaselineFare;
    private BigDecimal sharedFarePaid;
    private BigDecimal individualSavings;
    private LocalDateTime recordedAt;

    public FareRecord() {}

    public FareRecord(int recordId, int rideId, int studentId, String studentRole,
                      BigDecimal soloBaselineFare, BigDecimal sharedFarePaid,
                      BigDecimal individualSavings, LocalDateTime recordedAt) {
        this.recordId = recordId;
        this.rideId = rideId;
        this.studentId = studentId;
        this.studentRole = studentRole;
        this.soloBaselineFare = soloBaselineFare;
        this.sharedFarePaid = sharedFarePaid;
        this.individualSavings = individualSavings;
        this.recordedAt = recordedAt;
    }

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

    public int getRideId() { return rideId; }
    public void setRideId(int rideId) { this.rideId = rideId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentRole() { return studentRole; }
    public void setStudentRole(String studentRole) { this.studentRole = studentRole; }

    public BigDecimal getSoloBaselineFare() { return soloBaselineFare; }
    public void setSoloBaselineFare(BigDecimal soloBaselineFare) { this.soloBaselineFare = soloBaselineFare; }

    public BigDecimal getSharedFarePaid() { return sharedFarePaid; }
    public void setSharedFarePaid(BigDecimal sharedFarePaid) { this.sharedFarePaid = sharedFarePaid; }

    public BigDecimal getIndividualSavings() { return individualSavings; }
    public void setIndividualSavings(BigDecimal individualSavings) { this.individualSavings = individualSavings; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
