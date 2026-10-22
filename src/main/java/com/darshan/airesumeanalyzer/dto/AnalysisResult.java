package com.darshan.airesumeanalyzer.dto;

import com.darshan.airesumeanalyzer.dto.ai.GeminiResumeAnalysis;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object containing the full results of a resume analysis.
 * Stored in server-side session during the analysis flow.
 */
public class AnalysisResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String fileName;
    private long fileSize;
    private String jobTitle;
    private boolean hasJobDescription;

    private int overallScore;
    private String scoreLabel;
    private Map<String, Integer> scoreBreakdown = new LinkedHashMap<>();

    private List<String> matchedSkills = new ArrayList<>();
    private List<String> missingRequiredSkills = new ArrayList<>();
    private List<String> missingPreferredSkills = new ArrayList<>();
    private List<String> additionalSkills = new ArrayList<>();

    private int keywordCoveragePercent;
    private List<String> matchedKeywords = new ArrayList<>();
    private List<String> missingKeywords = new ArrayList<>();

    private List<String> detectedSections = new ArrayList<>();
    private int wordCount;
    private int characterCount;

    private List<String> suggestions = new ArrayList<>();
    private String disclaimer;
    private String analyzedAt;

    private GeminiResumeAnalysis aiAnalysis;

    public AnalysisResult() {
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public boolean isHasJobDescription() {
        return hasJobDescription;
    }

    public void setHasJobDescription(boolean hasJobDescription) {
        this.hasJobDescription = hasJobDescription;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public String getScoreLabel() {
        return scoreLabel;
    }

    public void setScoreLabel(String scoreLabel) {
        this.scoreLabel = scoreLabel;
    }

    public Map<String, Integer> getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(Map<String, Integer> scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown != null ? scoreBreakdown : new LinkedHashMap<>();
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills != null ? matchedSkills : new ArrayList<>();
    }

    public List<String> getMissingRequiredSkills() {
        return missingRequiredSkills;
    }

    public void setMissingRequiredSkills(List<String> missingRequiredSkills) {
        this.missingRequiredSkills = missingRequiredSkills != null ? missingRequiredSkills : new ArrayList<>();
    }

    public List<String> getMissingPreferredSkills() {
        return missingPreferredSkills;
    }

    public void setMissingPreferredSkills(List<String> missingPreferredSkills) {
        this.missingPreferredSkills = missingPreferredSkills != null ? missingPreferredSkills : new ArrayList<>();
    }

    public List<String> getAdditionalSkills() {
        return additionalSkills;
    }

    public void setAdditionalSkills(List<String> additionalSkills) {
        this.additionalSkills = additionalSkills != null ? additionalSkills : new ArrayList<>();
    }

    public int getKeywordCoveragePercent() {
        return keywordCoveragePercent;
    }

    public void setKeywordCoveragePercent(int keywordCoveragePercent) {
        this.keywordCoveragePercent = keywordCoveragePercent;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords != null ? matchedKeywords : new ArrayList<>();
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords != null ? missingKeywords : new ArrayList<>();
    }

    public List<String> getDetectedSections() {
        return detectedSections;
    }

    public void setDetectedSections(List<String> detectedSections) {
        this.detectedSections = detectedSections != null ? detectedSections : new ArrayList<>();
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(int characterCount) {
        this.characterCount = characterCount;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions != null ? suggestions : new ArrayList<>();
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(String analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public GeminiResumeAnalysis getAiAnalysis() {
        return aiAnalysis;
    }

    public void setAiAnalysis(GeminiResumeAnalysis aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }
}
