package com.skillsense.user.dto;

import com.skillsense.user.Role;
import com.skillsense.user.User;

import java.time.LocalDateTime;

public class UserResponse {
    private Long userId;
    private String collegeId;
    private String name;
    private String email;
    private Role role;
    private String branch;
    private Integer studyYear;
    private String section;
    private Boolean isActive;
    private Boolean mustChangePassword;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(Long userId, String collegeId, String name, String email, Role role,
                        String branch, Integer studyYear, String section,
                        Boolean isActive, Boolean mustChangePassword, LocalDateTime createdAt) {
        this.userId = userId;
        this.collegeId = collegeId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.branch = branch;
        this.studyYear = studyYear;
        this.section = section;
        this.isActive = isActive;
        this.mustChangePassword = mustChangePassword;
        this.createdAt = createdAt;
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .userId(user.getUserId())
                .collegeId(user.getCollegeId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .branch(user.getBranch())
                .studyYear(user.getStudyYear())
                .section(user.getSection())
                .isActive(user.getIsActive())
                .mustChangePassword(user.getMustChangePassword())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String collegeId;
        private String name;
        private String email;
        private Role role;
        private String branch;
        private Integer studyYear;
        private String section;
        private Boolean isActive;
        private Boolean mustChangePassword;
        private LocalDateTime createdAt;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

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

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder mustChangePassword(Boolean mustChangePassword) {
            this.mustChangePassword = mustChangePassword;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserResponse build() {
            return new UserResponse(userId, collegeId, name, email, role, branch, studyYear, section, isActive, mustChangePassword, createdAt);
        }
    }
}
