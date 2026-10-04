package com.example.studentskillsplacementtracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.AnnouncementRepository;
import com.example.studentskillsplacementtracker.model.Announcement;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

/**
 * Add / edit form for a single placement announcement (FR-ANN-01, FR-ADM-01).
 *
 * The form works in "edit" mode when an announcement id is supplied as an
 * Intent extra; only that id is ever read from the Intent. Announcements are
 * shared documents, so no uid is involved here.
 *
 * The date field cannot be typed into: it opens a date picker and the picked
 * day is stored as a local midnight timestamp, matching the certification form.
 */
public class AddEditAnnouncementActivity extends AppCompatActivity {

    /** Extra carrying the id of the announcement being edited. */
    public static final String EXTRA_ANNOUNCEMENT_ID = "extra_announcement_id";

    private FirebaseAuth firebaseAuth;
    private AnnouncementRepository announcementRepository;

    private TextView announcementFormTitleTextView;
    private EditText announcementTitleEditText;
    private EditText announcementDescriptionEditText;
    private EditText announcementCompanyEditText;
    private EditText announcementDateEditText;
    private ProgressBar announcementFormProgressBar;
    private Button saveAnnouncementButton;
    private Button cancelAnnouncementButton;

    /** Id of the announcement being edited, or {@code null} when publishing. */
    private String announcementId;

    /** Date picked by the user, or {@code null} when none has been picked yet. */
    private Timestamp selectedDate;

    // Guards against duplicate save requests
    private boolean isSaving = false;

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

        // Only the announcement id may travel through the Intent
        announcementId = getIntent().getStringExtra(EXTRA_ANNOUNCEMENT_ID);

        setContentView(R.layout.activity_add_edit_announcement);

        // Connect XML components with Java
        announcementFormTitleTextView = findViewById(R.id.announcementFormTitleTextView);
        announcementTitleEditText = findViewById(R.id.announcementTitleEditText);
        announcementDescriptionEditText = findViewById(R.id.announcementDescriptionEditText);
        announcementCompanyEditText = findViewById(R.id.announcementCompanyEditText);
        announcementDateEditText = findViewById(R.id.announcementDateEditText);
        announcementFormProgressBar = findViewById(R.id.announcementFormProgressBar);
        saveAnnouncementButton = findViewById(R.id.saveAnnouncementButton);
        cancelAnnouncementButton = findViewById(R.id.cancelAnnouncementButton);

        saveAnnouncementButton.setOnClickListener(view -> saveAnnouncement());
        cancelAnnouncementButton.setOnClickListener(view -> finish());

        // The date field is not editable by typing; it opens a date picker.
        announcementDateEditText.setOnClickListener(view -> showDatePicker());

