package com.darshan.airesumeanalyzer.dto.ai;

import java.io.Serializable;

/**
 * Represents an improved resume bullet point suggestion grounded strictly in existing facts.
 */
public class ResumeBulletSuggestion implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originalContext;
    private String suggestedBullet;
    private String explanation;

    public ResumeBulletSuggestion() {
    }

    public ResumeBulletSuggestion(String originalContext, String suggestedBullet, String explanation) {
        this.originalContext = originalContext;
        this.suggestedBullet = suggestedBullet;
        this.explanation = explanation;
    }

    public String getOriginalContext() {
        return originalContext;
    }

    public void setOriginalContext(String originalContext) {
        this.originalContext = originalContext;
    }

    public String getSuggestedBullet() {
        return suggestedBullet;
    }

    public void setSuggestedBullet(String suggestedBullet) {
        this.suggestedBullet = suggestedBullet;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
