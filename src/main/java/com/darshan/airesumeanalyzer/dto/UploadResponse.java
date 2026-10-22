package com.darshan.airesumeanalyzer.dto;

/**
 * Structured JSON response returned by the resume upload endpoint.
 * Used for both success and error cases.
 */
public class UploadResponse {

    private boolean success;
    private String message;
    private String fileName;
    private long fileSize;
    private String extractedText;
    private String redirectUrl;

    public UploadResponse() {
    }

    public UploadResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public UploadResponse(boolean success, String message, String fileName, long fileSize) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public UploadResponse(boolean success, String message, String fileName, long fileSize, String extractedText) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.extractedText = extractedText;
    }

    public UploadResponse(boolean success, String message, String fileName, long fileSize, String extractedText, String redirectUrl) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.extractedText = extractedText;
        this.redirectUrl = redirectUrl;
    }

    // --- Getters and Setters ---

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
