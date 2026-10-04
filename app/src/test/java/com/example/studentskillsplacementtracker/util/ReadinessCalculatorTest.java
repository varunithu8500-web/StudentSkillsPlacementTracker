package com.example.studentskillsplacementtracker.util;

import com.example.studentskillsplacementtracker.model.Certification;
import com.example.studentskillsplacementtracker.model.CodingStats;
import com.example.studentskillsplacementtracker.model.Company;
import com.example.studentskillsplacementtracker.model.Project;
import com.example.studentskillsplacementtracker.model.ReadinessSummary;
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
 * Unit tests for {@link ReadinessCalculator}.
 *
 * These lock in the documented behaviour: profile completion over the five
 * profile fields, every component capped at its target, eligibility expressed as
 * the share of companies met, eligibility left out of the average when no
 * company has been published yet, and a score that always stays within 0 to 100.
 */
public class ReadinessCalculatorTest {

    private static final String UID = "uid-1";

    // ------------------------------------------------------------------ setup

    private Student validStudent() {
        return new Student(UID, "Asha", "asha@example.com", "CSE", 3, 8.5,
                Student.ROLE_STUDENT);
    }

    private List<Skill> skills(int count) {

        List<Skill> skills = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            skills.add(new Skill("skill-" + index, UID, "Skill " + index, 3, 50));
        }

