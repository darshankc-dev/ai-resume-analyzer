package com.darshan.airesumeanalyzer.controller;

import com.darshan.airesumeanalyzer.dto.AnalysisResult;
import com.darshan.airesumeanalyzer.dto.UploadResponse;
import com.darshan.airesumeanalyzer.dto.ai.GeminiResumeAnalysis;
import com.darshan.airesumeanalyzer.service.GeminiResumeAnalysisService;
import com.darshan.airesumeanalyzer.service.ResumeMatchingService;
import com.darshan.airesumeanalyzer.service.ResumeParsingService;
import com.darshan.airesumeanalyzer.service.ResumeParsingService.ParseResult;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for resume upload, text extraction, and compatibility analysis.
 * Workflow: Upload → Validate → Parse → Rule-based Match → Gemini AI Insights → Store in session.
 */
@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeParsingService parsingService;
    private final ResumeMatchingService matchingService;
    private final GeminiResumeAnalysisService geminiService;

    public ResumeController(ResumeParsingService parsingService,
                            ResumeMatchingService matchingService,
                            GeminiResumeAnalysisService geminiService) {
        this.parsingService = parsingService;
        this.matchingService = matchingService;
        this.geminiService = geminiService;
    }

    /**
     * Accepts a PDF resume upload, validates it, extracts text, performs
     * deterministic skill and keyword analysis, invokes Gemini AI for personalized insights,
     * stores results in the HTTP session, and returns a structured response with a redirect URL.
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadResume(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "jobDescription", required = false) String jobDescription,
            HttpSession session) {

        long startTime = System.currentTimeMillis();

        // Stage 1: Validation and PDF text extraction
        ParseResult result = parsingService.parseResume(file);
        long t1 = System.currentTimeMillis();
        logger.info("Stage 1 (PDF parsing): Completed in {} ms (success={})", (t1 - startTime), result.isSuccess());

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(false, result.getMessage()));
        }

        String fileName = file.getOriginalFilename();
        long fileSize = file.getSize();

        // Stage 2: Deterministic rule-based analysis
        AnalysisResult analysisResult = matchingService.analyze(fileName, fileSize, result.getText(), jobDescription);
        long t2 = System.currentTimeMillis();
        logger.info("Stage 2 (Rule-based matching): Completed in {} ms (overallScore={})", (t2 - t1), analysisResult.getOverallScore());

        // Stage 3: Gemini AI personalized analysis
        if (geminiService != null) {
            try {
                GeminiResumeAnalysis aiAnalysis = geminiService.analyze(result.getText(), jobDescription, analysisResult);
                analysisResult.setAiAnalysis(aiAnalysis);
                long t3 = System.currentTimeMillis();
                logger.info("Stage 3 (Gemini AI analysis): Completed in {} ms (aiAvailable={})",
                        (t3 - t2), (aiAnalysis != null && aiAnalysis.isAvailable()));
            } catch (Exception ex) {
                logger.warn("Stage 3 (Gemini AI analysis): Failed with exception, falling back gracefully: {}", ex.getMessage());
                analysisResult.setAiAnalysis(GeminiResumeAnalysis.unavailable("AI insights are temporarily unavailable."));
            }
        }

        // Stage 4: Session storage
        if (session != null) {
            session.setAttribute(ResultsController.SESSION_ANALYSIS_KEY, analysisResult);
        }
        long tEnd = System.currentTimeMillis();
        logger.info("Stage 4 (Session storage): Complete. Total upload request processing time: {} ms", (tEnd - startTime));

        return ResponseEntity.ok(
                new UploadResponse(true, result.getMessage(), fileName, fileSize, result.getText(), "/results")
        );
    }
}
