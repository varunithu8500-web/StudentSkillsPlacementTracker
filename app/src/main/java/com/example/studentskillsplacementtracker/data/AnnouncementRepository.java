package com.example.studentskillsplacementtracker.data;

import com.example.studentskillsplacementtracker.model.Announcement;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single point of access to Cloud Firestore for placement announcements
 * (FR-ANN-01, FR-ADM-01).
 *
 * Announcements live in the top level {@code announcements} collection because
 * they are shared placement notices published by coordinators and read by every
 * student, so no uid is involved. Firestore security rules restrict writes to
 * coordinators. This class contains no UI code.
 */
public class AnnouncementRepository {

    public static final String COLLECTION_ANNOUNCEMENTS = "announcements";

    private static final String FIELD_ANNOUNCEMENT_ID = "announcementId";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_COMPANY_NAME = "companyName";
    private static final String FIELD_DATE = "date";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final CollectionReference announcementsCollection;

    public AnnouncementRepository() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        announcementsCollection = firestore.collection(COLLECTION_ANNOUNCEMENTS);
    }

    /**
     * Loads every announcement, newest published first. {@code onSuccess}
     * always receives a list (possibly empty).
     */
    public void getAnnouncements(OnSuccessListener<List<Announcement>> onSuccess,
                                 OnFailureListener onFailure) {

        announcementsCollection
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Announcement> announcements = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Announcement announcement = document.toObject(Announcement.class);
                        if (announcement != null) {
                            announcements.add(announcement);
                        }
                    }

                    onSuccess.onSuccess(announcements);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Loads a single announcement. {@code onSuccess} receives {@code null} when
     * no document exists for {@code announcementId}.
     */
    public void getAnnouncement(String announcementId,
                                OnSuccessListener<Announcement> onSuccess,
                                OnFailureListener onFailure) {

        announcementsCollection.document(announcementId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        onSuccess.onSuccess(snapshot.toObject(Announcement.class));
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Publishes a new announcement. A document reference is generated up front
     * so its id can be stored as {@code announcementId} inside the document, and
     * the server sets both bookkeeping timestamps.
     */
    public void addAnnouncement(Announcement announcement, OnCompleteListener<Void> listener) {

        DocumentReference document = announcementsCollection.document();
        String announcementId = document.getId();

        announcement.setAnnouncementId(announcementId);

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_ANNOUNCEMENT_ID, announcementId);
        data.put(FIELD_TITLE, announcement.getTitle());
        data.put(FIELD_DESCRIPTION, announcement.getDescription());
        data.put(FIELD_COMPANY_NAME, announcement.getCompanyName());
        data.put(FIELD_DATE, announcement.getDate());
        data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        document.set(data).addOnCompleteListener(listener);
    }

    /**
     * Updates the editable fields of an existing announcement.
     * {@code announcementId} and {@code createdAt} are preserved and the server
     * refreshes {@code updatedAt}.
     */
    public void updateAnnouncement(Announcement announcement, OnCompleteListener<Void> listener) {

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_ANNOUNCEMENT_ID, announcement.getAnnouncementId());
        data.put(FIELD_TITLE, announcement.getTitle());
        data.put(FIELD_DESCRIPTION, announcement.getDescription());
        data.put(FIELD_COMPANY_NAME, announcement.getCompanyName());
        data.put(FIELD_DATE, announcement.getDate());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        announcementsCollection.document(announcement.getAnnouncementId())
                .update(data)
                .addOnCompleteListener(listener);
    }

    /**
     * Deletes the announcement document identified by {@code announcementId}.
     */
    public void deleteAnnouncement(String announcementId, OnCompleteListener<Void> listener) {

        announcementsCollection.document(announcementId)
                .delete()
                .addOnCompleteListener(listener);
    }
}
