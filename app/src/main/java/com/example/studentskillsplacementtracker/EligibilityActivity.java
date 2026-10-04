package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.CompanyRepository;
import com.example.studentskillsplacementtracker.data.ProjectRepository;
import com.example.studentskillsplacementtracker.data.SkillRepository;
import com.example.studentskillsplacementtracker.data.StudentRepository;
import com.example.studentskillsplacementtracker.model.Company;
import com.example.studentskillsplacementtracker.model.EligibilityResult;
import com.example.studentskillsplacementtracker.model.Project;
import com.example.studentskillsplacementtracker.model.Skill;
import com.example.studentskillsplacementtracker.model.Student;
import com.example.studentskillsplacementtracker.util.EligibilityEvaluator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Eligibility result screen for one company (FR-ELG-01).
 *
 * Reads the company requirements and the student's own profile, skills and
 * projects, then hands them to {@link EligibilityEvaluator}. The comparison
 * itself lives in that pure utility class; this screen only loads data and
 * renders the outcome.
 */
public class EligibilityActivity extends AppCompatActivity {

    /** Extra carrying the id of the company being checked. */
    public static final String EXTRA_COMPANY_ID = "extra_company_id";

    /** Independent reads required before the comparison can run. */
    private static final int REQUIRED_READS = 4;

    private FirebaseAuth firebaseAuth;
    private CompanyRepository companyRepository;
    private StudentRepository studentRepository;
    private SkillRepository skillRepository;
    private ProjectRepository projectRepository;

    private TextView eligibilityCompanyNameTextView;
    private TextView eligibilityCompanyRoleTextView;
    private ProgressBar eligibilityProgressBar;
    private TextView eligibilityMessageTextView;
    private LinearLayout eligibilityResultsContainer;
    private TextView eligibilityResultTextView;
    private TextView eligibilityMatchedTextView;
    private TextView eligibilityMissingTextView;

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

    /** Id of the company being checked, read from the Intent. */
    private String companyId;

    // Values collected from the four reads before evaluating
    private Company company;
    private Student student;
    private List<Skill> skills;
    private List<Project> projects;

    private int readsRemaining;
    private boolean readFailed;

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

        // Ownership always comes from the authenticated user, never an Intent
        uid = currentUser.getUid();

        // Only the company id travels through the Intent
        companyId = getIntent().getStringExtra(EXTRA_COMPANY_ID);

        companyRepository = new CompanyRepository();
        studentRepository = new StudentRepository();
        skillRepository = new SkillRepository();
        projectRepository = new ProjectRepository();

        setContentView(R.layout.activity_eligibility);

        // Connect XML components with Java
        eligibilityCompanyNameTextView = findViewById(R.id.eligibilityCompanyNameTextView);
        eligibilityCompanyRoleTextView = findViewById(R.id.eligibilityCompanyRoleTextView);
        eligibilityProgressBar = findViewById(R.id.eligibilityProgressBar);
        eligibilityMessageTextView = findViewById(R.id.eligibilityMessageTextView);
        eligibilityResultsContainer = findViewById(R.id.eligibilityResultsContainer);
        eligibilityResultTextView = findViewById(R.id.eligibilityResultTextView);
        eligibilityMatchedTextView = findViewById(R.id.eligibilityMatchedTextView);
        eligibilityMissingTextView = findViewById(R.id.eligibilityMissingTextView);

        if (companyId == null || companyId.isEmpty()) {
            showMessage(R.string.eligibility_company_missing);
            return;
        }

