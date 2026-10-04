package com.example.studentskillsplacementtracker.data;

import com.example.studentskillsplacementtracker.model.Student;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Single point of access to Cloud Firestore for student profiles.
 *
 * Profiles live in the {@code students} collection and use the Firebase
 * Authentication UID as the document id. This class contains no UI code.
 */
public class StudentRepository {

    public static final String COLLECTION_STUDENTS = "students";

    private static final String FIELD_UID = "uid";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_DEPARTMENT = "department";
    private static final String FIELD_YEAR = "year";
    private static final String FIELD_CGPA = "cgpa";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final CollectionReference studentsCollection;

    public StudentRepository() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        studentsCollection = firestore.collection(COLLECTION_STUDENTS);
    }

    /**
     * Creates (or overwrites) the profile document for {@code student}, keyed by
     * the student's Firebase Authentication UID. Creation and update timestamps
     * are set by the server.
     */
    public void createProfile(Student student, OnCompleteListener<Void> listener) {

        Map<String, Object> profile = new HashMap<>();
        profile.put(FIELD_UID, student.getUid());
        profile.put(FIELD_NAME, student.getName());
        profile.put(FIELD_EMAIL, student.getEmail());
        profile.put(FIELD_DEPARTMENT, student.getDepartment());
        profile.put(FIELD_YEAR, student.getYear());
        profile.put(FIELD_CGPA, student.getCgpa());
        profile.put(FIELD_ROLE, student.getRole());
        profile.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        profile.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        studentsCollection.document(student.getUid())
                .set(profile)
                .addOnCompleteListener(listener);
    }

    /**
     * Loads the profile document for {@code uid}.
     *
     * {@code onSuccess} receives the mapped {@link Student}, or {@code null}
     * when no document exists for that UID. {@code onFailure} is invoked for
     * Firestore / network errors.
     */
    public void getProfile(String uid,
                           OnSuccessListener<Student> onSuccess,
                           OnFailureListener onFailure) {

        studentsCollection.document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        onSuccess.onSuccess(snapshot.toObject(Student.class));
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }
}
