package com.skillsense.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.response.dto.SubmitResponseRequest;
import com.skillsense.response.service.ResponseService;
import com.skillsense.survey.dto.CreateSurveyRequest;
import com.skillsense.survey.dto.SurveyResponse;
import com.skillsense.survey.model.TargetFilter;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.entity.SurveyTemplate;
import com.skillsense.survey.repository.SurveyRepository;
import com.skillsense.survey.repository.SurveyTemplateRepository;
import com.skillsense.survey.service.SurveyService;
import com.skillsense.term.AcademicTerm;
import com.skillsense.term.AcademicTermRepository;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Phase 11: Demo Profile Seeder
 * Populates repeatable seed data according to docs/SKILLSENSE_SPEC.md Section 12.
 * Uses a fixed random seed (Random(42)) so every run produces identical numbers.
 * Submits responses through the real ResponseService to ensure participation_log consistency.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final AcademicTermRepository termRepository;
    private final SurveyTemplateRepository templateRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyService surveyService;
    private final ResponseService responseService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public DemoDataSeeder(UserRepository userRepository,
                          AcademicTermRepository termRepository,
                          SurveyTemplateRepository templateRepository,
                          SurveyRepository surveyRepository,
                          SurveyService surveyService,
                          ResponseService responseService,
                          PasswordEncoder passwordEncoder,
                          ObjectMapper objectMapper,
                          Environment environment) {
        this.userRepository = userRepository;
        this.termRepository = termRepository;
        this.templateRepository = templateRepository;
        this.surveyRepository = surveyRepository;
        this.surveyService = surveyService;
        this.responseService = responseService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean isDemoProfile = Arrays.asList(environment.getActiveProfiles()).contains("demo");
        boolean hasSeedFlag = args.containsOption("seed-demo") ||
                "true".equalsIgnoreCase(environment.getProperty("app.seed.demo"));

        if (isDemoProfile || hasSeedFlag) {
            log.info("Triggering SkillSense Phase 11 Demo Data Seeder (demo profile / flag active)...");
            seedDemoData();
        }
    }

    @Transactional
    public synchronized Map<String, Object> seedDemoData() {
        log.info("=== STARTING SKILLSENSE DEMO PROFILE SEEDER ===");
        Map<String, Object> summary = new LinkedHashMap<>();

        // 1. Create Demo Accounts
        Map<String, User> seededUsers = seedAccounts();
        summary.put("users_count", seededUsers.size());

        // 2. Create Academic Terms
        Map<String, AcademicTerm> seededTerms = seedTerms();
        summary.put("terms", seededTerms.keySet());

        // 3. Seed 6 Surveys and their responses
        Map<String, Long> surveysCreated = seedSurveys(seededUsers, seededTerms);
        summary.put("surveys", surveysCreated);

        log.info("=== SKILLSENSE DEMO PROFILE SEEDER COMPLETED SUCCESSFULLY ===");
        return summary;
    }

    private Map<String, User> seedAccounts() {
        log.info("Seeding Section 12 demo accounts...");
        String defaultHash = passwordEncoder.encode("Demo@1234");
        Map<String, User> users = new HashMap<>();

        // Officials
        users.put("admin", createOrUpdateUser("admin", "System Administrator", "admin@skillsense.edu", Role.ADMIN, "IT", null, null, defaultHash));
        users.put("po01", createOrUpdateUser("po01", "Placement Officer Sharma", "po01@skillsense.edu", Role.PLACEMENT_OFFICER, "TPO", null, null, defaultHash));
        users.put("hod_cse", createOrUpdateUser("hod_cse", "Dr. Ramesh (HOD CSE)", "hod_cse@skillsense.edu", Role.HOD, "CSE", null, null, defaultHash));
        users.put("fac01", createOrUpdateUser("fac01", "Prof. Ananya Rao", "fac01@skillsense.edu", Role.FACULTY, "CSE", null, null, defaultHash));
        users.put("fac02", createOrUpdateUser("fac02", "Prof. Vikram Patel", "fac02@skillsense.edu", Role.FACULTY, "IT", null, null, defaultHash));

        // 120 Students: 60 CSE, 30 IT, 30 ECE
        // 21CSE001 to 21CSE060
        for (int i = 1; i <= 60; i++) {
            String cid = String.format("21CSE%03d", i);
            int year = (i % 3) + 2; // years 2, 3, 4
            String section = (i % 2 == 0) ? "A" : "B";
            users.put(cid, createOrUpdateUser(cid, "CSE Student " + i, cid.toLowerCase() + "@skillsense.edu", Role.STUDENT, "CSE", year, section, defaultHash));
        }

        // 21IT001 to 21IT030
        for (int i = 1; i <= 30; i++) {
            String cid = String.format("21IT%03d", i);
            int year = (i % 3) + 2;
            String section = (i % 2 == 0) ? "A" : "B";
            users.put(cid, createOrUpdateUser(cid, "IT Student " + i, cid.toLowerCase() + "@skillsense.edu", Role.STUDENT, "IT", year, section, defaultHash));
        }

        // 21ECE001 to 21ECE030
        for (int i = 1; i <= 30; i++) {
            String cid = String.format("21ECE%03d", i);
            int year = (i % 3) + 2;
            String section = (i % 2 == 0) ? "A" : "B";
            users.put(cid, createOrUpdateUser(cid, "ECE Student " + i, cid.toLowerCase() + "@skillsense.edu", Role.STUDENT, "ECE", year, section, defaultHash));
        }

        log.info("Created {} demo users successfully.", users.size());
        return users;
    }

    private User createOrUpdateUser(String collegeId, String name, String email, Role role, String branch, Integer year, String section, String hash) {
        Optional<User> existing = userRepository.findByCollegeId(collegeId);
        User user;
        if (existing.isPresent()) {
            user = existing.get();
            user.setName(name);
            user.setEmail(email);
            user.setRole(role);
            user.setBranch(branch);
            user.setStudyYear(year);
            user.setSection(section);
            user.setPasswordHash(hash);
            user.setIsActive(true);
            user.setMustChangePassword(false);
        } else {
            user = User.builder()
                    .collegeId(collegeId)
                    .name(name)
                    .email(email)
                    .role(role)
                    .branch(branch)
                    .studyYear(year)
                    .section(section)
                    .passwordHash(hash)
                    .isActive(true)
                    .mustChangePassword(false)
                    .build();
        }
        return userRepository.save(user);
    }

    private Map<String, AcademicTerm> seedTerms() {
        log.info("Seeding Section 12 academic terms...");
        Map<String, AcademicTerm> terms = new HashMap<>();

        // 2025-26 Odd (previous term)
        AcademicTerm term1 = termRepository.findByLabel("2025-26 Odd").orElseGet(() ->
                termRepository.save(new AcademicTerm(null, "2025-26 Odd", LocalDate.of(2025, 7, 1), LocalDate.of(2025, 12, 31), false))
        );
        terms.put("2025-26 Odd", term1);

        // 2026-27 Odd (current term)
        AcademicTerm term2 = termRepository.findByLabel("2026-27 Odd").orElseGet(() ->
                termRepository.save(new AcademicTerm(null, "2026-27 Odd", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), true))
        );
        // Ensure 2026-27 Odd is marked as current
        term2.setIsCurrent(true);
        termRepository.save(term2);
        terms.put("2026-27 Odd", term2);

        return terms;
    }

    private Map<String, Long> seedSurveys(Map<String, User> users, Map<String, AcademicTerm> terms) {
        log.info("Seeding Section 12 6 surveys and repeatable response distributions...");
        Map<String, Long> results = new LinkedHashMap<>();

        User po = users.get("po01");
        User hodCse = users.get("hod_cse");
        User fac01 = users.get("fac01");
        User admin = users.get("admin");

        AcademicTerm prevTerm = terms.get("2025-26 Odd");
        AcademicTerm currTerm = terms.get("2026-27 Odd");

        JsonNode placementPreset = loadPreset("PLACEMENT");
        JsonNode academicPreset = loadPreset("ACADEMIC");
        JsonNode facilityPreset = loadPreset("FACILITY");
        JsonNode wellbeingPreset = loadPreset("WELLBEING");
        JsonNode eventPreset = loadPreset("EVENT");

        Map<String, Double> placementTargets = Map.of(
                "java", 70.0,
                "dsa", 70.0,
                "aptitude", 70.0,
                "communication", 65.0,
                "interview", 65.0
        );

        // =========================================================================
        // SURVEY 1: Placement Readiness, 2025-26 Odd (v1)
        // Status: CLOSED, ~85 responses, non-anonymous
        // Story: DSA 45, Java 62, Aptitude 58, Communication 55, Interview 40
        // =========================================================================
        log.info("Seeding Survey 1: Placement Readiness v1 (CLOSED, ~85 responses)...");
        SurveyTemplate placementTemplate = templateRepository.findAll().stream()
                .filter(t -> "Placement Readiness".equals(t.getName()))
                .findFirst()
                .orElseGet(() -> templateRepository.save(SurveyTemplate.builder()
                        .name("Placement Readiness")
                        .category(SurveyCategory.PLACEMENT)
                        .createdBy(po)
                        .build()));

        Survey survey1 = surveyRepository.findByTemplate_TemplateIdAndVersion(placementTemplate.getTemplateId(), 1)
                .orElse(null);

        if (survey1 == null) {
            survey1 = createSurveyRecord(
                    placementTemplate,
                    1,
                    "Placement Readiness, 2025-26 Odd (v1)",
                    "Comprehensive assessment of technical skills, DSA problem solving, and aptitude for 2025-26 cohort.",
                    SurveyCategory.PLACEMENT,
                    prevTerm,
                    placementPreset,
                    placementTargets,
                    false,
                    po
            );

            // Activate so participation log is populated
            surveyService.activateSurvey(survey1.getSurveyId(), po);

            // Submit 85 responses using Fixed Random (Seed 101)
            Random rngV1 = new Random(101);
            List<User> students = getStudentList(users);
            for (int i = 0; i < 85 && i < students.size(); i++) {
                User s = students.get(i);
                Map<String, Object> answers = generatePlacementAnswersV1(rngV1);
                try {
                    responseService.submitResponse(survey1.getSurveyId(), new SubmitResponseRequest(answers), s);
                } catch (Exception e) {
                    log.debug("Submit response error v1 {}: {}", s.getCollegeId(), e.getMessage());
                }
            }

            // Close survey 1
            survey1 = surveyRepository.findById(survey1.getSurveyId()).orElseThrow();
            survey1.setStatus(SurveyStatus.CLOSED);
            survey1.setClosesAt(LocalDateTime.now().minusDays(120));
            surveyRepository.save(survey1);
        }
        results.put("placement_v1", survey1.getSurveyId());

        // =========================================================================
        // SURVEY 2: Placement Readiness, 2026-27 Odd (v2)
        // Status: ACTIVE, ~70 responses, non-anonymous
        // Story: DSA 55 (up), Java 64 (flat), Aptitude 57 (flat), Communication 52 (down), Interview 52 (up)
        // =========================================================================
        log.info("Seeding Survey 2: Placement Readiness v2 (ACTIVE, ~70 responses)...");
        Survey survey2 = surveyRepository.findByTemplate_TemplateIdAndVersion(placementTemplate.getTemplateId(), 2)
                .orElse(null);

        if (survey2 == null) {
            survey2 = createSurveyRecord(
                    placementTemplate,
                    2,
                    "Placement Readiness, 2026-27 Odd (v2)",
                    "Year-over-year technical readiness evaluation and benchmark against targets.",
                    SurveyCategory.PLACEMENT,
                    currTerm,
                    placementPreset,
                    placementTargets,
                    false,
                    po
            );

            surveyService.activateSurvey(survey2.getSurveyId(), po);

            Random rngV2 = new Random(202);
            List<User> students = getStudentList(users);
            for (int i = 0; i < 70 && i < students.size(); i++) {
                User s = students.get(i);
                Map<String, Object> answers = generatePlacementAnswersV2(rngV2);
                try {
                    responseService.submitResponse(survey2.getSurveyId(), new SubmitResponseRequest(answers), s);
                } catch (Exception e) {
                    log.debug("Submit response error v2 {}: {}", s.getCollegeId(), e.getMessage());
                }
            }
        }
        results.put("placement_v2", survey2.getSurveyId());

        // =========================================================================
        // SURVEY 3: Course Feedback: DBMS
        // Status: ACTIVE, ~60 responses, Anonymous (true)
        // IT branch has EXACTLY 3 responses so it triggers "Hidden: fewer than 5 responses"
        // =========================================================================
        log.info("Seeding Survey 3: Course Feedback: DBMS (ACTIVE, Anonymous, IT n=3)...");
        SurveyTemplate dbmsTemplate = templateRepository.findAll().stream()
                .filter(t -> "Course Feedback: DBMS".equals(t.getName()))
                .findFirst()
                .orElseGet(() -> templateRepository.save(SurveyTemplate.builder()
                        .name("Course Feedback: DBMS")
                        .category(SurveyCategory.ACADEMIC)
                        .createdBy(fac01)
                        .build()));

        Survey survey3 = surveyRepository.findByTemplate_TemplateIdAndVersion(dbmsTemplate.getTemplateId(), 1)
                .orElse(null);

        if (survey3 == null) {
            survey3 = createSurveyRecord(
                    dbmsTemplate,
                    1,
                    "Course Feedback: DBMS",
                    "End-semester evaluation of Database Management Systems theory and lab.",
                    SurveyCategory.ACADEMIC,
                    currTerm,
                    academicPreset,
                    null,
                    true,
                    fac01
            );

            surveyService.activateSurvey(survey3.getSurveyId(), fac01);

            Random rngDbms = new Random(303);
            // 50 CSE students
            for (int i = 1; i <= 50; i++) {
                User s = users.get(String.format("21CSE%03d", i));
                if (s != null) {
                    responseService.submitResponse(survey3.getSurveyId(), new SubmitResponseRequest(generateAcademicAnswers(rngDbms)), s);
                }
            }
            // EXACTLY 3 IT students (Demonstrating K=5 suppression notice)
            for (int i = 1; i <= 3; i++) {
                User s = users.get(String.format("21IT%03d", i));
                if (s != null) {
                    responseService.submitResponse(survey3.getSurveyId(), new SubmitResponseRequest(generateAcademicAnswers(rngDbms)), s);
                }
            }
            // 7 ECE students
            for (int i = 1; i <= 7; i++) {
                User s = users.get(String.format("21ECE%03d", i));
                if (s != null) {
                    responseService.submitResponse(survey3.getSurveyId(), new SubmitResponseRequest(generateAcademicAnswers(rngDbms)), s);
                }
            }
        }
        results.put("course_feedback_dbms", survey3.getSurveyId());

        // =========================================================================
        // SURVEY 4: Hostel and Facility Satisfaction
        // Status: CLOSED, ~90 responses, Anonymous (true)
        // =========================================================================
        log.info("Seeding Survey 4: Hostel and Facility Satisfaction (CLOSED, Anonymous, ~90 responses)...");
        SurveyTemplate facilityTemplate = templateRepository.findAll().stream()
                .filter(t -> "Hostel and Facility Satisfaction".equals(t.getName()))
                .findFirst()
                .orElseGet(() -> templateRepository.save(SurveyTemplate.builder()
                        .name("Hostel and Facility Satisfaction")
                        .category(SurveyCategory.FACILITY)
                        .createdBy(admin)
                        .build()));

        Survey survey4 = surveyRepository.findByTemplate_TemplateIdAndVersion(facilityTemplate.getTemplateId(), 1)
                .orElse(null);

        if (survey4 == null) {
            survey4 = createSurveyRecord(
                    facilityTemplate,
                    1,
                    "Hostel and Facility Satisfaction",
                    "Comprehensive student audit of hostel living conditions, dining, Wi-Fi, and labs.",
                    SurveyCategory.FACILITY,
                    currTerm,
                    facilityPreset,
                    null,
                    true,
                    admin
            );

            surveyService.activateSurvey(survey4.getSurveyId(), admin);

            Random rngFac = new Random(404);
            List<User> students = getStudentList(users);
            for (int i = 0; i < 90 && i < students.size(); i++) {
                User s = students.get(i);
                try {
                    responseService.submitResponse(survey4.getSurveyId(), new SubmitResponseRequest(generateFacilityAnswers(rngFac)), s);
                } catch (Exception e) {
                    log.debug("Submit facility error {}: {}", s.getCollegeId(), e.getMessage());
                }
            }

            survey4 = surveyRepository.findById(survey4.getSurveyId()).orElseThrow();
            survey4.setStatus(SurveyStatus.CLOSED);
            survey4.setClosesAt(LocalDateTime.now().minusDays(10));
            surveyRepository.save(survey4);
        }
        results.put("facility_satisfaction", survey4.getSurveyId());

        // =========================================================================
        // SURVEY 5: Wellbeing Pulse
        // Status: ACTIVE, EXACTLY 4 responses, Anonymous (true, forced)
        // Demonstrates the "4 of 5 responses" waiting banner
        // =========================================================================
        log.info("Seeding Survey 5: Wellbeing Pulse (ACTIVE, Anonymous, EXACTLY 4 responses for waiting banner)...");
        SurveyTemplate wellbeingTemplate = templateRepository.findAll().stream()
                .filter(t -> "Wellbeing Pulse".equals(t.getName()))
                .findFirst()
                .orElseGet(() -> templateRepository.save(SurveyTemplate.builder()
                        .name("Wellbeing Pulse")
                        .category(SurveyCategory.WELLBEING)
                        .createdBy(admin)
                        .build()));

        Survey survey5 = surveyRepository.findByTemplate_TemplateIdAndVersion(wellbeingTemplate.getTemplateId(), 1)
                .orElse(null);

        if (survey5 == null) {
            survey5 = createSurveyRecord(
                    wellbeingTemplate,
                    1,
                    "Wellbeing Pulse",
                    "Confidential mental health and academic workload assessment.",
                    SurveyCategory.WELLBEING,
                    currTerm,
                    wellbeingPreset,
                    null,
                    true,
                    admin
            );

            surveyService.activateSurvey(survey5.getSurveyId(), admin);

            Random rngWb = new Random(505);
            // Exactly 4 students submit
            for (int i = 1; i <= 4; i++) {
                User s = users.get(String.format("21CSE%03d", i));
                if (s != null) {
                    responseService.submitResponse(survey5.getSurveyId(), new SubmitResponseRequest(generateWellbeingAnswers(rngWb)), s);
                }
            }
        }
        results.put("wellbeing_pulse", survey5.getSurveyId());

        // =========================================================================
        // SURVEY 6: Tech Fest Feedback
        // Status: DRAFT, 0 responses, Anonymous (true)
        // Ready for live activation in front of evaluators
        // =========================================================================
        log.info("Seeding Survey 6: Tech Fest Feedback (DRAFT, 0 responses)...");
        SurveyTemplate eventTemplate = templateRepository.findAll().stream()
                .filter(t -> "Tech Fest Feedback".equals(t.getName()))
                .findFirst()
                .orElseGet(() -> templateRepository.save(SurveyTemplate.builder()
                        .name("Tech Fest Feedback")
                        .category(SurveyCategory.EVENT)
                        .createdBy(fac01)
                        .build()));

        Survey survey6 = surveyRepository.findByTemplate_TemplateIdAndVersion(eventTemplate.getTemplateId(), 1)
                .orElse(null);

        if (survey6 == null) {
            survey6 = createSurveyRecord(
                    eventTemplate,
                    1,
                    "Tech Fest Feedback",
                    "Post-event feedback for annual tech symposium and project exhibitions.",
                    SurveyCategory.EVENT,
                    currTerm,
                    eventPreset,
                    null,
                    true,
                    fac01
            );
            // Remains in DRAFT status with 0 responses
        }
        results.put("tech_fest_feedback", survey6.getSurveyId());

        return results;
    }

    private Survey createSurveyRecord(SurveyTemplate template,
                                      int version,
                                      String title,
                                      String desc,
                                      SurveyCategory category,
                                      AcademicTerm term,
                                      JsonNode questionPreset,
                                      Map<String, Double> targets,
                                      boolean isAnon,
                                      User creator) {
        try {
            String schemaJson = objectMapper.writeValueAsString(questionPreset);
            String targetsJson = targets != null ? objectMapper.writeValueAsString(targets) : null;
            String filterJson = objectMapper.writeValueAsString(new TargetFilter("ALL", List.of(), List.of(), List.of()));

            Survey s = Survey.builder()
                    .template(template)
                    .version(version)
                    .title(title)
                    .description(desc)
                    .category(category)
                    .term(term)
                    .questionSchema(schemaJson)
                    .skillTargets(targetsJson)
                    .isAnonymous(isAnon)
                    .targetFilter(filterJson)
                    .status(SurveyStatus.DRAFT)
                    .eligibleCount(0)
                    .createdBy(creator)
                    .opensAt(LocalDateTime.now().minusDays(1))
                    .build();

            return surveyRepository.save(s);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create survey record: " + e.getMessage(), e);
        }
    }

    private List<User> getStudentList(Map<String, User> users) {
        List<User> list = new ArrayList<>();
        for (User u : users.values()) {
            if (u.getRole() == Role.STUDENT) {
                list.add(u);
            }
        }
        list.sort(Comparator.comparing(User::getCollegeId));
        return list;
    }

    // Story v1: DSA 45, Java 62, Aptitude 58, Communication 55, Interview 40
    private Map<String, Object> generatePlacementAnswersV1(Random rng) {
        Map<String, Object> ans = new HashMap<>();
        ans.put("java_skill", pickRating(rng, 62.0));
        ans.put("python_skill", pickRating(rng, 58.0));
        ans.put("dsa_skill", pickRating(rng, 45.0));
        ans.put("aptitude_skill", pickRating(rng, 58.0));
        ans.put("communication_skill", pickRating(rng, 55.0));
        // Interview ~40 (0: 0, 1-2: 50, 3+: 100)
        double r = rng.nextDouble();
        if (r < 0.35) ans.put("mock_interviews", "0");
        else if (r < 0.85) ans.put("mock_interviews", "1-2");
        else ans.put("mock_interviews", "3+");

        ans.put("projects_completed", rng.nextBoolean() ? "2+" : "1");
        ans.put("has_certification", rng.nextDouble() > 0.4);
        ans.put("target_role", rng.nextBoolean() ? "swe" : "cloud");
        ans.put("blockers", rng.nextBoolean() ? "DSA graph algorithms and dynamic programming" : "Balancing exam prep with coding practice");
        return ans;
    }

    // Story v2: DSA 55 (up), Java 64 (flat), Aptitude 57 (flat), Communication 52 (down), Interview 52 (up)
    private Map<String, Object> generatePlacementAnswersV2(Random rng) {
        Map<String, Object> ans = new HashMap<>();
        ans.put("java_skill", pickRating(rng, 64.0));
        ans.put("python_skill", pickRating(rng, 60.0));
        ans.put("dsa_skill", pickRating(rng, 55.0));
        ans.put("aptitude_skill", pickRating(rng, 57.0));
        ans.put("communication_skill", pickRating(rng, 52.0));
        // Interview ~52 (0: 0, 1-2: 50, 3+: 100)
        double r = rng.nextDouble();
        if (r < 0.20) ans.put("mock_interviews", "0");
        else if (r < 0.75) ans.put("mock_interviews", "1-2");
        else ans.put("mock_interviews", "3+");

        ans.put("projects_completed", rng.nextDouble() > 0.3 ? "2+" : "1");
        ans.put("has_certification", rng.nextDouble() > 0.3);
        ans.put("target_role", rng.nextBoolean() ? "swe" : "data");
        ans.put("blockers", rng.nextBoolean() ? "System design and mock interview confidence" : "Resume shortlisting and technical assessments");
        return ans;
    }

    private int pickRating(Random rng, double targetNormalizedMean) {
        // target rating = 1.0 + (targetNormalizedMean / 25.0)
        double targetRating = 1.0 + (targetNormalizedMean / 25.0);
        // add Gaussian noise around targetRating
        double val = targetRating + (rng.nextGaussian() * 0.6);
        int clamped = (int) Math.round(val);
        if (clamped < 1) clamped = 1;
        if (clamped > 5) clamped = 5;
        return clamped;
    }

    private Map<String, Object> generateAcademicAnswers(Random rng) {
        Map<String, Object> ans = new HashMap<>();
        ans.put("clarity_of_teaching", pickRating(rng, 70.0));
        ans.put("course_pace", pickRating(rng, 68.0));
        ans.put("study_material", pickRating(rng, 65.0));
        ans.put("fairness_of_assessment", pickRating(rng, 72.0));
        ans.put("practical_relevance", pickRating(rng, 75.0));
        ans.put("overall_rating", pickRating(rng, 73.0));
        ans.put("academic_suggestions", rng.nextBoolean() ? "Provide practical SQL query tuning case studies and lab assignments." : "LMS slides are clear and helpful.");
        return ans;
    }

    private Map<String, Object> generateFacilityAnswers(Random rng) {
        Map<String, Object> ans = new HashMap<>();
        ans.put("hostel_rooms", pickRating(rng, 55.0));
        ans.put("mess_food", pickRating(rng, 48.0));
        ans.put("wifi_connectivity", pickRating(rng, 52.0));
        ans.put("library_resources", pickRating(rng, 78.0));
        ans.put("lab_equipment", pickRating(rng, 74.0));
        ans.put("transport_services", pickRating(rng, 62.0));
        ans.put("sports_facilities", pickRating(rng, 60.0));
        ans.put("campus_cleanliness", pickRating(rng, 70.0));
        ans.put("priority_fix", rng.nextBoolean() ? "mess" : "wifi");
        ans.put("facility_comments", "Hostel Wi-Fi bandwidth drops during peak evening hours.");
        return ans;
    }

    private Map<String, Object> generateWellbeingAnswers(Random rng) {
        Map<String, Object> ans = new HashMap<>();
        ans.put("workload_manageable", pickRating(rng, 60.0));
        ans.put("faculty_support", pickRating(rng, 75.0));
        ans.put("sense_of_belonging", pickRating(rng, 70.0));
        ans.put("knows_counselling", true);
        ans.put("wellbeing_suggestions", "Organize peer mentorship sessions before semester end assessments.");
        return ans;
    }

    private JsonNode loadPreset(String category) {
        try {
            ClassPathResource res = new ClassPathResource("presets/" + category + ".json");
            try (InputStream in = res.getInputStream()) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return objectMapper.readTree(content);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load preset for " + category + ": " + e.getMessage(), e);
        }
    }
}
