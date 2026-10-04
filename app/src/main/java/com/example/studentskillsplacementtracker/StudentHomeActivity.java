package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Placeholder authenticated landing screen for students.
 *
 * Phase 1B scope: session guard, welcome message and logout only. The real
 * student dashboard replaces this screen in a later phase.
 */
public class StudentHomeActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private TextView welcomeTextView;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();

        // Navigation guard: this screen must never be shown without a session
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_student_home);

        // Connect XML components with Java
        welcomeTextView = findViewById(R.id.welcomeTextView);
        logoutButton = findViewById(R.id.logoutButton);

        // Welcome the authenticated user by email
        String email = currentUser.getEmail();
        if (email == null || email.isEmpty()) {
            welcomeTextView.setText(R.string.student_home_welcome_unknown);
        } else {
            welcomeTextView.setText(getString(R.string.student_home_welcome, email));
        }

        // Logout button click
        logoutButton.setOnClickListener(view -> logout());
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
