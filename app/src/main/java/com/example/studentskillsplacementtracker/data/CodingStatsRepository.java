package com.example.studentskillsplacementtracker.data;

import com.example.studentskillsplacementtracker.model.CodingStats;
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
 * Single point of access to Cloud Firestore for coding practice statistics.
 *
 * Statistics live in the {@code coding} sub-collection of the student document:
 * {@code students/{uid}/coding/{codingId}}. The uid always comes from the
 * authenticated Firebase user and never from the UI layer. This class contains
 * no UI code.
 */
public class CodingStatsRepository {

    public static final String COLLECTION_STUDENTS = "students";
    public static final String COLLECTION_CODING = "coding";

    private static final String FIELD_CODING_ID = "codingId";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_PLATFORM = "platform";
    private static final String FIELD_EASY = "easy";
    private static final String FIELD_MEDIUM = "medium";
    private static final String FIELD_HARD = "hard";
    private static final String FIELD_TOTAL = "total";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final FirebaseFirestore firestore;

    public CodingStatsRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Reference to the coding sub-collection that belongs to {@code uid}.
     */
    private CollectionReference codingCollection(String uid) {
        return firestore.collection(COLLECTION_STUDENTS)
                .document(uid)
                .collection(COLLECTION_CODING);
    }

    /**
     * Loads every coding statistics document of {@code uid}, newest created
     * first. {@code onSuccess} always receives a list (possibly empty).
     */
    public void getCodingStats(String uid,
                               OnSuccessListener<List<CodingStats>> onSuccess,
                               OnFailureListener onFailure) {

        codingCollection(uid)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<CodingStats> codingStatsList = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        CodingStats codingStats = document.toObject(CodingStats.class);
                        if (codingStats != null) {
                            codingStatsList.add(codingStats);
                        }
                    }

                    onSuccess.onSuccess(codingStatsList);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Loads a single coding statistics document. {@code onSuccess} receives
     * {@code null} when no document exists for {@code codingId}.
     */
    public void getCodingStat(String uid,
                              String codingId,
                              OnSuccessListener<CodingStats> onSuccess,
                              OnFailureListener onFailure) {

        codingCollection(uid).document(codingId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        onSuccess.onSuccess(snapshot.toObject(CodingStats.class));
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Creates a new coding statistics document. A document reference is
     * generated up front so its id can be stored as {@code codingId} inside the
     * document. The authenticated {@code uid} is always stored as
     * {@code userId}, the total is derived from the difficulty counts, and the
     * server sets both timestamps.
     */
    public void addCodingStats(String uid, CodingStats codingStats,
                               OnCompleteListener<Void> listener) {

        DocumentReference document = codingCollection(uid).document();
        String codingId = document.getId();

        codingStats.setCodingId(codingId);
        codingStats.setUserId(uid);
        codingStats.setTotal(
                CodingStats.calculateTotal(
                        codingStats.getEasy(),
                        codingStats.getMedium(),
                        codingStats.getHard()
                )
        );

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_CODING_ID, codingId);
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_PLATFORM, codingStats.getPlatform());
        data.put(FIELD_EASY, codingStats.getEasy());
        data.put(FIELD_MEDIUM, codingStats.getMedium());
        data.put(FIELD_HARD, codingStats.getHard());
        data.put(FIELD_TOTAL, codingStats.getTotal());
        data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        document.set(data).addOnCompleteListener(listener);
    }

    /**
     * Updates the editable fields of an existing coding statistics document.
     * {@code codingId}, {@code userId} and {@code createdAt} are preserved, the
     * total is re-derived from the difficulty counts, and the server refreshes
     * {@code updatedAt}.
     */
    public void updateCodingStats(String uid, CodingStats codingStats,
                                  OnCompleteListener<Void> listener) {

        codingStats.setTotal(
                CodingStats.calculateTotal(
                        codingStats.getEasy(),
                        codingStats.getMedium(),
                        codingStats.getHard()
                )
        );

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_CODING_ID, codingStats.getCodingId());
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_PLATFORM, codingStats.getPlatform());
        data.put(FIELD_EASY, codingStats.getEasy());
        data.put(FIELD_MEDIUM, codingStats.getMedium());
        data.put(FIELD_HARD, codingStats.getHard());
        data.put(FIELD_TOTAL, codingStats.getTotal());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        codingCollection(uid).document(codingStats.getCodingId())
                .update(data)
                .addOnCompleteListener(listener);
    }

    /**
     * Deletes the coding statistics document identified by {@code codingId}.
     */
    public void deleteCodingStats(String uid, String codingId,
                                  OnCompleteListener<Void> listener) {

        codingCollection(uid).document(codingId)
                .delete()
                .addOnCompleteListener(listener);
    }
}
