package com.darshan.airesumeanalyzer.service;

import com.darshan.airesumeanalyzer.dto.AnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResumeMatchingService.
 * Validates deterministic skill extraction, case normalization,
 * substring false-positive prevention, intelligent role extraction,
 * generic keyword filtering, and accurate scoring breakdown sums.
 */
class ResumeMatchingServiceTest {

    private final ResumeMatchingService matchingService = new ResumeMatchingService();

    @Test
    @DisplayName("Extracts skills with case-insensitive matching")
    void testCaseNormalization() {
        String text = "Experienced with java, PYTHON, and Docker containers.";
        Set<String> skills = matchingService.extractSkills(text);

        assertTrue(skills.contains("Java"), "Should extract Java regardless of lowercase");
        assertTrue(skills.contains("Python"), "Should extract Python regardless of uppercase");
        assertTrue(skills.contains("Docker"), "Should extract Docker");
    }

    @Test
    @DisplayName("Avoids substring false positives for 'Go'")
    void testGoSubstringFalsePositives() {
        String unrelatedText = "We are going to visit Google to discuss a good algorithm for sorting.";
        Set<String> skills = matchingService.extractSkills(unrelatedText);

        assertFalse(skills.contains("Go"), "'Go' should not match inside 'going', 'Google', 'good', or 'algorithm'");

        String realGoText = "Backend developer working with Go and Docker microservices.";
        Set<String> realSkills = matchingService.extractSkills(realGoText);
        assertTrue(realSkills.contains("Go"), "'Go' should match when used as a distinct token");
    }

    @Test
    @DisplayName("Avoids substring false positives for 'Java' inside 'JavaScript'")
    void testJavaVsJavaScript() {
        String jsOnly = "Built interactive frontend web applications using JavaScript and CSS.";
        Set<String> skills = matchingService.extractSkills(jsOnly);

        assertTrue(skills.contains("JavaScript"));
        assertFalse(skills.contains("Java"), "'Java' must not match inside 'JavaScript'");

        String javaAndJs = "Developed full-stack features using Java 17, Spring Boot, and JavaScript.";
        Set<String> bothSkills = matchingService.extractSkills(javaAndJs);
        assertTrue(bothSkills.contains("Java"));
        assertTrue(bothSkills.contains("JavaScript"));
    }

    @Test
    @DisplayName("Avoids substring false positives for 'C' and handles 'C++', 'C#' correctly")
    void testCAndSpecialSymbols() {
        String cssCloud = "Cloud engineer styling web interfaces with CSS.";
        Set<String> skills = matchingService.extractSkills(cssCloud);
        assertFalse(skills.contains("C"), "'C' should not match inside 'CSS' or 'Cloud'");

        String cppText = "Low-level system programmer with C++ and C# expertise.";
        Set<String> cppSkills = matchingService.extractSkills(cppText);
        assertTrue(cppSkills.contains("C++"), "Should recognize 'C++'");
        assertTrue(cppSkills.contains("C#"), "Should recognize 'C#'");
        assertFalse(cppSkills.contains("C"), "'C' should not match as a substring of 'C++' or 'C#'");

        String pureCText = "Embedded systems developer with C and Assembly experience.";
        Set<String> pureCSkills = matchingService.extractSkills(pureCText);
        assertTrue(pureCSkills.contains("C"), "Should recognize 'C' as an independent language");
    }

    @Test
    @DisplayName("Avoids substring false positives for 'REST', 'AWS', and 'Git'")
    void testOtherSubstringFalsePositives() {
        String trickyText = "Restored database flaws using digital workflows.";
        Set<String> falseSkills = matchingService.extractSkills(trickyText);

        assertFalse(falseSkills.contains("REST"), "'REST' must not match inside 'Restored'");
        assertFalse(falseSkills.contains("AWS"), "'AWS' must not match inside 'flaws'");
        assertFalse(falseSkills.contains("Git"), "'Git' must not match inside 'digital'");

        String realText = "Deployed REST APIs on AWS using Git for version control.";
        Set<String> realSkills = matchingService.extractSkills(realText);
        assertTrue(realSkills.contains("REST API") || realSkills.contains("REST"));
        assertTrue(realSkills.contains("AWS"));
        assertTrue(realSkills.contains("Git"));
    }

