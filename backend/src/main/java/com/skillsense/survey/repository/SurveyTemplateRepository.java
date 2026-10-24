package com.skillsense.survey.repository;

import com.skillsense.survey.entity.SurveyTemplate;
import com.skillsense.survey.model.SurveyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyTemplateRepository extends JpaRepository<SurveyTemplate, Long> {

    List<SurveyTemplate> findByCategory(SurveyCategory category);

    List<SurveyTemplate> findByCreatedBy_UserId(Long userId);
}
