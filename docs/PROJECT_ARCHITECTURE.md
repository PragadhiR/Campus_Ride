# CAMPUSRIDE — SYSTEM ARCHITECTURE & OOP DESIGN

This document explains the software architecture, design patterns, and Object-Oriented Programming (OOP) principles implemented in **CampusRide**.

---

## 1. Architectural Layers & Separation of Concerns

CampusRide follows an Enterprise Multi-Tier Architecture:

```
┌────────────────────────────────────────────────────────┐
│               PRESENTATION LAYER (SWING)              │
│  MainFrame · LoginPanel · RegisterPanel · Dashboard    │
│  CreateRidePanel · FindRidePanel · MyRides · Bookings  │
│  HolidayRushPanel · SavingsPanel · ProfilePanel        │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    SERVICE LAYER                       │
│  AuthService · RideService · BookingService            │
│  FareService · ReportService                           │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│              DATA ACCESS OBJECT (DAO) LAYER            │
│  StudentDAO · RideDAO · BookingDAO                     │
│  DestinationDAO · ReportDAO                            │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│              DATABASE ENGINE (MYSQL 8.0)               │
│  InnoDB Tables · Constraints · Indexes · Triggers      │
└────────────────────────────────────────────────────────┘
```

- **UI Layer (`com.campusride.ui`)**: Handles Swing rendering, event dispatching, asynchronous `SwingWorker` data fetching, and user notifications. Contains no direct SQL statements.
- **Service Layer (`com.campusride.service`)**: Enforces business logic, authorization, fare-sharing algorithms, transaction demarcation (`commit`/`rollback`), and validation.
- **DAO Layer (`com.campusride.dao`)**: Maps domain entities to JDBC `PreparedStatement` executions, manages result sets, and executes row-level locking.
- **Model Layer (`com.campusride.model`)**: Encapsulates business objects, vehicle polymorphism, and state invariants.

---

## 2. Object-Oriented Programming (OOP) Principles

### 1. Encapsulation
- **Implementation**: All entity fields in `Student`, `Ride`, `Booking`, and `Destination` are declared `private`.
- Access and mutation occur strictly through public getters, setters, and business methods.
- The `Ride.validate()` method verifies internal consistency before any database write.

### 2. Abstraction
- **Implementation**: The abstract base class `Ride.java` defines the core contract for all vehicular journeys:
  ```java
  public abstract class Ride {
      public abstract String getVehicleType();
      public abstract int getMaximumPassengerCapacity();
      public abstract BigDecimal calculateEstimatedFare(double distanceKm);
      public abstract BigDecimal calculateSoloFare(double distanceKm);
  }
  ```
- Concrete classes hide their internal mathematical formulas behind high-level abstract methods.

### 3. Inheritance
- **Implementation**: Three distinct vehicle subclasses inherit from `Ride`:
  - `AutoRide extends Ride`
  - `CabRide extends Ride`
  - `BikeRide extends Ride`
- Common state (`rideId`, `originId`, `departureTime`, `totalFare`, `availableSeats`) is declared once in the parent class and reused across subclasses.

### 4. Polymorphism
- **Dynamic Method Dispatch**: When calculating fare estimates or capacity constraints, client code invokes methods on a generic `Ride` reference:
  ```java
  Ride ride = Ride.create(vehicleType);
  BigDecimal suggestedFare = ride.calculateEstimatedFare(distanceKm);
  ```
  The Java runtime dynamically executes the specific overridden formula:
  - `AutoRide`: Base ₹40 + (Distance × ₹15/km), Max Capacity: 3
  - `CabRide`: Base ₹100 + (Distance × ₹22/km), Max Capacity: 4
  - `BikeRide`: Base ₹25 + (Distance × ₹9/km), Max Capacity: 1

### 5. Composition
- **Implementation**: Services compose DAO components:
  - `RideService` has a `RideDAO`, `DestinationDAO`, `BookingDAO`, `ReportDAO`, and `FareService`.
  - `RideMatch` composes a `Ride` object along with route distance, solo fare baseline, and relative time-match explanations.

### 6. Robust Exception Handling
- Custom checked exception hierarchy:
  - `ValidationException`: Thrown when form fields or business rules are violated.
  - `BookingException`: Thrown during booking concurrency violations, self-booking attempts, or seat exhaustion.
- Caught gracefully by Swing workers to show clear, helpful dialogs instead of crashing.

---

## 3. End-to-End Operational Workflow

1. **Authentication**:
   - Student registers with college email, department, year, gender, and password.
   - Password is salted and hashed using PBKDF2 (`PasswordHasher.java`).
2. **Ride Creation**:
   - Ride creator selects predefined hotspots (e.g. College Campus → Railway Station).
   - System calculates route distance from `distance_matrix` and displays suggested vehicle fare.
   - Creator confirms fare, capacity, and preferences (Women-only, Same-department).
   - Ride row is created with status `'OPEN'`.
3. **Smart Discovery**:
   - Passenger selects origin, destination, and target departure time.
   - SQL query locates rides on the exact route departing within `[target - 30 MIN]` to `[target + 30 MIN]`.
   - Ineligible rides (creator's own rides, full rides, restricted rides) are excluded.
4. **Concurrency-Safe Booking**:
   - JDBC transaction locks the ride row using `SELECT ... FOR UPDATE`.
   - System rechecks seat availability and inserts reservation into `bookings`.
   - Decrements `available_seats = available_seats - 1`. If 0 seats remain, ride status becomes `'FULL'`.
   - Transaction commits atomically.
5. **Dynamic Fare Sharing**:
   - Total participants = $1 \text{ (driver)} + N \text{ (passengers)}$.
   - Share per person = $\text{Total Fare} / \text{Participants}$.
   - UI recalculates live share preview on dashboards.
6. **Trip Completion & Savings Archiving**:
   - Driver marks ride `'COMPLETED'`.
   - System writes immutable cost allocations to `fare_records`.
   - Student Savings dashboard updates with genuine savings: $\text{Solo Baseline} - \text{Shared Fare Paid}$.
