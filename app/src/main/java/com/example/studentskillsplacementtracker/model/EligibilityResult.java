package com.example.studentskillsplacementtracker.model;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of comparing a student against one company's placement requirements
 * (FR-ELG-01).
 *
 * This is a computed, in memory value object. It is never persisted to
 * Firestore - {@code Company} remains the only company document model.
 */
public class EligibilityResult {

    private final boolean eligible;
    private final boolean cgpaMet;
    private final double studentCgpa;
    private final double requiredCgpa;
    private final List<String> matchedSkills;
    private final List<String> missingSkills;
    private final boolean projectsMet;
    private final int studentProjectCount;
    private final int requiredProjectCount;

    public EligibilityResult(boolean eligible,
                             boolean cgpaMet,
                             double studentCgpa,
                             double requiredCgpa,
                             List<String> matchedSkills,
                             List<String> missingSkills,
                             boolean projectsMet,
                             int studentProjectCount,
                             int requiredProjectCount) {
        this.eligible = eligible;
        this.cgpaMet = cgpaMet;
        this.studentCgpa = studentCgpa;
        this.requiredCgpa = requiredCgpa;
        this.matchedSkills = (matchedSkills == null)
                ? Collections.<String>emptyList()
                : matchedSkills;
        this.missingSkills = (missingSkills == null)
                ? Collections.<String>emptyList()
                : missingSkills;
        this.projectsMet = projectsMet;
        this.studentProjectCount = studentProjectCount;
        this.requiredProjectCount = requiredProjectCount;
    }

    /** True when every requirement is met. */
    public boolean isEligible() {
        return eligible;
    }

    /** True when the student's CGPA meets or exceeds the company minimum. */
    public boolean isCgpaMet() {
        return cgpaMet;
    }

    public double getStudentCgpa() {
        return studentCgpa;
    }

    public double getRequiredCgpa() {
        return requiredCgpa;
    }

    /** Required skills the student has, in the company's spelling. */
    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    /** Required skills the student does not have, in the company's spelling. */
    public List<String> getMissingSkills() {
        return missingSkills;
    }

    /** True when the student's project count meets the company minimum. */
    public boolean isProjectsMet() {
        return projectsMet;
    }

    public int getStudentProjectCount() {
        return studentProjectCount;
    }

    public int getRequiredProjectCount() {
        return requiredProjectCount;
    }
}
