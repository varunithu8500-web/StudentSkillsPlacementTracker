package com.example.studentskillsplacementtracker.model;

import com.google.firebase.Timestamp;

/**
 * Student profile document stored in Cloud Firestore under
 * {@code students/{uid}}.
 *
 * The no-argument constructor and the getters/setters are required by
 * Firestore's automatic de-serialization (DocumentSnapshot#toObject).
 */
public class Student {

    /** Role assigned to every registered student. */
    public static final String ROLE_STUDENT = "student";

    /** Role that may manage company placement requirements. */
    public static final String ROLE_COORDINATOR = "coordinator";

    /** Administrative role, granted the same company management rights. */
    public static final String ROLE_ADMIN = "admin";

    private String uid;
    private String name;
    private String email;
    private String department;
    private int year;
    private double cgpa;
    private String role;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore. Do not remove. */
    public Student() {
    }

    public Student(String uid, String name, String email, String department,
                   int year, double cgpa, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.department = department;
        this.year = year;
        this.cgpa = cgpa;
        this.role = role;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