        loadEligibility();
    }

    /**
     * Starts the four independent reads. They run in parallel and the
     * comparison only runs once all four have completed successfully.
     */
    private void loadEligibility() {

        showLoadingState();

        readsRemaining = REQUIRED_READS;
        readFailed = false;
        company = null;
        student = null;
        skills = null;
        projects = null;

        companyRepository.getCompany(companyId,
                value -> {
                    company = value;
                    onReadComplete();
                },
                exception -> onReadFailed());

        studentRepository.getProfile(uid,
                value -> {
                    student = value;
                    onReadComplete();
                },
                exception -> onReadFailed());

        skillRepository.listSkills(uid,
                value -> {
                    skills = value;
                    onReadComplete();
                },
                exception -> onReadFailed());

        projectRepository.getProjects(uid,
                value -> {
                    projects = value;
                    onReadComplete();
                },
                exception -> onReadFailed());
    }

    /**
     * Counts down the pending reads and evaluates once they are all in.
     */
    private void onReadComplete() {

        if (readFailed) {
            return;
        }

        readsRemaining--;

        if (readsRemaining > 0) {
            return;
        }

        evaluateAndRender();
    }

    /**
     * A failed read must never be reported as "Not Eligible", so the first
     * failure stops the check and reports a load error instead.
     */
    private void onReadFailed() {

        if (readFailed) {
            return;
        }

        readFailed = true;
        showMessage(R.string.eligibility_load_error);
    }

    /**
     * Runs the deterministic comparison and renders the outcome. Missing data is
     * reported as a data error rather than a negative result.
     */
    private void evaluateAndRender() {

        if (company == null) {
            showMessage(R.string.eligibility_company_missing);
            return;
        }

        if (student == null) {
            showMessage(R.string.eligibility_profile_missing);
            return;
        }

        List<Skill> studentSkills = (skills == null) ? new ArrayList<>() : skills;
        List<Project> studentProjects = (projects == null) ? new ArrayList<>() : projects;

        EligibilityResult result = EligibilityEvaluator.evaluate(
                company,
                student,
                studentSkills,
                studentProjects.size()
        );

        bindResult(result);
    }

    /**
     * Renders the comparison outcome.
     */
    private void bindResult(EligibilityResult result) {

        eligibilityProgressBar.setVisibility(View.GONE);
        eligibilityMessageTextView.setVisibility(View.GONE);
        eligibilityResultsContainer.setVisibility(View.VISIBLE);

        eligibilityCompanyNameTextView.setText(company.getCompanyName());
        eligibilityCompanyRoleTextView.setText(
                getString(R.string.eligibility_company_role, safe(company.getRole()))
        );

        eligibilityResultTextView.setText(result.isEligible()
                ? R.string.eligibility_result_eligible
                : R.string.eligibility_result_not_eligible);

        eligibilityMatchedTextView.setText(buildRequirementText(result, true));
        eligibilityMissingTextView.setText(buildRequirementText(result, false));
    }

    /**
     * Builds the bullet list for one section. The CGPA line, every skill and the
     * project line appear either as matched or as missing depending on the
     * result, so the same formatting is reused for both sections.
     */
    private String buildRequirementText(EligibilityResult result, boolean matchedSection) {

        List<String> lines = new ArrayList<>();

        // CGPA
        if (result.isCgpaMet() == matchedSection) {
            lines.add(getString(
                    R.string.eligibility_item_cgpa,
                    formatCgpa(result.getRequiredCgpa()),
                    formatCgpa(result.getStudentCgpa())
            ));
        }

        // Skills
        List<String> sectionSkills = matchedSection
                ? result.getMatchedSkills()
                : result.getMissingSkills();

        for (String skill : sectionSkills) {
            lines.add(getString(R.string.eligibility_item_skill, skill));
        }

        // Projects
        if (result.isProjectsMet() == matchedSection) {
            lines.add(getString(
                    R.string.eligibility_item_projects,
                    result.getRequiredProjectCount(),
                    result.getStudentProjectCount()
            ));
        }

        if (lines.isEmpty()) {
            return getString(matchedSection
                    ? R.string.eligibility_none_matched
                    : R.string.eligibility_none_missing);
        }

        return TextUtils.join("\n", lines);
    }

    /**
     * Formats a CGPA with two decimal places for display.
     */
    private String formatCgpa(double value) {
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showLoadingState() {
        eligibilityProgressBar.setVisibility(View.VISIBLE);
        eligibilityResultsContainer.setVisibility(View.GONE);
        eligibilityMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        eligibilityProgressBar.setVisibility(View.GONE);
        eligibilityResultsContainer.setVisibility(View.GONE);
        eligibilityMessageTextView.setText(messageResId);
        eligibilityMessageTextView.setVisibility(View.VISIBLE);
    }

    private void goToLogin() {
        Intent intent = new Intent(EligibilityActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
