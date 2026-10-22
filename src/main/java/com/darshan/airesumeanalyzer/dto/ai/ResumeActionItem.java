package com.darshan.airesumeanalyzer.dto.ai;

import java.io.Serializable;

/**
 * Represents a prioritized action item for the candidate.
 */
public class ResumeActionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private int step;
    private String action;
    private String rationale;

    public ResumeActionItem() {
    }

    public ResumeActionItem(int step, String action, String rationale) {
        this.step = step;
        this.action = action;
        this.rationale = rationale;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
