# CAMPUSRIDE — COMPREHENSIVE TEST SPECIFICATION & VERIFICATION

This test suite details manual and automated verification procedures covering all 27 critical validation and workflow scenarios for the **CampusRide** system.

---

## Summary Matrix of 27 Core Test Scenarios

| # | Test Scenario | Preconditions / Input | Expected Result | Status |
|---|---|---|---|---|
| **TC-01** | Student Registration | New email, password ≥ 6 chars, valid year 1-5 | Account created; hashed password stored in MySQL; auto-assigned student ID | **PASS** |
| **TC-02** | Duplicate Email Rejection | Attempt to register with existing email | Form rejected with: "An account with this email address already exists" | **PASS** |
| **TC-03** | Login with Correct Credentials | Valid registered email and password | Authentication succeeds; MainFrame loads Dashboard with student profile | **PASS** |
| **TC-04** | Login with Incorrect Credentials | Correct email, wrong password | Login rejected with: "Invalid email or password" | **PASS** |
| **TC-05** | Creating a Valid Ride | Future date, diff origin/dest, fare > 0, capacity ≤ vehicle max | Ride saved in `rides` table with status 'OPEN'; seats = capacity | **PASS** |
| **TC-06** | Rejecting Invalid Fares | Fare = ₹0 or negative | Form rejected with: "Total estimated fare must be greater than ₹0" | **PASS** |
| **TC-07** | Rejecting Identical Origin/Destination | Origin = Destination (e.g. Campus to Campus) | Rejected with: "Origin and destination must be different locations" | **PASS** |
| **TC-08** | Rejecting Past Departure Times | Date/time set before `LocalDateTime.now()` | Rejected with: "Departure date and time must be in the future" | **PASS** |
| **TC-09** | Finding a Matching Ride | Same route, departure within ±30 min window | Ride returned in search results table with dynamic match reason | **PASS** |
| **TC-10** | Matching at Exact 30-Min Boundary | Departure exactly 30 mins before or after target | Ride returned in search results; labeled "30 mins earlier" or "30 mins later" | **PASS** |
| **TC-11** | Excluding Rides Outside Time Window | Departure 31+ mins before or after target | Ride filtered out of results | **PASS** |
| **TC-12** | Preventing Self-Booking | Ride creator attempts to join own ride | Rejected with: "Self-booking not allowed: You cannot join a ride you created" | **PASS** |
| **TC-13** | Successful Booking | Eligible student clicks Join on OPEN ride | Booking row inserted; available seats decremented by 1; success dialog | **PASS** |
| **TC-14** | Duplicate Booking Prevention | Student attempts to join the same ride twice | Rejected with: "You have already booked a seat on this ride" | **PASS** |
| **TC-15** | Full Ride Rejection | Student attempts to join ride when available seats = 0 | Rejected with: "This ride is already full. No seats are remaining" | **PASS** |
| **TC-16** | Correct Seat-Count Updates | 4-capacity cab receives 1 booking | Available seats decrements to 3; triggers no error | **PASS** |
| **TC-17** | Correct Fare-Share Calculation | ₹600 total fare, 1 driver + 2 passengers = 3 | Each participant's share is exactly ₹200.00 (`600 / 3`) | **PASS** |
| **TC-18** | Cancelling a Booking | Passenger cancels confirmed booking | Booking status becomes 'CANCELLED'; ride seats increment by 1 | **PASS** |
| **TC-19** | Restoring Seat After Cancellation | Passenger cancels on a FULL ride | Available seats becomes 1; status automatically reopens to 'OPEN' | **PASS** |
| **TC-20** | Cancelling a Ride | Creator cancels ride | Ride status becomes 'CANCELLED'; all confirmed bookings set to 'CANCELLED' | **PASS** |
| **TC-21** | Women-Only Restriction | Male student searches/attempts to join Women-Only ride | Ride excluded from search or rejected with: "Restriction: Designated for women students only" | **PASS** |
| **TC-22** | Same-Department Restriction | Student from Mech attempts to join CS-only ride | Rejected with: "Restriction: This ride is reserved for Computer Science students only" | **PASS** |
| **TC-23** | Completing a Ride | Creator marks OPEN/FULL ride as completed | Status becomes 'COMPLETED'; trip fares written to `fare_records` | **PASS** |
| **TC-24** | Accurate Savings Calculation | Completed trip: Solo baseline ₹430, shared fare ₹143.33 | Student individual savings recorded as ₹286.67; dashboard updates | **PASS** |
| **TC-25** | Holiday Rush Reports | View Holiday Rush Hub | Destinations ranked by ride count; peak hours and vehicle breakdown rendered | **PASS** |
| **TC-26** | Database Connection Failure Handling | MySQL service stopped or wrong port | UI displays friendly setup dialog; prevents unhandled application crashes | **PASS** |
| **TC-27** | Concurrent Attempts for Final Seat | Two sessions attempt booking 1 remaining seat | `SELECT ... FOR UPDATE` locks row; 1 succeeds, 2nd receives friendly 'Ride is full' message | **PASS** |

---

## Automated JUnit 5 Test Execution Details

Automated unit tests are implemented in `src/test/java/com/campusride/`:
- `FareServiceTest`: Validates 2-participant and 3-participant fare splitting, non-negative savings bounds, and zero-participant edge cases.
- `VehicleModelTest`: Validates polymorphism, capacity limits, and rate formulas for `AutoRide`, `CabRide`, and `BikeRide`.
- `PasswordHasherTest`: Validates PBKDF2 hash generation, constant-time slow equals, verification, and salt uniqueness.
- `TimeWindowMatchTest`: Validates exact time match, ±20 min earlier/later, exact 30-min boundary inclusion, and 31+ min exclusion.
