package com.darshan.airesumeanalyzer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extracts readable text from uploaded PDF resumes using Apache PDFBox.
 * Reuses {@link ResumeValidationService} for pre-parse validation.
 *
 * <p>Phase 3: text extraction only — no AI analysis or persistence.</p>
 */
@Service
public class ResumeParsingService {

    private static final int MAX_TEXT_LENGTH = 30_000;

    private final ResumeValidationService validationService;

    public ResumeParsingService(ResumeValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * Result holder for a parsing attempt.
     */
    public static class ParseResult {
        private final boolean success;
        private final String message;
        private final String text;

        private ParseResult(boolean success, String message, String text) {
            this.success = success;
            this.message = message;
            this.text = text;
        }

        public static ParseResult success(String message, String text) {
            return new ParseResult(true, message, text);
        }

        public static ParseResult error(String message) {
            return new ParseResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getText() { return text; }
    }

    /**
     * Validates and parses the uploaded PDF, returning extracted text or an error.
     */
    public ParseResult parseResume(MultipartFile file) {
        // Step 1: Reuse existing validation
        String validationError = validationService.validate(file);
        if (validationError != null) {
            return ParseResult.error(validationError);
        }

        // Step 2: Extract text via PDFBox
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            // Reject encrypted PDFs that require a password to read
            if (document.isEncrypted()) {
                return ParseResult.error(
                        "This PDF is encrypted or password-protected. Please upload an unprotected resume.");
            }

            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                return ParseResult.error(
                        "The uploaded PDF has no pages. Please upload a valid resume.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);

            // Normalize whitespace and trim
            String text = rawText == null ? "" : rawText.strip();

            if (text.isEmpty()) {
                return ParseResult.error(
                        "No extractable text was found. The PDF may be a scanned image. "
                        + "Please upload a text-based resume.");
            }

            // Truncate to max length
            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH);
            }

            return ParseResult.success(
                    "Resume text extracted successfully (" + pageCount
                            + (pageCount == 1 ? " page" : " pages") + ").",
                    text
            );

        } catch (IOException e) {
            return ParseResult.error(
                    "The PDF could not be read. The file may be corrupted or malformed.");
        }
    }
}
