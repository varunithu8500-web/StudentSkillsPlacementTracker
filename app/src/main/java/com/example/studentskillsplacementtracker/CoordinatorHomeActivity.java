package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Authenticated landing screen for placement coordinators and admins.
 *
 * The role that leads here is decided in MainActivity from
 * students/{uid}.role. Company placement requirements are managed from this
 * screen; student facing placement features arrive in later phases.
 */
public class CoordinatorHomeActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private TextView welcomeTextView;
    private Button manageCompaniesButton;
    private Button manageAnnouncementsButton;
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

        setContentView(R.layout.activity_coordinator_home);

        // Connect XML components with Java
        welcomeTextView = findViewById(R.id.coordinatorWelcomeTextView);
        manageCompaniesButton = findViewById(R.id.manageCompaniesButton);
        manageAnnouncementsButton = findViewById(R.id.manageAnnouncementsButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Welcome the coordinator using the signed in account email
        String email = currentUser.getEmail();
        if (email == null || email.isEmpty()) {
            welcomeTextView.setText(R.string.coordinator_home_welcome_unknown);
        } else {
            welcomeTextView.setText(getString(R.string.coordinator_home_welcome, email));
        }

        // Open the company management list
        manageCompaniesButton.setOnClickListener(view ->
                startActivity(new Intent(CoordinatorHomeActivity.this, CompaniesActivity.class)));

        // Open the announcement management list
        manageAnnouncementsButton.setOnClickListener(view ->
                startActivity(new Intent(CoordinatorHomeActivity.this, AnnouncementsActivity.class)));

        // Logout button click
        logoutButton.setOnClickListener(view -> logout());
    }

    /**
     * Signs the user out and returns to the login screen. The task is cleared so
     * this screen cannot be reached again from the back stack.
     */
    private void logout() {
        firebaseAuth.signOut();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(CoordinatorHomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
