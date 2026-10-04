package com.example.studentskillsplacementtracker.model;

import com.google.firebase.Timestamp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Project document stored in Cloud Firestore under
 * {@code students/{uid}/projects/{projectId}}.
 *
 * The no-argument constructor and the getters/setters are required by
 * Firestore's automatic de-serialization (DocumentSnapshot#toObject).
 */
public class Project {

    /** Allowed values for {@link #status}. */
    public static final String STATUS_PLANNED = "Planned";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_COMPLETED = "Completed";

    /** The complete set of accepted status values, in display order. */
    private static final List<String> ALLOWED_STATUSES = Collections.unmodifiableList(
            Arrays.asList(STATUS_PLANNED, STATUS_IN_PROGRESS, STATUS_COMPLETED)
    );

    private String projectId;
    private String userId;
    private String projectName;
    private String description;
    private List<String> technologies;
    private String status;
    private String repositoryLink;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore. Do not remove. */
    public Project() {
    }

    public Project(String projectId, String userId, String projectName, String description,
                   List<String> technologies, String status, String repositoryLink) {
        this.projectId = projectId;
        this.userId = userId;
        this.projectName = projectName;
        this.description = description;
        this.technologies = technologies;
        this.status = status;
        this.repositoryLink = repositoryLink;
    }

    /**
     * Returns {@code true} when {@code status} matches one of the allowed
     * values, ignoring case and surrounding whitespace.
     */
    public static boolean isStatusAllowed(String status) {
        return canonicalStatus(status) != null
                && containsIgnoreCase(ALLOWED_STATUSES, canonicalStatus(status));
    }

    /**
     * Returns the canonical spelling of an allowed status (for example
     * {@code "in progress"} becomes {@code "In Progress"}), or {@code null}
     * when {@code status} is {@code null}. Unrecognised values are returned
     * trimmed so callers can validate them.
     */
    public static String canonicalStatus(String status) {

        if (status == null) {
            return null;
        }

        String trimmed = status.trim();

        for (String allowed : ALLOWED_STATUSES) {
            if (allowed.equalsIgnoreCase(trimmed)) {
                return allowed;
            }
        }

        return trimmed;
    }

    private static boolean containsIgnoreCase(List<String> values, String value) {
        for (String candidate : values) {
            if (candidate.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRepositoryLink() {
        return repositoryLink;
    }

    public void setRepositoryLink(String repositoryLink) {
        this.repositoryLink = repositoryLink;
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
