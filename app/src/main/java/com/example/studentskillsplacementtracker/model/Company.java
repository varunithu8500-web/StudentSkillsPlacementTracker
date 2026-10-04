package com.example.studentskillsplacementtracker.model;

import com.google.firebase.Timestamp;

import java.util.List;

/**
 * Company / placement requirement document stored in Cloud Firestore under
 * {@code companies/{companyId}}.
 *
 * Companies are shared placement data managed by coordinators, so this is the
 * only collection that is not scoped to a student uid.
 *
 * NOTE: {@link #role} is the JOB role being offered (for example
 * "Software Engineer"). It is not the Firebase user role held by the profile
 * document.
 *
 * {@link #minimumCGPA} keeps the document field spelling on purpose so that
 * Firestore's automatic de-serialization (DocumentSnapshot#toObject) maps it
 * without any extra annotation.
 *
 * The no-argument constructor and the getters/setters are required by
 * Firestore.
 */
public class Company {

    /** Inclusive bounds for {@link #minimumCGPA}. */
    public static final double MIN_CGPA_LOWER_BOUND = 0.0;
    public static final double MIN_CGPA_UPPER_BOUND = 10.0;

    /** Inclusive lower bound for {@link #minimumProjects}. */
    public static final int MIN_PROJECTS_LOWER_BOUND = 0;

    private String companyId;
    private String companyName;
    private String role;
    private double minimumCGPA;
    private List<String> requiredSkills;
    private int minimumProjects;
    private String location;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore. Do not remove. */
    public Company() {
    }

    public Company(String companyId, String companyName, String role, double minimumCGPA,
                   List<String> requiredSkills, int minimumProjects, String location) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.role = role;
        this.minimumCGPA = minimumCGPA;
        this.requiredSkills = requiredSkills;
        this.minimumProjects = minimumProjects;
        this.location = location;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getMinimumCGPA() {
        return minimumCGPA;
    }

    public void setMinimumCGPA(double minimumCGPA) {
        this.minimumCGPA = minimumCGPA;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public int getMinimumProjects() {
        return minimumProjects;
    }

    public void setMinimumProjects(int minimumProjects) {
        this.minimumProjects = minimumProjects;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
