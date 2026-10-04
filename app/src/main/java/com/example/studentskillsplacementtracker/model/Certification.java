package com.example.studentskillsplacementtracker.model;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Certification document stored in Cloud Firestore under
 * {@code students/{uid}/certifications/{certificationId}}.
 *
 * The no-argument constructor and the getters/setters are required by
 * Firestore's automatic de-serialization (DocumentSnapshot#toObject).
 */
public class Certification {

    /** Display pattern for {@link #date}, for example {@code 04 Oct 2026}. */
    public static final String DATE_PATTERN = "dd MMM yyyy";

    private String certificationId;
    private String userId;
    private String certificationName;
    private String organization;
    private Timestamp date;
    private String credential;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore. Do not remove. */
    public Certification() {
    }

    public Certification(String certificationId, String userId, String certificationName,
                         String organization, Timestamp date, String credential) {
        this.certificationId = certificationId;
        this.userId = userId;
        this.certificationName = certificationName;
        this.organization = organization;
        this.date = date;
        this.credential = credential;
    }

    /**
     * Builds the {@link Timestamp} stored for a picked calendar date, using
     * midnight in the device's local time zone.
     *
     * {@code monthOfYear} is zero based, matching both {@code DatePickerDialog}
     * and {@code Calendar}.
     */
    public static Timestamp toTimestamp(int year, int monthOfYear, int dayOfMonth) {

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, monthOfYear, dayOfMonth, 0, 0, 0);

        return new Timestamp(calendar.getTime());
    }

    /**
     * Renders {@code timestamp} as {@code dd MMM yyyy}, or an empty string when
     * the timestamp is {@code null}.
     */
    public static String formatTimestamp(Timestamp timestamp) {

        if (timestamp == null) {
            return "";
        }

        Date date = timestamp.toDate();
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());

        return formatter.format(date);
    }

    public String getCertificationId() {
        return certificationId;
    }

    public void setCertificationId(String certificationId) {
        this.certificationId = certificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCertificationName() {
        return certificationName;
    }

    public void setCertificationName(String certificationName) {
        this.certificationName = certificationName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
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
