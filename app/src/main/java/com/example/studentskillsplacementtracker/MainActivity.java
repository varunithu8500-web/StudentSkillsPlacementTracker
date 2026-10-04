package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.StudentRepository;
import com.example.studentskillsplacementtracker.model.Student;
import com.example.studentskillsplacementtracker.util.AuthErrorMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar loginProgressBar;
    private TextView registerTextView;

    private FirebaseAuth firebaseAuth;
    private StudentRepository studentRepository;

    // Guards against duplicate login requests
    private boolean isLoggingIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();
        studentRepository = new StudentRepository();

        // Connect XML components with Java
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        registerTextView = findViewById(R.id.registerTextView);

        // Login button click
        loginButton.setOnClickListener(view -> loginUser());

        // Open the student registration screen
        registerTextView.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));

        // Already authenticated? Resolve the account role and route straight to
        // the matching dashboard instead of showing the login form.
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            routeAuthenticatedUser(currentUser.getUid());
        }
    }

    /**
     * Reads {@code students/{uid}.role} and opens the dashboard that matches the
     * account role. Students land on their home screen; coordinators and admins
     * land on the coordinator dashboard. A missing profile or an unrecognised
     * role enters neither dashboard.
     */
    private void routeAuthenticatedUser(String uid) {

        setLoadingState(true);

        studentRepository.getProfile(uid,
                student -> {
                    setLoadingState(false);
                    openDashboardFor(student);
                },
                exception -> {
                    setLoadingState(false);
                    showRoutingError(R.string.student_home_profile_error);
                });
    }

    private void openDashboardFor(Student student) {

        String role = (student == null) ? null : student.getRole();

        if (Student.ROLE_STUDENT.equals(role)) {
            openDashboard(StudentHomeActivity.class);
        } else if (Student.ROLE_COORDINATOR.equals(role)
                || Student.ROLE_ADMIN.equals(role)) {
            openDashboard(CoordinatorHomeActivity.class);
        } else {
            showRoutingError(R.string.err_role_unknown);
        }
    }

    private void openDashboard(Class<?> dashboardActivity) {
        startActivity(new Intent(MainActivity.this, dashboardActivity));
        finish();
    }

    private void showRoutingError(int messageResId) {
        passwordEditText.setText("");
        Toast.makeText(MainActivity.this, messageResId, Toast.LENGTH_LONG).show();
    }

    private void loginUser() {

        if (isLoggingIn) {
            return;
        }

        String email = emailEditText.getText().toString().trim();
        // The password is never trimmed.
        String password = passwordEditText.getText().toString();

        // Validate email
        if (email.isEmpty()) {
            emailEditText.setError(getString(R.string.err_email_required));
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.err_email_invalid));
            emailEditText.requestFocus();
            return;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordEditText.setError(getString(R.string.err_password_required));
            passwordEditText.requestFocus();
            return;
        }

        setLoadingState(true);

        // Firebase login
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        setLoadingState(false);
                        Toast.makeText(
                                MainActivity.this,
                                AuthErrorMapper.getMessage(MainActivity.this, task.getException()),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    FirebaseUser user = (task.getResult() != null)
                            ? task.getResult().getUser()
                            : null;

                    if (user == null) {
                        setLoadingState(false);
                        Toast.makeText(
                                MainActivity.this,
                                R.string.err_auth_generic,
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    Toast.makeText(
                            MainActivity.this,
                            R.string.msg_login_success,
                            Toast.LENGTH_SHORT
                    ).show();

                    // Resolve the role and leave the login screen so Back cannot
                    // return to it.
                    routeAuthenticatedUser(user.getUid());
                });
    }

    private void setLoadingState(boolean loading) {
        isLoggingIn = loading;
        loginProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        registerTextView.setEnabled(!loading);
    }
}