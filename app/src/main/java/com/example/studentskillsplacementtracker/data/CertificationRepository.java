package com.example.studentskillsplacementtracker.data;

import com.example.studentskillsplacementtracker.model.Certification;
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
 * Single point of access to Cloud Firestore for student certifications.
 *
 * Certifications live in the {@code certifications} sub-collection of the
 * student document: {@code students/{uid}/certifications/{certificationId}}.
 * The uid always comes from the authenticated Firebase user and never from the
 * UI layer. This class contains no UI code.
 */
public class CertificationRepository {

    public static final String COLLECTION_STUDENTS = "students";
    public static final String COLLECTION_CERTIFICATIONS = "certifications";

    private static final String FIELD_CERTIFICATION_ID = "certificationId";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_CERTIFICATION_NAME = "certificationName";
    private static final String FIELD_ORGANIZATION = "organization";
    private static final String FIELD_DATE = "date";
    private static final String FIELD_CREDENTIAL = "credential";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final FirebaseFirestore firestore;

    public CertificationRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Reference to the certifications sub-collection that belongs to {@code uid}.
     */
    private CollectionReference certificationsCollection(String uid) {
        return firestore.collection(COLLECTION_STUDENTS)
                .document(uid)
                .collection(COLLECTION_CERTIFICATIONS);
    }

    /**
     * Loads every certification of {@code uid}, newest created first.
     * {@code onSuccess} always receives a list (possibly empty).
     */
    public void getCertifications(String uid,
                                  OnSuccessListener<List<Certification>> onSuccess,
                                  OnFailureListener onFailure) {

        certificationsCollection(uid)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Certification> certifications = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Certification certification = document.toObject(Certification.class);
                        if (certification != null) {
                            certifications.add(certification);
                        }
                    }

                    onSuccess.onSuccess(certifications);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Loads a single certification. {@code onSuccess} receives {@code null} when
     * no document exists for {@code certificationId}.
     */
    public void getCertification(String uid,
                                 String certificationId,
                                 OnSuccessListener<Certification> onSuccess,
                                 OnFailureListener onFailure) {

        certificationsCollection(uid).document(certificationId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        onSuccess.onSuccess(snapshot.toObject(Certification.class));
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Creates a new certification document. A document reference is generated up
     * front so its id can be stored as {@code certificationId} inside the
     * document. The authenticated {@code uid} is always stored as
     * {@code userId}, and the server sets both timestamps.
     */
    public void addCertification(String uid, Certification certification,
                                 OnCompleteListener<Void> listener) {

        DocumentReference document = certificationsCollection(uid).document();
        String certificationId = document.getId();

        certification.setCertificationId(certificationId);
        certification.setUserId(uid);

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_CERTIFICATION_ID, certificationId);
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_CERTIFICATION_NAME, certification.getCertificationName());
        data.put(FIELD_ORGANIZATION, certification.getOrganization());
        data.put(FIELD_DATE, certification.getDate());
        data.put(FIELD_CREDENTIAL, certification.getCredential());
        data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        document.set(data).addOnCompleteListener(listener);
    }

    /**
     * Updates the editable fields of an existing certification.
     * {@code certificationId} and {@code userId} are preserved,
     * {@code createdAt} is left untouched and the server refreshes
     * {@code updatedAt}.
     */
    public void updateCertification(String uid, Certification certification,
                                    OnCompleteListener<Void> listener) {

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_CERTIFICATION_ID, certification.getCertificationId());
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_CERTIFICATION_NAME, certification.getCertificationName());
        data.put(FIELD_ORGANIZATION, certification.getOrganization());
        data.put(FIELD_DATE, certification.getDate());
        data.put(FIELD_CREDENTIAL, certification.getCredential());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        certificationsCollection(uid).document(certification.getCertificationId())
                .update(data)
                .addOnCompleteListener(listener);
    }

    /**
     * Deletes the certification document identified by {@code certificationId}.
     */
    public void deleteCertification(String uid, String certificationId,
                                    OnCompleteListener<Void> listener) {

        certificationsCollection(uid).document(certificationId)
                .delete()
                .addOnCompleteListener(listener);
    }
}
