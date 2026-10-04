package com.example.studentskillsplacementtracker.util;

import com.example.studentskillsplacementtracker.model.Company;
import com.example.studentskillsplacementtracker.model.EligibilityResult;
import com.example.studentskillsplacementtracker.model.Skill;
import com.example.studentskillsplacementtracker.model.Student;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link EligibilityEvaluator}.
 *
 * These lock in the documented comparison rules: CGPA compared with {@code >=}
 * and never rounded, exact skill matching after trimming and lower casing, blank
 * or missing required skills carrying no requirement, and every project counted
 * whatever its status.
 *
 * The tests use only the public API of the evaluator and the existing models.
 */
public class EligibilityEvaluatorTest {

    private static final String UID = "uid-1";
    private static final String COMPANY_ID = "company-1";

    // ------------------------------------------------------------------ setup

    private Student studentWithCgpa(double cgpa) {
        return new Student(UID, "Asha", "asha@example.com", "CSE", 3, cgpa,
                Student.ROLE_STUDENT);
    }

    private Company companyWithRequirements(double minimumCgpa,
                                            List<String> requiredSkills,
                                            int minimumProjects) {
        return new Company(COMPANY_ID, "Acme", "Software Engineer", minimumCgpa,
                requiredSkills, minimumProjects, "Bengaluru");
    }

    private List<Skill> skillsNamed(String... names) {

        List<Skill> skills = new ArrayList<>();

        for (String name : names) {
            skills.add(new Skill("skill-1", UID, name, 3, 50));
        }

        return skills;
    }

    // ------------------------------------------------------------ CGPA boundary

    @Test
    public void cgpaExactlyEqualToMinimumMeetsTheRequirement() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(8.0, Collections.<String>emptyList(), 0),
                studentWithCgpa(8.0),
                Collections.<Skill>emptyList(),
                0
        );

        assertTrue(result.isCgpaMet());
        assertTrue(result.isEligible());
        assertEquals(8.0, result.getRequiredCgpa(), 0.0001);
        assertEquals(8.0, result.getStudentCgpa(), 0.0001);
    }

    @Test
    public void cgpaBelowMinimumFailsTheRequirement() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(8.0, Collections.<String>emptyList(), 0),
                studentWithCgpa(7.99),
                Collections.<Skill>emptyList(),
                0
        );

        assertFalse(result.isCgpaMet());
        assertFalse(result.isEligible());
    }

    @Test
    public void cgpaJustBelowMinimumIsNotRoundedUpwards() {

        // 7.999 must not be rounded to 8.00 and accepted.
        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(8.0, Collections.<String>emptyList(), 0),
                studentWithCgpa(7.999),
                Collections.<Skill>emptyList(),
                0
        );

        assertFalse(result.isCgpaMet());
    }

    @Test
    public void nonFiniteCgpaNeverMeetsTheRequirement() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(0.0, Collections.<String>emptyList(), 0),
                studentWithCgpa(Double.NaN),
                Collections.<Skill>emptyList(),
                0
        );

        assertFalse(result.isCgpaMet());
        assertFalse(result.isEligible());
    }

    // ------------------------------------------------------ skill normalization

    @Test
    public void requiredSkillMatchesIgnoringCaseAndSurroundingWhitespace() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(0.0, Arrays.asList("  Data   Analyst "), 0),
                studentWithCgpa(9.0),
                skillsNamed("data analyst"),
                0
        );

        assertTrue(result.isEligible());
        assertTrue(result.getMissingSkills().isEmpty());
        assertEquals(1, result.getMatchedSkills().size());
        // The company's own spelling is kept for display.
        assertEquals("Data   Analyst", result.getMatchedSkills().get(0));
    }

    @Test
    public void missingRequiredSkillIsReportedAsMissing() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(0.0, Arrays.asList("Java", "SQL"), 0),
                studentWithCgpa(9.0),
                skillsNamed("java"),
                0
        );

        assertFalse(result.isEligible());
        assertEquals(Arrays.asList("SQL"), result.getMissingSkills());
        assertEquals(Arrays.asList("Java"), result.getMatchedSkills());
    }

    @Test
    public void blankRequiredSkillsCarryNoRequirement() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(0.0, Arrays.asList("", "   ", null), 0),
                studentWithCgpa(9.0),
                Collections.<Skill>emptyList(),
                0
        );

        assertTrue(result.isEligible());
        assertTrue(result.getMissingSkills().isEmpty());
        assertTrue(result.getMatchedSkills().isEmpty());
    }

    @Test
    public void absentRequiredSkillsListCarriesNoRequirement() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(0.0, null, 0),
                studentWithCgpa(9.0),
                Collections.<Skill>emptyList(),
                0
        );

        assertTrue(result.isEligible());
        assertTrue(result.getMissingSkills().isEmpty());
    }

    @Test
    public void blankStudentSkillNamesAreIgnored() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(0.0, Arrays.asList("Java"), 0),
                studentWithCgpa(9.0),
                skillsNamed("   "),
                0
        );

        assertFalse(result.isEligible());
        assertEquals(Arrays.asList("Java"), result.getMissingSkills());
    }

    // ------------------------------------------------------------ project count

    @Test
    public void projectCountExactlyAtMinimumMeetsTheRequirement() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(0.0, Collections.<String>emptyList(), 2),
                studentWithCgpa(9.0),
                Collections.<Skill>emptyList(),
                2
        );

        assertTrue(result.isProjectsMet());
        assertTrue(result.isEligible());
        assertEquals(2, result.getStudentProjectCount());
        assertEquals(2, result.getRequiredProjectCount());
    }

    @Test
    public void fewerProjectsThanRequiredFailsTheRequirement() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(0.0, Collections.<String>emptyList(), 2),
                studentWithCgpa(9.0),
                Collections.<Skill>emptyList(),
                1
        );

        assertFalse(result.isProjectsMet());
        assertFalse(result.isEligible());
    }

    // ------------------------------------------------------------- composition

    @Test
    public void studentMeetingEveryRequirementIsEligible() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(7.5, Arrays.asList("Java", "SQL"), 2),
                studentWithCgpa(8.25),
                skillsNamed("java", "SQL"),
                3
        );

        assertTrue(result.isEligible());
        assertTrue(result.isCgpaMet());
        assertTrue(result.isProjectsMet());
        assertTrue(result.getMissingSkills().isEmpty());
        assertEquals(2, result.getMatchedSkills().size());
    }

    @Test
    public void failingSeveralRequirementsProducesNotEligibleWithMissingDetails() {

        EligibilityResult result = EligibilityEvaluator.evaluate(
                companyWithRequirements(9.0, Arrays.asList("Java", "SQL"), 5),
                studentWithCgpa(6.0),
                skillsNamed("python"),
                1
        );

        assertFalse(result.isEligible());
        assertFalse(result.isCgpaMet());
        assertFalse(result.isProjectsMet());
        assertEquals(Arrays.asList("Java", "SQL"), result.getMissingSkills());
        assertEquals(1, result.getStudentProjectCount());
        assertEquals(5, result.getRequiredProjectCount());
    }
}
