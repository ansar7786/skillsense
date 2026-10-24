package com.skillsense.survey.entity;

import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.term.AcademicTerm;
import com.skillsense.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "surveys")
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_id")
    private Long surveyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private SurveyTemplate template;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private SurveyCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm term;

    @Column(name = "question_schema", nullable = false, columnDefinition = "JSON")
    private String questionSchema;

    @Column(name = "skill_targets", columnDefinition = "JSON")
    private String skillTargets;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;

    @Column(name = "target_filter", nullable = false, columnDefinition = "JSON")
    private String targetFilter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SurveyStatus status = SurveyStatus.DRAFT;

    @Column(name = "opens_at")
    private LocalDateTime opensAt;

    @Column(name = "closes_at")
    private LocalDateTime closesAt;

    @Column(name = "eligible_count", nullable = false)
    private Integer eligibleCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    public Survey() {
    }

    public Survey(Long surveyId, SurveyTemplate template, Integer version, String title, String description,
                  SurveyCategory category, AcademicTerm term, String questionSchema, String skillTargets,
                  Boolean isAnonymous, String targetFilter, SurveyStatus status, LocalDateTime opensAt,
                  LocalDateTime closesAt, Integer eligibleCount, User createdBy, LocalDateTime createdAt,
                  LocalDateTime activatedAt) {
        this.surveyId = surveyId;
        this.template = template;
        this.version = version != null ? version : 1;
        this.title = title;
        this.description = description;
        this.category = category;
        this.term = term;
        this.questionSchema = questionSchema;
        this.skillTargets = skillTargets;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
        this.targetFilter = targetFilter;
        this.status = status != null ? status : SurveyStatus.DRAFT;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.eligibleCount = eligibleCount != null ? eligibleCount : 0;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.activatedAt = activatedAt;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public SurveyTemplate getTemplate() {
        return template;
    }

    public void setTemplate(SurveyTemplate template) {
        this.template = template;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SurveyCategory getCategory() {
        return category;
    }

    public void setCategory(SurveyCategory category) {
        this.category = category;
    }

    public AcademicTerm getTerm() {
        return term;
    }

    public void setTerm(AcademicTerm term) {
        this.term = term;
    }

    public String getQuestionSchema() {
        return questionSchema;
    }

    public void setQuestionSchema(String questionSchema) {
        this.questionSchema = questionSchema;
    }

    public String getSkillTargets() {
        return skillTargets;
    }

    public void setSkillTargets(String skillTargets) {
        this.skillTargets = skillTargets;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public String getTargetFilter() {
        return targetFilter;
    }

    public void setTargetFilter(String targetFilter) {
        this.targetFilter = targetFilter;
    }

    public SurveyStatus getStatus() {
        return status;
    }

    public void setStatus(SurveyStatus status) {
        this.status = status;
    }

    public LocalDateTime getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(LocalDateTime opensAt) {
        this.opensAt = opensAt;
    }

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalDateTime closesAt) {
        this.closesAt = closesAt;
    }

    public Integer getEligibleCount() {
        return eligibleCount;
    }

    public void setEligibleCount(Integer eligibleCount) {
        this.eligibleCount = eligibleCount;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long surveyId;
        private SurveyTemplate template;
        private Integer version = 1;
        private String title;
        private String description;
        private SurveyCategory category;
        private AcademicTerm term;
        private String questionSchema;
        private String skillTargets;
        private Boolean isAnonymous = false;
        private String targetFilter;
        private SurveyStatus status = SurveyStatus.DRAFT;
        private LocalDateTime opensAt;
        private LocalDateTime closesAt;
        private Integer eligibleCount = 0;
        private User createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime activatedAt;

        public Builder surveyId(Long surveyId) {
            this.surveyId = surveyId;
            return this;
        }

        public Builder template(SurveyTemplate template) {
            this.template = template;
            return this;
        }

        public Builder version(Integer version) {
            this.version = version;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(SurveyCategory category) {
            this.category = category;
            return this;
        }

        public Builder term(AcademicTerm term) {
            this.term = term;
            return this;
        }

        public Builder questionSchema(String questionSchema) {
            this.questionSchema = questionSchema;
            return this;
        }

        public Builder skillTargets(String skillTargets) {
            this.skillTargets = skillTargets;
            return this;
        }

        public Builder isAnonymous(Boolean isAnonymous) {
            this.isAnonymous = isAnonymous;
            return this;
        }

        public Builder targetFilter(String targetFilter) {
            this.targetFilter = targetFilter;
            return this;
        }

        public Builder status(SurveyStatus status) {
            this.status = status;
            return this;
        }

        public Builder opensAt(LocalDateTime opensAt) {
            this.opensAt = opensAt;
            return this;
        }

        public Builder closesAt(LocalDateTime closesAt) {
            this.closesAt = closesAt;
            return this;
        }

        public Builder eligibleCount(Integer eligibleCount) {
            this.eligibleCount = eligibleCount;
            return this;
        }

        public Builder createdBy(User createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder activatedAt(LocalDateTime activatedAt) {
            this.activatedAt = activatedAt;
            return this;
        }

        public Survey build() {
            return new Survey(surveyId, template, version, title, description, category, term,
                    questionSchema, skillTargets, isAnonymous, targetFilter, status, opensAt,
                    closesAt, eligibleCount, createdBy, createdAt, activatedAt);
        }
    }
}
