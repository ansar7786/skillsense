package com.skillsense.analytics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.analytics.client.AnalyticsClient;
import com.skillsense.analytics.dto.DashboardSummaryResponse;
import com.skillsense.analytics.dto.InsightResponse;
import com.skillsense.analytics.dto.SurveyAnalyticsResponse;
import com.skillsense.analytics.dto.fastapi.FastApiAnalyzeRequest;
import com.skillsense.analytics.dto.fastapi.FastApiInsightsRequest;
import com.skillsense.analytics.dto.fastapi.FastApiResponseRow;
import com.skillsense.analytics.dto.fastapi.FastApiSurveyMeta;
import com.skillsense.analytics.dto.fastapi.FastApiTrendRequest;
import com.skillsense.analytics.entity.AiUsage;
import com.skillsense.analytics.entity.AnalyticsCache;
import com.skillsense.analytics.entity.AnalyticsCacheId;
import com.skillsense.analytics.entity.AnalyticsCacheKind;
import com.skillsense.analytics.repository.AiUsageRepository;
import com.skillsense.analytics.repository.AnalyticsCacheRepository;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.response.entity.Response;
import com.skillsense.survey.repository.ParticipationLogRepository;
import com.skillsense.response.repository.ResponseRepository;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.model.TargetFilter;
import com.skillsense.survey.repository.SurveyRepository;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final SurveyRepository surveyRepository;
    private final ResponseRepository responseRepository;
    private final ParticipationLogRepository participationLogRepository;
    private final AnalyticsCacheRepository analyticsCacheRepository;
    private final AiUsageRepository aiUsageRepository;
    private final AnalyticsClient analyticsClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.daily-limit:50}")
    private int aiDailyLimit;

    public AnalyticsService(
            SurveyRepository surveyRepository,
            ResponseRepository responseRepository,
            ParticipationLogRepository participationLogRepository,
            AnalyticsCacheRepository analyticsCacheRepository,
            AiUsageRepository aiUsageRepository,
            AnalyticsClient analyticsClient,
            ObjectMapper objectMapper
    ) {
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
        this.participationLogRepository = participationLogRepository;
        this.analyticsCacheRepository = analyticsCacheRepository;
        this.aiUsageRepository = aiUsageRepository;
        this.analyticsClient = analyticsClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SurveyAnalyticsResponse getSurveyAnalytics(Long surveyId, User user) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertCanViewAnalytics(survey, user);

        long liveCount = responseRepository.countBySurvey_SurveyId(surveyId);

        // Section 5 Privacy Gate: If anonymous and < 5 responses, analytics is unavailable
        if (Boolean.TRUE.equals(survey.getIsAnonymous()) && liveCount < 5) {
            return new SurveyAnalyticsResponse(false, liveCount, 5, Collections.emptyMap());
        }

        // Check cache
        Optional<AnalyticsCache> cachedOpt = analyticsCacheRepository.findById_SurveyIdAndId_Kind(
                surveyId, AnalyticsCacheKind.AGGREGATE);

        if (cachedOpt.isPresent() && cachedOpt.get().getResponseCount() == (int) liveCount) {
            try {
                Object payloadObj = objectMapper.readValue(cachedOpt.get().getPayload(), Object.class);
                return new SurveyAnalyticsResponse(true, liveCount, 5, payloadObj);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached analytics for survey {}, recalculating", surveyId);
            }
        }

        // Recalculate through FastAPI
        JsonNode computedNode = computeAndCacheAggregate(survey, liveCount);
        return new SurveyAnalyticsResponse(true, liveCount, 5, computedNode);
    }

    @Transactional
    public Object getSurveyTrend(Long surveyId, User user) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertCanViewAnalytics(survey, user);

        if (survey.getVersion() == null || survey.getVersion() <= 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND,
                    "No previous version exists for this survey template");
        }

        Survey previousSurvey = surveyRepository.findByTemplate_TemplateIdAndVersion(
                survey.getTemplate().getTemplateId(), survey.getVersion() - 1)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND,
                        "Previous version of survey not found"));

        long liveCount = responseRepository.countBySurvey_SurveyId(surveyId);

        // Check cached trend
        Optional<AnalyticsCache> cachedOpt = analyticsCacheRepository.findById_SurveyIdAndId_Kind(
                surveyId, AnalyticsCacheKind.TREND);

        if (cachedOpt.isPresent() && cachedOpt.get().getResponseCount() == (int) liveCount) {
            try {
                return objectMapper.readValue(cachedOpt.get().getPayload(), Object.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached trend for survey {}, recalculating", surveyId);
            }
        }

        // Get current aggregate payload
        JsonNode currentAggregate = getOrComputeAggregate(survey, liveCount);

        // Get previous aggregate payload
        long prevLiveCount = responseRepository.countBySurvey_SurveyId(previousSurvey.getSurveyId());
        JsonNode prevAggregate = getOrComputeAggregate(previousSurvey, prevLiveCount);

        // Call FastAPI /trend
        FastApiTrendRequest trendReq = new FastApiTrendRequest(currentAggregate, prevAggregate);
        JsonNode trendNode = analyticsClient.trend(trendReq);

        // Cache trend payload
        try {
            AnalyticsCache cache = new AnalyticsCache(
                    new AnalyticsCacheId(surveyId, AnalyticsCacheKind.TREND),
                    (int) liveCount,
                    objectMapper.writeValueAsString(trendNode),
                    LocalDateTime.now()
            );
            analyticsCacheRepository.save(cache);
        } catch (Exception e) {
            log.error("Failed to cache trend for survey {}", surveyId, e);
        }

        return trendNode;
    }

    @Transactional
    public InsightResponse getSurveyInsights(Long surveyId, boolean refresh, User user) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertCanViewAnalytics(survey, user);

        long liveCount = responseRepository.countBySurvey_SurveyId(surveyId);

        Optional<AnalyticsCache> cachedOpt = analyticsCacheRepository.findById_SurveyIdAndId_Kind(
                surveyId, AnalyticsCacheKind.INSIGHT);

        if (cachedOpt.isPresent()) {
            AnalyticsCache cached = cachedOpt.get();
            int cachedCount = cached.getResponseCount();
            LocalDateTime computedAt = cached.getComputedAt();

            if (!refresh) {
                // If survey is CLOSED, cached insight remains valid
                if (survey.getStatus() == SurveyStatus.CLOSED) {
                    return deserializeInsight(cached.getPayload(), computedAt);
                }

                // Reuse until count changes by at least 10%
                boolean countChangedBy10Pct;
                if (cachedCount == 0) {
                    countChangedBy10Pct = liveCount > 0;
                } else {
                    countChangedBy10Pct = (Math.abs(liveCount - cachedCount) / (double) cachedCount) >= 0.10;
                }

                if (!countChangedBy10Pct) {
                    return deserializeInsight(cached.getPayload(), computedAt);
                }
            } else {
                // Manual Regenerate throttle: 1 call per survey per 10 minutes
                if (computedAt != null && computedAt.plusMinutes(10).isAfter(LocalDateTime.now())) {
                    log.info("Regenerate throttled for survey {}: reuse cached insight within 10-minute window", surveyId);
                    return deserializeInsight(cached.getPayload(), computedAt);
                }
            }
        }

        // Compute new insights
        JsonNode currentAggregate = getOrComputeAggregate(survey, liveCount);
        JsonNode trendNode = null;
        if (survey.getVersion() != null && survey.getVersion() > 1) {
            try {
                trendNode = (JsonNode) getSurveyTrend(surveyId, user);
            } catch (Exception e) {
                log.debug("Trend not available for insights on survey {}", surveyId);
            }
        }

        // Daily AI quota enforcement in Spring Boot
        Optional<AiUsage> usageOpt = aiUsageRepository.findByUsageDate(LocalDate.now());
        int callsToday = usageOpt.map(AiUsage::getCalls).orElse(0);
        boolean allowAi = callsToday < aiDailyLimit;

        FastApiInsightsRequest req = new FastApiInsightsRequest(
                currentAggregate,
                trendNode,
                survey.getCategory().name(),
                allowAi
        );

        InsightResponse response = analyticsClient.insights(req);

        // Only REAL successful Gemini call (source == AI) increments quota
        if (response != null && "AI".equalsIgnoreCase(response.getSource())) {
            aiUsageRepository.incrementCalls(LocalDate.now());
        }

        LocalDateTime now = LocalDateTime.now();
        if (response != null) {
            response.setGeneratedAt(now.toString());
            try {
                AnalyticsCache cache = new AnalyticsCache(
                        new AnalyticsCacheId(surveyId, AnalyticsCacheKind.INSIGHT),
                        (int) liveCount,
                        objectMapper.writeValueAsString(response),
                        now
                );
                analyticsCacheRepository.save(cache);
            } catch (Exception e) {
                log.error("Failed to cache insight for survey {}", surveyId, e);
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<String> getSurveyTexts(Long surveyId, String key, User user) {
        Survey survey = findSurveyOrThrow(surveyId);
        assertCanViewAnalytics(survey, user);

        long liveCount = responseRepository.countBySurvey_SurveyId(surveyId);

        // Privacy Gate: If anonymous and < 5 responses, text answers are completely hidden
        if (Boolean.TRUE.equals(survey.getIsAnonymous()) && liveCount < 5) {
            return Collections.emptyList();
        }

        if (key == null || key.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Response> responses = responseRepository.findBySurvey_SurveyId(surveyId);
        List<String> textAnswers = new ArrayList<>();

        for (Response r : responses) {
            try {
                Map<String, Object> answers = objectMapper.readValue(
                        r.getAnswerData(), new TypeReference<Map<String, Object>>() {});
                Object val = answers.get(key);
                if (val != null) {
                    String str = val.toString().trim();
                    if (!str.isEmpty()) {
                        textAnswers.add(str);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Section 5 rule: If fewer than 5 respondents answered that question, hide and return empty list
        if (textAnswers.size() < 5) {
            return Collections.emptyList();
        }

        // Shuffled randomly so order cannot be correlated with submission time
        Collections.shuffle(textAnswers);
        return textAnswers;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(User user) {
        if (user.getRole() == Role.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                    "Students cannot access official dashboard summary");
        }

        List<Survey> all = surveyRepository.findAll();
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("DRAFT", 0L);
        statusCounts.put("ACTIVE", 0L);
        statusCounts.put("CLOSED", 0L);

        double totalRateSum = 0.0;
        int rateSurveyCount = 0;

        List<Survey> visibleSurveys = new ArrayList<>();
        for (Survey s : all) {
            if (canViewAnalytics(s, user)) {
                visibleSurveys.add(s);
                String st = s.getStatus() != null ? s.getStatus().name() : "DRAFT";
                statusCounts.put(st, statusCounts.getOrDefault(st, 0L) + 1L);

                int eligible = s.getEligibleCount() != null ? s.getEligibleCount() : 0;
                if (eligible > 0) {
                    long responded = participationLogRepository.countRespondedBySurveyId(s.getSurveyId());
                    double rate = (double) responded / eligible;
                    totalRateSum += rate;
                    rateSurveyCount++;
                }
            }
        }

        double avgRate = rateSurveyCount > 0 ? (totalRateSum / rateSurveyCount) : 0.0;

        // Latest insight headlines
        List<DashboardSummaryResponse.DashboardInsightSummary> latestInsights = new ArrayList<>();
        for (Survey s : visibleSurveys) {
            Optional<AnalyticsCache> opt = analyticsCacheRepository.findById_SurveyIdAndId_Kind(
                    s.getSurveyId(), AnalyticsCacheKind.INSIGHT);
            if (opt.isPresent()) {
                try {
                    InsightResponse ir = objectMapper.readValue(opt.get().getPayload(), InsightResponse.class);
                    latestInsights.add(new DashboardSummaryResponse.DashboardInsightSummary(
                            s.getSurveyId(),
                            s.getTitle(),
                            ir.getHeadline(),
                            ir.getSource(),
                            opt.get().getComputedAt() != null ? opt.get().getComputedAt().toString() : null
                    ));
                } catch (Exception ignored) {
                }
            }
        }

        // Sort latest insights by computedAt desc, limit to 5
        latestInsights.sort(Comparator.comparing(
                DashboardSummaryResponse.DashboardInsightSummary::getComputedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        if (latestInsights.size() > 5) {
            latestInsights = latestInsights.subList(0, 5);
        }

        return new DashboardSummaryResponse(statusCounts, Math.round(avgRate * 1000.0) / 1000.0, latestInsights);
    }

    public void assertCanViewAnalytics(Survey survey, User user) {
        if (!canViewAnalytics(survey, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                    "Access denied for this survey's analytics");
        }
    }

    public boolean canViewAnalytics(Survey survey, User user) {
        if (user == null) return false;
        Role role = user.getRole();
        if (role == Role.STUDENT) return false;
        if (role == Role.ADMIN) return true;
        if (survey.getCreatedBy() != null && survey.getCreatedBy().getUserId().equals(user.getUserId())) return true;
        if (role == Role.PLACEMENT_OFFICER && survey.getCategory() == SurveyCategory.PLACEMENT) return true;
        if (role == Role.HOD) {
            return isSurveyTargetingBranch(survey, user.getBranch());
        }
        return false;
    }

    private boolean isSurveyTargetingBranch(Survey s, String branch) {
        if (branch == null || branch.trim().isEmpty()) return false;
        if (s.getTargetFilter() == null || s.getTargetFilter().trim().isEmpty()) return false;
        try {
            TargetFilter tf = objectMapper.readValue(s.getTargetFilter(), TargetFilter.class);
            if ("ALL".equalsIgnoreCase(tf.getScope())) return true;
            if (tf.getBranches() == null || tf.getBranches().isEmpty()) return true;
            return tf.getBranches().stream().anyMatch(b -> b.equalsIgnoreCase(branch.trim()));
        } catch (Exception e) {
            return false;
        }
    }

    private JsonNode getOrComputeAggregate(Survey survey, long liveCount) {
        Optional<AnalyticsCache> opt = analyticsCacheRepository.findById_SurveyIdAndId_Kind(
                survey.getSurveyId(), AnalyticsCacheKind.AGGREGATE);

        if (opt.isPresent() && opt.get().getResponseCount() == (int) liveCount) {
            try {
                return objectMapper.readTree(opt.get().getPayload());
            } catch (Exception e) {
                log.warn("Failed to read cached aggregate for survey {}", survey.getSurveyId());
            }
        }

        return computeAndCacheAggregate(survey, liveCount);
    }

    private JsonNode computeAndCacheAggregate(Survey survey, long liveCount) {
        try {
            Object schemaObj = objectMapper.readValue(survey.getQuestionSchema(), Object.class);
            Map<String, Double> targets = survey.getSkillTargets() != null
                    ? objectMapper.readValue(survey.getSkillTargets(), new TypeReference<Map<String, Double>>() {})
                    : Collections.emptyMap();

            FastApiSurveyMeta meta = new FastApiSurveyMeta(
                    survey.getSurveyId(),
                    survey.getCategory().name(),
                    survey.getIsAnonymous(),
                    survey.getEligibleCount() != null ? survey.getEligibleCount() : 0,
                    schemaObj,
                    targets
            );

            List<Response> responses = responseRepository.findBySurvey_SurveyId(survey.getSurveyId());
            List<FastApiResponseRow> rows = new ArrayList<>();

            for (Response r : responses) {
                Long studentId = Boolean.TRUE.equals(survey.getIsAnonymous())
                        ? null
                        : (r.getStudent() != null ? r.getStudent().getUserId() : null);

                Map<String, Object> answers = objectMapper.readValue(
                        r.getAnswerData(), new TypeReference<Map<String, Object>>() {});

                rows.add(new FastApiResponseRow(studentId, r.getGroupBranch(), r.getGroupYear(), answers));
            }

            FastApiAnalyzeRequest req = new FastApiAnalyzeRequest(meta, rows);
            JsonNode computedNode = analyticsClient.analyze(req);

            // Cache aggregate
            AnalyticsCache cache = new AnalyticsCache(
                    new AnalyticsCacheId(survey.getSurveyId(), AnalyticsCacheKind.AGGREGATE),
                    (int) liveCount,
                    objectMapper.writeValueAsString(computedNode),
                    LocalDateTime.now()
            );
            analyticsCacheRepository.save(cache);

            return computedNode;
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Failed to build or compute aggregate for survey {}: {}", survey.getSurveyId(), e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.VALIDATION_FAILED,
                    "Failed to process survey analytics: " + e.getMessage());
        }
    }

    private InsightResponse deserializeInsight(String payload, LocalDateTime computedAt) {
        try {
            InsightResponse ir = objectMapper.readValue(payload, InsightResponse.class);
            if (computedAt != null) {
                ir.setGeneratedAt(computedAt.toString());
            }
            return ir;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.VALIDATION_FAILED,
                    "Failed to deserialize insight: " + e.getMessage());
        }
    }

    private Survey findSurveyOrThrow(Long surveyId) {
        return surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND,
                        "Survey not found: " + surveyId));
    }
}
