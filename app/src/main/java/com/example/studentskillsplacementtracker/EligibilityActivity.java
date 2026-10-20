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
import java.util.concurrent.atomic.AtomicInteger;

public class EligibilityActivity extends AppCompatActivity {

    public static final String EXTRA_COMPANY_ID = "extra_company_id";

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

    private String uid;
    private String companyId;

    private Company company;
    private Student student;
    private List<Skill> skills;
    private List<Project> projects;

    private AtomicInteger readsRemaining = new AtomicInteger(0);
    private volatile boolean readFailed;
    private long currentLoadId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            goToLogin();
            return;
        }

        uid = currentUser.getUid();
        companyId = getIntent().getStringExtra(EXTRA_COMPANY_ID);

        companyRepository = new CompanyRepository();
        studentRepository = new StudentRepository();
        skillRepository = new SkillRepository();
        projectRepository = new ProjectRepository();

        setContentView(R.layout.activity_eligibility);

        eligibilityCompanyNameTextView =
                findViewById(R.id.eligibilityCompanyNameTextView);
        eligibilityCompanyRoleTextView =
                findViewById(R.id.eligibilityCompanyRoleTextView);
        eligibilityProgressBar = findViewById(R.id.eligibilityProgressBar);
        eligibilityMessageTextView =
                findViewById(R.id.eligibilityMessageTextView);
        eligibilityResultsContainer =
                findViewById(R.id.eligibilityResultsContainer);
        eligibilityResultTextView =
                findViewById(R.id.eligibilityResultTextView);
        eligibilityMatchedTextView =
                findViewById(R.id.eligibilityMatchedTextView);
        eligibilityMissingTextView =
                findViewById(R.id.eligibilityMissingTextView);

        if (companyId == null || companyId.isEmpty()) {
            showMessage(R.string.eligibility_company_missing);
            return;
        }

        loadEligibility();
    }

    private void loadEligibility() {
        showLoadingState();

        final long loadId = ++currentLoadId;
        final AtomicInteger remaining = new AtomicInteger(REQUIRED_READS);

        readsRemaining = remaining;
        readFailed = false;

        company = null;
        student = null;
        skills = null;
        projects = null;

        companyRepository.getCompany(
                companyId,
                value -> {
                    if (!isCurrentLoad(loadId)) {
                        return;
                    }

                    company = value;
                    onReadComplete(loadId, remaining);
                },
                exception -> onReadFailed(loadId)
        );

        studentRepository.getProfile(
                uid,
                value -> {
                    if (!isCurrentLoad(loadId)) {
                        return;
                    }

                    student = value;
                    onReadComplete(loadId, remaining);
                },
                exception -> onReadFailed(loadId)
        );

        skillRepository.listSkills(
                uid,
                value -> {
                    if (!isCurrentLoad(loadId)) {
                        return;
                    }

                    skills = value;
                    onReadComplete(loadId, remaining);
                },
                exception -> onReadFailed(loadId)
        );

        projectRepository.getProjects(
                uid,
                value -> {
                    if (!isCurrentLoad(loadId)) {
                        return;
                    }

                    projects = value;
                    onReadComplete(loadId, remaining);
                },
                exception -> onReadFailed(loadId)
        );
    }

    private boolean isCurrentLoad(long loadId) {
        return loadId == currentLoadId
                && !isFinishing()
                && !isDestroyed();
    }

    private void onReadComplete(
            long loadId,
            AtomicInteger remaining) {

        if (!isCurrentLoad(loadId) || readFailed) {
            return;
        }

        int pending = remaining.decrementAndGet();

        if (pending == 0) {
            evaluateAndRender();
        }
    }

    private void onReadFailed(long loadId) {
        if (!isCurrentLoad(loadId) || readFailed) {
            return;
        }

        readFailed = true;
        showMessage(R.string.eligibility_load_error);
    }

    private void evaluateAndRender() {
        if (company == null) {
            showMessage(R.string.eligibility_company_missing);
            return;
        }

        if (student == null) {
            showMessage(R.string.eligibility_profile_missing);
            return;
        }

        List<Skill> studentSkills =
                skills == null ? new ArrayList<>() : skills;

        List<Project> studentProjects =
                projects == null ? new ArrayList<>() : projects;

        EligibilityResult result = EligibilityEvaluator.evaluate(
                company,
                student,
                studentSkills,
                studentProjects.size()
        );

        bindResult(result);
    }

    private void bindResult(EligibilityResult result) {
        eligibilityProgressBar.setVisibility(View.GONE);
        eligibilityMessageTextView.setVisibility(View.GONE);
        eligibilityResultsContainer.setVisibility(View.VISIBLE);

        eligibilityCompanyNameTextView.setText(company.getCompanyName());

        eligibilityCompanyRoleTextView.setText(
                getString(
                        R.string.eligibility_company_role,
                        safe(company.getRole())
                )
        );

        eligibilityResultTextView.setText(
                result.isEligible()
                        ? R.string.eligibility_result_eligible
                        : R.string.eligibility_result_not_eligible
        );

        eligibilityMatchedTextView.setText(
                buildRequirementText(result, true)
        );

        eligibilityMissingTextView.setText(
                buildRequirementText(result, false)
        );
    }

    private String buildRequirementText(
            EligibilityResult result,
            boolean matchedSection) {

        List<String> lines = new ArrayList<>();

        if (result.isCgpaMet() == matchedSection) {
            lines.add(getString(
                    R.string.eligibility_item_cgpa,
                    formatCgpa(result.getRequiredCgpa()),
                    formatCgpa(result.getStudentCgpa())
            ));
        }

        List<String> sectionSkills = matchedSection
                ? result.getMatchedSkills()
                : result.getMissingSkills();

        for (String skill : sectionSkills) {
            lines.add(getString(
                    R.string.eligibility_item_skill,
                    skill
            ));
        }

        if (result.isProjectsMet() == matchedSection) {
            lines.add(getString(
                    R.string.eligibility_item_projects,
                    result.getRequiredProjectCount(),
                    result.getStudentProjectCount()
            ));
        }

        if (lines.isEmpty()) {
            return getString(
                    matchedSection
                            ? R.string.eligibility_none_matched
                            : R.string.eligibility_none_missing
            );
        }

        return TextUtils.join("\n", lines);
    }

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
        Intent intent = new Intent(
                EligibilityActivity.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}