# CAMPUSRIDE — DBMS FEATURES & FACULTY VIVA GUIDE

This document details the Database Management System (DBMS) concepts, relational schema design, SQL constructs, transaction isolation mechanics, and trigger logic implemented in **CampusRide**.

---

## 1. Schema Architecture & Relational Integrity

### Entity-Relationship Summary
- **`students`**: Central identity table storing authenticated student profiles.
- **`destinations`**: Master lookup table of predefined campus hotspots and transit hubs.
- **`distance_matrix`**: Documented distance pairs (in km) connecting hotspots.
- **`rides`**: Ride listings published by students acting as drivers.
- **`bookings`**: Passenger reservations on published rides.
- **`fare_records`**: Immutable historical audit of completed trip fare shares and student savings.

---

## 2. Core DBMS Concepts Demonstrated

### 1. Primary Keys
- **Implementation**: Every table features a surrogate primary key with `AUTO_INCREMENT`:
  - `students(student_id)`
  - `destinations(destination_id)`
  - `rides(ride_id)`
  - `bookings(booking_id)`
  - `fare_records(record_id)`
  - Composite Primary Key on `distance_matrix(from_destination_id, to_destination_id)`.
- **Why it matters**: Guarantees entity integrity and uniquely identifies each tuple for fast indexed row lookup.

### 2. Foreign Keys & Referential Integrity
- **Implementation**:
  - `rides.driver_id -> students.student_id (ON DELETE RESTRICT)`
  - `rides.origin_id -> destinations.destination_id (ON DELETE RESTRICT)`
  - `rides.destination_id -> destinations.destination_id (ON DELETE RESTRICT)`
  - `bookings.ride_id -> rides.ride_id (ON DELETE RESTRICT)`
  - `bookings.passenger_id -> students.student_id (ON DELETE RESTRICT)`
  - `fare_records.ride_id -> rides.ride_id (ON DELETE CASCADE)`
  - `fare_records.student_id -> students.student_id (ON DELETE CASCADE)`
- **Why it matters**: Prevents orphaned records. A student or destination cannot be deleted while active rides or bookings depend on them.

### 3. Unique Constraints
- **Implementation**:
  - `students.email`: Prevents duplicate student registrations.
  - `destinations.destination_name`: Ensures hotspot names remain canonical.
  - `bookings(ride_id, passenger_id)` (`uk_ride_passenger`): Enforces at the database level that a student cannot book multiple concurrent active seats on the same ride.

### 4. Check Constraints (`CHECK`)
- **Implementation**:
  - `students`: `CONSTRAINT chk_year_of_study CHECK (year_of_study BETWEEN 1 AND 5)`
  - `rides`:
    - `CONSTRAINT chk_diff_origin_dest CHECK (origin_id <> destination_id)`
    - `CONSTRAINT chk_total_fare CHECK (total_fare > 0)`
    - `CONSTRAINT chk_passenger_capacity CHECK (passenger_capacity >= 1)`
    - `CONSTRAINT chk_available_seats CHECK (available_seats >= 0)`
    - `CONSTRAINT chk_seats_within_cap CHECK (available_seats <= passenger_capacity)`
    - `CONSTRAINT chk_vehicle_type CHECK (vehicle_type IN ('AUTO', 'CAB', 'BIKE'))`
    - `CONSTRAINT chk_ride_status CHECK (status IN ('OPEN', 'FULL', 'CANCELLED', 'COMPLETED'))`
  - `bookings`: `CONSTRAINT chk_booking_status CHECK (booking_status IN ('CONFIRMED', 'CANCELLED'))`
- **Why it matters**: Domain integrity is strictly enforced inside the database engine regardless of which client connects.

### 5. Indexing & Query Optimization
- **Implementation**:
  - `CREATE INDEX idx_rides_search ON rides(origin_id, destination_id, departure_time, status);`
  - `CREATE INDEX idx_rides_driver ON rides(driver_id);`
  - `CREATE INDEX idx_bookings_passenger ON bookings(passenger_id, booking_status);`
  - `CREATE INDEX idx_bookings_ride ON bookings(ride_id, booking_status);`
  - `CREATE INDEX idx_fare_records_student ON fare_records(student_id);`
- **Why it matters**: Transforms $O(N)$ full table scans into $O(\log N)$ B-Tree index lookups for the smart time-window search query and dashboard aggregations.

---

## 3. Advanced Transactions & Concurrency Handling

### ACID Transaction Semantics
During ride booking, cancellation, and trip completion, atomicity and consistency are paramount:

```java
try (Connection conn = DBConnection.getConnection()) {
    conn.setAutoCommit(false); // Begin Transaction
    try {
        // 1. Pessimistic Row Lock
        Optional<Ride> locked = rideDAO.lockRideForUpdate(conn, rideId);
        
        // 2. Concurrency checks (Status, Available Seats > 0)
        ...
        
        // 3. Insert or Reactivate Booking
        bookingDAO.create(conn, booking);
        
        // 4. Update available seats and adjust status
        rideDAO.updateSeatsAndStatus(conn, rideId, newSeats, newStatus);
        
        conn.commit(); // Atomic Commit
    } catch (Exception e) {
        conn.rollback(); // Rollback on error
        throw e;
    }
}
```

