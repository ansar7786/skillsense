package com.skillsense.user.dto;

import com.skillsense.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "College ID is required")
    @Size(max = 30, message = "College ID cannot exceed 30 characters")
    private String collegeId;

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name cannot exceed 120 characters")
    private String name;

    @Size(max = 160, message = "Email cannot exceed 160 characters")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    @Size(max = 20, message = "Branch cannot exceed 20 characters")
    private String branch;

    private Integer studyYear;

    @Size(max = 5, message = "Section cannot exceed 5 characters")
    private String section;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String collegeId, String name, String email, Role role, String branch, Integer studyYear, String section) {
        this.collegeId = collegeId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.branch = branch;
        this.studyYear = studyYear;
        this.section = section;
    }

    public String getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(String collegeId) {
        this.collegeId = collegeId;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String collegeId;
        private String name;
        private String email;
        private Role role;
        private String branch;
        private Integer studyYear;
        private String section;

        public Builder collegeId(String collegeId) {
            this.collegeId = collegeId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
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

        public CreateUserRequest build() {
            return new CreateUserRequest(collegeId, name, email, role, branch, studyYear, section);
        }
    }
}
