package com.skillsense;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.auth.JwtUtil;
import com.skillsense.response.dto.SubmitResponseRequest;
import com.skillsense.response.entity.Response;
import com.skillsense.response.repository.ResponseRepository;
import com.skillsense.response.service.ResponseService;
import com.skillsense.survey.dto.CreateSurveyRequest;
import com.skillsense.survey.dto.SurveyResponse;
import com.skillsense.survey.entity.ParticipationLog;
import com.skillsense.survey.entity.ParticipationLogId;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.model.QuestionType;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.model.TargetFilter;
import com.skillsense.survey.model.schema.OptionDefinition;
import com.skillsense.survey.model.schema.QuestionDefinition;
import com.skillsense.survey.model.schema.RatingDefinition;
import com.skillsense.survey.model.schema.ScoringDefinition;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import com.skillsense.survey.repository.ParticipationLogRepository;
import com.skillsense.survey.repository.SurveyRepository;
import com.skillsense.survey.repository.SurveyTemplateRepository;
import com.skillsense.term.AcademicTerm;
import com.skillsense.term.AcademicTermRepository;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ResponseSubmissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AcademicTermRepository termRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyTemplateRepository templateRepository;

    @Autowired
    private ParticipationLogRepository participationLogRepository;

    @Autowired
    private ResponseRepository responseRepository;

    @Autowired
    private ResponseService responseService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User adminUser;
    private User student1;
    private User student2;
    private User unassignedStudent;
    private AcademicTerm defaultTerm;

    private String adminToken;
    private String student1Token;
    private String student2Token;
    private String unassignedStudentToken;

    @BeforeEach
    public void setUp() {
        // Clear runtime response, analytics and survey tables
        jdbcTemplate.execute("DELETE FROM student_results");
        jdbcTemplate.execute("DELETE FROM analytics_cache");
        responseRepository.deleteAll();
        participationLogRepository.deleteAll();
        surveyRepository.deleteAll();
        templateRepository.deleteAll();

        // Ensure terms exist
        if (termRepository.count() == 0) {
            defaultTerm = termRepository.save(new AcademicTerm(null, "2026-27 Odd", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), true));
        } else {
            defaultTerm = termRepository.findAll().get(0);
        }

        // Clean up any dynamic test users from previous runs
        userRepository.findByCollegeId("26TEST_LATE").ifPresent(userRepository::delete);
        userRepository.findByCollegeId("26UNASSIGNED").ifPresent(userRepository::delete);

        // Seed 10 deterministic students (26TEST01 to 26TEST10)
        seedDeterministicStudents();

        adminUser = getOrCreateUser("admin_phase4", "Admin Phase 4", Role.ADMIN, null, null, null);
        student1 = userRepository.findByCollegeId("26TEST01").orElseThrow();
        student2 = userRepository.findByCollegeId("26TEST02").orElseThrow();

        // Create an unassigned student in a different branch (e.g. MECH)
        unassignedStudent = getOrCreateUser("26UNASSIGNED", "Unassigned Student", Role.STUDENT, "MECH", 1, "A");

        adminToken = jwtUtil.generateToken(adminUser.getUserId(), adminUser.getCollegeId(), adminUser.getRole(), adminUser.getName());
        student1Token = jwtUtil.generateToken(student1.getUserId(), student1.getCollegeId(), student1.getRole(), student1.getName());
        student2Token = jwtUtil.generateToken(student2.getUserId(), student2.getCollegeId(), student2.getRole(), student2.getName());
        unassignedStudentToken = jwtUtil.generateToken(unassignedStudent.getUserId(), unassignedStudent.getCollegeId(), unassignedStudent.getRole(), unassignedStudent.getName());
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        jdbcTemplate.execute("DELETE FROM student_results");
        jdbcTemplate.execute("DELETE FROM analytics_cache");
        responseRepository.deleteAll();
        participationLogRepository.deleteAll();
        surveyRepository.deleteAll();
        templateRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM users WHERE college_id LIKE '26ANON%' OR college_id IN ('26UNASSIGNED', '26TEST_LATE', 'admin_phase4')");
    }

    private void seedDeterministicStudents() {
        createStudentIfMissing("26TEST01", "Student 1", "CSE", 2, "A", true);
        createStudentIfMissing("26TEST02", "Student 2", "CSE", 2, "B", true);
        createStudentIfMissing("26TEST03", "Student 3", "CSE", 3, "A", true);
        createStudentIfMissing("26TEST04", "Student 4", "CSE", 3, "B", true);
        createStudentIfMissing("26TEST05", "Student 5", "IT", 2, "A", true);
        createStudentIfMissing("26TEST06", "Student 6", "IT", 3, "A", true);
        createStudentIfMissing("26TEST07", "Student 7", "IT", 4, "B", true);
        createStudentIfMissing("26TEST08", "Student 8", "ECE", 3, "A", true);
        createStudentIfMissing("26TEST09", "Student 9", "ECE", 4, "B", true);
        createStudentIfMissing("26TEST10", "Student 10", "CSE", 3, "A", false); // Inactive student
    }

    private void createStudentIfMissing(String collegeId, String name, String branch, int year, String section, boolean active) {
        if (!userRepository.existsByCollegeId(collegeId)) {
            User s = User.builder()
                    .collegeId(collegeId)
                    .name(name)
                    .email(collegeId.toLowerCase() + "@skillsense.edu")
                    .passwordHash(passwordEncoder.encode("Test@1234"))
                    .role(Role.STUDENT)
                    .branch(branch)
                    .studyYear(year)
                    .section(section)
                    .isActive(active)
                    .mustChangePassword(false)
                    .build();
            userRepository.save(s);
        }
    }

    private User getOrCreateUser(String collegeId, String name, Role role, String branch, Integer year, String section) {
        return userRepository.findByCollegeId(collegeId).orElseGet(() -> {
            User u = User.builder()
                    .collegeId(collegeId)
                    .name(name)
                    .email(collegeId + "@skillsense.edu")
                    .passwordHash(passwordEncoder.encode("Test@1234"))
                    .role(role)
                    .branch(branch)
                    .studyYear(year)
                    .section(section)
                    .isActive(true)
                    .mustChangePassword(false)
                    .build();
            return userRepository.save(u);
        });
    }

    private SurveyQuestionSchema createScoredSchema() {
        return SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(Arrays.asList(
                        QuestionDefinition.builder()
                                .key("java_skill")
                                .type(QuestionType.RATING)
                                .prompt("Rate your Java skills")
                                .required(true)
                                .rating(new RatingDefinition(1, 5, "Beginner", "Expert"))
                                .scoring(new ScoringDefinition("java", 2.0))
                                .build(),
                        QuestionDefinition.builder()
                                .key("mock_interviews")
                                .type(QuestionType.SINGLE_CHOICE)
                                .prompt("How many mock interviews given?")
                                .required(true)
                                .options(Arrays.asList(
                                        new OptionDefinition("0", "None", 0.0),
                                        new OptionDefinition("1-2", "1 to 2", 50.0),
                                        new OptionDefinition("3+", "3 or more", 100.0)
                                ))
                                .scoring(new ScoringDefinition("interview", 1.0))
                                .build(),
                        QuestionDefinition.builder()
                                .key("blockers")
                                .type(QuestionType.TEXT)
                                .prompt("Any preparation blockers?")
                                .required(false)
                                .maxLength(500)
                                .build()
                ))
                .build();
    }

    private Long createAndActivateSurvey(boolean isAnonymous, SurveyCategory category, TargetFilter filter) throws Exception {
        CreateSurveyRequest req = CreateSurveyRequest.builder()
                .title("Phase 4 Survey " + (isAnonymous ? "Anon" : "Identified"))
                .category(category)
                .termId(defaultTerm.getTermId())
                .questionSchema(createScoredSchema())
                .isAnonymous(isAnonymous)
                .targetFilter(filter)
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        SurveyResponse created = objectMapper.readValue(createRes.getResponse().getContentAsString(), SurveyResponse.class);

        mockMvc.perform(post("/api/surveys/" + created.getSurveyId() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        return created.getSurveyId();
    }

    // -------------------------------------------------------------
    // 1. STUDENT LIST & DETAIL SANITIZATION TESTS
    // -------------------------------------------------------------

    @Test
    public void testStudentListAssignedSurveys() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        // Student 1 (CSE) lists assigned surveys
        mockMvc.perform(get("/api/my/surveys")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].surveyId").value(surveyId))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].isAnonymous").value(false));

        // Unassigned student (MECH) gets empty list
        mockMvc.perform(get("/api/my/surveys")
                        .header("Authorization", "Bearer " + unassignedStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testStudentDetailSanitizesScoringWeightsAndTargets() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.PLACEMENT,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        MvcResult result = mockMvc.perform(get("/api/my/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surveyId").value(surveyId))
                .andExpect(jsonPath("$.anonymityNotice").isNotEmpty())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();

        // Verify CRITICAL requirement: student response JSON must NOT contain scoring, weights, or targets
        assertFalse(responseContent.contains("\"scoring\""), "Student detail must strip scoring");
        assertFalse(responseContent.contains("\"weight\""), "Student detail must strip weights");
        assertFalse(responseContent.contains("\"skill\""), "Student detail must strip skill tags");
        assertFalse(responseContent.contains("\"score\""), "Student detail must strip option scores");
        assertFalse(responseContent.contains("\"skill_targets\""), "Student detail must strip skill_targets");

        // Verify questions and options are intact
        assertTrue(responseContent.contains("java_skill"));
        assertTrue(responseContent.contains("mock_interviews"));
        assertTrue(responseContent.contains("Beginner"));
        assertTrue(responseContent.contains("Expert"));
    }

    @Test
    public void testUnassignedStudentForbiddenFromSurveyDetail() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        mockMvc.perform(get("/api/my/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + unassignedStudentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_ELIGIBLE"));
    }

    // -------------------------------------------------------------
    // 2. SUBMISSION ALGORITHM & ANONYMITY HANDLING TESTS
    // -------------------------------------------------------------

    @Test
    public void testAnonymousSubmissionEnforcesNullIdentityAndTime() throws Exception {
        Long surveyId = createAndActivateSurvey(true, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        Map<String, Object> answers = new HashMap<>();
        answers.put("java_skill", 4);
        answers.put("mock_interviews", "1-2");
        answers.put("blockers", "None so far");

        SubmitResponseRequest req = new SubmitResponseRequest(answers);

        // Submit as Student 1 (CSE, Year 2, Sec A)
        mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submitted").value(true));

        // Query responses directly from database
        List<Response> responses = responseRepository.findBySurvey_SurveyId(surveyId);
        assertEquals(1, responses.size());
        Response resp = responses.get(0);

        // Strict Section 5 Anonymity assertions:
        assertNull(resp.getStudent(), "Anonymous survey response MUST have student_id = NULL");
        assertNull(resp.getSubmittedAt(), "Anonymous survey response MUST have submitted_at = NULL");
        assertNull(resp.getGroupSection(), "Anonymous survey response MUST have group_section = NULL");
        assertEquals("CSE", resp.getGroupBranch(), "Anonymous response keeps branch");
        assertEquals(2, resp.getGroupYear(), "Anonymous response keeps year");

        // Verify participation_log state
        ParticipationLog log = participationLogRepository.findById(new ParticipationLogId(surveyId, student1.getUserId())).orElseThrow();
        assertTrue(log.getHasResponded(), "Participation log must mark has_responded = true");

        // Student's list now shows SUBMITTED
        mockMvc.perform(get("/api/my/surveys")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));
    }

    @Test
    public void testNonAnonymousSubmissionRecordsIdentityAndTime() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.PLACEMENT,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        Map<String, Object> answers = new HashMap<>();
        answers.put("java_skill", 5);
        answers.put("mock_interviews", "3+");

        SubmitResponseRequest req = new SubmitResponseRequest(answers);

        mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submitted").value(true));

        List<Response> responses = responseRepository.findBySurvey_SurveyId(surveyId);
        assertEquals(1, responses.size());
        Response resp = responses.get(0);

        // Non-anonymous assertions:
        assertNotNull(resp.getStudent(), "Non-anonymous survey must record student");
        assertEquals(student1.getUserId(), resp.getStudent().getUserId());
        assertNotNull(resp.getSubmittedAt(), "Non-anonymous survey must record timestamp");
        assertEquals("A", resp.getGroupSection(), "Non-anonymous survey records section");
        assertEquals("CSE", resp.getGroupBranch());
        assertEquals(2, resp.getGroupYear());
    }

    @Test
    public void testDuplicateSubmissionReturns409AndLeavesOneResponseRow() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        Map<String, Object> answers = new HashMap<>();
        answers.put("java_skill", 3);
        answers.put("mock_interviews", "0");

        SubmitResponseRequest req = new SubmitResponseRequest(answers);

        // First submit -> 201 Created
        mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second submit -> 409 Conflict ALREADY_SUBMITTED
        mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_SUBMITTED"));

        // Exactly one response row remains in database
        assertEquals(1, responseRepository.countBySurvey_SurveyId(surveyId));
    }

    @Test
    public void testUnassignedStudentCannotSubmit() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        Map<String, Object> answers = new HashMap<>();
        answers.put("java_skill", 3);
        answers.put("mock_interviews", "0");

        SubmitResponseRequest req = new SubmitResponseRequest(answers);

        mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                        .header("Authorization", "Bearer " + unassignedStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_ELIGIBLE"));

        assertEquals(0, responseRepository.countBySurvey_SurveyId(surveyId));
    }

    @Test
    public void testClosedSurveyRejectsSubmissionWith409SurveyNotActive() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        // Close the survey
        mockMvc.perform(post("/api/surveys/" + surveyId + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Map<String, Object> answers = new HashMap<>();
        answers.put("java_skill", 3);
        answers.put("mock_interviews", "0");

        SubmitResponseRequest req = new SubmitResponseRequest(answers);

        mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SURVEY_NOT_ACTIVE"));

        assertEquals(0, responseRepository.countBySurvey_SurveyId(surveyId));
    }

    // -------------------------------------------------------------
    // 3. ANSWER VALIDATION VIA SUBMIT API
    // -------------------------------------------------------------

    @Test
    public void testSubmitMissingRequiredAnswerReturns400() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        Map<String, Object> answers = new HashMap<>();
        answers.put("java_skill", 3);
        // mock_interviews is required but missing

        SubmitResponseRequest req = new SubmitResponseRequest(answers);

        mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("mock_interviews")));
    }

    @Test
    public void testSubmitUnknownAnswerKeyReturns400() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        Map<String, Object> answers = new HashMap<>();
        answers.put("java_skill", 3);
        answers.put("mock_interviews", "1-2");
        answers.put("unexpected_injected_key", "malicious_payload");

        SubmitResponseRequest req = new SubmitResponseRequest(answers);

        mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("unexpected_injected_key")));
    }

    @Test
    public void testSubmitInvalidRatingValueReturns400() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        Map<String, Object> answers = new HashMap<>();
        answers.put("java_skill", 10); // Allowed range is 1..5
        answers.put("mock_interviews", "1-2");

        SubmitResponseRequest req = new SubmitResponseRequest(answers);

        mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("java_skill")));
    }

    // -------------------------------------------------------------
    // 4. TEN ANONYMOUS SUBMISSIONS ANONYMITY VERIFICATION
    // -------------------------------------------------------------

    @Test
    public void testTenAnonymousSubmissionsHaveNoUsableJoinKey() throws Exception {
        // Create anonymous survey targeted to ALL
        Long surveyId = createAndActivateSurvey(true, SurveyCategory.WELLBEING,
                new TargetFilter("ALL", List.of(), List.of(), List.of()));

        // Ensure we have at least 10 active students
        for (int i = 1; i <= 10; i++) {
            String cid = String.format("26ANON%02d", i);
            createStudentIfMissing(cid, "Anon Student " + i, "CSE", 3, "A", true);
        }

        // Add them to participation_log for this survey
        for (int i = 1; i <= 10; i++) {
            User s = userRepository.findByCollegeId(String.format("26ANON%02d", i)).orElseThrow();
            if (!participationLogRepository.existsById_SurveyIdAndId_StudentId(surveyId, s.getUserId())) {
                participationLogRepository.save(new ParticipationLog(surveyId, s.getUserId(), false));
            }
        }

        // Submit 10 anonymous responses through API
        for (int i = 1; i <= 10; i++) {
            User s = userRepository.findByCollegeId(String.format("26ANON%02d", i)).orElseThrow();
            String token = jwtUtil.generateToken(s.getUserId(), s.getCollegeId(), s.getRole(), s.getName());

            Map<String, Object> answers = new HashMap<>();
            answers.put("java_skill", (i % 5) + 1);
            answers.put("mock_interviews", (i % 2 == 0) ? "1-2" : "3+");
            answers.put("blockers", "Student " + i + " feedback");

            mockMvc.perform(post("/api/my/surveys/" + surveyId + "/submit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SubmitResponseRequest(answers))))
                    .andExpect(status().isCreated());
        }

        assertEquals(10, responseRepository.countBySurvey_SurveyId(surveyId));

        // Direct SQL check: Every anonymous response has student_id NULL, submitted_at NULL, group_section NULL
        Integer nonNullStudentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM responses WHERE survey_id = ? AND student_id IS NOT NULL",
                Integer.class, surveyId
        );
        assertEquals(0, nonNullStudentCount, "All anonymous responses must have student_id IS NULL");

        Integer nonNullTimeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM responses WHERE survey_id = ? AND submitted_at IS NOT NULL",
                Integer.class, surveyId
        );
        assertEquals(0, nonNullTimeCount, "All anonymous responses must have submitted_at IS NULL");

        Integer nonNullSectionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM responses WHERE survey_id = ? AND group_section IS NOT NULL",
                Integer.class, surveyId
        );
        assertEquals(0, nonNullSectionCount, "All anonymous responses must have group_section IS NULL");

        // Section 5 Architectural Defense Check:
        // 1. Attempting to join anonymous responses to participation_log on student_id yields 0 matches
        Integer joinedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM responses r JOIN participation_log p ON r.survey_id = p.survey_id AND r.student_id = p.student_id WHERE r.survey_id = ?",
                Integer.class, surveyId
        );
        assertEquals(0, joinedCount, "Joining anonymous responses with participation_log on student_id yields 0 matches");

        // 2. participation_log has NO response_id, timestamp, or sequence key that could correlate to responses
        List<String> partColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'skillsense' AND TABLE_NAME = 'participation_log'",
                String.class
        );
        assertFalse(partColumns.contains("response_id"), "participation_log must NOT have response_id column");
        assertFalse(partColumns.contains("submitted_at"), "participation_log must NOT have submitted_at column");
        assertFalse(partColumns.contains("created_at"), "participation_log must NOT have timestamp column");
    }

    // -------------------------------------------------------------
    // 5. TRANSACTION ROLLBACK TEST
    // -------------------------------------------------------------

    @Test
    public void testTransactionRollbackOnExceptionAfterParticipationLogUpdate() throws Exception {
        Long surveyId = createAndActivateSurvey(false, SurveyCategory.ACADEMIC,
                TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build());

        // Verify initial state: participation_log has_responded = false, responses = 0
        ParticipationLog logBefore = participationLogRepository.findById(new ParticipationLogId(surveyId, student1.getUserId())).orElseThrow();
        assertFalse(logBefore.getHasResponded());
        assertEquals(0, responseRepository.countBySurvey_SurveyId(surveyId));

        // Create a custom failing runner using a mock or transaction failure:
        // We simulate a failure in the transaction by executing a transactional block that updates participation_log
        // and then throws an unhandled RuntimeException
        assertThrows(RuntimeException.class, () -> {
            executeTransactionalFailure(surveyId, student1.getUserId());
        });

        // Verify that participation_log was ROLLED BACK to false
        ParticipationLog logAfter = participationLogRepository.findById(new ParticipationLogId(surveyId, student1.getUserId())).orElseThrow();
        assertFalse(logAfter.getHasResponded(), "Participation log must revert to false on rollback");

        // Verify that responses table contains 0 rows
        assertEquals(0, responseRepository.countBySurvey_SurveyId(surveyId));
    }

    @org.springframework.transaction.annotation.Transactional
    public void executeTransactionalFailure(Long surveyId, Long studentId) {
        // 1. Mark responded
        int updated = participationLogRepository.markResponded(surveyId, studentId);
        assertEquals(1, updated);

        // 2. Intentionally throw exception before commit
        throw new RuntimeException("Simulated unexpected failure during response persistence to trigger rollback");
    }
}
