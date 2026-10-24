package com.skillsense.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCollegeId(String collegeId);

    boolean existsByCollegeId(String collegeId);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:branch IS NULL OR u.branch = :branch) AND " +
           "(:q IS NULL OR :q = '' OR " +
           " LOWER(u.collegeId) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<User> searchUsers(
            @Param("role") Role role,
            @Param("branch") String branch,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("SELECT u FROM User u WHERE u.role = com.skillsense.user.Role.STUDENT AND u.isActive = true AND " +
           "(:useBranches = false OR u.branch IN :branches) AND " +
           "(:useYears = false OR u.studyYear IN :years) AND " +
           "(:useSections = false OR u.section IN :sections)")
    java.util.List<User> findEligibleStudents(
            @Param("useBranches") boolean useBranches,
            @Param("branches") java.util.List<String> branches,
            @Param("useYears") boolean useYears,
            @Param("years") java.util.List<Integer> years,
            @Param("useSections") boolean useSections,
            @Param("sections") java.util.List<String> sections
    );

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = com.skillsense.user.Role.STUDENT AND u.isActive = true AND " +
           "(:useBranches = false OR u.branch IN :branches) AND " +
           "(:useYears = false OR u.studyYear IN :years) AND " +
           "(:useSections = false OR u.section IN :sections)")
    long countEligibleStudents(
            @Param("useBranches") boolean useBranches,
            @Param("branches") java.util.List<String> branches,
            @Param("useYears") boolean useYears,
            @Param("years") java.util.List<Integer> years,
            @Param("useSections") boolean useSections,
            @Param("sections") java.util.List<String> sections
    );
}
