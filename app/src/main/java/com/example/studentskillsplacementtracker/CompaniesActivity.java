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

import com.example.studentskillsplacementtracker.data.CompanyRepository;
import com.example.studentskillsplacementtracker.model.Company;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Company placement requirements list screen, used by coordinators.
 *
 * Companies are shared documents in the top level {@code companies} collection,
 * so no uid is involved. This class contains no Firestore code of its own - all
 * access goes through {@link CompanyRepository}.
 */
public class CompaniesActivity extends AppCompatActivity
        implements CompanyAdapter.OnCompanyActionListener {

    private FirebaseAuth firebaseAuth;
    private CompanyRepository companyRepository;

    private RecyclerView companiesRecyclerView;
    private CompanyAdapter companyAdapter;
    private ProgressBar companiesProgressBar;
    private TextView companiesMessageTextView;
    private Button addCompanyButton;

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

        setContentView(R.layout.activity_companies);

        // Connect XML components with Java
        companiesRecyclerView = findViewById(R.id.companiesRecyclerView);
        companiesProgressBar = findViewById(R.id.companiesProgressBar);
        companiesMessageTextView = findViewById(R.id.companiesMessageTextView);
        addCompanyButton = findViewById(R.id.addCompanyButton);

        companyAdapter = new CompanyAdapter(this);
        companiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        companiesRecyclerView.setAdapter(companyAdapter);

        addCompanyButton.setOnClickListener(view -> openAddCompanyForm());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload every time the screen becomes visible again, e.g. after the
        // add / edit form finishes. The adapter is null when the session guard
        // already sent the user back to login.
        if (companyAdapter != null) {
            loadCompanies();
        }
    }

    /**
     * Loads every company placement requirement.
     */
    private void loadCompanies() {

        showLoadingState();

        companyRepository.getCompanies(
                companies -> {

                    companiesProgressBar.setVisibility(View.GONE);
                    companyAdapter.setCompanies(companies);

                    if (companies.isEmpty()) {
                        showMessage(R.string.companies_empty);
                    } else {
                        companiesMessageTextView.setVisibility(View.GONE);
                        companiesRecyclerView.setVisibility(View.VISIBLE);
                    }
                },
                exception -> {
                    companiesProgressBar.setVisibility(View.GONE);
                    companyAdapter.setCompanies(null);
                    showMessage(R.string.companies_load_error);
                });
    }

    private void showLoadingState() {
        companiesProgressBar.setVisibility(View.VISIBLE);
        companiesRecyclerView.setVisibility(View.GONE);
        companiesMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        companiesRecyclerView.setVisibility(View.GONE);
        companiesMessageTextView.setText(messageResId);
        companiesMessageTextView.setVisibility(View.VISIBLE);
    }

    private void openAddCompanyForm() {
        startActivity(new Intent(CompaniesActivity.this, AddEditCompanyActivity.class));
    }

    @Override
    public void onEditCompany(Company company) {

        // Only the company id travels through the Intent
        Intent intent = new Intent(CompaniesActivity.this, AddEditCompanyActivity.class);
        intent.putExtra(AddEditCompanyActivity.EXTRA_COMPANY_ID, company.getCompanyId());
        startActivity(intent);
    }

    @Override
    public void onDeleteCompany(Company company) {

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_company_title)
                .setMessage(R.string.dialog_delete_company_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(
                        R.string.btn_delete,
                        (dialog, which) -> deleteCompany(company))
                .show();
    }

    /**
     * Deletes the company once the user has confirmed, then reloads the list.
     */
    private void deleteCompany(Company company) {

        showLoadingState();

        companyRepository.deleteCompany(company.getCompanyId(), task -> {

            if (task.isSuccessful()) {
                Toast.makeText(
                        CompaniesActivity.this,
                        R.string.msg_company_deleted,
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        CompaniesActivity.this,
                        R.string.err_company_delete_failed,
                        Toast.LENGTH_LONG
                ).show();
            }

            loadCompanies();
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(CompaniesActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
