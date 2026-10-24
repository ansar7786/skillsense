package com.skillsense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skillsense.analytics.client.AnalyticsClient;
import com.skillsense.analytics.dto.fastapi.FastApiAnalyzeRequest;
import com.skillsense.analytics.repository.AnalyticsCacheRepository;
import com.skillsense.auth.JwtUtil;
import com.skillsense.report.ReportExportService;
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
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ReportExportIntegrationTest {

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
    private ResponseRepository responseRepository;

    @Autowired
    private ParticipationLogRepository participationLogRepository;

    @Autowired
    private AnalyticsCacheRepository analyticsCacheRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private AnalyticsClient analyticsClient;

    private User adminUser;
    private User facultyUser;
    private User otherFacultyUser;
    private User studentUser;
    private AcademicTerm defaultTerm;

    private String adminToken;
    private String facultyToken;
    private String otherFacultyToken;
    private String studentToken;

    @BeforeEach
    public void setup() {
        defaultTerm = termRepository.findByIsCurrentTrue()
                .or(() -> termRepository.findByLabel("2026-27 Odd"))
                .or(() -> termRepository.findByLabel("2026-27 Even"))
                .orElseGet(() -> termRepository.save(new AcademicTerm(null, "2026-27 Odd",
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), true)));

        adminUser = getOrCreateUser("admin_exp_test", "Admin Export Tester", Role.ADMIN, null, null, null);
        facultyUser = getOrCreateUser("faculty_exp_test", "Faculty CSE Tester", Role.FACULTY, "CSE", null, null);
        otherFacultyUser = getOrCreateUser("faculty_ece_test", "Faculty ECE Tester", Role.FACULTY, "ECE", null, null);
        studentUser = getOrCreateUser("student_exp_test", "Student Export Tester", Role.STUDENT, "MECH", 1, "Z");

        adminToken = jwtUtil.generateToken(adminUser.getUserId(), adminUser.getCollegeId(), adminUser.getRole(), adminUser.getName());
        facultyToken = jwtUtil.generateToken(facultyUser.getUserId(), facultyUser.getCollegeId(), facultyUser.getRole(), facultyUser.getName());
        otherFacultyToken = jwtUtil.generateToken(otherFacultyUser.getUserId(), otherFacultyUser.getCollegeId(), otherFacultyUser.getRole(), otherFacultyUser.getName());
        studentToken = jwtUtil.generateToken(studentUser.getUserId(), studentUser.getCollegeId(), studentUser.getRole(), studentUser.getName());
    }

    @AfterEach
    public void cleanup() {
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
        userRepository.findByCollegeId("student_exp_test").ifPresent(userRepository::delete);
        userRepository.findByCollegeId("admin_exp_test").ifPresent(userRepository::delete);
        userRepository.findByCollegeId("faculty_exp_test").ifPresent(userRepository::delete);
        userRepository.findByCollegeId("faculty_ece_test").ifPresent(userRepository::delete);
    }

    private User getOrCreateUser(String collegeId, String name, Role role, String branch, Integer year, String section) {
        return userRepository.findByCollegeId(collegeId).orElseGet(() -> userRepository.save(User.builder()
                .collegeId(collegeId)
                .name(name)
                .email(collegeId.toLowerCase() + "@test.edu")
                .passwordHash(passwordEncoder.encode("Test@1234"))
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
        String questionSchema = "{\"questions\":[{\"key\":\"q1\",\"type\":\"RATING\",\"prompt\":\"=SUM(1,2) Injected Prompt\",\"scoring\":{\"skill\":\"java\",\"weight\":1.0}},{\"key\":\"q2\",\"type\":\"SINGLE_CHOICE\",\"prompt\":\"Preferred Language\",\"options\":[{\"value\":\"java\",\"label\":\"+Java Option\"},{\"value\":\"python\",\"label\":\"-Python Option\"}]}]}";
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

    private ObjectNode createMockAggregatePayload(int responseCount) {
        ObjectNode aggregate = objectMapper.createObjectNode();
        aggregate.put("available", true);
        aggregate.put("response_count", responseCount);
        aggregate.put("eligible_count", 20);
        aggregate.put("completion_rate", 0.30);
        aggregate.put("readiness_index", 82.5);

        // Bands
        ObjectNode bands = aggregate.putObject("bands");
        ObjectNode ready = bands.putObject("READY");
        ready.put("count", 4);
        ready.put("percentage", 0.67);
        ObjectNode dev = bands.putObject("DEVELOPING");
        dev.put("count", 2);
        dev.put("percentage", 0.33);

        // Skills
        ArrayNode skills = aggregate.putArray("skills");
        ObjectNode s1 = skills.addObject();
        s1.put("skill", "java");
        s1.put("cohort_mean", 85.0);
        s1.put("target", 70.0);
        s1.put("gap", 0.0);
        s1.put("band", "READY");
        s1.put("priority", "LOW");

        // Questions
        ArrayNode questions = aggregate.putArray("questions");
        ObjectNode q1 = questions.addObject();
        q1.put("key", "q1");
        q1.put("prompt", "=SUM(1,2) Injected Prompt");
        q1.put("type", "RATING");
        q1.put("count", 6);
        q1.put("mean", 4.25);
        q1.put("median", 4.0);

        ObjectNode dist1 = q1.putObject("distribution");
        dist1.put("4", 3);
        dist1.put("5", 3);

        // Demographic breakdown
        ObjectNode bd = aggregate.putObject("breakdown");
        ObjectNode byDep = bd.putObject("by_department");
        ObjectNode cseDep = byDep.putObject("CSE");
        cseDep.put("count", 5);
        cseDep.put("suppressed", false);
        cseDep.put("mean", 85.0);

        ObjectNode eceDep = byDep.putObject("ECE");
        eceDep.put("count", 1);
        eceDep.put("suppressed", true);
        eceDep.putNull("mean");

        return aggregate;
    }

    // 1. PDF export returns HTTP 200 for authorized official
    // 2. PDF export produces a non-empty PDF
    @Test
    public void testExportPdf_AuthorizedOfficial_Success() throws Exception {
        Survey survey = createSurvey("Placement Readiness 2026", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        when(analyticsClient.analyze(any(FastApiAnalyzeRequest.class))).thenReturn(createMockAggregatePayload(6));

        MvcResult result = mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/export?format=pdf")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF export should be non-empty and well-structured");

        // Verify PDF Magic Bytes (%PDF-)
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    // 3. XLSX export returns HTTP 200
    // 4. XLSX export produces a valid non-empty workbook
    @Test
    public void testExportXlsx_AuthorizedOfficial_Success() throws Exception {
        Survey survey = createSurvey("Placement Readiness 2026", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        when(analyticsClient.analyze(any(FastApiAnalyzeRequest.class))).thenReturn(createMockAggregatePayload(6));

        MvcResult result = mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/export?format=xlsx")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        byte[] xlsxBytes = result.getResponse().getContentAsByteArray();
        assertNotNull(xlsxBytes);
        assertTrue(xlsxBytes.length > 1000, "XLSX export should be a non-empty workbook");

        // Verify Apache POI can load it and required sheets exist
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            assertNotNull(wb.getSheet("Summary"));
            assertNotNull(wb.getSheet("Questions"));
            assertNotNull(wb.getSheet("Skills"));
            assertNotNull(wb.getSheet("Breakdown"));
            assertNotNull(wb.getSheet("Trend"));
            assertNotNull(wb.getSheet("Insights"));

            // Verify freeze panes exist
            assertTrue(wb.getSheet("Summary").getPaneInformation().isFreezePane());
        }
    }

    // 5. XLSX formula injection is escaped (=, +, -, @)
    @Test
    public void testExportXlsx_FormulaInjectionEscaped() throws Exception {
        assertEquals("'=SUM(A1:A10)", ReportExportService.escapeFormula("=SUM(A1:A10)"));
        assertEquals("'+CMD|' /C calc'!A0", ReportExportService.escapeFormula("+CMD|' /C calc'!A0"));
        assertEquals("'-2+3*[1]Sheet!A1", ReportExportService.escapeFormula("-2+3*[1]Sheet!A1"));
        assertEquals("'@SUM(1,2)", ReportExportService.escapeFormula("@SUM(1,2)"));
        assertEquals("Normal text", ReportExportService.escapeFormula("Normal text"));
        assertNull(ReportExportService.escapeFormula(null));

        Survey survey = createSurvey("Malicious =HYPERLINK Survey", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);
        when(analyticsClient.analyze(any(FastApiAnalyzeRequest.class))).thenReturn(createMockAggregatePayload(6));

        MvcResult result = mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/export?format=xlsx")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        byte[] xlsxBytes = result.getResponse().getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            XSSFSheet questionsSheet = wb.getSheet("Questions");
            assertNotNull(questionsSheet);

            // Row 1 prompt cell contains "=SUM(1,2) Injected Prompt"
            // Verify that in the workbook cell, it has been escaped with leading apostrophe
            String cellVal = questionsSheet.getRow(1).getCell(1).getStringCellValue();
            assertTrue(cellVal.startsWith("'="), "Formula injection must be escaped with apostrophe prefix: " + cellVal);
        }
    }

    // 6. Anonymous export contains no individual response rows
    // 7. Anonymous suppressed groups are not exposed
    @Test
    public void testExport_AnonymousSurvey_SuppressionEnforced() throws Exception {
        Survey anonSurvey = createSurvey("Anonymous Wellbeing", SurveyCategory.WELLBEING, true, "{\"scope\":\"ALL\"}", adminUser);
        // Survey has 0 responses < 5: K=5 privacy gate
        MvcResult pdfResult = mockMvc.perform(get("/api/surveys/" + anonSurvey.getSurveyId() + "/export?format=pdf")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 200);

        MvcResult xlsxResult = mockMvc.perform(get("/api/surveys/" + anonSurvey.getSurveyId() + "/export?format=xlsx")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxResult.getResponse().getContentAsByteArray()))) {
            XSSFSheet summarySheet = wb.getSheet("Summary");
            assertNotNull(summarySheet);
            // Verify Questions and Skills sheets were omitted or have 0 data rows because of K=5
            assertNull(wb.getSheet("Questions"), "Aggregate sheets should not be generated for anonymous survey under K=5 threshold");
        }
    }

    // 8. Student cannot export official survey reports (403 Forbidden)
    @Test
    public void testExport_StudentAccessDenied_403() throws Exception {
        Survey survey = createSurvey("Staff Placement Survey", SurveyCategory.PLACEMENT, false, "{\"scope\":\"ALL\"}", adminUser);

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/export?format=pdf")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/surveys/" + survey.getSurveyId() + "/export?format=xlsx")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    // 9. Scope authorization is enforced (Faculty out of department scope gets 403)
    @Test
    public void testExport_ScopeEnforced_OutOfScopeOfficialGets403() throws Exception {
        // Survey targeted only to CSE branch, created by CSE faculty
        Survey cseSurvey = createSurvey("CSE Technical Survey", SurveyCategory.ACADEMIC, false,
                "{\"scope\":\"FILTER\",\"branches\":[\"CSE\"]}", facultyUser);

        // ECE Faculty attempts to export CSE survey -> 403
        mockMvc.perform(get("/api/surveys/" + cseSurvey.getSurveyId() + "/export?format=pdf")
                        .header("Authorization", "Bearer " + otherFacultyToken))
                .andExpect(status().isForbidden());
    }
}
