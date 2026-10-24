package com.skillsense.analytics.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class FastApiResponseRow {

    @JsonProperty("student_id")
    private Long studentId;

    private String branch;
    private Integer year;
    private Map<String, Object> answers;

    public FastApiResponseRow() {
    }

    public FastApiResponseRow(Long studentId, String branch, Integer year, Map<String, Object> answers) {
        this.studentId = studentId;
        this.branch = branch;
        this.year = year;
        this.answers = answers;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Map<String, Object> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, Object> answers) {
        this.answers = answers;
    }
}
