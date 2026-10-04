package com.example.studentskillsplacementtracker.util;

import com.example.studentskillsplacementtracker.model.Certification;
import com.example.studentskillsplacementtracker.model.CodingStats;
import com.example.studentskillsplacementtracker.model.Company;
import com.example.studentskillsplacementtracker.model.EligibilityResult;
import com.example.studentskillsplacementtracker.model.Project;
import com.example.studentskillsplacementtracker.model.ReadinessSummary;
import com.example.studentskillsplacementtracker.model.Skill;
import com.example.studentskillsplacementtracker.model.Student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Placement readiness calculation for FR-PRS-01.
 *
 * Pure business logic: no Android, UI or Firebase code lives here, so the
 * formulas can be reviewed and unit tested in isolation.
 *
 * The score is the equal average of five components, each worth 20%:
 * skills, projects, certifications, coding practice and eligibility progress.
 * Every component is capped at its target, so the score stays within 0 to 100.
 *
 * Eligibility progress reuses {@link EligibilityEvaluator} exactly as it is,
 * once per company, so the dashboard can never disagree with the per company
 * eligibility screen.
 *
 * The score is an app-defined progress indicator only. It is not a placement
 * probability and not a recruitment prediction.
 */
public final class ReadinessCalculator {

    /** Skills a student is expected to list. */
    public static final int TARGET_SKILLS = 10;

    /** Projects a student is expected to list, whatever their status. */
    public static final int TARGET_PROJECTS = 3;

    /** Certifications a student is expected to record. */
    public static final int TARGET_CERTIFICATIONS = 3;

    /** Solved coding problems a student is expected to reach. */
    public static final int TARGET_CODING_PROBLEMS = 100;

    /** Number of profile fields that make up a complete profile. */
    private static final int PROFILE_FIELD_COUNT = 5;

    /** Accepted range for the profile year. */
    private static final int YEAR_MIN = 1;
    private static final int YEAR_MAX = 4;

    /** Accepted range for the profile CGPA. */
    private static final double CGPA_MIN = 0.0;
    private static final double CGPA_MAX = 10.0;

    private ReadinessCalculator() {
        // Utility class - no instances.
    }

    /**
     * Builds the readiness summary from the student's own documents and the
     * shared company list. Any list may be {@code null} or empty: missing data
     * scores zero rather than failing.
     */
    public static ReadinessSummary calculate(Student student,
                                             List<Skill> skills,
                                             List<Project> projects,
                                             List<Certification> certifications,
                                             List<CodingStats> codingStatsList,
                                             List<Company> companies) {

        List<Skill> studentSkills = safeList(skills);
        List<Project> studentProjects = safeList(projects);
        List<Certification> studentCertifications = safeList(certifications);
        List<CodingStats> studentCodingStats = safeList(codingStatsList);
        List<Company> companyList = safeList(companies);

        // Profile completion - the five profile fields carry equal weight.
        int profileCompletionPercent = calculateProfileCompletionPercent(student);

        // Portfolio counts - raw document counts, no de-duplication.
        int totalSkills = studentSkills.size();
        int totalProjects = studentProjects.size();
        int totalCertifications = studentCertifications.size();

        // Coding practice - summed from the three difficulty counts.
        int codingEasy = 0;
        int codingMedium = 0;
        int codingHard = 0;

        for (CodingStats codingStats : studentCodingStats) {
            if (codingStats == null) {
                continue;
            }
            codingEasy += codingStats.getEasy();
            codingMedium += codingStats.getMedium();
            codingHard += codingStats.getHard();
        }

        int codingTotal = codingEasy + codingMedium + codingHard;

        // Eligibility progress - the existing evaluator, once per company.
        int eligibleCompanyCount = 0;

        for (Company company : companyList) {
            if (company == null) {
                continue;
            }
            EligibilityResult result = EligibilityEvaluator.evaluate(
                    company,
                    student,
                    studentSkills,
                    totalProjects
            );
            if (result.isEligible()) {
                eligibleCompanyCount++;
            }
        }

        int totalCompanyCount = companyList.size();
        int eligibilityPercent = (totalCompanyCount == 0)
                ? 0
                : (eligibleCompanyCount * 100) / totalCompanyCount;

        // Component progress, each capped at its target.
        int skillsPercent = percentOfTarget(totalSkills, TARGET_SKILLS);
        int projectsPercent = percentOfTarget(totalProjects, TARGET_PROJECTS);
        int certificationsPercent =
                percentOfTarget(totalCertifications, TARGET_CERTIFICATIONS);
        int codingPercent = percentOfTarget(codingTotal, TARGET_CODING_PROBLEMS);

        // Equal weighting. Eligibility is left out of the average when no
        // company has been published yet, so missing coordinator data cannot
        // lower the score.
        List<Integer> components = new ArrayList<>();
        components.add(skillsPercent);
        components.add(projectsPercent);
        components.add(certificationsPercent);
        components.add(codingPercent);

        if (totalCompanyCount > 0) {
            components.add(eligibilityPercent);
        }

        int readinessScore = average(components);

        return new ReadinessSummary(
                profileCompletionPercent,
                totalSkills,
                skillsPercent,
                totalProjects,
                projectsPercent,
                totalCertifications,
                certificationsPercent,
                codingEasy,
                codingMedium,
                codingHard,
                codingTotal,
                codingPercent,
                eligibleCompanyCount,
                totalCompanyCount,
                eligibilityPercent,
                readinessScore
        );
    }

    /**
     * Percentage of the five profile fields that are present. A profile document
     * that does not exist yet scores zero.
     */
    private static int calculateProfileCompletionPercent(Student student) {

        if (student == null) {
            return 0;
        }

        int presentFields = 0;

        if (isNonBlank(student.getName())) {
            presentFields++;
        }
        if (isNonBlank(student.getEmail())) {
            presentFields++;
        }
        if (isNonBlank(student.getDepartment())) {
            presentFields++;
        }
        if (student.getYear() >= YEAR_MIN && student.getYear() <= YEAR_MAX) {
            presentFields++;
        }
        if (isValidCgpa(student.getCgpa())) {
            presentFields++;
        }

        return (presentFields * 100) / PROFILE_FIELD_COUNT;
    }

    /**
     * Progress of {@code value} against {@code target}, capped at 100 so a single
     * component can never outweigh its share of the score.
     */
    private static int percentOfTarget(int value, int target) {

        if (target <= 0 || value <= 0) {
            return 0;
        }

        int cappedValue = Math.min(value, target);

        return (cappedValue * 100) / target;
    }

    /**
     * Equal average of the component percentages, rounded to the nearest whole
     * number. Never returns anything outside 0 to 100.
     */
    private static int average(List<Integer> components) {

        if (components.isEmpty()) {
            return 0;
        }

        int total = 0;

        for (Integer component : components) {
            total += (component == null) ? 0 : component;
        }

        return (int) Math.round((double) total / components.size());
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isValidCgpa(double cgpa) {
        return !Double.isNaN(cgpa)
                && !Double.isInfinite(cgpa)
                && cgpa >= CGPA_MIN
                && cgpa <= CGPA_MAX;
    }

    private static <T> List<T> safeList(List<T> values) {
        return (values == null) ? Collections.<T>emptyList() : values;
    }
}
