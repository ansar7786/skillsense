package com.skillsense;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.auth.JwtUtil;
import com.skillsense.survey.dto.CreateSurveyRequest;
import com.skillsense.survey.dto.EligibleCountResponse;
import com.skillsense.survey.dto.NewVersionRequest;
import com.skillsense.survey.dto.ProgressResponse;
import com.skillsense.survey.dto.SurveyResponse;
import com.skillsense.survey.dto.UpdateSurveyRequest;
import com.skillsense.survey.entity.ParticipationLog;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.entity.SurveyTemplate;
import com.skillsense.survey.model.QuestionType;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.model.TargetFilter;
import com.skillsense.survey.model.schema.QuestionDefinition;
import com.skillsense.survey.model.schema.RatingDefinition;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import com.skillsense.survey.repository.ParticipationLogRepository;
import com.skillsense.survey.repository.SurveyRepository;
import com.skillsense.survey.repository.SurveyTemplateRepository;
import com.skillsense.survey.service.PresetService;
import com.skillsense.survey.service.SurveySchedulerService;
import com.skillsense.survey.service.TargetingService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SurveyEngineIntegrationTest {

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
    private TargetingService targetingService;

    @Autowired
    private PresetService presetService;

    @Autowired
    private SurveySchedulerService schedulerService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User hodUser;
    private User facultyUser;
    private User studentUser;
    private AcademicTerm defaultTerm;
    private AcademicTerm nextTerm;

    private String adminToken;
    private String hodToken;
    private String facultyToken;
    private String studentToken;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        // Clear surveys and all referencing child tables to have a clean slate for each test
        jdbcTemplate.execute("DELETE FROM student_results");
        jdbcTemplate.execute("DELETE FROM analytics_cache");
        jdbcTemplate.execute("DELETE FROM responses");
        participationLogRepository.deleteAll();
        surveyRepository.deleteAll();
        templateRepository.deleteAll();

        // Ensure terms exist
        if (termRepository.count() == 0) {
            defaultTerm = termRepository.save(new AcademicTerm(null, "2026-27 Odd", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), true));
            nextTerm = termRepository.save(new AcademicTerm(null, "2026-27 Even", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 30), false));
        } else {
            defaultTerm = termRepository.findAll().get(0);
            if (termRepository.count() > 1) {
                nextTerm = termRepository.findAll().get(1);
            } else {
                nextTerm = termRepository.save(new AcademicTerm(null, "2026-27 Even", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 30), false));
            }
        }

        // Ensure late student from previous tests is removed
        userRepository.findByCollegeId("26TEST_LATE").ifPresent(userRepository::delete);

        // Seed deterministic 10 students if not present
        seedDeterministicStudents();

        // Seed officials
        adminUser = getOrCreateUser("test_admin_survey", "Survey Admin", Role.ADMIN, null, null, null);
        hodUser = getOrCreateUser("test_hod_cse", "HOD CSE", Role.HOD, "CSE", null, null);
        facultyUser = getOrCreateUser("test_fac_cse", "Faculty CSE", Role.FACULTY, "CSE", null, null);
        studentUser = userRepository.findByCollegeId("26TEST01").orElseThrow();

        adminToken = jwtUtil.generateToken(adminUser.getUserId(), adminUser.getCollegeId(), adminUser.getRole(), adminUser.getName());
        hodToken = jwtUtil.generateToken(hodUser.getUserId(), hodUser.getCollegeId(), hodUser.getRole(), hodUser.getName());
        facultyToken = jwtUtil.generateToken(facultyUser.getUserId(), facultyUser.getCollegeId(), facultyUser.getRole(), facultyUser.getName());
        studentToken = jwtUtil.generateToken(studentUser.getUserId(), studentUser.getCollegeId(), studentUser.getRole(), studentUser.getName());
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
            User student = User.builder()
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
            userRepository.save(student);
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

    private SurveyQuestionSchema createSampleSchema() {
        return SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(Arrays.asList(
                        QuestionDefinition.builder()
                                .key("rating_q")
                                .type(QuestionType.RATING)
                                .prompt("Rate your confidence")
                                .required(true)
                                .rating(new RatingDefinition(1, 5, "Min", "Max"))
                                .build(),
                        QuestionDefinition.builder()
                                .key("yes_no_q")
                                .type(QuestionType.YES_NO)
                                .prompt("Do you understand the syllabus?")
                                .required(true)
                                .build()
                ))
                .build();
    }

    // -------------------------------------------------------------
    // 1. TARGETING TESTS
    // -------------------------------------------------------------

    @Test
    public void testTargetingAllActiveStudents() {
        TargetFilter allFilter = new TargetFilter("ALL", List.of(), List.of(), List.of());
        long count = targetingService.countEligibleStudents(allFilter);
        // Out of 10 seeded students, 9 are active, 1 is inactive
        assertTrue(count >= 9, "Active count must include all active students");
        List<User> students = targetingService.findEligibleStudents(allFilter);
        assertTrue(students.stream().noneMatch(u -> "26TEST10".equals(u.getCollegeId())), "Inactive student must be excluded");
    }

    @Test
    public void testTargetingBranchFilter() {
        TargetFilter cseFilter = TargetFilter.builder()
                .scope("FILTER")
                .branches(List.of("CSE"))
                .build();

        long count = targetingService.countEligibleStudents(cseFilter);
        // Active CSE students: 26TEST01, 26TEST02, 26TEST03, 26TEST04 (26TEST10 is inactive) -> 4 students
        assertEquals(4, count);
    }

    @Test
    public void testTargetingYearFilter() {
        TargetFilter year3Filter = TargetFilter.builder()
                .scope("FILTER")
                .years(List.of(3))
                .build();

        long count = targetingService.countEligibleStudents(year3Filter);
        // Active Year 3 students: 26TEST03, 26TEST04, 26TEST06, 26TEST08 -> 4 students
        assertEquals(4, count);
    }

    @Test
    public void testTargetingSectionFilter() {
        TargetFilter secAFilter = TargetFilter.builder()
                .scope("FILTER")
                .sections(List.of("A"))
                .build();

        long count = targetingService.countEligibleStudents(secAFilter);
        // Active Section A students: 26TEST01, 26TEST03, 26TEST05, 26TEST06, 26TEST08 -> 5 students
        assertEquals(5, count);
    }

    @Test
    public void testTargetingCombinedFilter() {
        TargetFilter combined = TargetFilter.builder()
                .scope("FILTER")
                .branches(List.of("CSE"))
                .years(List.of(2))
                .sections(List.of("A"))
                .build();

        long count = targetingService.countEligibleStudents(combined);
        // Active CSE Year 2 Sec A: only 26TEST01 -> 1 student
        assertEquals(1, count);
    }

    @Test
    public void testTargetingOmittedArraysMeansAllValues() {
        TargetFilter itOnly = TargetFilter.builder()
                .scope("FILTER")
                .branches(List.of("IT"))
                .build();

        long count = targetingService.countEligibleStudents(itOnly);
        // Active IT students: 26TEST05 (yr2 secA), 26TEST06 (yr3 secA), 26TEST07 (yr4 secB) -> 3 students
        assertEquals(3, count);
    }

    @Test
    public void testHodFilterIsForcedToOwnBranch() throws Exception {
        // HOD is CSE, but passes IT or ALL in the request
        TargetFilter hodFilter = TargetFilter.builder()
                .scope("ALL")
                .branches(List.of("IT"))
                .build();

        mockMvc.perform(post("/api/surveys/eligible-count")
                        .header("Authorization", "Bearer " + hodToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hodFilter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4)); // Forced to CSE active count (4)
    }

    @Test
    public void testFacultyCannotTargetCollegeWide() throws Exception {
        TargetFilter allFilter = TargetFilter.builder()
                .scope("ALL")
                .build();

        mockMvc.perform(post("/api/surveys/eligible-count")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allFilter)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // 2. CREATION & LIFECYCLE TESTS
    // -------------------------------------------------------------

    @Test
    public void testCreateDraftSurvey() throws Exception {
        CreateSurveyRequest req = CreateSurveyRequest.builder()
                .title("Midterm Academic Feedback")
                .description("Gathering feedback on course progress")
                .category(SurveyCategory.ACADEMIC)
                .termId(defaultTerm.getTermId())
                .questionSchema(createSampleSchema())
                .isAnonymous(false)
                .targetFilter(TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build())
                .build();

        MvcResult result = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.surveyId").isNumber())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.title").value("Midterm Academic Feedback"))
                .andReturn();

        SurveyResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), SurveyResponse.class);
        assertNotNull(response.getTemplateId());
    }

    @Test
    public void testWellbeingSurveyForcedAnonymous() throws Exception {
        CreateSurveyRequest req = CreateSurveyRequest.builder()
                .title("Campus Wellbeing Pulse")
                .category(SurveyCategory.WELLBEING)
                .termId(defaultTerm.getTermId())
                .questionSchema(createSampleSchema())
                .isAnonymous(false) // Client tries non-anonymous
                .targetFilter(new TargetFilter("ALL", List.of(), List.of(), List.of()))
                .build();

        mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isAnonymous").value(true)); // Forced to true
    }

    @Test
    public void testDraftEditSucceeds() throws Exception {
        // 1. Create Draft
        CreateSurveyRequest createReq = CreateSurveyRequest.builder()
                .title("Initial Draft Title")
                .category(SurveyCategory.ACADEMIC)
                .termId(defaultTerm.getTermId())
                .questionSchema(createSampleSchema())
                .targetFilter(new TargetFilter("ALL", List.of(), List.of(), List.of()))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        SurveyResponse created = objectMapper.readValue(createRes.getResponse().getContentAsString(), SurveyResponse.class);

        // 2. Update Draft
        UpdateSurveyRequest updateReq = UpdateSurveyRequest.builder()
                .title("Updated Draft Title")
                .description("Updated description")
                .category(SurveyCategory.EVENT)
                .build();

        mockMvc.perform(put("/api/surveys/" + created.getSurveyId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Draft Title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.category").value("EVENT"));
    }

    // -------------------------------------------------------------
    // 3. ACTIVATION & PARTICIPATION LOG TESTS
    // -------------------------------------------------------------

    @Test
    public void testActivateSurveyBuildsParticipationLog() throws Exception {
        // 1. Create Draft targeted to CSE (4 eligible active students)
        CreateSurveyRequest createReq = CreateSurveyRequest.builder()
                .title("Placement Readiness Pilot")
                .category(SurveyCategory.PLACEMENT)
                .termId(defaultTerm.getTermId())
                .questionSchema(createSampleSchema())
                .targetFilter(TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build())
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        SurveyResponse created = objectMapper.readValue(createRes.getResponse().getContentAsString(), SurveyResponse.class);
        Long surveyId = created.getSurveyId();

        // 2. Activate survey
        mockMvc.perform(post("/api/surveys/" + surveyId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.eligibleCount").value(4))
                .andExpect(jsonPath("$.activatedAt").isNotEmpty());

        // 3. Verify participation_log rows in DB
        List<ParticipationLog> logs = participationLogRepository.findById_SurveyId(surveyId);
        assertEquals(4, logs.size());
        for (ParticipationLog log : logs) {
            assertFalse(log.getHasResponded());
        }

        // 4. Verify progress endpoint
        mockMvc.perform(get("/api/surveys/" + surveyId + "/progress")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responded").value(0))
                .andExpect(jsonPath("$.eligible").value(4))
                .andExpect(jsonPath("$.rate").value(0.0));

        // 5. Verify adding a student after activation DOES NOT insert them into the active survey's participation_log
        User lateStudent = User.builder()
                .collegeId("26TEST_LATE")
                .name("Late Enrolled Student")
                .email("late@skillsense.edu")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .role(Role.STUDENT)
                .branch("CSE")
                .studyYear(2)
                .section("A")
                .isActive(true)
                .mustChangePassword(false)
                .build();
        userRepository.save(lateStudent);

        List<ParticipationLog> logsAfter = participationLogRepository.findById_SurveyId(surveyId);
        assertEquals(4, logsAfter.size(), "Students added after activation must NOT be added to existing participation_log");
    }

    // -------------------------------------------------------------
    // 4. ACTIVE SURVEY IMMUTABILITY TESTS
    // -------------------------------------------------------------

    @Test
    public void testActiveSurveyAllowsOnlyTitleDescriptionAndClosesAt() throws Exception {
        // Create & Activate
        CreateSurveyRequest createReq = CreateSurveyRequest.builder()
                .title("Active Survey Immutability Test")
                .category(SurveyCategory.ACADEMIC)
                .termId(defaultTerm.getTermId())
                .questionSchema(createSampleSchema())
                .targetFilter(TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build())
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        SurveyResponse created = objectMapper.readValue(createRes.getResponse().getContentAsString(), SurveyResponse.class);
        Long surveyId = created.getSurveyId();

        mockMvc.perform(post("/api/surveys/" + surveyId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 1. Changing title, description, closesAt succeeds
        LocalDateTime newClosesAt = LocalDateTime.now().plusDays(7);
        UpdateSurveyRequest allowedUpdate = UpdateSurveyRequest.builder()
                .title("Updated Active Title")
                .description("Updated Active Description")
                .closesAt(newClosesAt)
                .build();

        mockMvc.perform(put("/api/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allowedUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Active Title"))
                .andExpect(jsonPath("$.description").value("Updated Active Description"));

        // 2. Changing category fails
        UpdateSurveyRequest badCategory = UpdateSurveyRequest.builder()
                .category(SurveyCategory.EVENT)
                .build();
        mockMvc.perform(put("/api/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badCategory)))
                .andExpect(status().isBadRequest());

        // 3. Changing target fails
        UpdateSurveyRequest badTarget = UpdateSurveyRequest.builder()
                .targetFilter(new TargetFilter("ALL", List.of(), List.of(), List.of()))
                .build();
        mockMvc.perform(put("/api/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badTarget)))
                .andExpect(status().isBadRequest());

        // 4. Changing question schema fails
        SurveyQuestionSchema modifiedSchema = SurveyQuestionSchema.builder()
                .schemaVersion(1)
                .questions(List.of(QuestionDefinition.builder()
                        .key("new_key")
                        .type(QuestionType.YES_NO)
                        .prompt("New question")
                        .build()))
                .build();
        UpdateSurveyRequest badSchema = UpdateSurveyRequest.builder()
                .questionSchema(modifiedSchema)
                .build();
        mockMvc.perform(put("/api/surveys/" + surveyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badSchema)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------
    // 5. DELETION, CLOSING & VERSIONING TESTS
    // -------------------------------------------------------------

    @Test
    public void testDeleteDraftSurvey() throws Exception {
        CreateSurveyRequest createReq = CreateSurveyRequest.builder()
                .title("Draft Survey To Delete")
                .category(SurveyCategory.EVENT)
                .termId(defaultTerm.getTermId())
                .questionSchema(createSampleSchema())
                .targetFilter(TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build())
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        SurveyResponse created = objectMapper.readValue(createRes.getResponse().getContentAsString(), SurveyResponse.class);

        // Delete DRAFT survey
        mockMvc.perform(delete("/api/surveys/" + created.getSurveyId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(surveyRepository.existsById(created.getSurveyId()));
    }

    @Test
    public void testCloseSurveyAndCreateNewVersion() throws Exception {
        // 1. Create and activate
        CreateSurveyRequest createReq = CreateSurveyRequest.builder()
                .title("Placement Readiness v1")
                .category(SurveyCategory.PLACEMENT)
                .termId(defaultTerm.getTermId())
                .questionSchema(createSampleSchema())
                .targetFilter(TargetFilter.builder().scope("FILTER").branches(List.of("CSE")).build())
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn();
        SurveyResponse v1 = objectMapper.readValue(createRes.getResponse().getContentAsString(), SurveyResponse.class);
        Long v1Id = v1.getSurveyId();

        mockMvc.perform(post("/api/surveys/" + v1Id + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 2. Close survey
        mockMvc.perform(post("/api/surveys/" + v1Id + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // 3. Create new version under next term
        NewVersionRequest versionReq = new NewVersionRequest(nextTerm.getTermId());

        MvcResult v2Res = mockMvc.perform(post("/api/surveys/" + v1Id + "/new-version")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(versionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.termId").value(nextTerm.getTermId()))
                .andExpect(jsonPath("$.eligibleCount").value(0))
                .andReturn();

        SurveyResponse v2 = objectMapper.readValue(v2Res.getResponse().getContentAsString(), SurveyResponse.class);
        assertEquals(v1.getTemplateId(), v2.getTemplateId(), "Both versions must share the same template");

        // 4. Verify question schema is copied and identical
        Survey s1 = surveyRepository.findById(v1Id).orElseThrow();
        Survey s2 = surveyRepository.findById(v2.getSurveyId()).orElseThrow();
        assertEquals(s1.getQuestionSchema(), s2.getQuestionSchema(), "Question keys and schema must be preserved across versions");
    }

    // -------------------------------------------------------------
    // 6. PRESETS & SCHEDULED CLOSE TESTS
    // -------------------------------------------------------------

    @Test
    public void testAllSixPresetsLoadAndValidate() throws Exception {
        for (SurveyCategory category : SurveyCategory.values()) {
            JsonNode presetNode = presetService.getPreset(category.name());
            assertNotNull(presetNode, "Preset for " + category + " should exist");
            assertEquals(1, presetNode.get("schemaVersion").asInt());
            assertTrue(presetNode.get("questions").isArray());
            assertTrue(presetNode.get("questions").size() >= 1);

            mockMvc.perform(get("/api/survey-presets/" + category.name())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.schemaVersion").value(1));
        }
    }

    @Test
    public void testScheduledJobClosesExpiredActiveSurveys() {
        // Create an active survey with closesAt in the past
        SurveyTemplate template = templateRepository.save(
                SurveyTemplate.builder().name("Expired Survey").category(SurveyCategory.EVENT).createdBy(adminUser).build()
        );

        Survey expiredSurvey = Survey.builder()
                .template(template)
                .version(1)
                .title("Expired Event Survey")
                .category(SurveyCategory.EVENT)
                .term(defaultTerm)
                .questionSchema(objectMapper.valueToTree(createSampleSchema()).toString())
                .targetFilter(objectMapper.valueToTree(new TargetFilter("ALL", List.of(), List.of(), List.of())).toString())
                .status(SurveyStatus.ACTIVE)
                .opensAt(LocalDateTime.now().minusDays(2))
                .closesAt(LocalDateTime.now().minusHours(1)) // In the past!
                .eligibleCount(5)
                .createdBy(adminUser)
                .build();
        expiredSurvey = surveyRepository.save(expiredSurvey);

        // Run scheduler
        schedulerService.closeExpiredSurveys();

        // Verify it was transitioned to CLOSED
        Survey reloaded = surveyRepository.findById(expiredSurvey.getSurveyId()).orElseThrow();
        assertEquals(SurveyStatus.CLOSED, reloaded.getStatus());
    }
}
