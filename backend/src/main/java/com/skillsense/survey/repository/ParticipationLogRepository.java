package com.skillsense.survey.repository;

import com.skillsense.survey.entity.ParticipationLog;
import com.skillsense.survey.entity.ParticipationLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipationLogRepository extends JpaRepository<ParticipationLog, ParticipationLogId> {

    long countById_SurveyIdAndHasResponded(Long surveyId, Boolean hasResponded);

    long countById_SurveyId(Long surveyId);

    boolean existsById_SurveyIdAndId_StudentId(Long surveyId, Long studentId);

    List<ParticipationLog> findById_SurveyId(Long surveyId);

    @Query("SELECT COUNT(p) FROM ParticipationLog p WHERE p.id.surveyId = :surveyId AND p.hasResponded = true")
    long countRespondedBySurveyId(@Param("surveyId") Long surveyId);

    void deleteById_SurveyId(Long surveyId);

    List<ParticipationLog> findById_StudentId(Long studentId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ParticipationLog p SET p.hasResponded = true WHERE p.id.surveyId = :surveyId AND p.id.studentId = :studentId AND p.hasResponded = false")
    int markResponded(@Param("surveyId") Long surveyId, @Param("studentId") Long studentId);
}