### Pessimistic Concurrency Control (`SELECT ... FOR UPDATE`)
- **Problem**: If two students search for a ride with 1 remaining seat simultaneously, a naive check-then-act implementation in Java would allow both to book, resulting in negative available seats (overbooking race condition).
- **Solution**: CampusRide executes `SELECT * FROM rides WHERE ride_id = ? FOR UPDATE`. MySQL's InnoDB engine places an exclusive row-level lock on the ride tuple. The second transaction must wait until the first commits or rolls back, at which point it reads `available_seats = 0` and is cleanly rejected with an informative message.

---

## 4. Active Database Trigger (`BEFORE UPDATE`)

### Trigger Definition
```sql
DELIMITER //
CREATE TRIGGER trg_validate_ride_seats_update
BEFORE UPDATE ON rides
FOR EACH ROW
BEGIN
    -- 1. Invariant: Available seats cannot drop below 0
    IF NEW.available_seats < 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Integrity violation: Available seats cannot be negative.';
    END IF;

    -- 2. Invariant: Available seats cannot exceed vehicle passenger capacity
    IF NEW.available_seats > NEW.passenger_capacity THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Integrity violation: Available seats cannot exceed passenger capacity.';
    END IF;

    -- 3. Automatic State Transition to FULL
    IF NEW.available_seats = 0 AND NEW.status = 'OPEN' THEN
        SET NEW.status = 'FULL';
    END IF;

    -- 4. Automatic State Reopening if a seat is restored
    IF NEW.available_seats > 0 AND NEW.status = 'FULL' THEN
        SET NEW.status = 'OPEN';
    END IF;
END //
DELIMITER ;
```

- **Why it matters**: Guarantees that even if external queries or faulty code attempt an invalid update, the DBMS engine itself halts the operation via `SIGNAL SQLSTATE '45000'`.

---

## 5. SQL Queries for Faculty Demonstration

### 1. Smart 30-Minute Time-Window Matching Query
```sql
SELECT r.*,
       orig.destination_name AS origin_name,
       dest.destination_name AS destination_name,
       drv.full_name AS driver_name,
       drv.department AS driver_department,
       drv.gender AS driver_gender,
       (SELECT COUNT(*) FROM bookings b WHERE b.ride_id = r.ride_id AND b.booking_status = 'CONFIRMED') AS confirmed_passengers
FROM rides r
JOIN destinations orig ON r.origin_id = orig.destination_id
JOIN destinations dest ON r.destination_id = dest.destination_id
JOIN students drv ON r.driver_id = drv.student_id
WHERE r.origin_id = ?
  AND r.destination_id = ?
  AND r.departure_time BETWEEN DATE_SUB(?, INTERVAL 30 MINUTE) AND DATE_ADD(?, INTERVAL 30 MINUTE)
  AND r.status = 'OPEN'
  AND r.available_seats > 0
  AND r.driver_id <> ?
ORDER BY ABS(TIMESTAMPDIFF(MINUTE, r.departure_time, ?)) ASC;
```

### 2. Holiday Rush: Popular Destinations (`COUNT`, `GROUP BY`, `ORDER BY`)
```sql
SELECT d.destination_id, d.destination_name, d.category,
       COUNT(r.ride_id) AS total_rides,
       COALESCE(SUM(CASE WHEN r.status = 'OPEN' THEN r.available_seats ELSE 0 END), 0) AS open_seats,
       COALESCE(SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_rides
FROM destinations d
LEFT JOIN rides r ON d.destination_id = r.destination_id AND r.status IN ('OPEN', 'FULL', 'COMPLETED')
GROUP BY d.destination_id, d.destination_name, d.category
ORDER BY total_rides DESC, open_seats DESC;
```

### 3. Holiday Rush: Peak Departure Time Windows (`HOUR`, `COUNT`)
```sql
SELECT HOUR(departure_time) AS dep_hour, COUNT(*) AS ride_count
FROM rides
WHERE status IN ('OPEN', 'FULL', 'COMPLETED')
GROUP BY dep_hour
ORDER BY ride_count DESC;
```

### 4. Vehicle Breakdown & Average Fares (`AVG`, `COUNT`)
```sql
SELECT vehicle_type, COUNT(*) AS ride_count, AVG(total_fare) AS avg_fare
FROM rides
WHERE status IN ('OPEN', 'FULL', 'COMPLETED')
GROUP BY vehicle_type
ORDER BY ride_count DESC;
```

### 5. Individual Student Savings Aggregation
```sql
SELECT COUNT(*) AS total_trips,
       COALESCE(SUM(individual_savings), 0) AS total_savings,
       COALESCE(AVG(shared_fare_paid), 0) AS avg_paid
FROM fare_records
WHERE student_id = ?;
```
