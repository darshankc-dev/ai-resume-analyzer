package com.darshan.airesumeanalyzer.dto.ai;

import java.io.Serializable;

/**
 * Represents a key candidate strength aligned with the target role.
 */
public class ResumeStrength implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String relevance;

    public ResumeStrength() {
    }

    public ResumeStrength(String title, String relevance) {
        this.title = title;
        this.relevance = relevance;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRelevance() {
        return relevance;
    }

    public void setRelevance(String relevance) {
        this.relevance = relevance;
    }
}
