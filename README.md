# CAMPUSRIDE
## Smart Student Ride-Pooling and Fare-Sharing System

**TAGLINE:** *"Share the Ride. Split the Fare. Save More."*

CampusRide is a centralized student ride-pooling and cost-sharing desktop application built specifically for college campuses. It allows students to publish upcoming trips, discover other students travelling along the same route within a 30-minute window, reserve available passenger seats through transaction-safe bookings, split travel expenses, and track cumulative individual savings.

---

## 🌟 Key System Capabilities

- **College Authentication**: Secure student sign-up and login with PBKDF2 password hashing (HMAC-SHA256 with 16-byte cryptographically secure salt).
- **Vehicle Polymorphism (OOP)**: Real inheritance hierarchy with abstract base class `Ride` and subclasses `AutoRide`, `CabRide`, and `BikeRide`, each with distinct capacity limits, base charges, and per-km rate formulas.
- **Predefined Campus Hotspots**: Curated transit destinations (College Campus, Railway Station, Airport, Central Bus Stand, Metro Station, Shopping Mall, Tech Park, City Center) with a documented distance matrix.
- **Smart 30-Minute Time-Window Matching**: Matches rides along the same route departing within $\pm 30$ minutes of the requested travel time and displays relative match context (e.g. *"Same route · 15 mins earlier · 2 seats left"*).
- **Pessimistic Concurrency Booking**: Transactional reservations using JDBC `setAutoCommit(false)` and `SELECT ... FOR UPDATE` row locks to eliminate overbooking race conditions for the final seat.
- **Dynamic Fare Sharing**: 
  $$\text{Fare Share Per Person} = \frac{\text{Total Estimated Fare}}{1 \text{ (Driver)} + \text{Confirmed Passengers}}$$
- **Active Database Trigger**: MySQL 8 trigger `trg_validate_ride_seats_update` rejects negative seats and enforces state transitions between `OPEN` and `FULL`.
- **Holiday Rush Analytics**: Real-time SQL aggregations using `COUNT()`, `GROUP BY`, and `ORDER BY` to identify busiest transit destinations and peak departure windows.
- **Personal Savings Dashboard**: Compares shared trip costs against baseline solo travel fares for verified completed journeys.
- **Dark Desktop Interface**: Desktop UI built with Java Swing and custom dark aesthetic components (`#0F172A`, `#1E293B`, `#0EA5E9`).

---

## 🛠 Technology Stack

- **Language**: Java 21 (Eclipse Adoptium OpenJDK 21)
- **GUI Framework**: Java Swing + Java AWT (CardLayout, GridBagLayout, custom dark renderers)
- **Database Engine**: MySQL 8.0 Server (InnoDB)
- **Database Connectivity**: JDBC (`mysql-connector-j` 8.3.0) via Maven
- **Build & Dependency Management**: Apache Maven 3.10.0
- **Testing**: JUnit 5 (Jupiter API & Engine)

---

## 📂 Project Directory Structure

```text
CampusRide/
├── pom.xml                               # Maven build & packaging configuration
├── README.md                             # Setup guide and walkthrough
├── .gitignore                            # Excludes targets and secrets
├── .env.example                          # Database configuration template
├── database/
│   └── campusride.sql                    # Full MySQL schema, constraints, trigger, seed data
├── docs/
│   ├── DBMS_FEATURES.md                  # Comprehensive DBMS viva & query guide
│   ├── PROJECT_ARCHITECTURE.md           # Multi-tier architecture and OOP details
│   └── TEST_CASES.md                     # 27 verified test case specifications
└── src/
    ├── main/java/com/campusride/
    │   ├── Main.java                     # Application entry point
    │   ├── config/AppConfig.java         # .env and environment credential loader
    │   ├── db/DBConnection.java          # JDBC connection factory & connection validation
    │   ├── model/                        # Domain models (Ride, AutoRide, CabRide, BikeRide, ...)
    │   ├── dao/                          # JDBC PreparedStatement DAOs (Student, Ride, Booking, ...)
    │   ├── service/                      # Business & transaction services (Auth, Ride, Booking, ...)
    │   ├── ui/                           # Swing panels (Dashboard, CreateRide, FindRide, ...)
    │   └── util/                         # UITheme, PasswordHasher, DateUtil, Exceptions
    └── test/java/com/campusride/         # JUnit 5 automated unit tests
```

