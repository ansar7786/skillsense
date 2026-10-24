package com.skillsense.survey.repository;

import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    @Query("SELECT COALESCE(MAX(s.version), 0) FROM Survey s WHERE s.template.templateId = :templateId")
    int findMaxVersionByTemplateId(@Param("templateId") Long templateId);

    @Query("SELECT s FROM Survey s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:category IS NULL OR s.category = :category) AND " +
           "(:termId IS NULL OR s.term.termId = :termId) " +
           "ORDER BY s.createdAt DESC")
    List<Survey> searchSurveys(
            @Param("status") SurveyStatus status,
            @Param("category") SurveyCategory category,
            @Param("termId") Integer termId
    );

    List<Survey> findByStatusAndClosesAtBefore(SurveyStatus status, LocalDateTime now);

    @Query(value = "SELECT COUNT(*) FROM responses WHERE survey_id = :surveyId", nativeQuery = true)
    long countResponsesBySurveyId(@Param("surveyId") Long surveyId);

    java.util.Optional<Survey> findByTemplate_TemplateIdAndVersion(Long templateId, Integer version);
}
