package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.CompanyRepository;
import com.example.studentskillsplacementtracker.model.Company;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Add / edit form for a single company placement requirement.
 *
 * The form works in "edit" mode when a company id is supplied as an Intent
 * extra; only that id is ever read from the Intent. Companies are shared
 * documents, so no uid is involved here.
 */
public class AddEditCompanyActivity extends AppCompatActivity {

    /** Extra carrying the id of the company being edited. */
    public static final String EXTRA_COMPANY_ID = "extra_company_id";

    private FirebaseAuth firebaseAuth;
    private CompanyRepository companyRepository;

    private TextView companyFormTitleTextView;
    private EditText companyNameEditText;
    private EditText companyRoleEditText;
    private EditText minimumCgpaEditText;
    private EditText requiredSkillsEditText;
    private EditText minimumProjectsEditText;
    private EditText locationEditText;
    private ProgressBar companyFormProgressBar;
    private Button saveCompanyButton;
    private Button cancelCompanyButton;

    /** Id of the company being edited, or {@code null} when adding. */
    private String companyId;

    // Guards against duplicate save requests
    private boolean isSaving = false;

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

        // Only the company id may travel through the Intent
        companyId = getIntent().getStringExtra(EXTRA_COMPANY_ID);

        setContentView(R.layout.activity_add_edit_company);

        // Connect XML components with Java
        companyFormTitleTextView = findViewById(R.id.companyFormTitleTextView);
        companyNameEditText = findViewById(R.id.companyNameEditText);
        companyRoleEditText = findViewById(R.id.companyRoleEditText);
        minimumCgpaEditText = findViewById(R.id.minimumCgpaEditText);
        requiredSkillsEditText = findViewById(R.id.requiredSkillsEditText);
        minimumProjectsEditText = findViewById(R.id.minimumProjectsEditText);
        locationEditText = findViewById(R.id.locationEditText);
        companyFormProgressBar = findViewById(R.id.companyFormProgressBar);
        saveCompanyButton = findViewById(R.id.saveCompanyButton);
        cancelCompanyButton = findViewById(R.id.cancelCompanyButton);

        saveCompanyButton.setOnClickListener(view -> saveCompany());
        cancelCompanyButton.setOnClickListener(view -> finish());