---

## 🚀 Setup & Execution Guide

### 1. Database Setup in MySQL Workbench
1. Launch **MySQL Workbench**.
2. Connect to your local MySQL 8.0 instance.
3. Open `database/campusride.sql` from this project folder.
4. Execute the entire script. It will:
   - Create the `campusride` database.
   - Set up tables: `students`, `destinations`, `distance_matrix`, `rides`, `bookings`, `fare_records`.
   - Create performance indexes and the `trg_validate_ride_seats_update` trigger.
   - Insert predefined campus hotspots, distance matrix pairs, and sample student accounts.

### 2. Configure Database Credentials
Create a `.env` file in the project root (or copy `.env.example` to `.env`):
```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=campusride
DB_USER=root
DB_PASSWORD=your_mysql_password
```
*(Alternatively, if MySQL uses different credentials, CampusRide provides a built-in **"Database Settings"** dialog inside the application to test and save credentials at runtime).*

### 3. Build the Project
Open a PowerShell terminal in the project directory:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;C:\Program Files\Apache\Maven\apache-maven-3.10.0\bin;$env:Path"

# Compile and run automated unit tests
mvn clean test

# Build executable fat JAR
mvn clean package
```

### 4. Launch CampusRide
Run the compiled application:

```powershell
java -jar target/campusride-1.0.0.jar
```

---

## 🎬 End-to-End Demonstration Walkthrough

CampusRide includes pre-seeded demo accounts with password `Password@123`:
- **Student A**: `aarav.sharma@campus.edu` (Male, Computer Science, Year 3)
- **Student B**: `priya.patel@campus.edu` (Female, Computer Science, Year 3)

### Step-by-Step Flow:
1. **Launch CampusRide**: Click the demo shortcut **"Student A (Aarav)"** on the login screen and click **Log In**.
2. **Create a Ride**:
   - Navigate to **"Create a Ride"**.
   - Select Origin: `College Campus`, Destination: `Railway Station`.
   - Set departure date to tomorrow, time to `5:00 PM`.
   - Select Vehicle: `Cab (4-Wheeler Taxi)` (Capacity: 4 seats).
   - Notice the suggested fare breakdown calculated from the 14.5 km distance matrix: ₹419.00. Set Total Fare to ₹600.00.
   - Click **"Publish Ride Listing"**. The ride is published with status `OPEN`.
3. **Log Out**: Open **Profile** or click **"Log Out"** in the sidebar.
4. **Log In as Student B**: Click the demo shortcut **"Student B (Priya)"** and log in.
5. **Search with Smart Time-Window Matching**:
   - Navigate to **"Find a Ride"**.
   - Select Origin: `College Campus`, Destination: `Railway Station`.
   - Select the same departure date, target time `5:15 PM` (departing within 15 minutes of Student A's ride).
   - Click **"Search Matching Rides"**.
   - Student A's ride appears with match explanation: *"Same route · 15 mins earlier · 4 seat(s) available"*.
6. **Preview Live Fare Split & Join Ride**:
   - Click **"View Details & Join Ride"**.
   - Note the smart preview: 2 participants (1 driver + 1 passenger), your share: ₹300.00 (`₹600 / 2`), solo baseline: ₹419.00, your savings: ₹119.00.
   - Click **"Join Ride & Split Fare"**. The transaction locks the row, updates available seats to 3, and confirms your booking.
7. **Verify Passenger Booking**:
   - Open **"My Bookings"** to confirm the reservation is listed with status `CONFIRMED`.
8. **Switch back to Student A**:
   - Log in as Student A and navigate to **"My Rides"**.
   - Click **"View Passengers"** on Ride #1. Student B (`Priya Patel`) is listed in the passenger roster.
9. **Complete the Trip**:
   - Click **"Mark Trip Completed"**. The application atomically updates the ride to `COMPLETED` and writes immutable savings snapshots to `fare_records`.
10. **Verify Travel Savings & Analytics**:
   - Navigate to **"Savings Dashboard"** to view verified completed journeys and money saved.
   - Navigate to **"Holiday Rush Hub"** to view real DBMS destination rankings and peak rush hours.
