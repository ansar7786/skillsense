package com.skillsense.response.repository;

import com.skillsense.response.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResponseRepository extends JpaRepository<Response, String> {

    long countBySurvey_SurveyId(Long surveyId);

    List<Response> findBySurvey_SurveyId(Long surveyId);

    boolean existsBySurvey_SurveyIdAndStudent_UserId(Long surveyId, Long userId);
}
