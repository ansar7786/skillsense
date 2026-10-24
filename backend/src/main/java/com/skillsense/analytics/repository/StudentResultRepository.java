package com.skillsense.analytics.repository;

import com.skillsense.analytics.entity.StudentResult;
import com.skillsense.analytics.entity.StudentResultId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentResultRepository extends JpaRepository<StudentResult, StudentResultId> {

    Optional<StudentResult> findById_SurveyIdAndId_StudentId(Long surveyId, Long studentId);
}
