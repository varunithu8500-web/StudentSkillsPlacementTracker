package com.example.studentskillsplacementtracker.model;

/**
 * Result of the placement readiness calculation (FR-PRS-01).
 *
 * This is a computed, in memory value object. It is never persisted to
 * Firestore: the dashboard is rebuilt from the existing profile, skills,
 * projects, certifications, coding and company documents every time it is
 * shown, so no readiness data is ever stored.
 *
 * The readiness score is an app-defined progress indicator. It is not a
 * probability of placement and not a recruitment prediction.
 *
 * Every percentage and the score itself are integers in the range 0 to 100.
 */
public class ReadinessSummary {

    private final int profileCompletionPercent;
    private final int totalSkills;
    private final int skillsPercent;
    private final int totalProjects;
    private final int projectsPercent;
    private final int totalCertifications;
    private final int certificationsPercent;
    private final int codingEasy;
    private final int codingMedium;
    private final int codingHard;
    private final int codingTotal;
    private final int codingPercent;
    private final int eligibleCompanyCount;
    private final int totalCompanyCount;
    private final int eligibilityPercent;
    private final int readinessScore;

    public ReadinessSummary(int profileCompletionPercent,
                            int totalSkills,
                            int skillsPercent,
                            int totalProjects,
                            int projectsPercent,
                            int totalCertifications,
                            int certificationsPercent,
                            int codingEasy,
                            int codingMedium,
                            int codingHard,
                            int codingTotal,
                            int codingPercent,
                            int eligibleCompanyCount,
                            int totalCompanyCount,
                            int eligibilityPercent,
                            int readinessScore) {
        this.profileCompletionPercent = profileCompletionPercent;
        this.totalSkills = totalSkills;
        this.skillsPercent = skillsPercent;
        this.totalProjects = totalProjects;
        this.projectsPercent = projectsPercent;
        this.totalCertifications = totalCertifications;
        this.certificationsPercent = certificationsPercent;
        this.codingEasy = codingEasy;
        this.codingMedium = codingMedium;
        this.codingHard = codingHard;
        this.codingTotal = codingTotal;
        this.codingPercent = codingPercent;
        this.eligibleCompanyCount = eligibleCompanyCount;
        this.totalCompanyCount = totalCompanyCount;
        this.eligibilityPercent = eligibilityPercent;
        this.readinessScore = readinessScore;
    }

    /** Percentage of the five profile fields that are present, 0 to 100. */
    public int getProfileCompletionPercent() {
        return profileCompletionPercent;
    }

    /** Number of skill documents. */
    public int getTotalSkills() {
        return totalSkills;
    }

    /** Skills progress against the target, capped at 100. */
    public int getSkillsPercent() {
        return skillsPercent;
    }

    /** Number of project documents, whatever their status. */
    public int getTotalProjects() {
        return totalProjects;
    }

    /** Projects progress against the target, capped at 100. */
    public int getProjectsPercent() {
        return projectsPercent;
    }

    /** Number of certification documents. */
    public int getTotalCertifications() {
        return totalCertifications;
    }

    /** Certifications progress against the target, capped at 100. */
    public int getCertificationsPercent() {
        return certificationsPercent;
    }

    public int getCodingEasy() {
        return codingEasy;
    }

    public int getCodingMedium() {
        return codingMedium;
    }

    public int getCodingHard() {
        return codingHard;
    }

    /** Easy + medium + hard across every coding platform. */
    public int getCodingTotal() {
        return codingTotal;
    }

    /** Coding progress against the target, capped at 100. */
    public int getCodingPercent() {
        return codingPercent;
    }

    /** Companies the student currently meets. */
    public int getEligibleCompanyCount() {
        return eligibleCompanyCount;
    }

    public int getTotalCompanyCount() {
        return totalCompanyCount;
    }

    /** Eligible companies as a percentage of all companies, 0 to 100. */
    public int getEligibilityPercent() {
        return eligibilityPercent;
    }

    /** The readiness score, 0 to 100. */
    public int getReadinessScore() {
        return readinessScore;
    }

    /**
     * True when at least one company exists, so eligibility progress could be
     * scored and was included in the average.
     */
    public boolean isEligibilityAvailable() {
        return totalCompanyCount > 0;
    }
}
