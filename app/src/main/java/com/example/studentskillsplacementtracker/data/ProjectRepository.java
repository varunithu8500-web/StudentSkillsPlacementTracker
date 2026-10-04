package com.example.studentskillsplacementtracker.data;

import com.example.studentskillsplacementtracker.model.Project;
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
 * Single point of access to Cloud Firestore for student projects.
 *
 * Projects live in the {@code projects} sub-collection of the student
 * document: {@code students/{uid}/projects/{projectId}}. The uid always comes
 * from the authenticated Firebase user and never from the UI layer. This class
 * contains no UI code.
 */
public class ProjectRepository {

    public static final String COLLECTION_STUDENTS = "students";
    public static final String COLLECTION_PROJECTS = "projects";

    private static final String FIELD_PROJECT_ID = "projectId";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_PROJECT_NAME = "projectName";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_TECHNOLOGIES = "technologies";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_REPOSITORY_LINK = "repositoryLink";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final FirebaseFirestore firestore;

    public ProjectRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Reference to the projects sub-collection that belongs to {@code uid}.
     */
    private CollectionReference projectsCollection(String uid) {
        return firestore.collection(COLLECTION_STUDENTS)
                .document(uid)
                .collection(COLLECTION_PROJECTS);
    }

    /**
     * Loads every project of {@code uid}, newest created first.
     * {@code onSuccess} always receives a list (possibly empty).
     */
    public void getProjects(String uid,
                            OnSuccessListener<List<Project>> onSuccess,
                            OnFailureListener onFailure) {

        projectsCollection(uid)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Project> projects = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Project project = document.toObject(Project.class);
                        if (project != null) {
                            projects.add(project);
                        }
                    }

                    onSuccess.onSuccess(projects);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Loads a single project. {@code onSuccess} receives {@code null} when no
     * document exists for {@code projectId}.
     */
    public void getProject(String uid,
                           String projectId,
                           OnSuccessListener<Project> onSuccess,
                           OnFailureListener onFailure) {

        projectsCollection(uid).document(projectId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        onSuccess.onSuccess(snapshot.toObject(Project.class));
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Creates a new project document. A document reference is generated up front
     * so its id can be stored as {@code projectId} inside the document. The
     * authenticated {@code uid} is always stored as {@code userId}, and the
     * server sets both timestamps.
     */
    public void addProject(String uid, Project project, OnCompleteListener<Void> listener) {

        DocumentReference document = projectsCollection(uid).document();
        String projectId = document.getId();

        project.setProjectId(projectId);
        project.setUserId(uid);

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_PROJECT_ID, projectId);
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_PROJECT_NAME, project.getProjectName());
        data.put(FIELD_DESCRIPTION, project.getDescription());
        data.put(FIELD_TECHNOLOGIES, project.getTechnologies());
        data.put(FIELD_STATUS, project.getStatus());
        data.put(FIELD_REPOSITORY_LINK, project.getRepositoryLink());
        data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        document.set(data).addOnCompleteListener(listener);
    }

    /**
     * Updates the editable fields of an existing project. {@code projectId} and
     * {@code userId} are preserved, {@code createdAt} is left untouched and the
     * server refreshes {@code updatedAt}.
     */
    public void updateProject(String uid, Project project, OnCompleteListener<Void> listener) {

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_PROJECT_ID, project.getProjectId());
        data.put(FIELD_USER_ID, uid);
        data.put(FIELD_PROJECT_NAME, project.getProjectName());
        data.put(FIELD_DESCRIPTION, project.getDescription());
        data.put(FIELD_TECHNOLOGIES, project.getTechnologies());
        data.put(FIELD_STATUS, project.getStatus());
        data.put(FIELD_REPOSITORY_LINK, project.getRepositoryLink());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());

        projectsCollection(uid).document(project.getProjectId())
                .update(data)
                .addOnCompleteListener(listener);
    }

    /**
     * Deletes the project document identified by {@code projectId}.
     */
    public void deleteProject(String uid, String projectId, OnCompleteListener<Void> listener) {

        projectsCollection(uid).document(projectId)
                .delete()
                .addOnCompleteListener(listener);
    }
}
