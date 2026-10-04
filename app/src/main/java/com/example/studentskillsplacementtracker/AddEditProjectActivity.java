package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.ProjectRepository;
import com.example.studentskillsplacementtracker.model.Project;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Add / edit form for a single project (FR-PRJ-01).
 *
 * The form works in "edit" mode when a project id is supplied as an Intent
 * extra. Only the project id is ever read from the Intent: the owning uid
 * always comes from the authenticated Firebase user, never from the UI layer.
 */
public class AddEditProjectActivity extends AppCompatActivity {

    /** Extra carrying the id of the project being edited. */
    public static final String EXTRA_PROJECT_ID = "extra_project_id";

    private FirebaseAuth firebaseAuth;
    private ProjectRepository projectRepository;

    private TextView projectFormTitleTextView;
    private EditText projectNameEditText;
    private EditText descriptionEditText;
    private EditText technologiesEditText;
    private EditText statusEditText;
    private EditText repositoryLinkEditText;
    private ProgressBar projectFormProgressBar;
    private Button saveProjectButton;
    private Button cancelProjectButton;

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

    /** Id of the project being edited, or {@code null} when adding. */
    private String projectId;

    // Guards against duplicate save requests
    private boolean isSaving = false;

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

        // Only the project id may travel through the Intent
        projectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);

        setContentView(R.layout.activity_add_edit_project);

        // Connect XML components with Java
        projectFormTitleTextView = findViewById(R.id.projectFormTitleTextView);
        projectNameEditText = findViewById(R.id.projectNameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        technologiesEditText = findViewById(R.id.technologiesEditText);
        statusEditText = findViewById(R.id.statusEditText);
        repositoryLinkEditText = findViewById(R.id.repositoryLinkEditText);
        projectFormProgressBar = findViewById(R.id.projectFormProgressBar);
        saveProjectButton = findViewById(R.id.saveProjectButton);
        cancelProjectButton = findViewById(R.id.cancelProjectButton);

        saveProjectButton.setOnClickListener(view -> saveProject());
        cancelProjectButton.setOnClickListener(view -> finish());

        if (isEditMode()) {
            projectFormTitleTextView.setText(R.string.project_edit_title);
            loadProject();
        } else {
            projectFormTitleTextView.setText(R.string.project_add_title);
        }
    }

    private boolean isEditMode() {
        return projectId != null && !projectId.isEmpty();
    }

    /**
     * Loads the project being edited and fills the form.
     */
    private void loadProject() {

        setLoadingState(true);

        projectRepository.getProject(uid, projectId,
                project -> {
                    setLoadingState(false);

                    if (project == null) {
                        Toast.makeText(
                                AddEditProjectActivity.this,
                                R.string.err_project_load_failed,
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    } else {
                        bindProject(project);
                    }
                },
                exception -> {
                    setLoadingState(false);
                    Toast.makeText(
                            AddEditProjectActivity.this,
                            R.string.err_project_load_failed,
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    private void bindProject(Project project) {
        projectNameEditText.setText(project.getProjectName());
        descriptionEditText.setText(project.getDescription());
        technologiesEditText.setText(joinTechnologies(project.getTechnologies()));
        statusEditText.setText(project.getStatus());
        repositoryLinkEditText.setText(project.getRepositoryLink());
    }

    /**
     * Converts the stored technology list back into comma separated text for the
     * form field so it can be edited.
     */
    private String joinTechnologies(List<String> technologies) {

        if (technologies == null || technologies.isEmpty()) {
            return "";
        }

        return TextUtils.join(", ", technologies);
    }

    /**
     * Parses the comma separated technology field into a list, trimming each
     * entry and dropping blanks. Never returns {@code null}.
     */
    private List<String> parseTechnologies(String rawTechnologies) {

        List<String> technologies = new ArrayList<>();

        if (rawTechnologies == null) {
            return technologies;
        }

        String[] parts = rawTechnologies.split(",");

        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                technologies.add(value);
            }
        }

        return technologies;
    }

    private void saveProject() {

        if (isSaving) {
            return;
        }

        if (!isFormValid()) {
            return;
        }

        // Trimmed text fields; technologies are parsed from comma separated text.
        String projectName = projectNameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        List<String> technologies = parseTechnologies(
                technologiesEditText.getText().toString());
        String status = Project.canonicalStatus(statusEditText.getText().toString());
        String repositoryLink = repositoryLinkEditText.getText().toString().trim();

        Project project = new Project(
                projectId,
                uid,
                projectName,
                description,
                technologies,
                status,
                repositoryLink
        );

        setLoadingState(true);

        if (isEditMode()) {
            updateExistingProject(project);
        } else {
            addNewProject(project);
        }
    }

    /**
     * Creates a new project. The repository generates the document id and stores
     * it as {@code projectId}; the authenticated uid is stored as {@code userId}.
     */
    private void addNewProject(Project project) {

        projectRepository.addProject(uid, project, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditProjectActivity.this,
                        R.string.msg_project_added,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditProjectActivity.this,
                        R.string.err_project_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Updates the existing project. {@code projectId}, {@code userId} and
     * {@code createdAt} are preserved.
     */
    private void updateExistingProject(Project project) {

        projectRepository.updateProject(uid, project, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditProjectActivity.this,
                        R.string.msg_project_updated,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditProjectActivity.this,
                        R.string.err_project_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Required: the project name and a status from the allowed set.
     * Optional: a repository link, which must be a valid web URL when supplied.
     */
    private boolean isFormValid() {

        String projectName = projectNameEditText.getText().toString().trim();
        String status = statusEditText.getText().toString().trim();
        String repositoryLink = repositoryLinkEditText.getText().toString().trim();

        // Project name
        if (projectName.isEmpty()) {
            return showFieldError(projectNameEditText, R.string.err_project_name_required);
        }

        // Status
        if (status.isEmpty()) {
            return showFieldError(statusEditText, R.string.err_project_status_required);
        }
        if (!Project.isStatusAllowed(status)) {
            return showFieldError(statusEditText, R.string.err_project_status_invalid);
        }

        // Repository link (optional, but must be a web URL when provided)
        if (!repositoryLink.isEmpty()
                && !Patterns.WEB_URL.matcher(repositoryLink).matches()) {
            return showFieldError(
                    repositoryLinkEditText,
                    R.string.err_project_repository_link_invalid
            );
        }

        return true;
    }

    private boolean showFieldError(EditText field, int messageResId) {
        field.setError(getString(messageResId));
        field.requestFocus();
        return false;
    }

    private void setLoadingState(boolean loading) {
        isSaving = loading;
        projectFormProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveProjectButton.setEnabled(!loading);
        cancelProjectButton.setEnabled(!loading);
        projectNameEditText.setEnabled(!loading);
        descriptionEditText.setEnabled(!loading);
        technologiesEditText.setEnabled(!loading);
        statusEditText.setEnabled(!loading);
        repositoryLinkEditText.setEnabled(!loading);
    }

    private void goToLogin() {
        Intent intent = new Intent(AddEditProjectActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
