package com.darshan.airesumeanalyzer.dto.ai;

import java.io.Serializable;

/**
 * Represents a skill gap identified against the job description.
 * Distinguishes unmentioned skills from confirmed missing prerequisites.
 */
public class ResumeSkillGap implements Serializable {

    private static final long serialVersionUID = 1L;

    private String skill;
    private String status;
    private String recommendation;

    public ResumeSkillGap() {
    }

    public ResumeSkillGap(String skill, String status, String recommendation) {
        this.skill = skill;
        this.status = status;
        this.recommendation = recommendation;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