    @Test
    @DisplayName("Segments required vs preferred skills in job description")
    void testRequiredVsPreferredSkills() {
        String resume = "Proficient in Java, Spring Boot, PostgreSQL and Git. Experience with Docker.";
        String jobDesc = """
                Role: Senior Java Engineer
                
                Required Skills:
                - Java
                - Spring Boot
                - Kubernetes
                
                Preferred Skills:
                - Docker
                - AWS
                """;

        AnalysisResult result = matchingService.analyze("resume.pdf", 1024, resume, jobDesc);

        assertTrue(result.isHasJobDescription());
        assertEquals("Senior Java Engineer", result.getJobTitle());

        // Matched
        assertTrue(result.getMatchedSkills().contains("Java"));
        assertTrue(result.getMatchedSkills().contains("Spring Boot"));
        assertTrue(result.getMatchedSkills().contains("Docker"));

        // Missing Required
        assertTrue(result.getMissingRequiredSkills().contains("Kubernetes"));
        assertFalse(result.getMissingRequiredSkills().contains("Java"));

        // Missing Preferred
        assertTrue(result.getMissingPreferredSkills().contains("AWS"));
        assertFalse(result.getMissingPreferredSkills().contains("Docker"));

        // Additional (on resume but not in JD)
        assertTrue(result.getAdditionalSkills().contains("PostgreSQL") || result.getAdditionalSkills().contains("Git"));
    }

    @Test
    @DisplayName("Calculates documented score breakdown deterministically and sums to overall score")
    void testScoreCalculationAndBreakdown() {
        String resume = """
                John Doe
                Summary: Software engineer with Java and Spring Boot experience.
                
                Experience:
                Backend Developer at Acme Corp. Built REST APIs and microservices using Java and PostgreSQL.
                
                Education:
                B.S. in Computer Science.
                
                Skills:
                Java, Spring Boot, PostgreSQL, Docker, Git.
                
                Projects:
                Resume Analyzer application.
                """;

        String jobDesc = """
                Role: Java Backend Developer
                
                Requirements:
                - Java
                - Spring Boot
                - PostgreSQL
                
                Nice to have:
                - Docker
                - Kubernetes
                """;

        AnalysisResult result = matchingService.analyze("resume.pdf", 2048, resume, jobDesc);

        // Verification of scores and criteria breakdown
        assertNotNull(result.getScoreBreakdown());
        assertTrue(result.getScoreBreakdown().containsKey("Required Skills Match"));
        assertTrue(result.getScoreBreakdown().containsKey("Preferred Skills & Breadth"));
        assertTrue(result.getScoreBreakdown().containsKey("Keyword Coverage"));
        assertTrue(result.getScoreBreakdown().containsKey("Resume Structure"));

        // Critical requirement: breakdown values MUST sum to overallScore
        int breakdownSum = result.getScoreBreakdown().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(result.getOverallScore(), breakdownSum, "Breakdown component scores must sum exactly to overallScore");

        assertTrue(result.getOverallScore() > 0 && result.getOverallScore() <= 100);
        assertNotNull(result.getScoreLabel());
        assertEquals(ResumeMatchingService.DISCLAIMER_TEXT, result.getDisclaimer());

        // Verification of structure detection
        assertTrue(result.getDetectedSections().contains("Experience"));
        assertTrue(result.getDetectedSections().contains("Education"));
        assertTrue(result.getDetectedSections().contains("Skills"));
        assertTrue(result.getDetectedSections().contains("Projects"));

        // Verification of suggestions
        assertFalse(result.getSuggestions().isEmpty());
    }

    @Test
    @DisplayName("Handles empty job description with general resume audit and Target Role Not Specified")
    void testEmptyJobDescriptionAudit() {
        String resume = """
                Jane Doe
                Experience:
                5 years building backend microservices with Java, Python, and Docker.
                
                Education:
                Master in Software Engineering.
                
                Skills:
                Java, Python, Docker, PostgreSQL, Git.
                """;

        AnalysisResult result = matchingService.analyze("resume.pdf", 1500, resume, "");

        assertFalse(result.isHasJobDescription());
        assertEquals("Target Role Not Specified", result.getJobTitle());
        assertTrue(result.getOverallScore() > 0);
        assertTrue(result.getMissingRequiredSkills().isEmpty(), "No required skills should be missing when no JD was provided");
        assertFalse(result.getMatchedSkills().isEmpty(), "All detected skills should appear in matched skills");
        assertTrue(result.getScoreBreakdown().containsKey("Skills Detected"));

        // Breakdown sum invariant check
        int breakdownSum = result.getScoreBreakdown().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(result.getOverallScore(), breakdownSum);
    }

