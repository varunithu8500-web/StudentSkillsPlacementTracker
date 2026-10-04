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

/**
 * Placement readiness dashboard for the signed in student (FR-PRS-01).
 *
 * Summarizes the student's skills, projects, certifications, coding practice and
 * eligibility progress, and shows an app-defined readiness score. The score is a
 * progress indicator only - it is not a probability of placement and not a
 * recruitment prediction.
 *
 * The screen is read only: it reuses the existing repositories, collects the data
 * with six parallel reads and hands it to {@link ReadinessCalculator}. Nothing is
 * written and nothing is persisted.
 */
public class ReadinessActivity extends AppCompatActivity {

    /** Independent reads required before the summary can be calculated. */
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

    /** UID of the authenticated student. Always read from FirebaseAuth. */
    private String uid;

    // Values collected from the six reads before calculating
    private Student student;
    private List<Skill> skills;
    private List<Project> projects;
    private List<Certification> certifications;
    private List<CodingStats> codingStatsList;
    private List<Company> companies;

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

        studentRepository = new StudentRepository();
        skillRepository = new SkillRepository();
        projectRepository = new ProjectRepository();
        certificationRepository = new CertificationRepository();
        codingStatsRepository = new CodingStatsRepository();
        companyRepository = new CompanyRepository();

        setContentView(R.layout.activity_readiness);

        // Connect XML components with Java
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
        readinessCertificationsTextView = findViewById(R.id.readinessCertificationsTextView);
        readinessCertificationsProgressBar =
                findViewById(R.id.readinessCertificationsProgressBar);
        readinessCodingTextView = findViewById(R.id.readinessCodingTextView);
        readinessCodingProgressBar = findViewById(R.id.readinessCodingProgressBar);
        readinessEligibilityTextView = findViewById(R.id.readinessEligibilityTextView);
        readinessEligibilityProgressBar = findViewById(R.id.readinessEligibilityProgressBar);
        readinessMethodTextView = findViewById(R.id.readinessMethodTextView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload every time the screen becomes visible again, so the summary is
        // always current. The results container is null when the session guard
        // already sent the user back to login.
        if (readinessResultsContainer != null) {
            loadReadiness();
        }
    }

    /**
     * Starts the six independent reads. They run in parallel and the summary is
     * only calculated once all six have completed successfully.
     */
    private void loadReadiness() {

        showLoadingState();

        readsRemaining = REQUIRED_READS;
        readFailed = false;
        student = null;
        skills = null;
        projects = null;
        certifications = null;
        codingStatsList = null;
        companies = null;

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

        certificationRepository.getCertifications(uid,
                value -> {
                    certifications = value;
                    onReadComplete();
                },
                exception -> onReadFailed());

        codingStatsRepository.getCodingStats(uid,
                value -> {
                    codingStatsList = value;
                    onReadComplete();
                },
                exception -> onReadFailed());

        companyRepository.getCompanies(
                value -> {
                    companies = value;
                    onReadComplete();
                },
                exception -> onReadFailed());
    }

    /**
     * Counts down the pending reads and calculates the summary once they are all
     * in.
     */
    private void onReadComplete() {

        if (readFailed) {
            return;
        }

        readsRemaining--;

        if (readsRemaining > 0) {
            return;
        }

        renderSummary();
    }

    /**
     * A failed read must never produce a partial score, so the first failure stops
     * the calculation and reports a load error instead.
     */
    private void onReadFailed() {

        if (readFailed) {
            return;
        }

        readFailed = true;
        showMessage(R.string.readiness_load_error);
    }

    /**
     * Calculates and renders the summary. A missing profile document is reported
     * as a data error and no score is shown at all.
     */
    private void renderSummary() {

        if (student == null) {
            showMessage(R.string.readiness_profile_missing);
            return;
        }

        List<Skill> studentSkills = (skills == null) ? new ArrayList<>() : skills;
        List<Project> studentProjects = (projects == null) ? new ArrayList<>() : projects;
        List<Certification> studentCertifications =
                (certifications == null) ? new ArrayList<>() : certifications;
        List<CodingStats> studentCodingStats =
                (codingStatsList == null) ? new ArrayList<>() : codingStatsList;
        List<Company> companyList = (companies == null) ? new ArrayList<>() : companies;

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

    /**
     * Renders the readiness summary. Every bar is a percentage, so all of them
     * share the same 0 to 100 scale.
     */
    private void bindSummary(ReadinessSummary summary) {

        readinessProgressBar.setVisibility(View.GONE);
        readinessMessageTextView.setVisibility(View.GONE);
        readinessResultsContainer.setVisibility(View.VISIBLE);

        // Headline score
        readinessScoreTextView.setText(
                getString(R.string.readiness_score_label, summary.getReadinessScore()));
        readinessScoreProgressBar.setProgress(summary.getReadinessScore());

        // Profile completion
        readinessProfileTextView.setText(
                getString(
                        R.string.readiness_profile_completion,
                        summary.getProfileCompletionPercent()
                ));
        readinessProfileProgressBar.setProgress(summary.getProfileCompletionPercent());

        // Skills
        readinessSkillsTextView.setText(
                getString(
                        R.string.readiness_skills,
                        summary.getTotalSkills(),
                        ReadinessCalculator.TARGET_SKILLS
                ));
        readinessSkillsProgressBar.setProgress(summary.getSkillsPercent());

        // Projects
        readinessProjectsTextView.setText(
                getString(
                        R.string.readiness_projects,
                        summary.getTotalProjects(),
                        ReadinessCalculator.TARGET_PROJECTS
                ));
        readinessProjectsProgressBar.setProgress(summary.getProjectsPercent());

        // Certifications
        readinessCertificationsTextView.setText(
                getString(
                        R.string.readiness_certifications,
                        summary.getTotalCertifications(),
                        ReadinessCalculator.TARGET_CERTIFICATIONS
                ));
        readinessCertificationsProgressBar.setProgress(summary.getCertificationsPercent());

        // Coding practice, with the easy / medium / hard breakdown
        readinessCodingTextView.setText(
                getString(
                        R.string.readiness_coding,
                        summary.getCodingTotal(),
                        summary.getCodingEasy(),
                        summary.getCodingMedium(),
                        summary.getCodingHard()
                ));
        readinessCodingProgressBar.setProgress(summary.getCodingPercent());

        // Eligibility progress, or a clear note when no company exists yet
        if (summary.isEligibilityAvailable()) {
            readinessEligibilityTextView.setText(
                    getString(
                            R.string.readiness_eligibility,
                            summary.getEligibleCompanyCount(),
                            summary.getTotalCompanyCount(),
                            summary.getEligibilityPercent()
                    ));
        } else {
            readinessEligibilityTextView.setText(R.string.readiness_no_companies);
        }
        readinessEligibilityProgressBar.setProgress(summary.getEligibilityPercent());

        // How the score is built, so the number is never a black box
        readinessMethodTextView.setText(
                getString(
                        R.string.readiness_method_body,
                        ReadinessCalculator.TARGET_SKILLS,
                        ReadinessCalculator.TARGET_PROJECTS,
                        ReadinessCalculator.TARGET_CERTIFICATIONS,
                        ReadinessCalculator.TARGET_CODING_PROBLEMS
                ));
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
        Intent intent = new Intent(ReadinessActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
