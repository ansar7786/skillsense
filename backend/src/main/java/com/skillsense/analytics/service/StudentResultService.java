package com.skillsense.analytics.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.analytics.client.AnalyticsClient;
import com.skillsense.analytics.dto.StudentResultResponse;
import com.skillsense.analytics.dto.fastapi.FastApiStudentResultRequest;
import com.skillsense.analytics.dto.fastapi.FastApiSurveyMeta;
import com.skillsense.analytics.entity.StudentResult;
import com.skillsense.analytics.entity.StudentResultId;
import com.skillsense.analytics.repository.StudentResultRepository;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.response.entity.Response;
import com.skillsense.response.repository.ResponseRepository;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import com.skillsense.survey.repository.SurveyRepository;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentResultService {

    private static final Logger log = LoggerFactory.getLogger(StudentResultService.class);

    private final StudentResultRepository studentResultRepository;
    private final SurveyRepository surveyRepository;
    private final ResponseRepository responseRepository;
    private final AnalyticsClient analyticsClient;
    private final ObjectMapper objectMapper;

    public StudentResultService(
            StudentResultRepository studentResultRepository,
            SurveyRepository surveyRepository,
            ResponseRepository responseRepository,
            AnalyticsClient analyticsClient,
            ObjectMapper objectMapper
    ) {
        this.studentResultRepository = studentResultRepository;
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
        this.analyticsClient = analyticsClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public StudentResultResponse getMyResult(Long surveyId, User student) {
        if (student == null || student.getRole() != Role.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Only students can access personal results");
        }

        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + surveyId));

        // 404 for anonymous surveys
        if (Boolean.TRUE.equals(survey.getIsAnonymous())) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Result not available for anonymous surveys");
        }

        // 404 for unscored surveys
        if (!hasScoredQuestions(survey)) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey has no scored skills");
        }

        // Look up in student_results
        return studentResultRepository.findById_SurveyIdAndId_StudentId(surveyId, student.getUserId())
                .map(sr -> deserializeResult(sr.getPayload()))
                .orElseGet(() -> {
                    // Check if student responded; if so, compute and return
                    List<Response> responses = responseRepository.findBySurvey_SurveyId(surveyId);
                    Response userResp = responses.stream()
                            .filter(r -> r.getStudent() != null && r.getStudent().getUserId().equals(student.getUserId()))
                            .findFirst()
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Result not found for this survey"));

                    Map<String, Object> answers = parseAnswers(userResp.getAnswerData());
                    return computeAndSaveResult(survey, student, answers);
                });
    }

    @Transactional
    public StudentResultResponse computeAndSaveResult(Survey survey, User student, Map<String, Object> answers) {
        if (Boolean.TRUE.equals(survey.getIsAnonymous()) || !hasScoredQuestions(survey)) {
            return null;
        }

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

            FastApiStudentResultRequest req = new FastApiStudentResultRequest(meta, answers, Collections.emptyMap());
            StudentResultResponse response = analyticsClient.studentResult(req);

            // Add deterministic tips
            enrichWithDeterministicTips(response);

            // Persist to student_results table
            String payloadJson = objectMapper.writeValueAsString(response);
            StudentResult result = new StudentResult(
                    new StudentResultId(survey.getSurveyId(), student.getUserId()),
                    payloadJson,
                    LocalDateTime.now()
            );
            studentResultRepository.save(result);

            return response;
        } catch (Exception e) {
            log.error("Failed to compute student result for survey {} student {}: {}",
                    survey.getSurveyId(), student.getUserId(), e.getMessage());
            return null;
        }
    }

    public boolean hasScoredQuestions(Survey survey) {
        if (survey.getQuestionSchema() == null) return false;
        try {
            SurveyQuestionSchema schema = objectMapper.readValue(survey.getQuestionSchema(), SurveyQuestionSchema.class);
            if (schema.getQuestions() == null) return false;
            return schema.getQuestions().stream()
                    .anyMatch(q -> q.getScoring() != null && q.getScoring().getSkill() != null && !q.getScoring().getSkill().trim().isEmpty());
        } catch (Exception e) {
            return false;
        }
    }

    private void enrichWithDeterministicTips(StudentResultResponse response) {
        List<String> tips = new ArrayList<>();
        String weakest = response.getWeakestSkill();
        String strongest = response.getStrongestSkill();

        if (response.getSkills() != null && !response.getSkills().isEmpty()) {
            if (weakest != null) {
                response.getSkills().stream()
                        .filter(s -> weakest.equalsIgnoreCase(s.getSkill()))
                        .findFirst()
                        .ifPresent(s -> {
                            tips.add(String.format("Focus on improving %s: your score is %.1f against a target of %.1f.",
                                    s.getSkill(), s.getScore(), s.getTarget()));
                            tips.add(String.format("Review foundational practice problems and exercises in %s.", s.getSkill()));
                        });
            }

            if (strongest != null && !strongest.equalsIgnoreCase(weakest)) {
                response.getSkills().stream()
                        .filter(s -> strongest.equalsIgnoreCase(s.getSkill()))
                        .findFirst()
                        .ifPresent(s -> {
                            tips.add(String.format("Maintain your strength in %s (score: %.1f).", s.getSkill(), s.getScore()));
                        });
            }
        }

        if (tips.isEmpty()) {
            tips.add("Continue consistent practice across all assessed skill areas.");
            tips.add("Review standard interview and assessment preparation materials.");
        }

        response.setTips(tips);
        response.setDisclaimer("Self-assessed indicator, not an exam score");
    }

    private Map<String, Object> parseAnswers(String answerDataJson) {
        if (answerDataJson == null || answerDataJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(answerDataJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private StudentResultResponse deserializeResult(String payload) {
        try {
            return objectMapper.readValue(payload, StudentResultResponse.class);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.VALIDATION_FAILED, "Failed to deserialize student result");
        }
    }
}