    // =========================================================================
    // NEW TESTS: TARGET ROLE EXTRACTION
    // =========================================================================

    @Test
    @DisplayName("Intelligently extracts role heading and ignores 'About the Role'")
    void testDetermineJobTitle_ignoresGenericHeadingAboutTheRole() {
        String jd = """
                About the Role
                We are seeking a Java Backend Developer Intern to join our core cloud platform team.
                
                Responsibilities:
                Build microservices and write unit tests.
                
                Requirements:
                - Java
                - Spring Boot
                """;

        String role = matchingService.determineJobTitle(jd);
        assertEquals("Java Backend Developer Intern", role,
                "Should extract the role title and not the generic 'About the Role' header");
    }

    @Test
    @DisplayName("Extracts role from standalone heading line")
    void testDetermineJobTitle_fromHeadingLine() {
        String jd = """
                Java Backend Developer Intern
                
                About the Company
                Acme Tech provides cloud infrastructure.
                
                Requirements:
                - Java 17
                """;

        String role = matchingService.determineJobTitle(jd);
        assertEquals("Java Backend Developer Intern", role);
    }

    @Test
    @DisplayName("Extracts role from explicit label prefix")
    void testDetermineJobTitle_fromExplicitLabel() {
        String jd = """
                Job Title: Senior Cloud Infrastructure Architect
                Department: Engineering
                Location: Remote
                
                Requirements:
                - AWS, Terraform, Kubernetes
                """;

        String role = matchingService.determineJobTitle(jd);
        assertEquals("Senior Cloud Infrastructure Architect", role);
    }

    @Test
    @DisplayName("Returns 'Target Role Not Specified' when role cannot be determined reliably")
    void testDetermineJobTitle_unspecifiedFallback() {
        String jdGeneric = """
                About Us
                We are a dynamic, fast-growing startup revolutionizing industry standards.
                Our environment is fast-paced and collaborative.
                Please apply with your resume if interested.
                """;

        String role = matchingService.determineJobTitle(jdGeneric);
        assertEquals("Target Role Not Specified", role);

        assertEquals("Target Role Not Specified", matchingService.determineJobTitle(""));
        assertEquals("Target Role Not Specified", matchingService.determineJobTitle(null));
    }

    // =========================================================================
    // NEW TESTS: KEYWORD FILTERING & SUBSTRING SAFETY
    // =========================================================================

    @Test
    @DisplayName("Filters generic filler words like motivated, eager, learn, collaborate, applications")
    void testKeywordFiltering_filtersGenericWords() {
        String resume = "Software engineer who collaborated on backend applications. Motivated and eager to learn.";
        String jd = """
                Role: Backend Developer
                We are looking for motivated and eager engineers who love to learn and collaborate.
                You will build scalable applications using PostgreSQL, Docker, and REST APIs.
                """;

        AnalysisResult result = matchingService.analyze("resume.pdf", 1024, resume, jd);

        List<String> matchedKws = result.getMatchedKeywords();
        List<String> missingKws = result.getMissingKeywords();

        // Ensure generic buzzwords are NOT present
        assertFalse(matchedKws.contains("motivated"), "Keyword 'motivated' should be filtered");
        assertFalse(matchedKws.contains("eager"), "Keyword 'eager' should be filtered");
        assertFalse(matchedKws.contains("learn"), "Keyword 'learn' should be filtered");
        assertFalse(matchedKws.contains("collaborate"), "Keyword 'collaborate' should be filtered");
        assertFalse(matchedKws.contains("applications"), "Keyword 'applications' should be filtered");
        assertFalse(missingKws.contains("motivated"));
        assertFalse(missingKws.contains("applications"));

        // Ensure real technical terms are preserved
        assertTrue(matchedKws.contains("postgresql") || missingKws.contains("postgresql")
                || matchedKws.contains("docker") || missingKws.contains("docker")
                || matchedKws.contains("rest") || missingKws.contains("rest"));
    }

    // =========================================================================
    // NEW TESTS: SCORING ACCURACY (STRONG, PARTIAL, POOR MATCH)
    // =========================================================================

