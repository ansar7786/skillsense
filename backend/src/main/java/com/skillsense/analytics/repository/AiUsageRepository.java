package com.skillsense.analytics.repository;

import com.skillsense.analytics.entity.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AiUsageRepository extends JpaRepository<AiUsage, LocalDate> {

    Optional<AiUsage> findByUsageDate(LocalDate usageDate);

    @Modifying
    @Query(value = "INSERT INTO ai_usage (usage_date, calls) VALUES (:usageDate, 1) ON DUPLICATE KEY UPDATE calls = calls + 1", nativeQuery = true)
    void incrementCalls(@Param("usageDate") LocalDate usageDate);
}
