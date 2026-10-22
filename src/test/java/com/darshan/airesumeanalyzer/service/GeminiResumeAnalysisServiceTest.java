package com.darshan.airesumeanalyzer.service;

import com.darshan.airesumeanalyzer.dto.AnalysisResult;
import com.darshan.airesumeanalyzer.dto.ai.GeminiResumeAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiResumeAnalysisServiceTest {

    @Mock
    private GeminiClientAdapter mockClientAdapter;

    private ObjectMapper objectMapper;
    private GeminiResumeAnalysisService service;

    private static final String VALID_JSON_RESPONSE = """
            {
              "summary": "Experienced Java software engineer with deep expertise in Spring Boot, REST APIs, and microservices.",
              "keyStrengths": [
                {
                  "title": "Spring Boot Microservices",
                  "relevance": "Directly matches the backend development requirements of the role."
                },
                {
                  "title": "Relational Database Design",
                  "relevance": "Demonstrated SQL schema optimization experience matches database criteria."
                }
              ],
              "skillGaps": [
                {
                  "skill": "Kubernetes",
                  "status": "Not mentioned in resume",
                  "recommendation": "If experienced with container orchestration, add a bullet in Projects."
                }
              ],
              "improvementSuggestions": [
                {
                  "area": "Experience",
                  "suggestion": "Include project scope metrics where possible to convey business impact."
                }
              ],
              "bulletPointImprovements": [
                {
                  "originalContext": "Developed REST APIs for users",
                  "suggestedBullet": "Engineered RESTful web services in Spring Boot handling concurrent user requests",
                  "explanation": "Action verb with specific framework mention without inventing false metrics."
                }
              ],
              "projectExperienceRecommendations": [
                "Highlight the architecture of the resume analyzer project to showcase full-stack proficiency."
              ],
              "actionPlan": [
                {
                  "step": 1,
                  "action": "Address Kubernetes gap",
                  "rationale": "Key required skill in the target job description."
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new GeminiResumeAnalysisService(
                mockClientAdapter,
                objectMapper,
                "mock-gemini-api-key",
                "gemini-3.5-flash-lite",
                10
        );
    }

    @Test
    @DisplayName("Valid Gemini JSON response produces fully-populated GeminiResumeAnalysis DTO")
    void analyze_validResponse_returnsPopulatedDto() throws Exception {
        when(mockClientAdapter.generateContent(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(VALID_JSON_RESPONSE);

        AnalysisResult mockResult = new AnalysisResult();
        mockResult.setJobTitle("Java Developer");
        mockResult.setMatchedSkills(List.of("Java", "Spring Boot"));
        mockResult.setMissingRequiredSkills(List.of("Kubernetes"));

        GeminiResumeAnalysis analysis = service.analyze(
                "Experience: Java and Spring Boot engineer with 2 years of building REST APIs.",
                "Job Description: Looking for a Java Developer with Spring Boot and Kubernetes.",
                mockResult
        );

        assertNotNull(analysis);
        assertTrue(analysis.isAvailable());
        assertEquals("Experienced Java software engineer with deep expertise in Spring Boot, REST APIs, and microservices.", analysis.getSummary());
        assertEquals(2, analysis.getKeyStrengths().size());
        assertEquals("Spring Boot Microservices", analysis.getKeyStrengths().get(0).getTitle());
        assertEquals(1, analysis.getSkillGaps().size());
        assertEquals("Kubernetes", analysis.getSkillGaps().get(0).getSkill());
        assertEquals("Not mentioned in resume", analysis.getSkillGaps().get(0).getStatus());
        assertEquals(1, analysis.getImprovementSuggestions().size());
        assertEquals(1, analysis.getBulletPointImprovements().size());
        assertEquals("Engineered RESTful web services in Spring Boot handling concurrent user requests",
                analysis.getBulletPointImprovements().get(0).getSuggestedBullet());
        assertEquals(1, analysis.getProjectExperienceRecommendations().size());
        assertEquals(1, analysis.getActionPlan().size());
        assertEquals(1, analysis.getActionPlan().get(0).getStep());
        assertNotNull(analysis.getDisclaimer());

        verify(mockClientAdapter, times(1)).generateContent(eq("mock-gemini-api-key"), eq("gemini-3.5-flash-lite"), anyString(), eq(10));
    }

    @Test
    @DisplayName("Valid Gemini JSON response wrapped in markdown code fence is cleaned and parsed")
    void analyze_markdownFencedJson_isCleanedAndParsed() throws Exception {
        String fencedJson = "```json\n" + VALID_JSON_RESPONSE + "\n```";
        when(mockClientAdapter.generateContent(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(fencedJson);

        GeminiResumeAnalysis analysis = service.analyze("Java developer resume", "Java job", new AnalysisResult());

        assertTrue(analysis.isAvailable());
        assertEquals(2, analysis.getKeyStrengths().size());
    }

    @Test
    @DisplayName("Missing or empty API key fails gracefully without calling Gemini client adapter")
    void analyze_missingApiKey_returnsUnavailableGracefully() throws Exception {
        GeminiResumeAnalysisService noKeyService = new GeminiResumeAnalysisService(
                mockClientAdapter, objectMapper, "", "gemini-3.5-flash-lite", 10
        );

        GeminiResumeAnalysis analysis = noKeyService.analyze("Java developer resume", "Java job", new AnalysisResult());

        assertNotNull(analysis);
        assertFalse(analysis.isAvailable());
        assertTrue(analysis.getStatusMessage().contains("API key is not configured"));
        verifyNoInteractions(mockClientAdapter);
    }

    @Test
    @DisplayName("Null or empty resume text fails gracefully without calling Gemini client adapter")
    void analyze_emptyResumeText_returnsUnavailableGracefully() {
        GeminiResumeAnalysis analysis = service.analyze("", "Java job", new AnalysisResult());

        assertNotNull(analysis);
        assertFalse(analysis.isAvailable());
        assertTrue(analysis.getStatusMessage().contains("empty resume"));
        verifyNoInteractions(mockClientAdapter);
    }

    @Test
    @DisplayName("Malformed JSON from Gemini is handled gracefully without crashing")
    void analyze_malformedJson_returnsUnavailableGracefully() throws Exception {
        when(mockClientAdapter.generateContent(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("{ not valid json: broken... ");

        GeminiResumeAnalysis analysis = service.analyze("Java resume", "Java job", new AnalysisResult());

        assertNotNull(analysis);
        assertFalse(analysis.isAvailable());
        assertTrue(analysis.getStatusMessage().contains("could not be parsed") || analysis.getStatusMessage().contains("RESPONSE_PARSING_ERROR"));
    }

    @Test
    @DisplayName("Empty or blank model output is handled gracefully")
    void analyze_emptyModelOutput_returnsUnavailableGracefully() throws Exception {
        when(mockClientAdapter.generateContent(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("   ");

        GeminiResumeAnalysis analysis = service.analyze("Java resume", "Java job", new AnalysisResult());

        assertNotNull(analysis);
        assertFalse(analysis.isAvailable());
        assertTrue(analysis.getStatusMessage().contains("EMPTY_RESPONSE"));
    }

    @Test
    @DisplayName("SDK 404 NOT_FOUND exception (model deprecated/unavailable) is caught, logged, and categorized")
    void analyze_modelNotFoundApiException_handledGracefully() throws Exception {
        com.google.genai.errors.ApiException mockApiEx = mock(com.google.genai.errors.ApiException.class);
        when(mockApiEx.code()).thenReturn(404);
        when(mockApiEx.status()).thenReturn("NOT_FOUND");
        when(mockClientAdapter.generateContent(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(mockApiEx);

        GeminiResumeAnalysis analysis = service.analyze("Java resume", "Java job", new AnalysisResult());

        assertNotNull(analysis);
        assertFalse(analysis.isAvailable());
        assertTrue(analysis.getStatusMessage().contains("MODEL_NOT_FOUND_OR_DEPRECATED"));
        assertTrue(analysis.getStatusMessage().contains("404"));
    }

    @Test
    @DisplayName("SDK 401 UNAUTHENTICATED exception is caught and categorized")
    void analyze_unauthenticatedApiException_handledGracefully() throws Exception {
        com.google.genai.errors.ApiException mockApiEx = mock(com.google.genai.errors.ApiException.class);
        when(mockApiEx.code()).thenReturn(401);
        when(mockApiEx.status()).thenReturn("UNAUTHENTICATED");
        when(mockClientAdapter.generateContent(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(mockApiEx);

        GeminiResumeAnalysis analysis = service.analyze("Java resume", "Java job", new AnalysisResult());

        assertNotNull(analysis);
        assertFalse(analysis.isAvailable());
        assertTrue(analysis.getStatusMessage().contains("UNAUTHORIZED_API_KEY"));
        assertTrue(analysis.getStatusMessage().contains("401"));
    }

    @Test
    @DisplayName("SDK 429 RESOURCE_EXHAUSTED exception is caught and categorized")
    void analyze_quotaExceededApiException_handledGracefully() throws Exception {
        com.google.genai.errors.ApiException mockApiEx = mock(com.google.genai.errors.ApiException.class);
        when(mockApiEx.code()).thenReturn(429);
        when(mockApiEx.status()).thenReturn("RESOURCE_EXHAUSTED");
        when(mockClientAdapter.generateContent(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(mockApiEx);

        GeminiResumeAnalysis analysis = service.analyze("Java resume", "Java job", new AnalysisResult());

        assertNotNull(analysis);
        assertFalse(analysis.isAvailable());
        assertTrue(analysis.getStatusMessage().contains("QUOTA_OR_RATE_LIMIT_EXCEEDED"));
        assertTrue(analysis.getStatusMessage().contains("429"));
    }

    @Test
    @DisplayName("Timeout or network failure is caught and handled gracefully")
    void analyze_timeoutException_returnsUnavailableGracefully() throws Exception {
        when(mockClientAdapter.generateContent(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new TimeoutException("Request timed out after 30 seconds"));

        GeminiResumeAnalysis analysis = service.analyze("Java resume", "Java job", new AnalysisResult());

        assertNotNull(analysis);
        assertFalse(analysis.isAvailable());
        assertTrue(analysis.getStatusMessage().contains("REQUEST_TIMEOUT"));
    }

    @Test
    @DisplayName("Resume text PII (email, phone, URL) is masked before prompt construction")
    void sanitizeResumeText_masksEmailsPhonesAndUrls() {
        String rawResume = "Contact: john.doe@example.com, Phone: +1 (555) 123-4567, Site: https://johndoe.dev";
        String sanitized = service.sanitizeResumeText(rawResume);

        assertFalse(sanitized.contains("john.doe@example.com"));
        assertFalse(sanitized.contains("555"));
        assertFalse(sanitized.contains("https://johndoe.dev"));
        assertTrue(sanitized.contains("[EMAIL REDACTED]"));
        assertTrue(sanitized.contains("[PHONE REDACTED]"));
        assertTrue(sanitized.contains("[LINK REDACTED]"));
    }
}
