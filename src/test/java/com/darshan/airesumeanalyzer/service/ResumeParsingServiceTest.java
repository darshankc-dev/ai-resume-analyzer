package com.darshan.airesumeanalyzer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResumeParsingService.
 * Generates real PDFs in-memory using PDFBox — no external test files needed.
 */
class ResumeParsingServiceTest {

    private final ResumeValidationService validationService = new ResumeValidationService();
    private final ResumeParsingService parsingService = new ResumeParsingService(validationService);

    /**
     * Helper: creates a real PDF with the given text content.
     */
    private byte[] createPdfWithText(String text) throws IOException {
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

    /**
     * Helper: creates a real PDF with a page but no text content.
     */
    private byte[] createEmptyPagePdf() throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            doc.addPage(new PDPage());
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("Valid text-based PDF extracts text successfully")
    void parseValidTextPdf_shouldReturnSuccess() throws IOException {
        byte[] pdfBytes = createPdfWithText("Darshan Kumar - Software Engineer - Java Spring Boot");

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", pdfBytes);

        ResumeParsingService.ParseResult result = parsingService.parseResume(file);

        assertTrue(result.isSuccess(), "Should succeed for a valid text PDF");
        assertNotNull(result.getText(), "Extracted text should not be null");
        assertTrue(result.getText().contains("Darshan Kumar"), "Text should contain expected content");
        assertTrue(result.getMessage().contains("1 page"), "Message should mention page count");
    }

    @Test
    @DisplayName("PDF with no extractable text returns error")
    void parseEmptyPagePdf_shouldReturnError() throws IOException {
        byte[] pdfBytes = createEmptyPagePdf();

        MockMultipartFile file = new MockMultipartFile(
                "file", "empty-page.pdf", "application/pdf", pdfBytes);

        ResumeParsingService.ParseResult result = parsingService.parseResume(file);

        assertFalse(result.isSuccess(), "Should fail for a PDF with no text");
        assertTrue(result.getMessage().contains("No extractable text"),
                "Message should explain that no text was found");
        assertNull(result.getText(), "Text should be null on failure");
    }

    @Test
    @DisplayName("Malformed PDF content returns error")
    void parseMalformedPdf_shouldReturnError() {
        // Valid PDF header but garbage body — PDFBox can't parse it
        byte[] garbage = new byte[512];
        byte[] header = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34}; // %PDF-1.4
        System.arraycopy(header, 0, garbage, 0, header.length);
        // Fill rest with invalid data
        for (int i = header.length; i < garbage.length; i++) {
            garbage[i] = (byte) 0xFF;
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "corrupt.pdf", "application/pdf", garbage);

        ResumeParsingService.ParseResult result = parsingService.parseResume(file);

        assertFalse(result.isSuccess(), "Should fail for a malformed PDF");
        assertTrue(result.getMessage().toLowerCase().contains("corrupt")
                        || result.getMessage().toLowerCase().contains("malformed")
                        || result.getMessage().toLowerCase().contains("could not be read"),
                "Message should mention corruption or parsing failure");
    }

    @Test
    @DisplayName("Null file returns validation error")
    void parseNullFile_shouldReturnValidationError() {
        ResumeParsingService.ParseResult result = parsingService.parseResume(null);

        assertFalse(result.isSuccess(), "Should fail for null file");
        assertTrue(result.getMessage().contains("No file was uploaded"),
                "Should return the validation error message");
    }

    @Test
    @DisplayName("Non-PDF file returns validation error")
    void parseNonPdf_shouldReturnValidationError() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "Hello world".getBytes());

        ResumeParsingService.ParseResult result = parsingService.parseResume(file);

        assertFalse(result.isSuccess(), "Should fail for a non-PDF file");
        assertTrue(result.getMessage().contains("Invalid file type"),
                "Should return the validation error message");
    }

    @Test
    @DisplayName("Multi-page PDF reports correct page count")
    void parseMultiPagePdf_shouldReportPageCount() throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Add 3 pages with text
            for (int i = 1; i <= 3; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Page " + i + " content");
                    cs.endText();
                }
            }

            doc.save(baos);
            byte[] pdfBytes = baos.toByteArray();

            MockMultipartFile file = new MockMultipartFile(
                    "file", "multi-page.pdf", "application/pdf", pdfBytes);

            ResumeParsingService.ParseResult result = parsingService.parseResume(file);

            assertTrue(result.isSuccess(), "Should succeed for a multi-page PDF");
            assertTrue(result.getMessage().contains("3 pages"), "Message should mention 3 pages");
        }
    }
}
