package com.example.studentskillsplacementtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentskillsplacementtracker.data.CertificationRepository;
import com.example.studentskillsplacementtracker.data.CodingStatsRepository;
import com.example.studentskillsplacementtracker.data.CompanyRepository;
import com.example.studentskillsplacementtracker.data.ProjectRepository;
import com.example.studentskillsplacementtracker.data.SkillRepository;
import com.example.studentskillsplacementtracker.data.StudentRepository;
import com.example.studentskillsplacementtracker.model.Certification;
import com.example.studentskillsplacementtracker.model.CodingStats;
import com.example.studentskillsplacementtracker.model.Company;
import com.example.studentskillsplacementtracker.model.Project;
import com.example.studentskillsplacementtracker.model.ReadinessSummary;
import com.example.studentskillsplacementtracker.model.Skill;
import com.example.studentskillsplacementtracker.model.Student;
import com.example.studentskillsplacementtracker.util.ReadinessCalculator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadinessActivity extends AppCompatActivity {

    private static final int REQUIRED_READS = 6;

    private FirebaseAuth firebaseAuth;
    private StudentRepository studentRepository;
    private SkillRepository skillRepository;
    private ProjectRepository projectRepository;
    private CertificationRepository certificationRepository;
    private CodingStatsRepository codingStatsRepository;
    private CompanyRepository companyRepository;

    private ProgressBar readinessProgressBar;
    private TextView readinessMessageTextView;
    private LinearLayout readinessResultsContainer;
    private TextView readinessScoreTextView;
    private ProgressBar readinessScoreProgressBar;
    private TextView readinessProfileTextView;
    private ProgressBar readinessProfileProgressBar;
    private TextView readinessSkillsTextView;
    private ProgressBar readinessSkillsProgressBar;
    private TextView readinessProjectsTextView;
    private ProgressBar readinessProjectsProgressBar;
    private TextView readinessCertificationsTextView;
    private ProgressBar readinessCertificationsProgressBar;
    private TextView readinessCodingTextView;
    private ProgressBar readinessCodingProgressBar;
    private TextView readinessEligibilityTextView;
    private ProgressBar readinessEligibilityProgressBar;
    private TextView readinessMethodTextView;

    private String uid;

    private Student student;
    private List<Skill> skills;
    private List<Project> projects;
    private List<Certification> certifications;
    private List<CodingStats> codingStatsList;
    private List<Company> companies;

    private AtomicInteger readsRemaining = new AtomicInteger(0);
    private long currentLoadId = 0;
    private boolean readFailed = false;

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

        studentRepository = new StudentRepository();
        skillRepository = new SkillRepository();
        projectRepository = new ProjectRepository();
        certificationRepository = new CertificationRepository();
        codingStatsRepository = new CodingStatsRepository();
        companyRepository = new CompanyRepository();

        setContentView(R.layout.activity_readiness);

        readinessProgressBar = findViewById(R.id.readinessProgressBar);
        readinessMessageTextView = findViewById(R.id.readinessMessageTextView);
        readinessResultsContainer = findViewById(R.id.readinessResultsContainer);
        readinessScoreTextView = findViewById(R.id.readinessScoreTextView);
        readinessScoreProgressBar = findViewById(R.id.readinessScoreProgressBar);
        readinessProfileTextView = findViewById(R.id.readinessProfileTextView);
        readinessProfileProgressBar = findViewById(R.id.readinessProfileProgressBar);
        readinessSkillsTextView = findViewById(R.id.readinessSkillsTextView);
        readinessSkillsProgressBar = findViewById(R.id.readinessSkillsProgressBar);
        readinessProjectsTextView = findViewById(R.id.readinessProjectsTextView);
        readinessProjectsProgressBar = findViewById(R.id.readinessProjectsProgressBar);
        readinessCertificationsTextView =
                findViewById(R.id.readinessCertificationsTextView);
        readinessCertificationsProgressBar =
                findViewById(R.id.readinessCertificationsProgressBar);
        readinessCodingTextView = findViewById(R.id.readinessCodingTextView);
        readinessCodingProgressBar = findViewById(R.id.readinessCodingProgressBar);
        readinessEligibilityTextView = findViewById(R.id.readinessEligibilityTextView);
        readinessEligibilityProgressBar =
                findViewById(R.id.readinessEligibilityProgressBar);
        readinessMethodTextView = findViewById(R.id.readinessMethodTextView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (readinessResultsContainer != null) {
            loadReadiness();
        }
    }

    private void loadReadiness() {
        showLoadingState();

        // Every load gets a new identifier. Older callbacks become obsolete.
        final long loadId = ++currentLoadId;
        final AtomicInteger remaining = new AtomicInteger(REQUIRED_READS);

        readsRemaining = remaining;
        readFailed = false;

        student = null;
        skills = null;
        projects = null;
        certifications = null;
        codingStatsList = null;
        companies = null;

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

        certificationRepository.getCertifications(
                uid,
                value -> {
                    if (!isCurrentLoad(loadId)) {
                        return;
                    }
                    certifications = value;
                    onReadComplete(loadId, remaining);
                },
                exception -> onReadFailed(loadId)
        );

        codingStatsRepository.getCodingStats(
                uid,
                value -> {
                    if (!isCurrentLoad(loadId)) {
                        return;
                    }
                    codingStatsList = value;
                    onReadComplete(loadId, remaining);
                },
                exception -> onReadFailed(loadId)
        );

        companyRepository.getCompanies(
                value -> {
                    if (!isCurrentLoad(loadId)) {
                        return;
                    }
                    companies = value;
                    onReadComplete(loadId, remaining);
                },
                exception -> onReadFailed(loadId)
        );
    }

    private boolean isCurrentLoad(long loadId) {
        return loadId == currentLoadId && !isFinishing() && !isDestroyed();
    }

    private void onReadComplete(long loadId, AtomicInteger remaining) {
        if (!isCurrentLoad(loadId) || readFailed) {
            return;
        }

        int pending = remaining.decrementAndGet();

        if (pending == 0) {
            renderSummary();
        }
    }

    private void onReadFailed(long loadId) {
        if (!isCurrentLoad(loadId) || readFailed) {
            return;
        }

        readFailed = true;
        showMessage(R.string.readiness_load_error);
    }

    private void renderSummary() {
        if (student == null) {
            showMessage(R.string.readiness_profile_missing);
            return;
        }

        List<Skill> studentSkills =
                skills == null ? new ArrayList<>() : skills;
        List<Project> studentProjects =
                projects == null ? new ArrayList<>() : projects;
        List<Certification> studentCertifications =
                certifications == null ? new ArrayList<>() : certifications;
        List<CodingStats> studentCodingStats =
                codingStatsList == null ? new ArrayList<>() : codingStatsList;
        List<Company> companyList =
                companies == null ? new ArrayList<>() : companies;

        ReadinessSummary summary = ReadinessCalculator.calculate(
                student,
                studentSkills,
                studentProjects,
                studentCertifications,
                studentCodingStats,
                companyList
        );

        bindSummary(summary);
    }

    private void bindSummary(ReadinessSummary summary) {
        readinessProgressBar.setVisibility(View.GONE);
        readinessMessageTextView.setVisibility(View.GONE);
        readinessResultsContainer.setVisibility(View.VISIBLE);

        readinessScoreTextView.setText(
                getString(R.string.readiness_score_label,
                        summary.getReadinessScore()));
        readinessScoreProgressBar.setProgress(summary.getReadinessScore());

        readinessProfileTextView.setText(
                getString(R.string.readiness_profile_completion,
                        summary.getProfileCompletionPercent()));
        readinessProfileProgressBar.setProgress(
                summary.getProfileCompletionPercent());

        readinessSkillsTextView.setText(
                getString(R.string.readiness_skills,
                        summary.getTotalSkills(),
                        ReadinessCalculator.TARGET_SKILLS));
        readinessSkillsProgressBar.setProgress(summary.getSkillsPercent());

        readinessProjectsTextView.setText(
                getString(R.string.readiness_projects,
                        summary.getTotalProjects(),
                        ReadinessCalculator.TARGET_PROJECTS));
        readinessProjectsProgressBar.setProgress(summary.getProjectsPercent());

        readinessCertificationsTextView.setText(
                getString(R.string.readiness_certifications,
                        summary.getTotalCertifications(),
                        ReadinessCalculator.TARGET_CERTIFICATIONS));
        readinessCertificationsProgressBar.setProgress(
                summary.getCertificationsPercent());

        readinessCodingTextView.setText(
                getString(R.string.readiness_coding,
                        summary.getCodingTotal(),
                        summary.getCodingEasy(),
                        summary.getCodingMedium(),
                        summary.getCodingHard()));
        readinessCodingProgressBar.setProgress(summary.getCodingPercent());

        if (summary.isEligibilityAvailable()) {
            readinessEligibilityTextView.setText(
                    getString(R.string.readiness_eligibility,
                            summary.getEligibleCompanyCount(),
                            summary.getTotalCompanyCount(),
                            summary.getEligibilityPercent()));
        } else {
            readinessEligibilityTextView.setText(
                    R.string.readiness_no_companies);
        }

        readinessEligibilityProgressBar.setProgress(
                summary.getEligibilityPercent());

        readinessMethodTextView.setText(
                getString(R.string.readiness_method_body,
                        ReadinessCalculator.TARGET_SKILLS,
                        ReadinessCalculator.TARGET_PROJECTS,
                        ReadinessCalculator.TARGET_CERTIFICATIONS,
                        ReadinessCalculator.TARGET_CODING_PROBLEMS));
    }

    private void showLoadingState() {
        readinessProgressBar.setVisibility(View.VISIBLE);
        readinessResultsContainer.setVisibility(View.GONE);
        readinessMessageTextView.setVisibility(View.GONE);
    }

    private void showMessage(int messageResId) {
        readinessProgressBar.setVisibility(View.GONE);
        readinessResultsContainer.setVisibility(View.GONE);
        readinessMessageTextView.setText(messageResId);
        readinessMessageTextView.setVisibility(View.VISIBLE);
    }

    private void goToLogin() {
        Intent intent = new Intent(
                ReadinessActivity.this,
                MainActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}