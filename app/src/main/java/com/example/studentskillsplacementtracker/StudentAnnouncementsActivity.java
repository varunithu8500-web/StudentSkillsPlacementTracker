package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.data.AnnouncementRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Read only placement announcement list shown to students (FR-ANN-01).
 *
 * Announcements are shared documents in the top level announcements collection.
 * This screen offers no add, edit or delete controls and no click through - the
 * whole announcement is read straight from the list row, so there is no details
 * screen. All access goes through AnnouncementRepository.
 */
public class StudentAnnouncementsActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private AnnouncementRepository announcementRepository;

    private RecyclerView studentAnnouncementsRecyclerView;
    private StudentAnnouncementAdapter studentAnnouncementAdapter;
    private ProgressBar studentAnnouncementsProgressBar;
    private TextView studentAnnouncementsMessageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();
        announcementRepository = new AnnouncementRepository();

        // Navigation guard: this screen must never be shown without a session
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_student_announcements);

        // Connect XML components with Java
        studentAnnouncementsRecyclerView = findViewById(R.id.studentAnnouncementsRecyclerView);
        studentAnnouncementsProgressBar = findViewById(R.id.studentAnnouncementsProgressBar);
        studentAnnouncementsMessageTextView =
                findViewById(R.id.studentAnnouncementsMessageTextView);

        studentAnnouncementAdapter = new StudentAnnouncementAdapter();
        studentAnnouncementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentAnnouncementsRecyclerView.setAdapter(studentAnnouncementAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload every time the screen becomes visible again. The adapter is
        // null when the session guard already sent the user back to login.
        if (studentAnnouncementAdapter != null) {
            loadAnnouncements();
        }
    }

    /**
     * Loads every announcement, newest published first.
     */
    private void loadAnnouncements() {

        showLoadingState();

        announcementRepository.getAnnouncements(
                announcements -> {

                    studentAnnouncementsProgressBar.setVisibility(View.GONE);
                    studentAnnouncementAdapter.setAnnouncements(announcements);

                    if (announcements.isEmpty()) {
                        showMessage(R.string.student_announcements_empty);
                    } else {
                        studentAnnouncementsMessageTextView.setVisibility(View.GONE);
                        studentAnnouncementsRecyclerView.setVisibility(View.VISIBLE);
                    }
                },
                exception -> {
                    studentAnnouncementsProgressBar.setVisibility(View.GONE);
                    studentAnnouncementAdapter.setAnnouncements(null);
                    showMessage(R.string.student_announcements_load_error);
                });
    }

    private void showLoadingState() {
        studentAnnouncementsProgressBar.setVisibility(View.VISIBLE);
        studentAnnouncementsRecyclerView.setVisibility(View.GONE);
        studentAnnouncementsMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        studentAnnouncementsRecyclerView.setVisibility(View.GONE);
        studentAnnouncementsMessageTextView.setText(messageResId);
        studentAnnouncementsMessageTextView.setVisibility(View.VISIBLE);
    }

    private void goToLogin() {
        Intent intent = new Intent(StudentAnnouncementsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
