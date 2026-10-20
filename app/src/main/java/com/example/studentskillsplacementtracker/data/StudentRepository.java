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

public class StudentRepository {

    private static final String COLLECTION_STUDENTS = "students";

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

    // Create a new student profile.
    public void createProfile(
            Student student,
            OnCompleteListener<Void> listener) {

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

        studentsCollection
                .document(student.getUid())
                .set(profile)
                .addOnCompleteListener(listener);
    }

    // Retrieve a student's profile.
    public void getProfile(
            String uid,
            OnSuccessListener<Student> onSuccess,
            OnFailureListener onFailure) {

        studentsCollection
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.exists()) {
                        Student student = snapshot.toObject(Student.class);
                        onSuccess.onSuccess(student);
                    } else {
                        onSuccess.onSuccess(null);
                    }

                })
                .addOnFailureListener(onFailure);
    }

    // Update editable student profile fields.
    public void updateProfile(
            String uid,
            String name,
            String department,
            int year,
            double cgpa,
            OnCompleteListener<Void> listener) {

        Map<String, Object> updates = new HashMap<>();

        updates.put(FIELD_NAME, name);
        updates.put(FIELD_DEPARTMENT, department);
        updates.put(FIELD_YEAR, year);
        updates.put(FIELD_CGPA, cgpa);
        updates.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        studentsCollection
                .document(uid)
                .update(updates)
                .addOnCompleteListener(listener);
    }
}