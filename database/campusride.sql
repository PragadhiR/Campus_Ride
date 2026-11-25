-- ============================================================================
-- CAMPUSRIDE DATABASE SCHEMA
-- Smart Student Ride-Pooling and Fare-Sharing System
-- Compatible with MySQL 8.0+
-- ============================================================================

DROP DATABASE IF EXISTS campusride;
CREATE DATABASE campusride CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campusride;

-- ----------------------------------------------------------------------------
-- 1. TABLE: students
-- Stores authenticated student profiles with department, year, and gender
-- ----------------------------------------------------------------------------
CREATE TABLE students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    department VARCHAR(100) NOT NULL,
    year_of_study INT NOT NULL,
    gender VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_year_of_study CHECK (year_of_study BETWEEN 1 AND 5)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 2. TABLE: destinations
-- Fixed predefined campus and city transit hotspots
-- ----------------------------------------------------------------------------
CREATE TABLE destinations (
    destination_id INT AUTO_INCREMENT PRIMARY KEY,
    destination_name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 3. TABLE: distance_matrix
-- Documented distance table between hotspot pairs in kilometers
-- ----------------------------------------------------------------------------
CREATE TABLE distance_matrix (
    from_destination_id INT NOT NULL,
    to_destination_id INT NOT NULL,
    distance_km DECIMAL(6,2) NOT NULL,
    PRIMARY KEY (from_destination_id, to_destination_id),
    FOREIGN KEY (from_destination_id) REFERENCES destinations(destination_id) ON DELETE CASCADE,
    FOREIGN KEY (to_destination_id) REFERENCES destinations(destination_id) ON DELETE CASCADE,
    CONSTRAINT chk_distance_positive CHECK (distance_km > 0)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 4. TABLE: rides
-- Ride listings created by students acting as ride creators/drivers
-- ----------------------------------------------------------------------------
CREATE TABLE rides (
    ride_id INT AUTO_INCREMENT PRIMARY KEY,
    driver_id INT NOT NULL,
    origin_id INT NOT NULL,
    destination_id INT NOT NULL,
    departure_time DATETIME NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL,
    total_fare DECIMAL(10,2) NOT NULL,
    passenger_capacity INT NOT NULL,
    available_seats INT NOT NULL,
    women_only BOOLEAN DEFAULT FALSE,
    same_department_only BOOLEAN DEFAULT FALSE,
    notes VARCHAR(255) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (driver_id) REFERENCES students(student_id) ON DELETE RESTRICT,
    FOREIGN KEY (origin_id) REFERENCES destinations(destination_id) ON DELETE RESTRICT,
    FOREIGN KEY (destination_id) REFERENCES destinations(destination_id) ON DELETE RESTRICT,
    CONSTRAINT chk_diff_origin_dest CHECK (origin_id <> destination_id),
    CONSTRAINT chk_vehicle_type CHECK (vehicle_type IN ('AUTO', 'CAB', 'BIKE')),
    CONSTRAINT chk_total_fare CHECK (total_fare > 0),
    CONSTRAINT chk_passenger_capacity CHECK (passenger_capacity >= 1),
    CONSTRAINT chk_available_seats CHECK (available_seats >= 0),
    CONSTRAINT chk_seats_within_cap CHECK (available_seats <= passenger_capacity),
    CONSTRAINT chk_ride_status CHECK (status IN ('OPEN', 'FULL', 'CANCELLED', 'COMPLETED'))
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 5. TABLE: bookings
-- Records passenger reservations made on published rides
-- ----------------------------------------------------------------------------
CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    ride_id INT NOT NULL,
    passenger_id INT NOT NULL,
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    booking_status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    fare_share_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ride_id) REFERENCES rides(ride_id) ON DELETE RESTRICT,
    FOREIGN KEY (passenger_id) REFERENCES students(student_id) ON DELETE RESTRICT,
    CONSTRAINT chk_booking_status CHECK (booking_status IN ('CONFIRMED', 'CANCELLED')),
    UNIQUE KEY uk_ride_passenger (ride_id, passenger_id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 6. TABLE: fare_records
-- Preserves completed trip cost allocations and student savings records
-- ----------------------------------------------------------------------------
CREATE TABLE fare_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    ride_id INT NOT NULL,
    student_id INT NOT NULL,
    student_role VARCHAR(20) NOT NULL,
    solo_baseline_fare DECIMAL(10,2) NOT NULL,
    shared_fare_paid DECIMAL(10,2) NOT NULL,
    individual_savings DECIMAL(10,2) NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ride_id) REFERENCES rides(ride_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT chk_role CHECK (student_role IN ('DRIVER', 'PASSENGER'))
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 7. PERFORMANCE INDEXES
-- Optimized for time-window matching, driver lookup, and passenger queries
-- ----------------------------------------------------------------------------
CREATE INDEX idx_rides_search ON rides(origin_id, destination_id, departure_time, status);
CREATE INDEX idx_rides_driver ON rides(driver_id);
CREATE INDEX idx_bookings_passenger ON bookings(passenger_id, booking_status);
CREATE INDEX idx_bookings_ride ON bookings(ride_id, booking_status);
CREATE INDEX idx_fare_records_student ON fare_records(student_id);

-- ----------------------------------------------------------------------------
-- 8. MYSQL TRIGGER: trg_validate_ride_seats_update
-- Demonstrates active database constraints and automatic state consistency:
-- - Rejects negative seats or available seats exceeding capacity
-- - Automatically changes status to FULL when available seats reach 0
-- - Automatically reopens ride to OPEN if a seat is restored and was FULL
-- ----------------------------------------------------------------------------
DELIMITER //
CREATE TRIGGER trg_validate_ride_seats_update
BEFORE UPDATE ON rides
FOR EACH ROW
BEGIN
    IF NEW.available_seats < 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Integrity violation: Available seats cannot be negative.';
    END IF;

    IF NEW.available_seats > NEW.passenger_capacity THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Integrity violation: Available seats cannot exceed passenger capacity.';
    END IF;

    -- Automatic state transitions
    IF NEW.available_seats = 0 AND NEW.status = 'OPEN' THEN
        SET NEW.status = 'FULL';
    END IF;

    IF NEW.available_seats > 0 AND NEW.status = 'FULL' THEN
        SET NEW.status = 'OPEN';
    END IF;
END //
DELIMITER ;

-- ============================================================================
-- 9. INITIAL SEED DATA
-- Predefined hotspots, distance matrix, and initial demonstration students
-- ============================================================================

-- Predefined Destinations
INSERT INTO destinations (destination_id, destination_name, category) VALUES
(1, 'College Campus', 'Campus'),
(2, 'Railway Station', 'Transit'),
(3, 'Airport', 'Transit'),
(4, 'Central Bus Stand', 'Transit'),
(5, 'Metro Station', 'Transit'),
(6, 'Shopping Mall', 'Commercial'),
(7, 'Tech Park', 'Commercial'),
(8, 'City Center', 'Civic');

-- Distance Matrix (bidirectional pairs in kilometers)
INSERT INTO distance_matrix (from_destination_id, to_destination_id, distance_km) VALUES
-- From College Campus (1)
(1, 2, 14.50), (2, 1, 14.50),
(1, 3, 34.00), (3, 1, 34.00),
(1, 4, 11.20), (4, 1, 11.20),
(1, 5, 6.80),  (5, 1, 6.80),
(1, 6, 8.50),  (6, 1, 8.50),
(1, 7, 18.00), (7, 1, 18.00),
(1, 8, 12.00), (8, 1, 12.00),

-- From Railway Station (2)
(2, 3, 28.50), (3, 2, 28.50),
(2, 4, 5.00),  (4, 2, 5.00),
(2, 5, 8.20),  (5, 2, 8.20),
(2, 6, 9.00),  (6, 2, 9.00),
(2, 7, 22.00), (7, 2, 22.00),
(2, 8, 4.50),  (8, 2, 4.50),

-- From Airport (3)
(3, 4, 30.00), (4, 3, 30.00),
(3, 5, 27.00), (5, 3, 27.00),
(3, 6, 26.50), (6, 3, 26.50),
(3, 7, 15.00), (7, 3, 15.00),
(3, 8, 28.00), (8, 3, 28.00),

-- From Central Bus Stand (4)
(4, 5, 4.50),  (5, 4, 4.50),
(4, 6, 6.00),  (6, 4, 6.00),
(4, 7, 19.50), (7, 4, 19.50),
(4, 8, 3.20),  (8, 4, 3.20),

-- From Metro Station (5)
(5, 6, 4.00),  (6, 5, 4.00),
(5, 7, 14.50), (7, 5, 14.50),
(5, 8, 6.50),  (8, 5, 6.50),

-- From Shopping Mall (6)
(6, 7, 12.00), (7, 6, 12.00),
(6, 8, 7.50),  (8, 6, 7.50),

-- From Tech Park (7)
(7, 8, 16.00), (8, 7, 16.00);

-- Initial Demo Students for quick demonstration
-- All demo passwords are: "Password@123"
-- Stored using PBKDF2WithHmacSHA256 (format: iterations:saltHex:hashHex)
-- 1000:c3a2f7189d204f128be481e5927ad91b:07d9f79836ae05663675cf39f2824cf40b8a2e1d70146311ba30c5e3f4a4788c
INSERT INTO students (student_id, full_name, email, password_hash, department, year_of_study, gender) VALUES
(1, 'Aarav Sharma', 'aarav.sharma@campus.edu', '1000:c3a2f7189d204f128be481e5927ad91b:07d9f79836ae05663675cf39f2824cf40b8a2e1d70146311ba30c5e3f4a4788c', 'Computer Science', 3, 'Male'),
(2, 'Priya Patel', 'priya.patel@campus.edu', '1000:c3a2f7189d204f128be481e5927ad91b:07d9f79836ae05663675cf39f2824cf40b8a2e1d70146311ba30c5e3f4a4788c', 'Computer Science', 3, 'Female'),
(3, 'Rohan Verma', 'rohan.verma@campus.edu', '1000:c3a2f7189d204f128be481e5927ad91b:07d9f79836ae05663675cf39f2824cf40b8a2e1d70146311ba30c5e3f4a4788c', 'Mechanical Eng', 4, 'Male'),
(4, 'Ananya Iyer', 'ananya.iyer@campus.edu', '1000:c3a2f7189d204f128be481e5927ad91b:07d9f79836ae05663675cf39f2824cf40b8a2e1d70146311ba30c5e3f4a4788c', 'Electronics', 2, 'Female');
