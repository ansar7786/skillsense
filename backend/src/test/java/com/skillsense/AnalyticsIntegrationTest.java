package com.skillsense;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skillsense.analytics.client.AnalyticsClient;
import com.skillsense.analytics.dto.InsightResponse;
import com.skillsense.analytics.dto.RecommendationItem;
import com.skillsense.analytics.dto.StudentResultResponse;
import com.skillsense.analytics.dto.fastapi.FastApiAnalyzeRequest;
import com.skillsense.analytics.dto.fastapi.FastApiInsightsRequest;
import com.skillsense.analytics.dto.fastapi.FastApiResponseRow;
import com.skillsense.analytics.dto.fastapi.FastApiStudentResultRequest;
import com.skillsense.analytics.entity.AiUsage;
import com.skillsense.analytics.entity.AnalyticsCache;
import com.skillsense.analytics.entity.AnalyticsCacheId;
import com.skillsense.analytics.entity.AnalyticsCacheKind;
import com.skillsense.analytics.repository.AiUsageRepository;
import com.skillsense.analytics.repository.AnalyticsCacheRepository;
import com.skillsense.analytics.repository.StudentResultRepository;
import com.skillsense.analytics.service.AnalyticsService;
import com.skillsense.auth.JwtUtil;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.response.entity.Response;
import com.skillsense.response.repository.ResponseRepository;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.entity.SurveyTemplate;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.repository.ParticipationLogRepository;
import com.skillsense.survey.repository.SurveyRepository;
import com.skillsense.survey.repository.SurveyTemplateRepository;
import com.skillsense.term.AcademicTerm;
import com.skillsense.term.AcademicTermRepository;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyTemplateRepository templateRepository;

    @Autowired
    private AcademicTermRepository termRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParticipationLogRepository participationLogRepository;

    @Autowired
    private ResponseRepository responseRepository;

    @Autowired
    private AnalyticsCacheRepository analyticsCacheRepository;

    @Autowired
    private StudentResultRepository studentResultRepository;

    @Autowired
    private AiUsageRepository aiUsageRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AnalyticsClient analyticsClient;

    private User adminUser;
    private User hodCseUser;
    private User hodMechUser;
    private User facultyUser;
    private User placementUser;
    private User studentUser;
    private AcademicTerm defaultTerm;

    private String adminToken;
    private String hodCseToken;
    private String hodMechToken;
    private String facultyToken;
    private String placementToken;
    private String studentToken;

    @BeforeEach
    public void setUp() {
        cleanDb();

        if (termRepository.count() == 0) {
            defaultTerm = termRepository.save(new AcademicTerm(null, "2026-27 Odd", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), true));
        } else {
            defaultTerm = termRepository.findAll().get(0);
        }

        adminUser = getOrCreateUser("admin_p6", "Admin P6", Role.ADMIN, null, null, null);
        hodCseUser = getOrCreateUser("hod_cse_p6", "HOD CSE", Role.HOD, "CSE", null, null);
        hodMechUser = getOrCreateUser("hod_mech_p6", "HOD MECH", Role.HOD, "MECH", null, null);
        facultyUser = getOrCreateUser("faculty_p6", "Faculty P6", Role.FACULTY, "CSE", null, null);
        placementUser = getOrCreateUser("placement_p6", "Placement P6", Role.PLACEMENT_OFFICER, null, null, null);
        studentUser = getOrCreateUser("student_p6", "Student P6", Role.STUDENT, "CSE", 3, "A");

        adminToken = jwtUtil.generateToken(adminUser.getUserId(), adminUser.getCollegeId(), adminUser.getRole(), adminUser.getName());
        hodCseToken = jwtUtil.generateToken(hodCseUser.getUserId(), hodCseUser.getCollegeId(), hodCseUser.getRole(), hodCseUser.getName());
        hodMechToken = jwtUtil.generateToken(hodMechUser.getUserId(), hodMechUser.getCollegeId(), hodMechUser.getRole(), hodMechUser.getName());
        facultyToken = jwtUtil.generateToken(facultyUser.getUserId(), facultyUser.getCollegeId(), facultyUser.getRole(), facultyUser.getName());
        placementToken = jwtUtil.generateToken(placementUser.getUserId(), placementUser.getCollegeId(), placementUser.getRole(), placementUser.getName());
        studentToken = jwtUtil.generateToken(studentUser.getUserId(), studentUser.getCollegeId(), studentUser.getRole(), studentUser.getName());
    }

    @AfterEach
    public void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        jdbcTemplate.execute("DELETE FROM student_results");
        jdbcTemplate.execute("DELETE FROM analytics_cache");
        jdbcTemplate.execute("DELETE FROM ai_usage");
        responseRepository.deleteAll();
        participationLogRepository.deleteAll();
        surveyRepository.deleteAll();
        templateRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM users WHERE college_id LIKE 'resp_p6_%'");
    }

    private User getOrCreateUser(String collegeId, String name, Role role, String branch, Integer year, String section) {
        return userRepository.findByCollegeId(collegeId).orElseGet(() -> userRepository.save(User.builder()
                .collegeId(collegeId)
                .name(name)
                .email(collegeId.toLowerCase() + "@test.edu")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .role(role)
                .branch(branch)
                .studyYear(year)
                .section(section)
                .isActive(true)
                .mustChangePassword(false)
                .build()));
    }

    private Survey createSurvey(String title, SurveyCategory category, boolean isAnonymous, String targetFilter, User creator) {
        SurveyTemplate template = templateRepository.save(new SurveyTemplate(null, title + " Template", category, creator, LocalDateTime.now()));
        String questionSchema = "{\"questions\":[{\"key\":\"q1\",\"type\":\"RATING\",\"prompt\":\"Rate Java skills\",\"rating\":{\"min\":1,\"max\":5},\"scoring\":{\"skill\":\"java\",\"weight\":1.0}},{\"key\":\"q2\",\"type\":\"TEXT\",\"prompt\":\"What was challenging?\"}]}";
        String skillTargets = "{\"java\":70.0}";

        Survey s = Survey.builder()
                .template(template)
                .version(1)
                .title(title)
                .description("Test description")
                .category(category)
                .term(defaultTerm)
                .questionSchema(questionSchema)
                .skillTargets(skillTargets)
                .isAnonymous(isAnonymous)
                .targetFilter(targetFilter)
                .status(SurveyStatus.ACTIVE)
                .opensAt(LocalDateTime.now().minusDays(1))
                .closesAt(LocalDateTime.now().plusDays(10))
                .eligibleCount(20)
                .createdBy(creator)
                .build();
        return surveyRepository.save(s);
    }

    private void seedResponses(Survey survey, int count) {
        seedResponses(survey, count, 1);
    }

    private void seedResponses(Survey survey, int count, int startIndex) {
        boolean isAnon = Boolean.TRUE.equals(survey.getIsAnonymous());
        for (int i = startIndex; i < startIndex + count; i++) {
            User respondent = isAnon ? null : getOrCreateUser("resp_p6_" + i + "_" + survey.getSurveyId(), "Respondent " + i, Role.STUDENT, "CSE", 3, "A");
            Response r = Response.builder()
                    .responseId(UUID.randomUUID().toString())
                    .survey(survey)
                    .student(respondent)
                    .groupBranch("CSE")
                    .groupYear(3)
                    .groupSection(isAnon ? null : "A")
                    .answerData("{\"q1\":4,\"q2\":\"Time management problem " + i + "\"}")
                    .submittedAt(isAnon ? null : LocalDateTime.now())
                    .build();
            responseRepository.save(r);
        }
    }

    // -------------------------------------------------------------
    // 1. FIVE-RESPONSE PRIVACY GATE
    // -------------------------------------------------------------

    @Test
    public void testFiveResponseGate_ZeroResponses_AnonymousSurvey() throws Exception {
        Survey survey = createSurvey("Anon Survey", SurveyCategory.WELLBEING, true, "{\"scope\":\"ALL\"}", adminUser);

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.have").value(0))
                .andExpect(jsonPath("$.need").value(5))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(analyticsClient, never()).analyze(any());
    }

    @Test
    public void testFiveResponseGate_OneToFourResponses_AnonymousSurvey() throws Exception {
        Survey survey = createSurvey("Anon Survey", SurveyCategory.WELLBEING, true, "{\"scope\":\"ALL\"}", adminUser);
        seedResponses(survey, 3); // 3 responses < 5

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.have").value(3))
                .andExpect(jsonPath("$.need").value(5))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(analyticsClient, never()).analyze(any());
    }

    @Test
    public void testFiveResponseGate_FivePlusResponses_AnonymousSurvey() throws Exception {
        Survey survey = createSurvey("Anon Survey", SurveyCategory.WELLBEING, true, "{\"scope\":\"ALL\"}", adminUser);
        seedResponses(survey, 6);

        ObjectNode mockAggregate = objectMapper.createObjectNode();
        mockAggregate.put("response_count", 6);
        mockAggregate.put("index", 75.0);
        when(analyticsClient.analyze(any(FastApiAnalyzeRequest.class))).thenReturn(mockAggregate);

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.have").value(6))
                .andExpect(jsonPath("$.need").value(5))
                .andExpect(jsonPath("$.data.response_count").value(6));

        verify(analyticsClient, times(1)).analyze(any());
    }

    // -------------------------------------------------------------
    // 2. ANALYTICS CACHE (MISS, HIT, STALE, REVALIDATION)
    // -------------------------------------------------------------

    @Test
    public void testAnalyticsCache_MissThenHitThenStale() throws Exception {
        Survey survey = createSurvey("Scored Survey", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        seedResponses(survey, 5);

        ObjectNode mockAggregate1 = objectMapper.createObjectNode();
        mockAggregate1.put("response_count", 5);
        mockAggregate1.put("index", 80.0);
        when(analyticsClient.analyze(any(FastApiAnalyzeRequest.class))).thenReturn(mockAggregate1);

        // 1. Cache Miss -> Calls FastAPI
        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.have").value(5))
                .andExpect(jsonPath("$.data.index").value(80.0));

        verify(analyticsClient, times(1)).analyze(any());
        assertTrue(analyticsCacheRepository.findById_SurveyIdAndId_Kind(survey.getSurveyId(), AnalyticsCacheKind.AGGREGATE).isPresent());

        // 2. Cache Hit -> Reuses cached payload, FastAPI NOT called again
        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.have").value(5));

        verify(analyticsClient, times(1)).analyze(any()); // Still 1

        // 3. Stale Cache -> Response count changes from 5 to 6
        seedResponses(survey, 1, 6); // Now 6 responses
        ObjectNode mockAggregate2 = objectMapper.createObjectNode();
        mockAggregate2.put("response_count", 6);
        mockAggregate2.put("index", 82.5);
        when(analyticsClient.analyze(any(FastApiAnalyzeRequest.class))).thenReturn(mockAggregate2);

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.have").value(6))
                .andExpect(jsonPath("$.data.index").value(82.5));

        verify(analyticsClient, times(2)).analyze(any()); // Second call made
    }

    // -------------------------------------------------------------
    // 3. ANALYTICS CLIENT 503 ERROR MAPPING
    // -------------------------------------------------------------

    @Test
    public void testAnalyticsUnavailable_Returns503() throws Exception {
        Survey survey = createSurvey("Failing Survey", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        seedResponses(survey, 5);

        when(analyticsClient.analyze(any())).thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.ANALYTICS_UNAVAILABLE, "Analytics service is currently unavailable"));

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ANALYTICS_UNAVAILABLE"));
    }

    // -------------------------------------------------------------
    // 4. AUTHORIZATION & SCOPE ENFORCEMENT
    // -------------------------------------------------------------

    @Test
    public void testAuthorization_StudentForbiddenFromOfficialAnalytics() throws Exception {
        Survey survey = createSurvey("Placement", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAuthorization_HodOwnBranchAllowed_OtherBranchForbidden403() throws Exception {
        // Survey targeting ONLY CSE
        Survey cseSurvey = createSurvey("CSE Survey", SurveyCategory.ACADEMIC, false,
                "{\"scope\":\"FILTER\",\"branches\":[\"CSE\"]}", adminUser);

        // HOD CSE can access
        mockMvc.perform(get("/api/surveys/" + cseSurvey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + hodCseToken))
                .andExpect(status().isOk());

        // HOD MECH requesting CSE survey MUST get 403 FORBIDDEN
        mockMvc.perform(get("/api/surveys/" + cseSurvey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + hodMechToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAuthorization_FacultyAllowedOnlyOwnSurvey() throws Exception {
        Survey facultySurvey = createSurvey("Faculty Own", SurveyCategory.ACADEMIC, false, "{\"scope\":\"ALL\"}", facultyUser);
        Survey adminSurvey = createSurvey("Admin Survey", SurveyCategory.ACADEMIC, false, "{\"scope\":\"ALL\"}", adminUser);

        // Faculty accessing own survey -> OK
        mockMvc.perform(get("/api/surveys/" + facultySurvey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk());

        // Faculty accessing other's survey -> 403
        mockMvc.perform(get("/api/surveys/" + adminSurvey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAuthorization_PlacementOfficerCategoryScope() throws Exception {
        Survey placementSurvey = createSurvey("Campus Placement", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        Survey academicSurvey = createSurvey("Semester Feedback", SurveyCategory.ACADEMIC, false, "{\"scope\":\"ALL\"}", adminUser);

        // Placement officer accessing placement survey -> OK
        mockMvc.perform(get("/api/surveys/" + placementSurvey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + placementToken))
                .andExpect(status().isOk());

        // Placement officer accessing academic survey -> 403
        mockMvc.perform(get("/api/surveys/" + academicSurvey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + placementToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // 5. STUDENT RESULTS (IDENTIFIED VS ANONYMOUS VS UNSCORED)
    // -------------------------------------------------------------

    @Test
    public void testStudentResult_AnonymousSurveyReturns404() throws Exception {
        Survey anonSurvey = createSurvey("Anon Wellbeing", SurveyCategory.WELLBEING, true, "{\"scope\":\"ALL\"}", adminUser);

        mockMvc.perform(get("/api/my/surveys/" + anonSurvey.getSurveyId() + "/result")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testStudentResult_UnscoredSurveyReturns404() throws Exception {
        SurveyTemplate template = templateRepository.save(new SurveyTemplate(null, "Unscored Template", SurveyCategory.CUSTOM, adminUser, LocalDateTime.now()));
        String questionSchema = "{\"questions\":[{\"key\":\"q1\",\"type\":\"TEXT\",\"prompt\":\"Feedback\"}]}";

        Survey unscoredSurvey = surveyRepository.save(Survey.builder()
                .template(template)
                .version(1)
                .title("Unscored Survey")
                .category(SurveyCategory.CUSTOM)
                .term(defaultTerm)
                .questionSchema(questionSchema)
                .isAnonymous(false)
                .targetFilter("{\"scope\":\"ALL\"}")
                .status(SurveyStatus.ACTIVE)
                .eligibleCount(10)
                .createdBy(adminUser)
                .build());

        mockMvc.perform(get("/api/my/surveys/" + unscoredSurvey.getSurveyId() + "/result")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testStudentResult_IdentifiedScoredSurveyReturnsResult() throws Exception {
        Survey scoredSurvey = createSurvey("Placement Drive", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        Response r = Response.builder()
                .responseId(UUID.randomUUID().toString())
                .survey(scoredSurvey)
                .student(studentUser)
                .groupBranch("CSE")
                .groupYear(3)
                .groupSection("A")
                .answerData("{\"q1\":4,\"q2\":\"Time management\"}")
                .submittedAt(LocalDateTime.now())
                .build();
        responseRepository.save(r);

        StudentResultResponse mockResp = new StudentResultResponse();
        mockResp.setReadiness(65.0);
        mockResp.setBand("DEVELOPING");
        mockResp.setStrongestSkill("java");
        mockResp.setWeakestSkill("java");
        mockResp.setSkills(List.of(new StudentResultResponse.SkillResultItem("java", 65.0, 70.0, 5.0, 60.0)));
        when(analyticsClient.studentResult(any(FastApiStudentResultRequest.class))).thenReturn(mockResp);

        mockMvc.perform(get("/api/my/surveys/" + scoredSurvey.getSurveyId() + "/result")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readiness").value(65.0))
                .andExpect(jsonPath("$.band").value("DEVELOPING"))
                .andExpect(jsonPath("$.tips", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.disclaimer").value("Self-assessed indicator, not an exam score"));
    }

    // -------------------------------------------------------------
    // 6. AI QUOTA ENFORCEMENT & INSIGHT CACHING
    // -------------------------------------------------------------

    @Test
    public void testAiQuota_UnderLimit_PassesAllowAiTrue_AndIncrementsOnSourceAi() throws Exception {
        Survey survey = createSurvey("AI Placement", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        seedResponses(survey, 5);

        ObjectNode mockAggregate = objectMapper.createObjectNode();
        mockAggregate.put("response_count", 5);
        when(analyticsClient.analyze(any())).thenReturn(mockAggregate);

        InsightResponse aiInsight = new InsightResponse(
                "Java readiness is at 60%",
                List.of("Cohort average is 60%"),
                List.of(new RecommendationItem(1, "Workshop", "Run workshop", "Score is 60", "Placement cell")),
                Collections.emptyList(),
                "AI",
                LocalDateTime.now().toString()
        );
        when(analyticsClient.insights(any(FastApiInsightsRequest.class))).thenReturn(aiInsight);

        // 1. Initial daily usage: 0
        assertEquals(0, aiUsageRepository.findByUsageDate(LocalDate.now()).map(AiUsage::getCalls).orElse(0));

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/insights")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.headline").value("Java readiness is at 60%"));

        // 2. Incremented because source == AI
        assertEquals(1, aiUsageRepository.findByUsageDate(LocalDate.now()).map(AiUsage::getCalls).orElse(0));
    }

    @Test
    public void testAiQuota_AtDailyLimit_PassesAllowAiFalse() throws Exception {
        Survey survey = createSurvey("AI Quota Capped", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        seedResponses(survey, 5);

        ObjectNode mockAggregate = objectMapper.createObjectNode();
        mockAggregate.put("response_count", 5);
        when(analyticsClient.analyze(any())).thenReturn(mockAggregate);

        // Simulate quota reached (50 calls)
        aiUsageRepository.save(new AiUsage(LocalDate.now(), 50));

        InsightResponse rulesInsight = new InsightResponse(
                "Deterministic Headline",
                List.of("Finding 1"),
                List.of(new RecommendationItem(1, "Workshop", "Run workshop", "Score is 60", "Placement cell")),
                Collections.emptyList(),
                "RULES",
                LocalDateTime.now().toString()
        );
        when(analyticsClient.insights(any(FastApiInsightsRequest.class))).thenReturn(rulesInsight);

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/insights")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("RULES"));

        ArgumentCaptor<FastApiInsightsRequest> captor = ArgumentCaptor.forClass(FastApiInsightsRequest.class);
        verify(analyticsClient).insights(captor.capture());
        assertFalse(captor.getValue().getAllowAi(), "allow_ai must be false when daily quota is reached");

        // Quota is NOT incremented on RULES fallback
        assertEquals(50, aiUsageRepository.findByUsageDate(LocalDate.now()).map(AiUsage::getCalls).orElse(0));
    }

    @Test
    public void testAiInsightCaching_AndRegenerateThrottle() throws Exception {
        Survey survey = createSurvey("Insight Caching", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        seedResponses(survey, 10);

        ObjectNode mockAggregate = objectMapper.createObjectNode();
        mockAggregate.put("response_count", 10);
        when(analyticsClient.analyze(any())).thenReturn(mockAggregate);

        InsightResponse firstInsight = new InsightResponse(
                "Initial Insight",
                List.of("Finding A"),
                List.of(),
                List.of(),
                "RULES",
                LocalDateTime.now().toString()
        );
        when(analyticsClient.insights(any())).thenReturn(firstInsight);

        // 1. Initial insight call -> calls analyticsClient.insights
        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/insights")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Initial Insight"));

        verify(analyticsClient, times(1)).insights(any());

        // 2. Second call with unchanged count (<10% change) -> reuses cache
        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/insights")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Initial Insight"));

        verify(analyticsClient, times(1)).insights(any()); // Still 1

        // 3. Manual Regenerate (refresh=true) called within 10 minutes -> THROTTLED (reuses cache)
        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/insights?refresh=true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Initial Insight"));

        verify(analyticsClient, times(1)).insights(any()); // Still 1, throttle prevented Gemini call
    }

    // -------------------------------------------------------------
    // 7. PRIVACY TEST: VERIFY NO STUDENT PII IN FASTAPI REQUEST
    // -------------------------------------------------------------

    @Test
    public void testPrivacyBoundary_NoStudentPiiSentInFastApiRequest() throws Exception {
        Survey survey = createSurvey("Privacy Checked Survey", SurveyCategory.PLACEMENT, true, "{\"scope\":\"ALL\"}", adminUser);
        seedResponses(survey, 5);

        ObjectNode mockAggregate = objectMapper.createObjectNode();
        mockAggregate.put("response_count", 5);
        when(analyticsClient.analyze(any())).thenReturn(mockAggregate);

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/analytics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        ArgumentCaptor<FastApiAnalyzeRequest> captor = ArgumentCaptor.forClass(FastApiAnalyzeRequest.class);
        verify(analyticsClient).analyze(captor.capture());

        FastApiAnalyzeRequest request = captor.getValue();
        assertNotNull(request);
        assertNotNull(request.getResponses());
        assertEquals(5, request.getResponses().size());

        for (FastApiResponseRow row : request.getResponses()) {
            // For anonymous surveys, studentId must be null
            assertNull(row.getStudentId(), "Student ID must be NULL on anonymous requests");
            // No student name, roll number, email exists in FastApiResponseRow
            assertNotNull(row.getBranch());
            assertNotNull(row.getYear());
            assertNotNull(row.getAnswers());
        }

        // Verify JSON serialization contains NO student identity fields
        String serialized = objectMapper.writeValueAsString(request);
        assertFalse(serialized.contains("student_p6"), "Request must not contain student username/collegeId");
        assertFalse(serialized.contains("Student P6"), "Request must not contain student name");
        assertFalse(serialized.contains("student_p6@test.edu"), "Request must not contain student email");
    }

    // -------------------------------------------------------------
    // 8. TEXTS ENDPOINT (5-RESPONSE PRIVACY GATE & SHUFFLING)
    // -------------------------------------------------------------

    @Test
    public void testTextsEndpoint_FiveResponseGate() throws Exception {
        Survey anonSurvey = createSurvey("Texts Survey", SurveyCategory.ACADEMIC, true, "{\"scope\":\"ALL\"}", adminUser);

        // 3 responses < 5 -> returns empty list
        seedResponses(anonSurvey, 3);
        mockMvc.perform(get("/api/surveys/" + anonSurvey.getSurveyId() + "/texts?key=q2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Add 3 more responses -> total 6 responses >= 5
        seedResponses(anonSurvey, 3);
        mockMvc.perform(get("/api/surveys/" + anonSurvey.getSurveyId() + "/texts?key=q2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))));
    }

    // -------------------------------------------------------------
    // 9. DASHBOARD SUMMARY ENDPOINT
    // -------------------------------------------------------------

    @Test
    public void testDashboardSummary() throws Exception {
        createSurvey("Survey A", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        createSurvey("Survey B", SurveyCategory.ACADEMIC, true, "{\"scope\":\"ALL\"}", adminUser);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surveysPerStatus.ACTIVE").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.averageCompletionRate").isNumber());
    }
}
