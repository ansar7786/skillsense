package com.skillsense.analytics.repository;

import com.skillsense.analytics.entity.AnalyticsCache;
import com.skillsense.analytics.entity.AnalyticsCacheId;
import com.skillsense.analytics.entity.AnalyticsCacheKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalyticsCacheRepository extends JpaRepository<AnalyticsCache, AnalyticsCacheId> {

    Optional<AnalyticsCache> findById_SurveyIdAndId_Kind(Long surveyId, AnalyticsCacheKind kind);

    void deleteById_SurveyIdAndId_Kind(Long surveyId, AnalyticsCacheKind kind);
}
