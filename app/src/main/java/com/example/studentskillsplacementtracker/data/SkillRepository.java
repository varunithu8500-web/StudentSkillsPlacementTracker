package com.example.studentskillsplacementtracker.data;

import com.example.studentskillsplacementtracker.model.Skill;
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
 * Single point of access to Cloud Firestore for student skills.
 *
 * Skills live in the {@code skills} sub-collection of the student document:
 * {@code students/{uid}/skills/{skillId}}. The uid always comes from the
 * authenticated Firebase user and never from the UI layer. This class contains
 * no UI code.
 */
public class SkillRepository {

    public static final String COLLECTION_STUDENTS = "students";
    public static final String COLLECTION_SKILLS = "skills";

    private static final String FIELD_SKILL_ID = "skillId";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_SKILL_NAME = "skillName";
    private static final String FIELD_PROFICIENCY = "proficiency";
    private static final String FIELD_PROGRESS = "progress";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final FirebaseFirestore firestore;

    public SkillRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Reference to the skills sub-collection that belongs to {@code uid}.
     */
    private CollectionReference skillsCollection(String uid) {
        return firestore.collection(COLLECTION_STUDENTS)
                .document(uid)
                .collection(COLLECTION_SKILLS);
    }

    /**
     * Loads every skill of {@code uid}, newest first. {@code onSuccess} always
     * receives a list (possibly empty).
     */
    public void listSkills(String uid,
                           OnSuccessListener<List<Skill>> onSuccess,
                           OnFailureListener onFailure) {

        skillsCollection(uid)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Skill> skills = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Skill skill = document.toObject(Skill.class);
                        if (skill != null) {
                            skills.add(skill);
                        }
                    }

                    onSuccess.onSuccess(skills);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Loads a single skill. {@code onSuccess} receives {@code null} when no
     * document exists for {@code skillId}.
     */
    public void getSkill(String uid,
                         String skillId,
                         OnSuccessListener<Skill> onSuccess,
                         OnFailureListener onFailure) {

        skillsCollection(uid).document(skillId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        onSuccess.onSuccess(snapshot.toObject(Skill.class));
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Creates a new skill document. A document reference is generated up front
     * so its id can be stored as {@code skillId} inside the document. The
     * authenticated {@code uid} is always stored as {@code userId}, and the
     * server sets both timestamps.
     */
    public void addSkill(String uid, Skill skill, OnCompleteListener<Void> listener) {

        DocumentReference document = skillsCollection(uid).document();
        String skillId = document.getId();

        skill.setSkillId(skillId);
        skill.setUserId(uid);

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_SKILL_ID, skillId);
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_SKILL_NAME, skill.getSkillName());
        data.put(FIELD_PROFICIENCY, skill.getProficiency());
        data.put(FIELD_PROGRESS, skill.getProgress());
        data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        document.set(data).addOnCompleteListener(listener);
    }

    /**
     * Updates the editable fields of an existing skill. {@code skillId} and
     * {@code userId} are preserved, {@code createdAt} is left untouched and the
     * server refreshes {@code updatedAt}.
     */
    public void updateSkill(String uid, Skill skill, OnCompleteListener<Void> listener) {

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_SKILL_ID, skill.getSkillId());
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_SKILL_NAME, skill.getSkillName());
        data.put(FIELD_PROFICIENCY, skill.getProficiency());
        data.put(FIELD_PROGRESS, skill.getProgress());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        skillsCollection(uid).document(skill.getSkillId())
                .update(data)
                .addOnCompleteListener(listener);
    }

    /**
     * Deletes the skill document identified by {@code skillId}.
     */
    public void deleteSkill(String uid, String skillId, OnCompleteListener<Void> listener) {

        skillsCollection(uid).document(skillId)
                .delete()
                .addOnCompleteListener(listener);
    }
}
