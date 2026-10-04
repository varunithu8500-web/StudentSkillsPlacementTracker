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

import com.example.studentskillsplacementtracker.data.CertificationRepository;
import com.example.studentskillsplacementtracker.model.Certification;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

/**
 * Add / edit form for a single certification.
 *
 * The form works in "edit" mode when a certification id is supplied as an
 * Intent extra. Only the certification id is ever read from the Intent: the
 * owning uid always comes from the authenticated Firebase user, never from the
 * UI layer.
 */
public class AddEditCertificationActivity extends AppCompatActivity {

    /** Extra carrying the id of the certification being edited. */
    public static final String EXTRA_CERTIFICATION_ID = "extra_certification_id";

    private FirebaseAuth firebaseAuth;
    private CertificationRepository certificationRepository;

    private TextView certificationFormTitleTextView;
    private EditText certificationNameEditText;
    private EditText organizationEditText;
    private EditText dateEditText;
    private EditText credentialEditText;
    private ProgressBar certificationFormProgressBar;
    private Button saveCertificationButton;
    private Button cancelCertificationButton;

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

    /** Id of the certification being edited, or {@code null} when adding. */
    private String certificationId;

    /** Date picked by the user, or {@code null} when none has been picked yet. */
    private Timestamp selectedDate;

    // Guards against duplicate save requests
    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();
        certificationRepository = new CertificationRepository();

        // Navigation guard: this screen must never be shown without a session
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        // Ownership always comes from the authenticated user, never an Intent
        uid = currentUser.getUid();

        // Only the certification id may travel through the Intent
        certificationId = getIntent().getStringExtra(EXTRA_CERTIFICATION_ID);

        setContentView(R.layout.activity_add_edit_certification);

        // Connect XML components with Java
        certificationFormTitleTextView = findViewById(R.id.certificationFormTitleTextView);
        certificationNameEditText = findViewById(R.id.certificationNameEditText);
        organizationEditText = findViewById(R.id.organizationEditText);
        dateEditText = findViewById(R.id.dateEditText);
        credentialEditText = findViewById(R.id.credentialEditText);
        certificationFormProgressBar = findViewById(R.id.certificationFormProgressBar);
        saveCertificationButton = findViewById(R.id.saveCertificationButton);
        cancelCertificationButton = findViewById(R.id.cancelCertificationButton);

        saveCertificationButton.setOnClickListener(view -> saveCertification());
        cancelCertificationButton.setOnClickListener(view -> finish());

        // The date field is not editable by typing; it opens a date picker.
        dateEditText.setOnClickListener(view -> showDatePicker());

        if (isEditMode()) {
            certificationFormTitleTextView.setText(R.string.certification_edit_title);
            loadCertification();
        } else {
            certificationFormTitleTextView.setText(R.string.certification_add_title);
        }
    }

    private boolean isEditMode() {
        return certificationId != null && !certificationId.isEmpty();
    }

    /**
     * Loads the certification being edited and fills the form.
     */
    private void loadCertification() {

        setLoadingState(true);

        certificationRepository.getCertification(uid, certificationId,
                certification -> {
                    setLoadingState(false);

                    if (certification == null) {
                        Toast.makeText(
                                AddEditCertificationActivity.this,
                                R.string.err_certification_load_failed,
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    } else {
                        bindCertification(certification);
                    }
                },
                exception -> {
                    setLoadingState(false);
                    Toast.makeText(
                            AddEditCertificationActivity.this,
                            R.string.err_certification_load_failed,
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    private void bindCertification(Certification certification) {

        certificationNameEditText.setText(certification.getCertificationName());
        organizationEditText.setText(certification.getOrganization());
        credentialEditText.setText(certification.getCredential());

        selectedDate = certification.getDate();
        dateEditText.setText(Certification.formatTimestamp(selectedDate));
    }

    /**
     * Opens a date picker for the certification date. The picker starts on the
     * already selected date, or on today when nothing has been picked yet.
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

        selectedDate = Certification.toTimestamp(year, monthOfYear, dayOfMonth);
        dateEditText.setText(Certification.formatTimestamp(selectedDate));
    }

    private void saveCertification() {

        if (isSaving) {
            return;
        }

        if (!isFormValid()) {
            return;
        }

        // Trimmed text fields; the date comes from the date picker.
        String certificationName = certificationNameEditText.getText().toString().trim();
        String organization = organizationEditText.getText().toString().trim();
        String credential = credentialEditText.getText().toString().trim();

        Certification certification = new Certification(
                certificationId,
                uid,
                certificationName,
                organization,
                selectedDate,
                credential
        );

        setLoadingState(true);

        if (isEditMode()) {
            updateExistingCertification(certification);
        } else {
            addNewCertification(certification);
        }
    }

    /**
     * Creates a new certification. The repository generates the document id and
     * stores it as {@code certificationId}; the authenticated uid is stored as
     * {@code userId}.
     */
    private void addNewCertification(Certification certification) {

        certificationRepository.addCertification(uid, certification, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditCertificationActivity.this,
                        R.string.msg_certification_added,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditCertificationActivity.this,
                        R.string.err_certification_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Updates the existing certification. {@code certificationId},
     * {@code userId} and {@code createdAt} are preserved.
     */
    private void updateExistingCertification(Certification certification) {

        certificationRepository.updateCertification(uid, certification, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditCertificationActivity.this,
                        R.string.msg_certification_updated,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditCertificationActivity.this,
                        R.string.err_certification_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Required: certification name, organization and a deliberately picked date.
     * Optional: credential, which is accepted as typed (it may be a credential
     * id or a verification URL).
     */
    private boolean isFormValid() {

        String certificationName = certificationNameEditText.getText().toString().trim();
        String organization = organizationEditText.getText().toString().trim();

        // Certification name
        if (certificationName.isEmpty()) {
            return showFieldError(
                    certificationNameEditText,
                    R.string.err_certification_name_required
            );
        }

        // Organization
        if (organization.isEmpty()) {
            return showFieldError(
                    organizationEditText,
                    R.string.err_certification_organization_required
            );
        }

        // Date - the field starts empty, so a null value means nothing was picked
        if (selectedDate == null) {
            return showFieldError(dateEditText, R.string.err_certification_date_required);
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
        certificationFormProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveCertificationButton.setEnabled(!loading);
        cancelCertificationButton.setEnabled(!loading);
        certificationNameEditText.setEnabled(!loading);
        organizationEditText.setEnabled(!loading);
        dateEditText.setEnabled(!loading);
        credentialEditText.setEnabled(!loading);
    }

    private void goToLogin() {
        Intent intent = new Intent(AddEditCertificationActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