        return skills;
    }

    private List<Project> projects(int count) {

        List<Project> projects = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            projects.add(new Project("project-" + index, UID, "Project " + index, "", null,
                    Project.STATUS_COMPLETED, ""));
        }

        return projects;
    }

    private List<Certification> certifications(int count) {

        List<Certification> certifications = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            certifications.add(new Certification("certification-" + index, UID,
                    "Certification " + index, "Org", null, ""));
        }

        return certifications;
    }

    private List<CodingStats> coding(int easy, int medium, int hard) {
        return Collections.singletonList(
                new CodingStats("coding-1", UID, "LeetCode", easy, medium, hard));
    }

    private Company company(double minimumCgpa, List<String> requiredSkills, int minimumProjects) {
        return new Company("company-1", "Acme", "Software Engineer", minimumCgpa,
                requiredSkills, minimumProjects, "Bengaluru");
    }

    private ReadinessSummary calculate(Student student,
                                       List<Skill> skills,
                                       List<Project> projects,
                                       List<Certification> certifications,
                                       List<CodingStats> codingStats,
                                       List<Company> companies) {
        return ReadinessCalculator.calculate(student, skills, projects, certifications,
                codingStats, companies);
    }

    private ReadinessSummary calculateEmptyPortfolio(Student student, List<Company> companies) {
        return calculate(student, skills(0), projects(0), certifications(0),
                Collections.<CodingStats>emptyList(), companies);
    }

    // ------------------------------------------------------- profile completion

    @Test
    public void profileCompletionIsFullWhenEveryProfileFieldIsPresent() {

        ReadinessSummary summary = calculateEmptyPortfolio(
                validStudent(), Collections.<Company>emptyList());

        assertEquals(100, summary.getProfileCompletionPercent());
    }

    private List<CodingStats> noCoding() {
        return Collections.<CodingStats>emptyList();
    }

    private List<Company> noCompanies() {
        return Collections.<Company>emptyList();
    }

    @Test
    public void profileCompletionCountsOnlyValidProfileFields() {

        // A blank name and an out of range year leave three of the five fields valid.
        Student student = new Student(UID, "   ", "asha@example.com", "CSE", 0, 8.5,
                Student.ROLE_STUDENT);

        ReadinessSummary summary = calculateEmptyPortfolio(student, noCompanies());

        assertEquals(60, summary.getProfileCompletionPercent());
    }

    @Test
    public void profileCompletionFallsToZeroWhenTheProfileIsMissing() {

        ReadinessSummary summary = calculateEmptyPortfolio(null, noCompanies());

        assertEquals(0, summary.getProfileCompletionPercent());
        assertEquals(0, summary.getReadinessScore());
    }

    // ------------------------------------------------------------------- totals

    @Test
    public void totalsReflectTheDocumentCounts() {

        ReadinessSummary summary = calculate(validStudent(), skills(4), projects(2),
                certifications(3), coding(10, 5, 2), noCompanies());

        assertEquals(4, summary.getTotalSkills());
        assertEquals(2, summary.getTotalProjects());
        assertEquals(3, summary.getTotalCertifications());
    }

    // -------------------------------------------------------------- target caps

    @Test
    public void skillProgressIsCappedAtItsTarget() {

        assertEquals(50, calculate(validStudent(), skills(5), projects(0), certifications(0),
                noCoding(), noCompanies()).getSkillsPercent());

        assertEquals(100, calculate(validStudent(), skills(ReadinessCalculator.TARGET_SKILLS),
                projects(0), certifications(0), noCoding(), noCompanies()).getSkillsPercent());

        assertEquals(100, calculate(validStudent(), skills(25), projects(0), certifications(0),
                noCoding(), noCompanies()).getSkillsPercent());
    }

    @Test
    public void projectProgressIsCappedAtItsTarget() {

        assertEquals(33, calculate(validStudent(), skills(0), projects(1), certifications(0),
                noCoding(), noCompanies()).getProjectsPercent());

        assertEquals(100, calculate(validStudent(), skills(0),
                projects(ReadinessCalculator.TARGET_PROJECTS), certifications(0),
                noCoding(), noCompanies()).getProjectsPercent());

        assertEquals(100, calculate(validStudent(), skills(0), projects(8), certifications(0),
                noCoding(), noCompanies()).getProjectsPercent());
    }

    @Test
    public void certificationProgressIsCappedAtItsTarget() {

        assertEquals(100, calculate(validStudent(), skills(0), projects(0),
                certifications(ReadinessCalculator.TARGET_CERTIFICATIONS), noCoding(),
                noCompanies()).getCertificationsPercent());

        assertEquals(100, calculate(validStudent(), skills(0), projects(0), certifications(9),
                noCoding(), noCompanies()).getCertificationsPercent());
    }

    @Test
    public void codingProgressSumsTheDifficultyCountsAndIsCappedAtItsTarget() {

        ReadinessSummary summary = calculate(validStudent(), skills(0), projects(0),
                certifications(0), coding(60, 30, 40), noCompanies());

        assertEquals(60, summary.getCodingEasy());
        assertEquals(30, summary.getCodingMedium());
        assertEquals(40, summary.getCodingHard());
        assertEquals(130, summary.getCodingTotal());
        assertEquals(100, summary.getCodingPercent());

        ReadinessSummary belowTarget = calculate(validStudent(), skills(0), projects(0),
                certifications(0), coding(1, 2, 3), noCompanies());

        assertEquals(6, belowTarget.getCodingTotal());
        assertEquals(6, belowTarget.getCodingPercent());
    }

    @Test
    public void anEmptyPortfolioContributesNothing() {

        ReadinessSummary summary = calculateEmptyPortfolio(validStudent(), noCompanies());

        assertEquals(0, summary.getTotalSkills());
        assertEquals(0, summary.getSkillsPercent());
        assertEquals(0, summary.getProjectsPercent());
        assertEquals(0, summary.getCertificationsPercent());
        assertEquals(0, summary.getCodingTotal());
        assertEquals(0, summary.getCodingPercent());
    }

    private List<Skill> skillsNamed(String... names) {

        List<Skill> skills = new ArrayList<>();

        for (String name : names) {
            skills.add(new Skill("skill-" + name, UID, name, 3, 50));
        }

        return skills;
    }

    // -------------------------------------------------------------- eligibility

    @Test
    public void eligibilityPercentageIsTheShareOfCompaniesMet() {

        List<Company> companies = Arrays.asList(
                // Met: CGPA on the boundary, skill present after normalisation.
                company(8.0, Arrays.asList("java"), 2),
                // Met: no skills required and low thresholds.
                company(7.0, null, 1),
                // Not met: CGPA requirement too high.
                company(9.0, null, 0),
                // Not met: required skill missing.
                company(6.0, Arrays.asList("Python"), 0)
        );

        Student student = new Student(UID, "Asha", "asha@example.com", "CSE", 3, 8.0,
                Student.ROLE_STUDENT);

        ReadinessSummary summary = calculate(student, skillsNamed("Java"), projects(2),
                certifications(0), noCoding(), companies);

        assertEquals(2, summary.getEligibleCompanyCount());
        assertEquals(4, summary.getTotalCompanyCount());
        assertEquals(50, summary.getEligibilityPercent());
        assertTrue(summary.isEligibilityAvailable());
    }

    @Test
    public void eligibilityIsExcludedFromTheAverageWhenNoCompanyExists() {

        // Skills 5 of 10 is 50%; the rest of the portfolio is empty.
        ReadinessSummary withoutCompanies = calculate(validStudent(), skills(5), projects(0),
                certifications(0), noCoding(), noCompanies());

        assertEquals(0, withoutCompanies.getEligibilityPercent());
        assertFalse(withoutCompanies.isEligibilityAvailable());
        // Four components: (50 + 0 + 0 + 0) / 4 = 12.5, rounded to 13.
        assertEquals(13, withoutCompanies.getReadinessScore());

        // The same portfolio with one company the student does not meet averages
        // five components: (50 + 0 + 0 + 0 + 0) / 5 = 10.
        ReadinessSummary withCompany = calculate(validStudent(), skills(5), projects(0),
                certifications(0), noCoding(),
                Collections.singletonList(company(10.0, null, 0)));

        assertTrue(withCompany.isEligibilityAvailable());
        assertEquals(0, withCompany.getEligibilityPercent());
        assertEquals(10, withCompany.getReadinessScore());
    }

    // ------------------------------------------------------------ overall score

    @Test
    public void readinessScoreIsTheEqualAverageOfAllFiveComponents() {

        List<Company> companies = Collections.singletonList(company(0.0, null, 0));

        ReadinessSummary summary = calculate(validStudent(),
                skills(ReadinessCalculator.TARGET_SKILLS),
                projects(ReadinessCalculator.TARGET_PROJECTS),
                certifications(ReadinessCalculator.TARGET_CERTIFICATIONS),
                coding(ReadinessCalculator.TARGET_CODING_PROBLEMS, 0, 0),
                companies);

        assertEquals(100, summary.getSkillsPercent());
        assertEquals(100, summary.getProjectsPercent());
        assertEquals(100, summary.getCertificationsPercent());
        assertEquals(100, summary.getCodingPercent());
        assertEquals(100, summary.getEligibilityPercent());
        assertEquals(100, summary.getReadinessScore());
    }

    @Test
    public void readinessScoreAlwaysStaysWithinZeroToOneHundred() {

        for (Integer count : Arrays.asList(0, 1, 3, 10, 25, 200)) {

            ReadinessSummary summary = calculate(validStudent(), skills(count), projects(count),
                    certifications(count), coding(count, count, count),
                    Collections.singletonList(company(0.0, null, 0)));

            assertTrue(summary.getReadinessScore() >= 0);
            assertTrue(summary.getReadinessScore() <= 100);
        }
    }
}
