package com.example.studentskillsplacementtracker.util;

import com.example.studentskillsplacementtracker.model.Company;
import com.example.studentskillsplacementtracker.model.EligibilityResult;
import com.example.studentskillsplacementtracker.model.Skill;
import com.example.studentskillsplacementtracker.model.Student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic eligibility comparison for FR-ELG-01.
 *
 * Pure business logic: no Android, UI or Firebase code lives here, so the rules
 * can be reviewed and unit tested in isolation. The comparisons are:
 *
 * <ul>
 *     <li>student CGPA &gt;= company minimumCGPA (no rounding before comparing)</li>
 *     <li>every company required skill must be present in the student's skills</li>
 *     <li>student project count &gt;= company minimumProjects</li>
 * </ul>
 */
public final class EligibilityEvaluator {

    private EligibilityEvaluator() {
        // Utility class - no instances.
    }

    /**
     * Compares {@code student} (with their {@code skills} and {@code projectCount})
     * against the requirements configured on {@code company}.
     */
    public static EligibilityResult evaluate(Company company,
                                             Student student,
                                             List<Skill> skills,
                                             int projectCount) {

        // CGPA - compared without rounding so nobody is wrongly passed.
        double requiredCgpa = company.getMinimumCGPA();
        double studentCgpa = student.getCgpa();
        boolean cgpaMet = studentCgpa >= requiredCgpa;

        if (Double.isNaN(studentCgpa) || Double.isInfinite(studentCgpa)
                || Double.isNaN(requiredCgpa) || Double.isInfinite(requiredCgpa)) {
            cgpaMet = false;
        }

        // Skills - membership test over normalized names. Proficiency and
        // progress are deliberately not part of eligibility.
        Set<String> studentSkills = new LinkedHashSet<>();

        for (Skill skill : safeList(skills)) {
            String normalized = normalize(skill == null ? null : skill.getSkillName());
            if (!normalized.isEmpty()) {
                studentSkills.add(normalized);
            }
        }

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String requiredSkill : safeList(company.getRequiredSkills())) {

            String normalized = normalize(requiredSkill);

            // Blank entries carry no requirement.
            if (normalized.isEmpty()) {
                continue;
            }

            // Keep the company's original spelling for display.
            String displayValue = requiredSkill.trim();

            if (studentSkills.contains(normalized)) {
                matchedSkills.add(displayValue);
            } else {
                missingSkills.add(displayValue);
            }
        }

        // Projects - every project counts, whatever its status.
        int requiredProjects = company.getMinimumProjects();
        boolean projectsMet = projectCount >= requiredProjects;

        boolean eligible = cgpaMet && missingSkills.isEmpty() && projectsMet;

        return new EligibilityResult(
                eligible,
                cgpaMet,
                studentCgpa,
                requiredCgpa,
                matchedSkills,
                missingSkills,
                projectsMet,
                projectCount,
                requiredProjects
        );
    }

    /**
     * Trims the value, collapses internal whitespace and lower cases it, so
     * {@code "  Data   Analyst "} and {@code "data analyst"} compare equal.
     */
    private static String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.getDefault());
    }

    private static <T> List<T> safeList(List<T> values) {
        return (values == null) ? Collections.<T>emptyList() : values;
    }
}
