package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.StudentRepository;
import com.example.studentskillsplacementtracker.model.Student;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Authenticated landing screen for students.
 *
 * Phase 2 scope: session guard, student profile display (name, email,
 * department, year, CGPA) loaded from Firestore, and logout. Skills, projects,
 * certifications, coding practice, companies, eligibility and announcements are
 * added in later phases.
 */
public class StudentHomeActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private StudentRepository studentRepository;

    private TextView welcomeTextView;
    private ProgressBar profileProgressBar;
    private LinearLayout profileContainer;
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView departmentTextView;
    private TextView yearTextView;
    private TextView cgpaTextView;
    private TextView profileMessageTextView;
    private Button skillsButton;
    private Button projectsButton;
    private Button certificationsButton;
    private Button logoutButton;

    // Email of the signed-in account, used as a fallback welcome value.
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();
        studentRepository = new StudentRepository();

        // Navigation guard: this screen must never be shown without a session
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_student_home);

        // Connect XML components with Java
        welcomeTextView = findViewById(R.id.welcomeTextView);
        profileProgressBar = findViewById(R.id.profileProgressBar);
        profileContainer = findViewById(R.id.profileContainer);
        nameTextView = findViewById(R.id.nameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        departmentTextView = findViewById(R.id.departmentTextView);
        yearTextView = findViewById(R.id.yearTextView);
        cgpaTextView = findViewById(R.id.cgpaTextView);
        profileMessageTextView = findViewById(R.id.profileMessageTextView);
        skillsButton = findViewById(R.id.skillsButton);
        projectsButton = findViewById(R.id.projectsButton);
        certificationsButton = findViewById(R.id.certificationsButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Welcome the authenticated user right away; refined once the profile loads
        userEmail = currentUser.getEmail();
        setWelcome(userEmail);

        // Open the skills list
        skillsButton.setOnClickListener(view ->
                startActivity(new Intent(StudentHomeActivity.this, SkillsActivity.class)));

        // Open the projects list
        projectsButton.setOnClickListener(view ->
                startActivity(new Intent(StudentHomeActivity.this, ProjectsActivity.class)));

        // Open the certifications list
        certificationsButton.setOnClickListener(view ->
                startActivity(new Intent(StudentHomeActivity.this, CertificationsActivity.class)));

        // Logout button click
        logoutButton.setOnClickListener(view -> logout());

        loadProfile(currentUser.getUid());
    }

    /**
     * Loads the student profile document for the authenticated user.
     */
    private void loadProfile(String uid) {

        showLoadingState();

        studentRepository.getProfile(uid,
                student -> {
                    profileProgressBar.setVisibility(View.GONE);
                    if (student == null) {
                        showMessage(R.string.student_home_profile_missing);
                    } else {
                        bindProfile(student);
                    }
                },
                exception -> {
                    profileProgressBar.setVisibility(View.GONE);
                    showMessage(R.string.student_home_profile_error);
                });
    }

    private void bindProfile(Student student) {

        profileMessageTextView.setVisibility(View.GONE);
        profileContainer.setVisibility(View.VISIBLE);

        // Prefer the profile name; fall back to the account email.
        setWelcome(student.getName());

        nameTextView.setText(getString(R.string.student_home_label_name, safe(student.getName())));
        emailTextView.setText(getString(R.string.student_home_label_email, safe(student.getEmail())));
        departmentTextView.setText(
                getString(R.string.student_home_label_department, safe(student.getDepartment())));
        yearTextView.setText(getString(R.string.student_home_label_year, String.valueOf(student.getYear())));
        cgpaTextView.setText(getString(R.string.student_home_label_cgpa, String.valueOf(student.getCgpa())));
    }

    private void showLoadingState() {
        profileProgressBar.setVisibility(View.VISIBLE);
        profileContainer.setVisibility(View.GONE);
        profileMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        profileContainer.setVisibility(View.GONE);
        profileMessageTextView.setText(messageResId);
        profileMessageTextView.setVisibility(View.VISIBLE);
    }

    private void setWelcome(String preferredValue) {
        String value = (preferredValue != null && !preferredValue.isEmpty())
                ? preferredValue
                : userEmail;

        if (value == null || value.isEmpty()) {
            welcomeTextView.setText(R.string.student_home_welcome_unknown);
        } else {
            welcomeTextView.setText(getString(R.string.student_home_welcome, value));
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Signs the user out and returns to the login screen. The task is cleared so
     * this authenticated screen cannot be reached again from the back stack.
     */
    private void logout() {
        firebaseAuth.signOut();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(StudentHomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
