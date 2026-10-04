package com.example.studentskillsplacementtracker.model;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Placement announcement document stored in Cloud Firestore under
 * {@code announcements/{announcementId}} (FR-ANN-01).
 *
 * Announcements are shared placement notices published by coordinators and read
 * by every student, so this collection is not scoped to a student uid.
 *
 * {@link #companyName} is the free text company / drive reference the
 * announcement is about, for example {@code "TCS NQT Drive"}.
 *
 * {@link #date} is deliberately picked by the coordinator in a date picker and
 * stored as midnight in the device's local time zone, matching certifications.
 * {@link #createdAt} and {@link #updatedAt} are server owned bookkeeping fields.
 *
 * The no-argument constructor and the getters/setters are required by
 * Firestore's automatic de-serialization (DocumentSnapshot#toObject).
 */
public class Announcement {

    /** Display pattern for {@link #date}, for example {@code 04 Oct 2026}. */
    public static final String DATE_PATTERN = "dd MMM yyyy";

    private String announcementId;
    private String title;
    private String description;
    private String companyName;
    private Timestamp date;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /** Required by Firestore. Do not remove. */
    public Announcement() {
    }

    public Announcement(String announcementId, String title, String description,
                        String companyName, Timestamp date) {
        this.announcementId = announcementId;
        this.title = title;
        this.description = description;
        this.companyName = companyName;
        this.date = date;
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

    public String getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(String announcementId) {
        this.announcementId = announcementId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
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