        if (isEditMode()) {
            companyFormTitleTextView.setText(R.string.company_edit_title);
            loadCompany();
        } else {
            companyFormTitleTextView.setText(R.string.company_add_title);
        }
    }

    private boolean isEditMode() {
        return companyId != null && !companyId.isEmpty();
    }

    /**
     * Loads the company being edited and fills the form.
     */
    private void loadCompany() {

        setLoadingState(true);

        companyRepository.getCompany(companyId,
                company -> {
                    setLoadingState(false);

                    if (company == null) {
                        Toast.makeText(
                                AddEditCompanyActivity.this,
                                R.string.err_company_load_failed,
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    } else {
                        bindCompany(company);
                    }
                },
                exception -> {
                    setLoadingState(false);
                    Toast.makeText(
                            AddEditCompanyActivity.this,
                            R.string.err_company_load_failed,
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
    }

    private void bindCompany(Company company) {

        companyNameEditText.setText(company.getCompanyName());
        companyRoleEditText.setText(company.getRole());
        minimumCgpaEditText.setText(String.valueOf(company.getMinimumCGPA()));
        requiredSkillsEditText.setText(joinRequiredSkills(company.getRequiredSkills()));
        minimumProjectsEditText.setText(String.valueOf(company.getMinimumProjects()));
        locationEditText.setText(company.getLocation());
    }

    /**
     * Parses the comma separated skills field into a list, trimming each entry
     * and dropping blanks. Never returns {@code null}.
     */
    private List<String> parseRequiredSkills(String rawSkills) {

        List<String> skills = new ArrayList<>();

        if (rawSkills == null) {
            return skills;
        }

        String[] parts = rawSkills.split(",");

        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                skills.add(value);
            }
        }

        return skills;
    }

    /**
     * Converts the stored required skills back into comma separated text for the
     * form field so they can be edited.
     */
    private String joinRequiredSkills(List<String> skills) {

        if (skills == null || skills.isEmpty()) {
            return "";
        }

        return TextUtils.join(", ", skills);
    }

    private void saveCompany() {

        if (isSaving) {
            return;
        }

        if (!isFormValid()) {
            return;
        }

        // isFormValid() guarantees both numeric fields parse and are in range.
        String companyName = companyNameEditText.getText().toString().trim();
        String role = companyRoleEditText.getText().toString().trim();
        double minimumCgpa = Double.parseDouble(
                minimumCgpaEditText.getText().toString().trim());
        List<String> requiredSkills = parseRequiredSkills(
                requiredSkillsEditText.getText().toString());
        int minimumProjects = Integer.parseInt(
                minimumProjectsEditText.getText().toString().trim());
        String location = locationEditText.getText().toString().trim();

        Company company = new Company(
                companyId,
                companyName,
                role,
                minimumCgpa,
                requiredSkills,
                minimumProjects,
                location
        );

        setLoadingState(true);

        if (isEditMode()) {
            updateExistingCompany(company);
        } else {
            addNewCompany(company);
        }
    }

    /**
     * Creates a new company. The repository generates the document id and stores
     * it as {@code companyId}.
     */
    private void addNewCompany(Company company) {

        companyRepository.addCompany(company, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditCompanyActivity.this,
                        R.string.msg_company_added,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditCompanyActivity.this,
                        R.string.err_company_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Updates the existing company. {@code companyId} and {@code createdAt} are
     * preserved.
     */
    private void updateExistingCompany(Company company) {

        companyRepository.updateCompany(company, task -> {

            setLoadingState(false);

            if (task.isSuccessful()) {
                Toast.makeText(
                        AddEditCompanyActivity.this,
                        R.string.msg_company_updated,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            } else {
                Toast.makeText(
                        AddEditCompanyActivity.this,
                        R.string.err_company_save_failed,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    /**
     * Required: company name, job role, minimum CGPA (0 - 10) and minimum
     * projects (0 or more). Optional: required skills and location.
     */
    private boolean isFormValid() {

        String companyName = companyNameEditText.getText().toString().trim();
        String role = companyRoleEditText.getText().toString().trim();
        String minimumCgpa = minimumCgpaEditText.getText().toString().trim();
        String minimumProjects = minimumProjectsEditText.getText().toString().trim();

        // Company name
        if (companyName.isEmpty()) {
            return showFieldError(companyNameEditText, R.string.err_company_name_required);
        }

        // Job role
        if (role.isEmpty()) {
            return showFieldError(companyRoleEditText, R.string.err_company_role_required);
        }

        // Minimum CGPA
        if (minimumCgpa.isEmpty()) {
            return showFieldError(minimumCgpaEditText, R.string.err_company_min_cgpa_required);
        }
        if (!isMinimumCgpaValid(minimumCgpa)) {
            return showFieldError(minimumCgpaEditText, R.string.err_company_min_cgpa_invalid);
        }

        // Minimum projects
        if (minimumProjects.isEmpty()) {
            return showFieldError(
                    minimumProjectsEditText,
                    R.string.err_company_min_projects_required
            );
        }
        if (!isMinimumProjectsValid(minimumProjects)) {
            return showFieldError(
                    minimumProjectsEditText,
                    R.string.err_company_min_projects_invalid
            );
        }

        return true;
    }

    private boolean showFieldError(EditText field, int messageResId) {
        field.setError(getString(messageResId));
        field.requestFocus();
        return false;
    }

    /**
     * A minimum CGPA is valid when it is a number between 0 and 10.
     */
    private boolean isMinimumCgpaValid(String rawCgpa) {
        try {
            double value = Double.parseDouble(rawCgpa);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return false;
            }
            return value >= Company.MIN_CGPA_LOWER_BOUND
                    && value <= Company.MIN_CGPA_UPPER_BOUND;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * A minimum project count is valid when it is a whole number of 0 or more.
     */
    private boolean isMinimumProjectsValid(String rawProjects) {
        try {
            int value = Integer.parseInt(rawProjects);
            return value >= Company.MIN_PROJECTS_LOWER_BOUND;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void setLoadingState(boolean loading) {
        isSaving = loading;
        companyFormProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveCompanyButton.setEnabled(!loading);
        cancelCompanyButton.setEnabled(!loading);
        companyNameEditText.setEnabled(!loading);
        companyRoleEditText.setEnabled(!loading);
        minimumCgpaEditText.setEnabled(!loading);
        requiredSkillsEditText.setEnabled(!loading);
        minimumProjectsEditText.setEnabled(!loading);
        locationEditText.setEnabled(!loading);
    }

    private void goToLogin() {
        Intent intent = new Intent(AddEditCompanyActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
