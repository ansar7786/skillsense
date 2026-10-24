package com.skillsense;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 Integration Tests:
 * Verifies database schema creation and anonymity/immutability triggers via JdbcTemplate.
 * Strictly no JPA entities are used.
 */
@SpringBootTest
class DatabaseAnonymityTriggersTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long facultyUserId;
    private Long studentUserId;
    private Integer termId;
    private Long templateId;
    private Long anonSurveyId;
    private Long nonAnonSurveyId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        // 1. Insert minimum users
        jdbcTemplate.update(
            "INSERT INTO users (college_id, name, email, password_hash, role) " +
            "VALUES ('FAC_TEST_01', 'Faculty Test', 'fac@test.com', 'dummyhash', 'FACULTY')"
        );
        facultyUserId = jdbcTemplate.queryForObject(
            "SELECT user_id FROM users WHERE college_id = 'FAC_TEST_01'", Long.class
        );

        jdbcTemplate.update(
            "INSERT INTO users (college_id, name, email, password_hash, role, branch, study_year, section) " +
            "VALUES ('STU_TEST_01', 'Student Test', 'stu@test.com', 'dummyhash', 'STUDENT', 'CSE', 4, 'A')"
        );
        studentUserId = jdbcTemplate.queryForObject(
            "SELECT user_id FROM users WHERE college_id = 'STU_TEST_01'", Long.class
        );

        // 2. Insert academic term
        jdbcTemplate.update(
            "INSERT INTO academic_terms (label, start_date, end_date, is_current) " +
            "VALUES ('2026-27 Odd Test', '2026-08-01', '2026-12-31', TRUE)"
        );
        termId = jdbcTemplate.queryForObject(
            "SELECT term_id FROM academic_terms WHERE label = '2026-27 Odd Test'", Integer.class
        );

        // 3. Insert survey template
        jdbcTemplate.update(
            "INSERT INTO survey_templates (name, category, created_by) " +
            "VALUES ('Placement Readiness Test', 'PLACEMENT', ?)",
            facultyUserId
        );
        templateId = jdbcTemplate.queryForObject(
            "SELECT template_id FROM survey_templates WHERE name = 'Placement Readiness Test'", Long.class
        );

        // 4. Insert an anonymous survey (DRAFT)
        jdbcTemplate.update(
            "INSERT INTO surveys (template_id, version, title, category, term_id, question_schema, is_anonymous, target_filter, status, created_by) " +
            "VALUES (?, 1, 'Anonymous Placement Survey', 'PLACEMENT', ?, '{\"questions\":[]}', TRUE, '{}', 'DRAFT', ?)",
            templateId, termId, facultyUserId
        );
        anonSurveyId = jdbcTemplate.queryForObject(
            "SELECT survey_id FROM surveys WHERE template_id = ? AND version = 1", Long.class, templateId
        );

        // 5. Insert a non-anonymous survey (DRAFT)
        jdbcTemplate.update(
            "INSERT INTO surveys (template_id, version, title, category, term_id, question_schema, is_anonymous, target_filter, status, created_by) " +
            "VALUES (?, 2, 'Non-Anonymous Placement Survey', 'PLACEMENT', ?, '{\"questions\":[]}', FALSE, '{}', 'DRAFT', ?)",
            templateId, termId, facultyUserId
        );
        nonAnonSurveyId = jdbcTemplate.queryForObject(
            "SELECT survey_id FROM surveys WHERE template_id = ? AND version = 2", Long.class, templateId
        );
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE audit_log");
        jdbcTemplate.execute("TRUNCATE TABLE ai_usage");
        jdbcTemplate.execute("TRUNCATE TABLE analytics_cache");
        jdbcTemplate.execute("TRUNCATE TABLE student_results");
        jdbcTemplate.execute("TRUNCATE TABLE participation_log");
        jdbcTemplate.execute("TRUNCATE TABLE responses");
        jdbcTemplate.execute("TRUNCATE TABLE surveys");
        jdbcTemplate.execute("TRUNCATE TABLE survey_templates");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("TRUNCATE TABLE academic_terms");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Test
    @DisplayName("Test 1: Anonymous survey rejects student_id, submitted_at, and group_section")
    void testAnonymousSurveyRejectsIdentityFields() {
        String responseIdWithStudent = UUID.randomUUID().toString();

        // 1a. Inserting response with student_id on anonymous survey must fail
        DataAccessException exStudentId = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO responses (response_id, survey_id, student_id, answer_data) VALUES (?, ?, ?, '{}')",
                responseIdWithStudent, anonSurveyId, studentUserId
            );
        });
        assertNotNull(exStudentId.getMessage());
        assertTrue(
            exStudentId.getMessage().contains("Anonymous survey: identity, time and section must be NULL"),
            "Expected trigger error message for student_id on anonymous survey but got: " + exStudentId.getMessage()
        );

        // 1b. Inserting response with submitted_at on anonymous survey must fail
        String responseIdWithTime = UUID.randomUUID().toString();
        DataAccessException exSubmittedAt = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO responses (response_id, survey_id, student_id, submitted_at, answer_data) VALUES (?, ?, NULL, NOW(), '{}')",
                responseIdWithTime, anonSurveyId
            );
        });
        assertNotNull(exSubmittedAt.getMessage());
        assertTrue(
            exSubmittedAt.getMessage().contains("Anonymous survey: identity, time and section must be NULL"),
            "Expected trigger error message for submitted_at on anonymous survey but got: " + exSubmittedAt.getMessage()
        );

        // 1c. Inserting response with group_section on anonymous survey must fail
        String responseIdWithSection = UUID.randomUUID().toString();
        DataAccessException exSection = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO responses (response_id, survey_id, student_id, group_section, answer_data) VALUES (?, ?, NULL, 'A', '{}')",
                responseIdWithSection, anonSurveyId
            );
        });
        assertNotNull(exSection.getMessage());
        assertTrue(
            exSection.getMessage().contains("Anonymous survey: identity, time and section must be NULL"),
            "Expected trigger error message for group_section on anonymous survey but got: " + exSection.getMessage()
        );

        // 1d. Valid anonymous insert with NULL student_id, NULL submitted_at, NULL group_section must succeed
        String validAnonResponseId = UUID.randomUUID().toString();
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO responses (response_id, survey_id, student_id, group_branch, group_year, group_section, submitted_at, answer_data) " +
                "VALUES (?, ?, NULL, 'CSE', 4, NULL, NULL, '{\"q1\":\"val\"}')",
                validAnonResponseId, anonSurveyId
            );
        });
    }

    @Test
    @DisplayName("Test 2: Non-anonymous survey requires student_id")
    void testNonAnonymousSurveyRequiresIdentity() {
        String responseIdNullStudent = UUID.randomUUID().toString();

        // Inserting response with student_id = NULL on non-anonymous survey must fail
        DataAccessException ex = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO responses (response_id, survey_id, student_id, group_branch, group_year, group_section, submitted_at, answer_data) " +
                "VALUES (?, ?, NULL, 'CSE', 4, 'A', NOW(), '{\"q1\":\"val\"}')",
                responseIdNullStudent, nonAnonSurveyId
            );
        });
        assertNotNull(ex.getMessage());
        assertTrue(
            ex.getMessage().contains("Non-anonymous survey requires student_id"),
            "Expected trigger error message for non-anonymous survey requiring student_id but got: " + ex.getMessage()
        );

        // Valid non-anonymous insert with student_id must succeed
        String validResponseId = UUID.randomUUID().toString();
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO responses (response_id, survey_id, student_id, group_branch, group_year, group_section, submitted_at, answer_data) " +
                "VALUES (?, ?, ?, 'CSE', 4, 'A', NOW(), '{\"q1\":\"val\"}')",
                validResponseId, nonAnonSurveyId, studentUserId
            );
        });
    }

    @Test
    @DisplayName("Test 3: Responses are immutable (any UPDATE on responses fails)")
    void testResponsesAreImmutable() {
        String responseId = UUID.randomUUID().toString();
        jdbcTemplate.update(
            "INSERT INTO responses (response_id, survey_id, student_id, group_branch, group_year, group_section, submitted_at, answer_data) " +
            "VALUES (?, ?, ?, 'CSE', 4, 'A', NOW(), '{\"q1\":\"original\"}')",
            responseId, nonAnonSurveyId, studentUserId
        );

        // Attempting to update the response row must fail
        DataAccessException ex = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                "UPDATE responses SET group_branch = 'ECE' WHERE response_id = ?",
                responseId
            );
        });
        assertNotNull(ex.getMessage());
        assertTrue(
            ex.getMessage().contains("Responses are immutable"),
            "Expected trigger error message 'Responses are immutable' but got: " + ex.getMessage()
        );
    }

    @Test
    @DisplayName("Test 4: Activated survey anonymity cannot change")
    void testActivatedSurveyAnonymityCannotChange() {
        // Activate the survey
        jdbcTemplate.update(
            "UPDATE surveys SET status = 'ACTIVE', activated_at = NOW() WHERE survey_id = ?",
            anonSurveyId
        );

        // Attempting to change is_anonymous after activation must fail
        DataAccessException ex = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                "UPDATE surveys SET is_anonymous = FALSE WHERE survey_id = ?",
                anonSurveyId
            );
        });
        assertNotNull(ex.getMessage());
        assertTrue(
            ex.getMessage().contains("Anonymity cannot change after activation"),
            "Expected trigger error message 'Anonymity cannot change after activation' but got: " + ex.getMessage()
        );
    }

    @Test
    @DisplayName("Test 5: Activated survey question schema cannot change")
    void testActivatedSurveyQuestionSchemaCannotChange() {
        // Activate the survey
        jdbcTemplate.update(
            "UPDATE surveys SET status = 'ACTIVE', activated_at = NOW() WHERE survey_id = ?",
            anonSurveyId
        );

        // Attempting to modify question_schema after activation must fail
        DataAccessException ex = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                "UPDATE surveys SET question_schema = '{\"questions\":[{\"key\":\"q1\"}]}' WHERE survey_id = ?",
                anonSurveyId
            );
        });
        assertNotNull(ex.getMessage());
        assertTrue(
            ex.getMessage().contains("Questions cannot change after activation"),
            "Expected trigger error message 'Questions cannot change after activation' but got: " + ex.getMessage()
        );
    }

    @Test
    @DisplayName("Test 6: Wellbeing surveys must be anonymous")
    void testWellbeingSurveysMustBeAnonymous() {
        // 6a. Attempting to insert a WELLBEING survey with is_anonymous = FALSE must fail
        DataAccessException exInsert = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO surveys (template_id, version, title, category, term_id, question_schema, is_anonymous, target_filter, status, created_by) " +
                "VALUES (?, 99, 'Non-anon Wellbeing Survey', 'WELLBEING', ?, '{\"questions\":[]}', FALSE, '{}', 'DRAFT', ?)",
                templateId, termId, facultyUserId
            );
        });
        assertNotNull(exInsert.getMessage());
        assertTrue(
            exInsert.getMessage().contains("Wellbeing surveys must be anonymous"),
            "Expected trigger error message 'Wellbeing surveys must be anonymous' on INSERT but got: " + exInsert.getMessage()
        );

        // 6b. Inserting a WELLBEING survey with is_anonymous = TRUE must succeed
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO surveys (template_id, version, title, category, term_id, question_schema, is_anonymous, target_filter, status, created_by) " +
                "VALUES (?, 98, 'Valid Wellbeing Survey', 'WELLBEING', ?, '{\"questions\":[]}', TRUE, '{}', 'DRAFT', ?)",
                templateId, termId, facultyUserId
            );
        });
        Long wellbeingSurveyId = jdbcTemplate.queryForObject(
            "SELECT survey_id FROM surveys WHERE template_id = ? AND version = 98", Long.class, templateId
        );

        // 6c. Attempting to update a WELLBEING survey to is_anonymous = FALSE must also fail
        DataAccessException exUpdate = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                "UPDATE surveys SET is_anonymous = FALSE WHERE survey_id = ?",
                wellbeingSurveyId
            );
        });
        assertNotNull(exUpdate.getMessage());
        assertTrue(
            exUpdate.getMessage().contains("Wellbeing surveys must be anonymous"),
            "Expected trigger error message 'Wellbeing surveys must be anonymous' on UPDATE but got: " + exUpdate.getMessage()
        );
    }
}
