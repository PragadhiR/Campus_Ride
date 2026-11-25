package com.campusride.service;

import com.campusride.dao.BookingDAO;
import com.campusride.dao.ReportDAO;
import com.campusride.dao.RideDAO;
import com.campusride.model.Booking;
import com.campusride.model.Ride;
import com.campusride.model.Student;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Service for Holiday Rush analytics, Student Dashboard metrics, and Savings reports.
 */
public class ReportService {
    private final ReportDAO reportDAO;
    private final RideDAO rideDAO;
    private final BookingDAO bookingDAO;

    public record DashboardStats(
        int ridesCreatedCount,
        int activeBookingsCount,
        int completedTripsCount,
        BigDecimal totalSavings,
        List<Ride> recentCreatedRides,
        List<Booking> recentJoinedRides
    ) {}

    public ReportService() {
        this.reportDAO = new ReportDAO();
        this.rideDAO = new RideDAO();
        this.bookingDAO = new BookingDAO();
    }

    public ReportService(ReportDAO reportDAO, RideDAO rideDAO, BookingDAO bookingDAO) {
        this.reportDAO = reportDAO;
        this.rideDAO = rideDAO;
        this.bookingDAO = bookingDAO;
    }

    public DashboardStats getDashboardStats(Student student) throws SQLException {
        if (student == null) {
            return new DashboardStats(0, 0, 0, BigDecimal.ZERO, List.of(), List.of());
        }

        List<Ride> myRides = rideDAO.findByDriverId(student.getStudentId());
        List<Booking> myBookings = bookingDAO.findByPassengerId(student.getStudentId());
        ReportDAO.StudentSavingsSummary savings = reportDAO.getStudentSavingsSummary(student.getStudentId());

        int createdCount = myRides.size();
        int activeBookings = (int) myBookings.stream().filter(Booking::isConfirmed).count();
        int completedCount = savings.completedRidesCount();
        BigDecimal totalSavings = savings.totalSavings();

        List<Ride> recentCreated = myRides.stream().limit(5).toList();
        List<Booking> recentBooked = myBookings.stream().limit(5).toList();

        return new DashboardStats(
            createdCount,
            activeBookings,
            completedCount,
            totalSavings,
            recentCreated,
            recentBooked
        );
    }

    public List<ReportDAO.PopularDestinationStat> getHolidayRushDestinations() throws SQLException {
        return reportDAO.getPopularDestinations();
    }

    public List<ReportDAO.TimeSlotStat> getHolidayRushTimeSlots() throws SQLException {
        return reportDAO.getBusiestTimeSlots();
    }

    public List<ReportDAO.VehicleBreakdownStat> getHolidayRushVehicleBreakdown() throws SQLException {
        return reportDAO.getVehicleBreakdown();
    }

    public ReportDAO.StudentSavingsSummary getStudentSavings(int studentId) throws SQLException {
        return reportDAO.getStudentSavingsSummary(studentId);
    }
}
