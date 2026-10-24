package com.skillsense.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class StudentResultResponse {

    private double readiness;
    private String band;
    private List<SkillResultItem> skills = new ArrayList<>();

    @JsonProperty("strongest_skill")
    private String strongestSkill;

    @JsonProperty("weakest_skill")
    private String weakestSkill;

    private List<String> tips = new ArrayList<>();
    private String disclaimer = "Self-assessed indicator, not an exam score";

    public StudentResultResponse() {
    }

    public StudentResultResponse(double readiness, String band, List<SkillResultItem> skills,
                                 String strongestSkill, String weakestSkill, List<String> tips, String disclaimer) {
        this.readiness = readiness;
        this.band = band;
        this.skills = skills != null ? skills : new ArrayList<>();
        this.strongestSkill = strongestSkill;
        this.weakestSkill = weakestSkill;
        this.tips = tips != null ? tips : new ArrayList<>();
        if (disclaimer != null) {
            this.disclaimer = disclaimer;
        }
    }

    public double getReadiness() {
        return readiness;
    }

    public void setReadiness(double readiness) {
        this.readiness = readiness;
    }

    public String getBand() {
        return band;
    }

    public void setBand(String band) {
        this.band = band;
    }

    public List<SkillResultItem> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillResultItem> skills) {
        this.skills = skills;
    }

    public String getStrongestSkill() {
        return strongestSkill;
    }

    public void setStrongestSkill(String strongestSkill) {
        this.strongestSkill = strongestSkill;
    }

    public String getWeakestSkill() {
        return weakestSkill;
    }

    public void setWeakestSkill(String weakestSkill) {
        this.weakestSkill = weakestSkill;
    }

    public List<String> getTips() {
        return tips;
    }

    public void setTips(List<String> tips) {
        this.tips = tips;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public static class SkillResultItem {
        private String skill;
        private double score;
        private double target;
        private double gap;

        @JsonProperty("cohort_mean")
        private Double cohortMean;

        public SkillResultItem() {
        }

        public SkillResultItem(String skill, double score, double target, double gap, Double cohortMean) {
            this.skill = skill;
            this.score = score;
            this.target = target;
            this.gap = gap;
            this.cohortMean = cohortMean;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public double getTarget() {
            return target;
        }

        public void setTarget(double target) {
            this.target = target;
        }

        public double getGap() {
            return gap;
        }

        public void setGap(double gap) {
            this.gap = gap;
        }

        public Double getCohortMean() {
            return cohortMean;
        }

        public void setCohortMean(Double cohortMean) {
            this.cohortMean = cohortMean;
        }
    }
}
