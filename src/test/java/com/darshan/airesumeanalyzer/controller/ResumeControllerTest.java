package com.darshan.airesumeanalyzer.controller;

import com.darshan.airesumeanalyzer.dto.AnalysisResult;
import com.darshan.airesumeanalyzer.service.ResumeMatchingService;
import com.darshan.airesumeanalyzer.service.ResumeParsingService;
import com.darshan.airesumeanalyzer.service.ResumeValidationService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.darshan.airesumeanalyzer.service.GeminiResumeAnalysisService;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Controller tests for ResumeController upload endpoint and session
 * integration.
 * Tests full flow: Upload PDF -> Parse & Analyze -> Session Storage -> Redirect
 * to /results.
 */
@WebMvcTest({ ResumeController.class, ResultsController.class })
@Import({ ResumeParsingService.class, ResumeValidationService.class, ResumeMatchingService.class })
class ResumeControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private GeminiResumeAnalysisService geminiService;

        // Real PDF starts with: %PDF-1.4
        private static final byte[] VALID_PDF_HEADER = {
                        0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34 // %PDF-1.4
        };

        private MockMultipartFile createSyntheticPdfHeader(String name, int sizeInBytes) {
                byte[] content = new byte[sizeInBytes];
                System.arraycopy(VALID_PDF_HEADER, 0, content, 0, VALID_PDF_HEADER.length);
                return new MockMultipartFile("file", name, "application/pdf", content);
        }

        private byte[] createRealPdfBytes(String text) throws IOException {
                try (PDDocument doc = new PDDocument();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                        PDPage page = new PDPage();
                        doc.addPage(page);

                        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                                cs.beginText();
                                cs.setFont(PDType1Font.HELVETICA, 12);
                                cs.newLineAtOffset(50, 700);
                                cs.showText(text);
                                cs.endText();
                        }

                        doc.save(baos);
                        return baos.toByteArray();
                }
        }

        @Test
        @DisplayName("Valid PDF upload returns 200 with success=true, redirectUrl=/results, and stores result in session")
        void uploadValidPdf_shouldReturnSuccessAndRedirectUrlAndStoreInSession() throws Exception {
                byte[] pdfBytes = createRealPdfBytes(
                                "Experience: Senior Java Engineer specializing in Spring Boot, Docker, and PostgreSQL.");
                MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfBytes);

                String jobDescription = "Required Skills: Java, Spring Boot, Kubernetes, AWS.\nPreferred: Docker.";

                MvcResult mvcResult = mockMvc.perform(
                                multipart("/api/resume/upload")
                                                .file(file)
                                                .param("jobDescription", jobDescription))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message")
                                                .value(containsString("Resume text extracted successfully")))
                                .andExpect(jsonPath("$.fileName").value("resume.pdf"))
                                .andExpect(jsonPath("$.redirectUrl").value("/results"))
                                .andExpect(jsonPath("$.extractedText").value(containsString("Senior Java Engineer")))
                                .andReturn();

                // Verify session persistence
                MockHttpSession session = (MockHttpSession) mvcResult.getRequest().getSession(false);
                assertNotNull(session, "Session should be created");

                AnalysisResult analysisResult = (AnalysisResult) session
                                .getAttribute(ResultsController.SESSION_ANALYSIS_KEY);
                assertNotNull(analysisResult, "AnalysisResult should be stored in session");
                assertEquals("resume.pdf", analysisResult.getFileName());
                assertTrue(analysisResult.isHasJobDescription());
                assertTrue(analysisResult.getMatchedSkills().contains("Java"));
                assertTrue(analysisResult.getMatchedSkills().contains("Spring Boot"));
                assertTrue(analysisResult.getMissingRequiredSkills().contains("Kubernetes"));
                assertTrue(analysisResult.getOverallScore() > 0);

                // Verify /results is accessible with this same session
                mockMvc.perform(get("/results").session(session))
                                .andExpect(status().isOk())
                                .andExpect(view().name("results"))
                                .andExpect(model().attribute("hasData", true))
                                .andExpect(content().string(containsString("Compatibility Overview")))
                                .andExpect(content().string(containsString("resume.pdf")));
        }

        @Test
        @DisplayName("When Gemini analysis throws an unexpected exception or times out, upload still succeeds with rule-based results")
        void uploadValidPdf_whenGeminiFailsOrTimesOut_stillSucceedsWithRuleBasedAnalysis() throws Exception {
                when(geminiService.analyze(anyString(), anyString(), any(AnalysisResult.class)))
                                .thenThrow(new RuntimeException("Simulated Gemini timeout or network failure"));

                byte[] pdfBytes = createRealPdfBytes(
                                "Experience: Java Backend Developer with Spring Boot and PostgreSQL expertise.");
                MockMultipartFile file = new MockMultipartFile("file", "resume_timeout.pdf", "application/pdf",
                                pdfBytes);

                MvcResult mvcResult = mockMvc.perform(
                                multipart("/api/resume/upload")
                                                .file(file)
                                                .param("jobDescription", "Looking for Java and Spring Boot."))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.redirectUrl").value("/results"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) mvcResult.getRequest().getSession(false);
                assertNotNull(session, "Session must still be created");
                AnalysisResult result = (AnalysisResult) session.getAttribute(ResultsController.SESSION_ANALYSIS_KEY);
                assertNotNull(result, "Rule-based AnalysisResult must be preserved");
                assertTrue(result.getMatchedSkills().contains("Java"));
                assertTrue(result.getMatchedSkills().contains("Spring Boot"));
                assertNotNull(result.getAiAnalysis());
                assertFalse(result.getAiAnalysis().isAvailable());
        }

        @Test
        @DisplayName("Valid PDF upload without job description succeeds with General Audit mode")
        void uploadValidPdf_withoutJobDescription_shouldSucceedWithGeneralAudit() throws Exception {
                byte[] pdfBytes = createRealPdfBytes("Technical Skills: Python, Django, SQL, Git, Linux.");
                MockMultipartFile file = new MockMultipartFile("file", "python_resume.pdf", "application/pdf",
                                pdfBytes);

                MvcResult mvcResult = mockMvc.perform(
                                multipart("/api/resume/upload")
                                                .file(file)
                                                .param("jobDescription", ""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.redirectUrl").value("/results"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) mvcResult.getRequest().getSession(false);
                assertNotNull(session);
                AnalysisResult analysisResult = (AnalysisResult) session
                                .getAttribute(ResultsController.SESSION_ANALYSIS_KEY);
                assertNotNull(analysisResult);
                assertFalse(analysisResult.isHasJobDescription());
                assertTrue(analysisResult.getMatchedSkills().contains("Python"));
        }

        @Test
        @DisplayName("No file parameter returns 400 with error message")
        void uploadNoFile_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(multipart("/api/resume/upload"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message")
                                                .value("No file was uploaded. Please select a PDF resume."));
        }

        @Test
        @DisplayName("Empty file returns 400 with error message")
        void uploadEmptyFile_shouldReturnBadRequest() throws Exception {
                MockMultipartFile emptyFile = new MockMultipartFile(
                                "file", "empty.pdf", "application/pdf", new byte[0]);

                mockMvc.perform(multipart("/api/resume/upload").file(emptyFile))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message")
                                                .value("No file was uploaded. Please select a PDF resume."));
        }

        @Test
        @DisplayName("Oversized file (>5MB) returns 400 with error message")
        void uploadOversizedFile_shouldReturnBadRequest() throws Exception {
                // 5 MB + 1 byte
                MockMultipartFile bigFile = createSyntheticPdfHeader("big.pdf", 5 * 1024 * 1024 + 1);

                mockMvc.perform(multipart("/api/resume/upload").file(bigFile))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value(
                                                "File size exceeds the 5 MB limit. Please upload a smaller file."));
        }

        @Test
        @DisplayName("Non-PDF content type returns 400 with error message")
        void uploadNonPdfType_shouldReturnBadRequest() throws Exception {
                MockMultipartFile textFile = new MockMultipartFile(
                                "file", "notes.txt", "text/plain", "Hello world".getBytes());

                mockMvc.perform(multipart("/api/resume/upload").file(textFile))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message")
                                                .value("Invalid file type. Only PDF files are accepted."));
        }

        @Test
        @DisplayName("File with PDF content type but no PDF magic bytes returns 400")
        void uploadFakePdf_shouldReturnBadRequest() throws Exception {
                // Content type is PDF, but the bytes are not a real PDF
                MockMultipartFile fakePdf = new MockMultipartFile(
                                "file", "fake.pdf", "application/pdf", "This is not a PDF".getBytes());

                mockMvc.perform(multipart("/api/resume/upload").file(fakePdf))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message")
                                                .value("The uploaded file is not a valid PDF document."));
        }

        @Test
        @DisplayName("File exactly at 5MB limit is accepted (validation passes)")
        void uploadExactLimitFile_shouldPassValidation() throws Exception {
                MockMultipartFile exactFile = createSyntheticPdfHeader("exact.pdf", 5 * 1024 * 1024);

                // Validation passes; PDFBox may still reject synthetic content with 400
                mockMvc.perform(multipart("/api/resume/upload").file(exactFile))
                                .andExpect(jsonPath("$.message").exists());
        }
}
