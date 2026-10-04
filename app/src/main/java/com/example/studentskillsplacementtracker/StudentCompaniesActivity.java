package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentskillsplacementtracker.data.CompanyRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Read only company list shown to students so they can pick a company and check
 * their eligibility (FR-ELG-01).
 *
 * Companies are shared documents in the top level companies collection. This
 * screen offers no add, edit or delete controls - those belong to the
 * coordinator screens. All access goes through CompanyRepository.
 */
public class StudentCompaniesActivity extends AppCompatActivity
        implements StudentCompanyAdapter.OnCompanySelectedListener {

    private FirebaseAuth firebaseAuth;
    private CompanyRepository companyRepository;

    private RecyclerView studentCompaniesRecyclerView;
    private StudentCompanyAdapter studentCompanyAdapter;
    private ProgressBar studentCompaniesProgressBar;
    private TextView studentCompaniesMessageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();
        companyRepository = new CompanyRepository();

        // Navigation guard: this screen must never be shown without a session
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_student_companies);

        // Connect XML components with Java
        studentCompaniesRecyclerView = findViewById(R.id.studentCompaniesRecyclerView);
        studentCompaniesProgressBar = findViewById(R.id.studentCompaniesProgressBar);
        studentCompaniesMessageTextView = findViewById(R.id.studentCompaniesMessageTextView);

        studentCompanyAdapter = new StudentCompanyAdapter(this);
        studentCompaniesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentCompaniesRecyclerView.setAdapter(studentCompanyAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload every time the screen becomes visible again. The adapter is
        // null when the session guard already sent the user back to login.
        if (studentCompanyAdapter != null) {
            loadCompanies();
        }
    }

    @Override
    public void onCompanySelected(String companyId) {

        // Only the company id travels through the Intent
        Intent intent = new Intent(StudentCompaniesActivity.this, EligibilityActivity.class);
        intent.putExtra(EligibilityActivity.EXTRA_COMPANY_ID, companyId);
        startActivity(intent);
    }

    /**
     * Loads every company placement requirement.
     */
    private void loadCompanies() {

        showLoadingState();

        companyRepository.getCompanies(
                companies -> {

                    studentCompaniesProgressBar.setVisibility(View.GONE);
                    studentCompanyAdapter.setCompanies(companies);

                    if (companies.isEmpty()) {
                        showMessage(R.string.student_companies_empty);
                    } else {
                        studentCompaniesMessageTextView.setVisibility(View.GONE);
                        studentCompaniesRecyclerView.setVisibility(View.VISIBLE);
                    }
                },
                exception -> {
                    studentCompaniesProgressBar.setVisibility(View.GONE);
                    studentCompanyAdapter.setCompanies(null);
                    showMessage(R.string.student_companies_load_error);
                });
    }

    private void showLoadingState() {
        studentCompaniesProgressBar.setVisibility(View.VISIBLE);
        studentCompaniesRecyclerView.setVisibility(View.GONE);
        studentCompaniesMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        studentCompaniesRecyclerView.setVisibility(View.GONE);
        studentCompaniesMessageTextView.setText(messageResId);
        studentCompaniesMessageTextView.setVisibility(View.VISIBLE);
    }

    private void goToLogin() {
        Intent intent = new Intent(StudentCompaniesActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
