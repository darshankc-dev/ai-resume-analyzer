package com.darshan.airesumeanalyzer.controller;

import com.darshan.airesumeanalyzer.dto.AnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ResultsController.
 * Verifies results page access with active session data and missing session data handling.
 */
@WebMvcTest(ResultsController.class)
class ResultsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /results with active session data renders dashboard view")
    void getResultsWithSession_shouldRenderDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();

        AnalysisResult result = new AnalysisResult();
        result.setId("test-123");
        result.setFileName("my_resume.pdf");
        result.setJobTitle("Backend Developer");
        result.setOverallScore(82);
        result.setScoreLabel("Good Match");
        result.setScoreBreakdown(Map.of("Skills", 40, "Keywords", 18, "Structure", 12));
        result.setMatchedSkills(List.of("Java", "Spring Boot"));
        result.setMissingRequiredSkills(List.of("Kubernetes"));
        result.setAdditionalSkills(List.of("Docker"));
        result.setDetectedSections(List.of("Experience", "Education", "Skills"));
        result.setSuggestions(List.of("Add more project details"));
        result.setDisclaimer("Preliminary rule-based compatibility estimate, not an official ATS score.");

        session.setAttribute(ResultsController.SESSION_ANALYSIS_KEY, result);

        mockMvc.perform(get("/results").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("results"))
                .andExpect(model().attribute("hasData", true))
                .andExpect(model().attribute("result", hasProperty("fileName", equalTo("my_resume.pdf"))))
                .andExpect(model().attribute("result", hasProperty("overallScore", equalTo(82))))
                .andExpect(content().string(containsString("Compatibility Overview")))
                .andExpect(content().string(containsString("my_resume.pdf")))
                .andExpect(content().string(containsString("Backend Developer")))
                .andExpect(content().string(containsString("AI Insights Status")));
    }

    @Test
    @DisplayName("GET /results with active AI analysis renders AI insights and disclaimer")
    void getResultsWithAiAnalysis_shouldRenderAiInsights() throws Exception {
        MockHttpSession session = new MockHttpSession();

        AnalysisResult result = new AnalysisResult();
        result.setId("ai-test-123");
        result.setFileName("ai_resume.pdf");
        result.setJobTitle("Cloud Engineer");
        result.setOverallScore(90);
        result.setScoreLabel("Strong Match");

        com.darshan.airesumeanalyzer.dto.ai.GeminiResumeAnalysis ai = new com.darshan.airesumeanalyzer.dto.ai.GeminiResumeAnalysis();
        ai.setAvailable(true);
        ai.setSummary("Candidate demonstrates solid Java backend and AWS cloud knowledge.");
        ai.setKeyStrengths(List.of(new com.darshan.airesumeanalyzer.dto.ai.ResumeStrength("Cloud Architecture", "Strong AWS background aligns with the job")));
        ai.setSkillGaps(List.of(new com.darshan.airesumeanalyzer.dto.ai.ResumeSkillGap("Terraform", "Not mentioned in resume", "Highlight IaC experience")));
        ai.setImprovementSuggestions(List.of(new com.darshan.airesumeanalyzer.dto.ai.ResumeImprovement("Projects", "Add metrics to show infrastructure scale")));
        ai.setBulletPointImprovements(List.of(new com.darshan.airesumeanalyzer.dto.ai.ResumeBulletSuggestion("Built cloud apps", "Architected cloud microservices using Spring Cloud", "Clearer action verb")));
        ai.setProjectExperienceRecommendations(List.of("Detail AWS cost reduction project"));
        ai.setActionPlan(List.of(new com.darshan.airesumeanalyzer.dto.ai.ResumeActionItem(1, "Update Skills Section", "Add missing Terraform keyword if familiar")));
        result.setAiAnalysis(ai);

        session.setAttribute(ResultsController.SESSION_ANALYSIS_KEY, result);

        mockMvc.perform(get("/results").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("results"))
                .andExpect(model().attribute("hasData", true))
                .andExpect(content().string(containsString("Personalized <span class=\"gradient-text\">AI Resume Feedback</span>")))
                .andExpect(content().string(containsString("Executive Candidate Summary")))
                .andExpect(content().string(containsString("Candidate demonstrates solid Java backend")))
                .andExpect(content().string(containsString("Cloud Architecture")))
                .andExpect(content().string(containsString("Terraform")))
                .andExpect(content().string(containsString("Architected cloud microservices using Spring Cloud")))
                .andExpect(content().string(containsString("AI-generated insights are automated suggestions")));
    }

    @Test
    @DisplayName("GET /results without session data renders friendly missing-data view")
    void getResultsWithoutSession_shouldRenderFriendlyMissingDataView() throws Exception {
        mockMvc.perform(get("/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("results"))
                .andExpect(model().attribute("hasData", false))
                .andExpect(model().attribute("errorMessage", containsString("No analysis data found")))
                .andExpect(content().string(containsString("No Analysis Data Found")))
                .andExpect(content().string(containsString("Upload Resume")));
    }
}
