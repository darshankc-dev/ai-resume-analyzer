package com.darshan.airesumeanalyzer.dto.ai;

import java.io.Serializable;

/**
 * Represents an actionable suggestion for resume improvement.
 */
public class ResumeImprovement implements Serializable {

    private static final long serialVersionUID = 1L;

    private String area;
    private String suggestion;

    public ResumeImprovement() {
    }

    public ResumeImprovement(String area, String suggestion) {
        this.area = area;
        this.suggestion = suggestion;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
