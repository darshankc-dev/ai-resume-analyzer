package com.darshan.airesumeanalyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Validates uploaded resume files.
 * Checks: presence, non-empty, size limit, content type, and PDF magic bytes.
 */
@Service
public class ResumeValidationService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    // PDF files always start with these 5 bytes: %PDF-
    private static final byte[] PDF_MAGIC_BYTES = {0x25, 0x50, 0x44, 0x46, 0x2D};

    /**
     * Validates the uploaded file and returns an error message, or null if valid.
     */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "No file was uploaded. Please select a PDF resume.";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return "File size exceeds the 5 MB limit. Please upload a smaller file.";
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            return "Invalid file type. Only PDF files are accepted.";
        }

        if (!hasPdfMagicBytes(file)) {
            return "The uploaded file is not a valid PDF document.";
        }

        return null; // valid
    }

    /**
     * Reads the first 5 bytes of the file to verify the %PDF- header.
     */
    private boolean hasPdfMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[PDF_MAGIC_BYTES.length];
            int bytesRead = is.read(header);
            if (bytesRead < PDF_MAGIC_BYTES.length) {
                return false;
            }
            for (int i = 0; i < PDF_MAGIC_BYTES.length; i++) {
                if (header[i] != PDF_MAGIC_BYTES[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
