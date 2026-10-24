package com.skillsense.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.analytics.dto.SurveyAnalyticsResponse;
import com.skillsense.analytics.service.AnalyticsService;
import com.skillsense.response.dto.SubmitResponseRequest;
import com.skillsense.response.service.ResponseService;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.entity.SurveyTemplate;
import com.skillsense.survey.repository.SurveyRepository;
import com.skillsense.survey.repository.SurveyTemplateRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Phase 11: Performance Data Seeder & Benchmark Runner
 * Seeds 2,000 responses on a dedicated placement benchmark survey.
 * Measures cold and warm analytics dashboard response times against non-functional targets (< 2s).
 * Measures submit endpoint latency (< 500ms).
 */
@Component
public class PerformanceDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PerformanceDataSeeder.class);

    private final SurveyRepository surveyRepository;
    private final SurveyTemplateRepository templateRepository;
    private final AcademicTermRepository termRepository;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final ResponseService responseService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public PerformanceDataSeeder(SurveyRepository surveyRepository,
                                 SurveyTemplateRepository templateRepository,
                                 AcademicTermRepository termRepository,
                                 UserRepository userRepository,
                                 AnalyticsService analyticsService,
                                 ResponseService responseService,
                                 JdbcTemplate jdbcTemplate,
                                 ObjectMapper objectMapper,
                                 Environment environment) {
        this.surveyRepository = surveyRepository;
        this.templateRepository = templateRepository;
        this.termRepository = termRepository;
        this.userRepository = userRepository;
        this.analyticsService = analyticsService;
        this.responseService = responseService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean isPerfProfile = Arrays.asList(environment.getActiveProfiles()).contains("perf");
        boolean hasPerfFlag = args.containsOption("seed-perf") ||
                "true".equalsIgnoreCase(environment.getProperty("app.seed.perf"));

        if (isPerfProfile || hasPerfFlag) {
            log.info("Triggering SkillSense Phase 11 Performance Data Seeder (perf profile / flag active)...");
            seedAndBenchmark();
        }
    }

    public synchronized Map<String, Object> seedAndBenchmark() {
        log.info("=== STARTING 2,000-RESPONSE PERFORMANCE SEEDING & BENCHMARK ===");
        Map<String, Object> report = new LinkedHashMap<>();

        User admin = userRepository.findByCollegeId("admin")
                .orElseThrow(() -> new IllegalStateException("Admin account must exist to seed performance survey"));

        AcademicTerm currentTerm = termRepository.findAll().stream()
                .filter(AcademicTerm::getIsCurrent)
                .findFirst()
                .orElseGet(() -> termRepository.save(new AcademicTerm(null, "2026-27 Odd", java.time.LocalDate.now().minusMonths(2), java.time.LocalDate.now().plusMonths(4), true)));

        // 1. Create or retrieve Performance Benchmark Survey
        SurveyTemplate template = templateRepository.findAll().stream()
                .filter(t -> "Performance Benchmark Placement".equals(t.getName()))
                .findFirst()
                .orElseGet(() -> templateRepository.save(SurveyTemplate.builder()
                        .name("Performance Benchmark Placement")
                        .category(SurveyCategory.PLACEMENT)
                        .createdBy(admin)
                        .build()));

        Survey perfSurvey = surveyRepository.findByTemplate_TemplateIdAndVersion(template.getTemplateId(), 1)
                .orElse(null);

        JsonNode placementPreset = loadPreset("PLACEMENT");
        Map<String, Double> targets = Map.of(
                "java", 70.0,
                "dsa", 70.0,
                "aptitude", 70.0,
                "communication", 65.0,
                "interview", 65.0
        );

        if (perfSurvey == null) {
            try {
                perfSurvey = Survey.builder()
                        .template(template)
                        .version(1)
                        .title("Placement Readiness Benchmark (2,000 Cohort)")
                        .description("Performance testing survey with 2,000 seeded student responses.")
                        .category(SurveyCategory.PLACEMENT)
                        .term(currentTerm)
                        .questionSchema(objectMapper.writeValueAsString(placementPreset))
                        .skillTargets(objectMapper.writeValueAsString(targets))
                        .isAnonymous(true)
                        .targetFilter(objectMapper.writeValueAsString(Map.of("scope", "ALL", "branches", List.of(), "years", List.of(), "sections", List.of())))
                        .status(SurveyStatus.CLOSED)
                        .eligibleCount(2500)
                        .createdBy(admin)
                        .opensAt(LocalDateTime.now().minusDays(30))
                        .closesAt(LocalDateTime.now().minusDays(1))
                        .build();

                perfSurvey = surveyRepository.save(perfSurvey);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create performance survey: " + e.getMessage(), e);
            }
        }

        Long surveyId = perfSurvey.getSurveyId();
        report.put("survey_id", surveyId);

        // 2. Clear existing responses and cache for this survey
        jdbcTemplate.update("DELETE FROM analytics_cache WHERE survey_id = ?", surveyId);
        jdbcTemplate.update("DELETE FROM responses WHERE survey_id = ?", surveyId);

        // 3. Insert 2,000 responses in batches
        log.info("Batch inserting 2,000 responses for survey ID {}...", surveyId);
        long startSeed = System.currentTimeMillis();

        List<Object[]> batch = new ArrayList<>(500);
        String sql = "INSERT INTO responses (response_id, survey_id, student_id, group_branch, group_year, group_section, answer_data, submitted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        List<String> branches = List.of("CSE", "IT", "ECE", "MECH", "CIVIL");
        List<Integer> years = List.of(2, 3, 4);
        Random rng = new Random(777);

        for (int i = 0; i < 2000; i++) {
            String respId = UUID.randomUUID().toString();
            String branch = branches.get(rng.nextInt(branches.size()));
            int year = years.get(rng.nextInt(years.size()));

            Map<String, Object> ans = new HashMap<>();
            ans.put("java_skill", 1 + rng.nextInt(5));
            ans.put("python_skill", 1 + rng.nextInt(5));
            ans.put("dsa_skill", 1 + rng.nextInt(5));
            ans.put("aptitude_skill", 1 + rng.nextInt(5));
            ans.put("communication_skill", 1 + rng.nextInt(5));
            ans.put("mock_interviews", rng.nextBoolean() ? "1-2" : (rng.nextBoolean() ? "3+" : "0"));
            ans.put("projects_completed", "2+");
            ans.put("has_certification", rng.nextBoolean());
            ans.put("target_role", "swe");
            ans.put("blockers", "Mock interview practice");

            String ansJson;
            try {
                ansJson = objectMapper.writeValueAsString(ans);
            } catch (Exception e) {
                ansJson = "{}";
            }

            // Anonymous survey: student_id = null, submitted_at = null, group_section = null
            batch.add(new Object[]{respId, surveyId, null, branch, year, null, ansJson, null});

            if (batch.size() == 500 || i == 1999) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }
        long seedDuration = System.currentTimeMillis() - startSeed;
        log.info("Inserted 2,000 responses in {} ms.", seedDuration);
        report.put("seed_duration_ms", seedDuration);
        report.put("total_responses", 2000);

        // 4. Measure Cold Analytics Time (calls FastAPI without cache)
        log.info("Measuring COLD analytics time for 2,000 responses...");
        long tColdStart = System.currentTimeMillis();
        SurveyAnalyticsResponse coldResponse = analyticsService.getSurveyAnalytics(surveyId, admin);
        long coldDurationMs = System.currentTimeMillis() - tColdStart;
        log.info("COLD analytics completed in {} ms (Available: {})", coldDurationMs, coldResponse.isAvailable());
        report.put("cold_analytics_ms", coldDurationMs);
        report.put("cold_passed_target_under_2s", coldDurationMs < 2000);

        // 5. Measure Warm Analytics Time (served from analytics_cache)
        log.info("Measuring WARM analytics time for 2,000 responses...");
        long tWarmStart = System.currentTimeMillis();
        SurveyAnalyticsResponse warmResponse = analyticsService.getSurveyAnalytics(surveyId, admin);
        long warmDurationMs = System.currentTimeMillis() - tWarmStart;
        log.info("WARM analytics completed in {} ms", warmDurationMs);
        report.put("warm_analytics_ms", warmDurationMs);
        report.put("warm_passed_target_under_2s", warmDurationMs < 2000);

        // 6. Measure Submit Endpoint Latency (< 500 ms target)
        // Find an active survey and a student with has_responded = false
        try {
            List<Map<String, Object>> unrespondedRows = jdbcTemplate.queryForList(
                    "SELECT p.survey_id, p.student_id, u.college_id FROM participation_log p " +
                            "JOIN surveys s ON p.survey_id = s.survey_id " +
                            "JOIN users u ON p.student_id = u.user_id " +
                            "WHERE s.status = 'ACTIVE' AND p.has_responded = 0 LIMIT 1"
            );

            if (!unrespondedRows.isEmpty()) {
                Map<String, Object> target = unrespondedRows.get(0);
                Long activeSurveyId = ((Number) target.get("survey_id")).longValue();
                String collegeId = (String) target.get("college_id");
                User student = userRepository.findByCollegeId(collegeId).orElse(null);

                if (student != null) {
                    Survey activeSurvey = surveyRepository.findById(activeSurveyId).orElse(null);
                    if (activeSurvey != null) {
                        Map<String, Object> ans = new HashMap<>();
                        if (activeSurvey.getCategory() == SurveyCategory.PLACEMENT) {
                            ans.put("java_skill", 4);
                            ans.put("python_skill", 4);
                            ans.put("dsa_skill", 4);
                            ans.put("aptitude_skill", 4);
                            ans.put("communication_skill", 4);
                            ans.put("mock_interviews", "1-2");
                            ans.put("projects_completed", "2+");
                            ans.put("has_certification", true);
                            ans.put("target_role", "swe");
                            ans.put("blockers", "None");
                        } else if (activeSurvey.getCategory() == SurveyCategory.ACADEMIC) {
                            ans.put("clarity_of_teaching", 4);
                            ans.put("course_pace", 4);
                            ans.put("study_material", 4);
                            ans.put("fairness_of_assessment", 4);
                            ans.put("practical_relevance", 4);
                            ans.put("overall_rating", 4);
                        } else if (activeSurvey.getCategory() == SurveyCategory.WELLBEING) {
                            ans.put("workload_manageable", 4);
                            ans.put("faculty_support", 4);
                            ans.put("sense_of_belonging", 4);
                            ans.put("knows_counselling", true);
                        }

                        long tSubmitStart = System.currentTimeMillis();
                        responseService.submitResponse(activeSurveyId, new SubmitResponseRequest(ans), student);
                        long submitDurationMs = System.currentTimeMillis() - tSubmitStart;
                        log.info("Live submit endpoint latency: {} ms", submitDurationMs);
                        report.put("submit_latency_ms", submitDurationMs);
                        report.put("submit_passed_target_under_500ms", submitDurationMs < 500);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Submit benchmark check encountered: {}", e.getMessage());
        }

        log.info("=== PERFORMANCE BENCHMARK COMPLETE ===");
        return report;
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
