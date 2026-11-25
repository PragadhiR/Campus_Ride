package com.campusride.model;

import java.time.LocalDateTime;

/**
 * Represents a registered student in the CampusRide system.
 */
public class Student {
    private int studentId;
    private String fullName;
    private String email;
    private String passwordHash;
    private String department;
    private int yearOfStudy;
    private String gender;
    private LocalDateTime createdAt;

    public Student() {}

    public Student(int studentId, String fullName, String email, String passwordHash,
                   String department, int yearOfStudy, String gender, LocalDateTime createdAt) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.department = department;
        this.yearOfStudy = yearOfStudy;
        this.gender = gender;
        this.createdAt = createdAt;
    }

    public Student(String fullName, String email, String passwordHash,
                   String department, int yearOfStudy, String gender) {
        this(0, fullName, email, passwordHash, department, yearOfStudy, gender, LocalDateTime.now());
    }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isFemale() {
        return "Female".equalsIgnoreCase(gender);
    }

    @Override
    public String toString() {
        return fullName + " (" + department + ", Year " + yearOfStudy + ")";
    }
}
