package com.example.studentskillsplacementtracker.model;

import com.google.firebase.Timestamp;

/**
 * Coding practice statistics document stored in Cloud Firestore under
 * {@code students/{uid}/coding/{codingId}}.
 *
 * Statistics are self reported, one document per platform. The total is always
 * derived from the three difficulty counts and is never entered by the user.
 *
 * The no-argument constructor and the getters/setters are required by
 * Firestore's automatic de-serialization (DocumentSnapshot#toObject).
 */
public class CodingStats {

    private String codingId;
    private String userId;
    private String platform;
    private int easy;
    private int medium;
    private int hard;
    private int total;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore. Do not remove. */
    public CodingStats() {
    }

    public CodingStats(String codingId, String userId, String platform,
                       int easy, int medium, int hard) {
        this.codingId = codingId;
        this.userId = userId;
        this.platform = platform;
        this.easy = easy;
        this.medium = medium;
        this.hard = hard;
        this.total = calculateTotal(easy, medium, hard);
    }

    /**
     * The total number of solved problems is always the sum of the three
     * difficulty counts.
     */
    public static int calculateTotal(int easy, int medium, int hard) {
        return easy + medium + hard;
    }

    public String getCodingId() {
        return codingId;
    }

    public void setCodingId(String codingId) {
        this.codingId = codingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public int getEasy() {
        return easy;
    }

    public void setEasy(int easy) {
        this.easy = easy;
    }

    public int getMedium() {
        return medium;
    }

    public void setMedium(int medium) {
        this.medium = medium;
    }

    public int getHard() {
        return hard;
    }

    public void setHard(int hard) {
        this.hard = hard;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
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
