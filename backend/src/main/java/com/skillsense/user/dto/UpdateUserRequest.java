package com.skillsense.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name cannot exceed 120 characters")
    private String name;

    @Size(max = 160, message = "Email cannot exceed 160 characters")
    private String email;

    @Size(max = 20, message = "Branch cannot exceed 20 characters")
    private String branch;

    private Integer studyYear;

    @Size(max = 5, message = "Section cannot exceed 5 characters")
    private String section;

    private Boolean isActive;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String name, String email, String branch, Integer studyYear, String section, Boolean isActive) {
        this.name = name;
        this.email = email;
        this.branch = branch;
        this.studyYear = studyYear;
        this.section = section;
        this.isActive = isActive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getStudyYear() {
        return studyYear;
    }

    public void setStudyYear(Integer studyYear) {
        this.studyYear = studyYear;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String email;
        private String branch;
        private Integer studyYear;
        private String section;
        private Boolean isActive;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder branch(String branch) {
            this.branch = branch;
            return this;
        }

        public Builder studyYear(Integer studyYear) {
            this.studyYear = studyYear;
            return this;
        }

        public Builder section(String section) {
            this.section = section;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public UpdateUserRequest build() {
            return new UpdateUserRequest(name, email, branch, studyYear, section, isActive);
        }
    }
}
