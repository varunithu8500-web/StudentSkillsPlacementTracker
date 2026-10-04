package com.example.studentskillsplacementtracker.model;

import com.google.firebase.Timestamp;

/**
 * Skill document stored in Cloud Firestore under
 * {@code students/{uid}/skills/{skillId}}.
 *
 * The no-argument constructor and the getters/setters are required by
 * Firestore's automatic de-serialization (DocumentSnapshot#toObject).
 */
public class Skill {

    /** Inclusive bounds for {@link #proficiency}. */
    public static final int PROFICIENCY_MIN = 1;
    public static final int PROFICIENCY_MAX = 5;

    /** Inclusive bounds for {@link #progress}. */
    public static final int PROGRESS_MIN = 0;
    public static final int PROGRESS_MAX = 100;

    private String skillId;
    private String userId;
    private String skillName;
    private int proficiency;
    private int progress;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore. Do not remove. */
    public Skill() {
    }

    public Skill(String skillId, String userId, String skillName,
                 int proficiency, int progress) {
        this.skillId = skillId;
        this.userId = userId;
        this.skillName = skillName;
        this.proficiency = proficiency;
        this.progress = progress;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public int getProficiency() {
        return proficiency;
    }

    public void setProficiency(int proficiency) {
        this.proficiency = proficiency;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
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
