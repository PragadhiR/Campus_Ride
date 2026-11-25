package com.campusride.service;

import com.campusride.dao.StudentDAO;
import com.campusride.model.Student;
import com.campusride.util.PasswordHasher;
import com.campusride.util.ValidationException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Handles student authentication, registration, session state, and security validation.
 */
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    private final StudentDAO studentDAO;
    private static Student currentStudent = null;

    public AuthService() {
        this.studentDAO = new StudentDAO();
    }

    public AuthService(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    /**
     * Registers a new student after validating inputs, email format, and unique constraint.
     */
    public Student register(String fullName, String email, String password,
                             String department, int yearOfStudy, String gender)
            throws ValidationException, SQLException {

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full name is required.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email address is required.");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new ValidationException("Invalid email format. Please provide a valid email (e.g. name@campus.edu).");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new ValidationException("Department is required.");
        }
        if (yearOfStudy < 1 || yearOfStudy > 5) {
            throw new ValidationException("Year of study must be between 1 and 5.");
        }
        if (gender == null || gender.trim().isEmpty()) {
            gender = "Prefer not to say";
        }

        // Prevent duplicate registration
        if (studentDAO.emailExists(cleanEmail)) {
            throw new ValidationException("An account with this email address already exists. Please login instead.");
        }

        // Hash password securely using PBKDF2
        String passwordHash = PasswordHasher.hashPassword(password);

        Student student = new Student(fullName.trim(), cleanEmail, passwordHash,
                department.trim(), yearOfStudy, gender.trim());

        return studentDAO.create(student);
    }

    /**
     * Authenticates student credentials and establishes current session.
     */
    public Student login(String email, String password) throws ValidationException, SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email is required.");
        }
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required.");
        }

        Optional<Student> studentOpt = studentDAO.findByEmail(email.trim().toLowerCase());
        if (studentOpt.isEmpty()) {
            throw new ValidationException("Invalid email or password.");
        }

        Student student = studentOpt.get();
        boolean verified = PasswordHasher.verifyPassword(password, student.getPasswordHash());
        if (!verified) {
            throw new ValidationException("Invalid email or password.");
        }

        currentStudent = student;
        return currentStudent;
    }

    public static Student getCurrentStudent() {
        return currentStudent;
    }

    public static void setCurrentStudent(Student s) {
        currentStudent = s;
    }

    public static boolean isLoggedIn() {
        return currentStudent != null;
    }

    public static void logout() {
        currentStudent = null;
    }
}
