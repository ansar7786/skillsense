package com.skillsense.term;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Integer> {

    Optional<AcademicTerm> findByLabel(String label);

    boolean existsByLabel(String label);

    Optional<AcademicTerm> findByIsCurrentTrue();

    @Modifying
    @Query("UPDATE AcademicTerm t SET t.isCurrent = false WHERE t.termId != :termId")
    void resetOtherCurrentTerms(@Param("termId") Integer termId);

    @Modifying
    @Query("UPDATE AcademicTerm t SET t.isCurrent = false")
    void resetAllCurrentTerms();
}
