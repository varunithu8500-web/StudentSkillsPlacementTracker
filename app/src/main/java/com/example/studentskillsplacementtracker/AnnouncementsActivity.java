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

import com.example.studentskillsplacementtracker.data.AnnouncementRepository;
import com.example.studentskillsplacementtracker.model.Announcement;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Placement announcements list screen, used by coordinators (FR-ADM-01).
 *
 * Announcements are shared documents in the top level {@code announcements}
 * collection, so no uid is involved. This class contains no Firestore code of
 * its own - all access goes through {@link AnnouncementRepository}.
 */
public class AnnouncementsActivity extends AppCompatActivity
        implements AnnouncementAdapter.OnAnnouncementActionListener {

    private FirebaseAuth firebaseAuth;
    private AnnouncementRepository announcementRepository;

    private RecyclerView announcementsRecyclerView;
    private AnnouncementAdapter announcementAdapter;
    private ProgressBar announcementsProgressBar;
    private TextView announcementsMessageTextView;
    private Button addAnnouncementButton;

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

        setContentView(R.layout.activity_announcements);

        // Connect XML components with Java
        announcementsRecyclerView = findViewById(R.id.announcementsRecyclerView);
        announcementsProgressBar = findViewById(R.id.announcementsProgressBar);
        announcementsMessageTextView = findViewById(R.id.announcementsMessageTextView);
        addAnnouncementButton = findViewById(R.id.addAnnouncementButton);

        announcementAdapter = new AnnouncementAdapter(this);
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        announcementsRecyclerView.setAdapter(announcementAdapter);

        addAnnouncementButton.setOnClickListener(view -> openAddAnnouncementForm());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload every time the screen becomes visible again, e.g. after the
        // add / edit form finishes. The adapter is null when the session guard
        // already sent the user back to login.
        if (announcementAdapter != null) {
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

                    announcementsProgressBar.setVisibility(View.GONE);
                    announcementAdapter.setAnnouncements(announcements);

                    if (announcements.isEmpty()) {
                        showMessage(R.string.announcements_empty);
                    } else {
                        announcementsMessageTextView.setVisibility(View.GONE);
                        announcementsRecyclerView.setVisibility(View.VISIBLE);
                    }
                },
                exception -> {
                    announcementsProgressBar.setVisibility(View.GONE);
                    announcementAdapter.setAnnouncements(null);
                    showMessage(R.string.announcements_load_error);
                });
    }

    private void showLoadingState() {
        announcementsProgressBar.setVisibility(View.VISIBLE);
        announcementsRecyclerView.setVisibility(View.GONE);
        announcementsMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        announcementsRecyclerView.setVisibility(View.GONE);
        announcementsMessageTextView.setText(messageResId);
        announcementsMessageTextView.setVisibility(View.VISIBLE);
    }

    private void openAddAnnouncementForm() {
        startActivity(new Intent(AnnouncementsActivity.this, AddEditAnnouncementActivity.class));
    }

    @Override
    public void onEditAnnouncement(Announcement announcement) {

        // Only the announcement id travels through the Intent
        Intent intent = new Intent(AnnouncementsActivity.this, AddEditAnnouncementActivity.class);
        intent.putExtra(
                AddEditAnnouncementActivity.EXTRA_ANNOUNCEMENT_ID,
                announcement.getAnnouncementId()
        );
        startActivity(intent);
    }

    @Override
    public void onDeleteAnnouncement(Announcement announcement) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_announcement_title)
                .setMessage(R.string.dialog_delete_announcement_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(
                        R.string.btn_delete,
                        (dialog, which) -> deleteAnnouncement(announcement))
                .show();
    }

    /**
     * Deletes the announcement once the user has confirmed, then reloads the
     * list.
     */
    private void deleteAnnouncement(Announcement announcement) {

        showLoadingState();

        announcementRepository.deleteAnnouncement(announcement.getAnnouncementId(), task -> {

            if (task.isSuccessful()) {
                Toast.makeText(
                        AnnouncementsActivity.this,
                        R.string.msg_announcement_deleted,
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        AnnouncementsActivity.this,
                        R.string.err_announcement_delete_failed,
                        Toast.LENGTH_LONG
                ).show();
            }

            loadAnnouncements();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(AnnouncementsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