        if (isEditMode()) {
            announcementFormTitleTextView.setText(R.string.announcement_edit_title);
            loadAnnouncement();
        } else {
            announcementFormTitleTextView.setText(R.string.announcement_add_title);
        }
    }

    private boolean isEditMode() {
        return announcementId != null && !announcementId.isEmpty();
    }

    /**
     * Loads the announcement being edited and fills the form. A deleted or
     * missing announcement closes the form instead of leaving it empty.
     */
    private void loadAnnouncement() {

        setLoadingState(true);

        announcementRepository.getAnnouncement(announcementId,
                announcement -> {
                    setLoadingState(false);

                    if (announcement == null) {
                        Toast.makeText(
                                AddEditAnnouncementActivity.this,
                                R.string.err_announcement_load_failed,
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    } else {
                        bindAnnouncement(announcement);
                    }
                },
                exception -> {
                    setLoadingState(false);
                    Toast.makeText(
                            AddEditAnnouncementActivity.this,
                            R.string.err_announcement_load_failed,
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    private void bindAnnouncement(Announcement announcement) {

        announcementTitleEditText.setText(announcement.getTitle());
        announcementDescriptionEditText.setText(announcement.getDescription());
        announcementCompanyEditText.setText(announcement.getCompanyName());

        selectedDate = announcement.getDate();
        announcementDateEditText.setText(Announcement.formatTimestamp(selectedDate));
    }

    /**
     * Opens a date picker for the announcement date. The picker starts on the
     * already selected date, or on today when nothing has been picked yet.
     * Future dates are allowed because announcements usually reference an
     * upcoming drive.
     */
    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        if (selectedDate != null) {
            calendar.setTime(selectedDate.toDate());
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, monthOfYear, dayOfMonth) ->
                        setSelectedDate(year, monthOfYear, dayOfMonth),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    /**
     * Stores the picked date as a local midnight timestamp and shows it in the
     * form field using the {@code dd MMM yyyy} pattern.
     */
    private void setSelectedDate(int year, int monthOfYear, int dayOfMonth) {

        selectedDate = Announcement.toTimestamp(year, monthOfYear, dayOfMonth);
        announcementDateEditText.setText(Announcement.formatTimestamp(selectedDate));
    }

    private void saveAnnouncement() {

        if (isSaving) {
            return;
        }

        if (!isFormValid()) {
            return;
        }

        // Trimmed text fields; the date comes from the date picker.
        String title = announcementTitleEditText.getText().toString().trim();
        String description = announcementDescriptionEditText.getText().toString().trim();
        String companyName = announcementCompanyEditText.getText().toString().trim();

        Announcement announcement = new Announcement(
                announcementId,
                title,
                description,
                companyName,
                selectedDate
        );

        setLoadingState(true);

        if (isEditMode()) {
            updateExistingAnnouncement(announcement);
        } else {
            addNewAnnouncement(announcement);
        }
    }

    /**
     * Publishes a new announcement. The repository generates the document id and
     * stores it as {@code announcementId}. No uid is stored because
     * announcements are shared placement notices.
     */
    private void addNewAnnouncement(Announcement announcement) {

        announcementRepository.addAnnouncement(announcement, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditAnnouncementActivity.this,
                        R.string.msg_announcement_added,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditAnnouncementActivity.this,
                        R.string.err_announcement_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Updates the existing announcement. {@code announcementId} and
     * {@code createdAt} are preserved.
     */
    private void updateExistingAnnouncement(Announcement announcement) {

        announcementRepository.updateAnnouncement(announcement, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditAnnouncementActivity.this,
                        R.string.msg_announcement_updated,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditAnnouncementActivity.this,
                        R.string.err_announcement_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Required: title, description, company / drive reference and a deliberately
     * picked date. There is no uniqueness check, so duplicate titles are
     * allowed, and future dates are accepted.
     */
    private boolean isFormValid() {

        String title = announcementTitleEditText.getText().toString().trim();
        String description = announcementDescriptionEditText.getText().toString().trim();
        String companyName = announcementCompanyEditText.getText().toString().trim();

        // Title
        if (title.isEmpty()) {
            return showFieldError(
                    announcementTitleEditText,
                    R.string.err_announcement_title_required
            );
        }

        // Description
        if (description.isEmpty()) {
            return showFieldError(
                    announcementDescriptionEditText,
                    R.string.err_announcement_description_required
            );
        }

        // Company / drive reference
        if (companyName.isEmpty()) {
            return showFieldError(
                    announcementCompanyEditText,
                    R.string.err_announcement_company_required
            );
        }

        // Date - the field starts empty, so a null value means nothing was picked
        if (selectedDate == null) {
            return showFieldError(
                    announcementDateEditText,
                    R.string.err_announcement_date_required
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
        announcementFormProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveAnnouncementButton.setEnabled(!loading);
        cancelAnnouncementButton.setEnabled(!loading);
        announcementTitleEditText.setEnabled(!loading);
        announcementDescriptionEditText.setEnabled(!loading);
        announcementCompanyEditText.setEnabled(!loading);
        announcementDateEditText.setEnabled(!loading);
    }

    private void goToLogin() {
        Intent intent = new Intent(AddEditAnnouncementActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


