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

import com.example.studentskillsplacementtracker.data.CertificationRepository;
import com.example.studentskillsplacementtracker.model.Certification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Certifications list screen.
 *
 * Lists the certifications of the authenticated student and offers add, edit
 * and delete. Certifications always live under the authenticated user's uid;
 * the uid is never taken from an Intent. This class contains no Firestore code
 * of its own - all access goes through {@link CertificationRepository}.
 */
public class CertificationsActivity extends AppCompatActivity
        implements CertificationAdapter.OnCertificationActionListener {

    private FirebaseAuth firebaseAuth;
    private CertificationRepository certificationRepository;

    private RecyclerView certificationsRecyclerView;
    private CertificationAdapter certificationAdapter;
    private ProgressBar certificationsProgressBar;
    private TextView certificationsMessageTextView;
    private Button addCertificationButton;

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

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

        setContentView(R.layout.activity_certifications);

        // Connect XML components with Java
        certificationsRecyclerView = findViewById(R.id.certificationsRecyclerView);
        certificationsProgressBar = findViewById(R.id.certificationsProgressBar);
        certificationsMessageTextView = findViewById(R.id.certificationsMessageTextView);
        addCertificationButton = findViewById(R.id.addCertificationButton);

        certificationAdapter = new CertificationAdapter(this);
        certificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        certificationsRecyclerView.setAdapter(certificationAdapter);

        addCertificationButton.setOnClickListener(view -> openAddCertificationForm());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload every time the screen becomes visible again, e.g. after the
        // add / edit form finishes.
        if (uid != null) {
            loadCertifications();
        }
    }

    /**
     * Loads the authenticated student's certifications.
     */
    private void loadCertifications() {

        showLoadingState();

        certificationRepository.getCertifications(uid,
                certifications -> {

                    certificationsProgressBar.setVisibility(View.GONE);
                    certificationAdapter.setCertifications(certifications);

                    if (certifications.isEmpty()) {
                        showMessage(R.string.certifications_empty);
                    } else {
                        certificationsMessageTextView.setVisibility(View.GONE);
                        certificationsRecyclerView.setVisibility(View.VISIBLE);
                    }
                },
                exception -> {
                    certificationsProgressBar.setVisibility(View.GONE);
                    certificationAdapter.setCertifications(null);
                    showMessage(R.string.certifications_load_error);
                });
    }

    private void showLoadingState() {
        certificationsProgressBar.setVisibility(View.VISIBLE);
        certificationsRecyclerView.setVisibility(View.GONE);
        certificationsMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        certificationsRecyclerView.setVisibility(View.GONE);
        certificationsMessageTextView.setText(messageResId);
        certificationsMessageTextView.setVisibility(View.VISIBLE);
    }

    private void openAddCertificationForm() {
        startActivity(new Intent(CertificationsActivity.this, AddEditCertificationActivity.class));
    }

    @Override
    public void onEditCertification(Certification certification) {

        // Only the certification id travels through the Intent
        Intent intent = new Intent(CertificationsActivity.this, AddEditCertificationActivity.class);
        intent.putExtra(
                AddEditCertificationActivity.EXTRA_CERTIFICATION_ID,
                certification.getCertificationId()
        );
        startActivity(intent);
    }

    @Override
    public void onDeleteCertification(Certification certification) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_certification_title)
                .setMessage(R.string.dialog_delete_certification_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(
                        R.string.btn_delete,
                        (dialog, which) -> deleteCertification(certification))
                .show();
    }

    /**
     * Deletes the certification once the user has confirmed, then reloads the
     * list.
     */
    private void deleteCertification(Certification certification) {

        showLoadingState();

        certificationRepository.deleteCertification(
                uid,
                certification.getCertificationId(),
                task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                CertificationsActivity.this,
                                R.string.msg_certification_deleted,
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(
                                CertificationsActivity.this,
                                R.string.err_certification_delete_failed,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    loadCertifications();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(CertificationsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