    @Test
    @DisplayName("Strong match achieves high score and breakdown sums to overall score")
    void testScoring_strongMatch() {
        String resume = """
                Jane Doe - Senior Java Engineer
                
                Experience:
                5 years building distributed microservices with Java, Spring Boot, PostgreSQL, and Docker.
                Architected REST APIs and integrated AWS cloud infrastructure with automated CI/CD pipelines.
                
                Education:
                B.S. in Computer Science.
                
                Skills:
                Java, Spring Boot, PostgreSQL, Docker, AWS, Kubernetes, Git, REST API, Microservices.
                
                Projects:
                E-commerce payment gateway processing 10k transactions per minute.
                
                Certifications:
                AWS Certified Solutions Architect.
                """;

        String jd = """
                Role: Java Backend Developer
                
                Requirements:
                - Java
                - Spring Boot
                - PostgreSQL
                
                Preferred:
                - Docker
                - AWS
                - Kubernetes
                """;

        AnalysisResult result = matchingService.analyze("resume.pdf", 2048, resume, jd);

        // Strong match should score >= 80
        assertTrue(result.getOverallScore() >= 80, "Strong candidate should score >= 80, got: " + result.getOverallScore());
        assertTrue(result.getScoreLabel().equals("Strong Match") || result.getScoreLabel().equals("Good Match"));

        // Invariant: sum of breakdown equals overallScore
        int sum = result.getScoreBreakdown().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(result.getOverallScore(), sum, "Component scores must sum to overallScore");
    }

    @Test
    @DisplayName("Partial match achieves moderate score and breakdown sums to overall score")
    void testScoring_partialMatch() {
        String resume = """
                John Smith - Backend Developer
                
                Experience:
                Software developer working with Java, Spring Boot, and REST APIs.
                
                Education:
                B.S. in Information Systems.
                
                Skills:
                Java, Spring Boot, Git, REST API.
                
                Projects:
                Web inventory tracking service.
                """;

        String jd = """
                Role: Java Backend Engineer
                
                Requirements:
                - Java
                - Spring Boot
                - PostgreSQL
                - Docker
                
                Preferred:
                - AWS
                - Kubernetes
                """;

        AnalysisResult result = matchingService.analyze("resume.pdf", 1024, resume, jd);

        // Partial match with 1 of 4 required skills should score moderately (35 to 75)
        assertTrue(result.getOverallScore() >= 35 && result.getOverallScore() <= 75,
                "Partial match should score between 35 and 75, got: " + result.getOverallScore());

        int sum = result.getScoreBreakdown().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(result.getOverallScore(), sum);
    }

    @Test
    @DisplayName("Poor match achieves low score and breakdown sums to overall score")
    void testScoring_poorMatch() {
        String resume = """
                Graphic Designer
                
                Experience:
                Created brochures and marketing flyers using Photoshop and Illustrator.
                
                Education:
                B.A. in Graphic Design.
                
                Skills:
                Photoshop, Illustrator, InDesign.
                """;

        String jd = """
                Role: Senior Systems Architect
                
                Requirements:
                - Java
                - Spring Boot
                - Kubernetes
                - Docker
                - PostgreSQL
                """;

        AnalysisResult result = matchingService.analyze("resume.pdf", 1024, resume, jd);

        // Poor match with 0 matching skills should score low (< 35)
        assertTrue(result.getOverallScore() < 35,
                "Poor match should score < 35, got: " + result.getOverallScore());
        assertEquals("Needs Alignment", result.getScoreLabel());

        int sum = result.getScoreBreakdown().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(result.getOverallScore(), sum);
    }

    @Test
    @DisplayName("Does not assume skills in 'About the Role' or overview are mandatory required skills")
    void testDistinguishRequiredVsPreferred_doesNotMarkAllSkillsMandatory() {
        String jd = """
                About the Role:
                Our teams collaborate across various stacks including Python, Docker, and Kafka.
                
                Requirements:
                - Java
                - Spring Boot
                
                Nice to Have:
                - PostgreSQL
                """;

        String resume = "Java and Spring Boot software developer.";

        AnalysisResult result = matchingService.analyze("resume.pdf", 1024, resume, jd);

        // Java and Spring Boot are the only mandatory required skills
        assertTrue(result.getMissingRequiredSkills().isEmpty(),
                "Candidate has both Java and Spring Boot; Python/Docker/Kafka from overview must not be mandatory required");
        assertFalse(result.getMissingRequiredSkills().contains("Python"));
        assertFalse(result.getMissingRequiredSkills().contains("Docker"));
    }
}
