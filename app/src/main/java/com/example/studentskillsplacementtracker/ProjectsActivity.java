package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.data.ProjectRepository;
import com.example.studentskillsplacementtracker.model.Project;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Projects list screen (FR-PRJ-01).
 *
 * Lists the projects of the authenticated student and offers add, edit and
 * delete. Projects always live under the authenticated user's uid; the uid is
 * never taken from an Intent. This class contains no Firestore code of its own
 * - all access goes through {@link ProjectRepository}.
 */
public class ProjectsActivity extends AppCompatActivity
        implements ProjectAdapter.OnProjectActionListener {

    private FirebaseAuth firebaseAuth;
    private ProjectRepository projectRepository;

    private RecyclerView projectsRecyclerView;
    private ProjectAdapter projectAdapter;
    private ProgressBar projectsProgressBar;
    private TextView projectsMessageTextView;
    private Button addProjectButton;

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();
        projectRepository = new ProjectRepository();

        // Navigation guard: this screen must never be shown without a session
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        // Ownership always comes from the authenticated user, never an Intent
        uid = currentUser.getUid();

        setContentView(R.layout.activity_projects);

        // Connect XML components with Java
        projectsRecyclerView = findViewById(R.id.projectsRecyclerView);
        projectsProgressBar = findViewById(R.id.projectsProgressBar);
        projectsMessageTextView = findViewById(R.id.projectsMessageTextView);
        addProjectButton = findViewById(R.id.addProjectButton);

        projectAdapter = new ProjectAdapter(this);
        projectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        projectsRecyclerView.setAdapter(projectAdapter);

        addProjectButton.setOnClickListener(view -> openAddProjectForm());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload every time the screen becomes visible again, e.g. after the
        // add / edit form finishes.
        if (uid != null) {
            loadProjects();
        }
    }

    /**
     * Loads the authenticated student's projects.
     */
    private void loadProjects() {

        showLoadingState();

        projectRepository.getProjects(uid,
                projects -> {

                    projectsProgressBar.setVisibility(View.GONE);
                    projectAdapter.setProjects(projects);

                    if (projects.isEmpty()) {
                        showMessage(R.string.projects_empty);
                    } else {
                        projectsMessageTextView.setVisibility(View.GONE);
                        projectsRecyclerView.setVisibility(View.VISIBLE);
                    }
                },
                exception -> {
                    projectsProgressBar.setVisibility(View.GONE);
                    projectAdapter.setProjects(null);
                    showMessage(R.string.projects_load_error);
                });
    }

    private void showLoadingState() {
        projectsProgressBar.setVisibility(View.VISIBLE);
        projectsRecyclerView.setVisibility(View.GONE);
        projectsMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        projectsRecyclerView.setVisibility(View.GONE);
        projectsMessageTextView.setText(messageResId);
        projectsMessageTextView.setVisibility(View.VISIBLE);
    }

    private void openAddProjectForm() {
        startActivity(new Intent(ProjectsActivity.this, AddEditProjectActivity.class));
    }

    @Override
    public void onEditProject(Project project) {

        // Only the project id travels through the Intent
        Intent intent = new Intent(ProjectsActivity.this, AddEditProjectActivity.class);
        intent.putExtra(AddEditProjectActivity.EXTRA_PROJECT_ID, project.getProjectId());
        startActivity(intent);
    }

    @Override
    public void onDeleteProject(Project project) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_project_title)
                .setMessage(R.string.dialog_delete_project_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> deleteProject(project))
                .show();
    }

    /**
     * Deletes the project once the user has confirmed, then reloads the list.
     */
    private void deleteProject(Project project) {

        showLoadingState();

        projectRepository.deleteProject(uid, project.getProjectId(), task -> {

            if (task.isSuccessful()) {
                Toast.makeText(
                        ProjectsActivity.this,
                        R.string.msg_project_deleted,
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        ProjectsActivity.this,
                        R.string.err_project_delete_failed,
                        Toast.LENGTH_LONG
                ).show();
            }

            loadProjects();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(ProjectsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
