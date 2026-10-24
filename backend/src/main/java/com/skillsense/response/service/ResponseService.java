package com.skillsense.response.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.response.dto.StudentSurveyDetailResponse;
import com.skillsense.response.dto.StudentSurveySummaryResponse;
import com.skillsense.response.dto.SubmitResponseRequest;
import com.skillsense.response.dto.SubmitResponseResult;
import com.skillsense.response.entity.Response;
import com.skillsense.response.repository.ResponseRepository;
import com.skillsense.survey.entity.ParticipationLog;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import com.skillsense.survey.repository.ParticipationLogRepository;
import com.skillsense.survey.repository.SurveyRepository;
import com.skillsense.survey.validation.AnswerValidator;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ResponseService {

    private final SurveyRepository surveyRepository;
    private final ParticipationLogRepository participationLogRepository;
    private final ResponseRepository responseRepository;
    private final AnswerValidator answerValidator;
    private final ObjectMapper objectMapper;
    private final com.skillsense.analytics.service.StudentResultService studentResultService;

    public ResponseService(SurveyRepository surveyRepository,
                           ParticipationLogRepository participationLogRepository,
                           ResponseRepository responseRepository,
                           AnswerValidator answerValidator,
                           ObjectMapper objectMapper,
                           com.skillsense.analytics.service.StudentResultService studentResultService) {
        this.surveyRepository = surveyRepository;
        this.participationLogRepository = participationLogRepository;
        this.responseRepository = responseRepository;
        this.answerValidator = answerValidator;
        this.objectMapper = objectMapper;
        this.studentResultService = studentResultService;
    }

    @Transactional(readOnly = true)
    public List<StudentSurveySummaryResponse> getMySurveys(User student) {
        assertIsStudent(student);

        List<ParticipationLog> logs = participationLogRepository.findById_StudentId(student.getUserId());
        List<StudentSurveySummaryResponse> list = new ArrayList<>();

        for (ParticipationLog log : logs) {
            Survey survey = surveyRepository.findById(log.getId().getSurveyId()).orElse(null);
            if (survey != null) {
                String status = Boolean.TRUE.equals(log.getHasResponded()) ? "SUBMITTED" : "PENDING";
                list.add(new StudentSurveySummaryResponse(
                        survey.getSurveyId(),
                        survey.getTitle(),
                        survey.getCategory(),
                        survey.getClosesAt(),
                        survey.getIsAnonymous(),
                        status
                ));
            }
        }

        // Sort: PENDING first, then by closesAt ascending (nulls last)
        list.sort(Comparator.comparing((StudentSurveySummaryResponse s) -> "SUBMITTED".equals(s.getStatus()))
                .thenComparing(StudentSurveySummaryResponse::getClosesAt, Comparator.nullsLast(Comparator.naturalOrder())));

        return list;
    }

    @Transactional(readOnly = true)
    public StudentSurveyDetailResponse getMySurveyDetail(Long surveyId, User student) {
        assertIsStudent(student);

        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + surveyId));

        boolean eligible = participationLogRepository.existsById_SurveyIdAndId_StudentId(surveyId, student.getUserId());
        if (!eligible) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.NOT_ELIGIBLE, "You are not eligible for this survey");
        }

        JsonNode sanitizedSchema = sanitizeQuestionSchema(survey.getQuestionSchema());

        String notice = Boolean.TRUE.equals(survey.getIsAnonymous())
                ? "Anonymous: your name is not stored with your answers"
                : "Not anonymous: your name is recorded with your answers";

        return new StudentSurveyDetailResponse(
                survey.getSurveyId(),
                survey.getTitle(),
                survey.getDescription(),
                survey.getCategory(),
                survey.getIsAnonymous(),
                notice,
                survey.getClosesAt(),
                sanitizedSchema
        );
    }

    @Transactional
    public SubmitResponseResult submitResponse(Long surveyId, SubmitResponseRequest request, User student) {
        assertIsStudent(student);

        // Step 1: Load survey and check timing
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + surveyId));

        if (survey.getStatus() != SurveyStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.SURVEY_NOT_ACTIVE, "Survey is not currently active");
        }

        LocalDateTime now = LocalDateTime.now();
        // Allow 2-second grace for database datetime rounding / clock resolution
        if (survey.getOpensAt() != null && now.plusSeconds(2).isBefore(survey.getOpensAt())) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.SURVEY_NOT_ACTIVE, "Survey is not open yet");
        }
        if (survey.getClosesAt() != null && now.isAfter(survey.getClosesAt())) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.SURVEY_NOT_ACTIVE, "Survey has closed");
        }

        // Step 2: Check eligibility
        boolean eligible = participationLogRepository.existsById_SurveyIdAndId_StudentId(surveyId, student.getUserId());
        if (!eligible) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.NOT_ELIGIBLE, "You are not eligible for this survey");
        }

        // Step 3: Validate answers against schema
        SurveyQuestionSchema schema;
        try {
            schema = objectMapper.readValue(survey.getQuestionSchema(), SurveyQuestionSchema.class);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.VALIDATION_FAILED, "Failed to parse survey question schema");
        }
        answerValidator.validateAndThrow(schema, request.getAnswers());

        // Step 4: Atomically mark participation as responded
        int rowsUpdated = participationLogRepository.markResponded(surveyId, student.getUserId());
        if (rowsUpdated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.ALREADY_SUBMITTED, "You have already submitted this survey");
        }

        // Step 5 & 6: Build response row and insert
        String responseId = UUID.randomUUID().toString();
        String answerDataJson;
        try {
            answerDataJson = objectMapper.writeValueAsString(request.getAnswers());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Failed to serialize answer data");
        }

        Response.Builder responseBuilder = Response.builder()
                .responseId(responseId)
                .survey(survey)
                .answerData(answerDataJson)
                .groupBranch(student.getBranch())
                .groupYear(student.getStudyYear());

        if (Boolean.TRUE.equals(survey.getIsAnonymous())) {
            // Strict Section 5 Anonymity: identity, time, and section must be NULL
            responseBuilder
                    .student(null)
                    .submittedAt(null)
                    .groupSection(null);
        } else {
            // Non-anonymous: record student, time, section
            responseBuilder
                    .student(student)
                    .submittedAt(LocalDateTime.now())
                    .groupSection(student.getSection());
        }

        responseRepository.save(responseBuilder.build());

        // Step 7: Compute student_results for non-anonymous scored surveys
        if (!Boolean.TRUE.equals(survey.getIsAnonymous())) {
            studentResultService.computeAndSaveResult(survey, student, request.getAnswers());
        }

        return new SubmitResponseResult(true);
    }

    public JsonNode sanitizeQuestionSchema(String questionSchemaJson) {
        try {
            JsonNode root = objectMapper.readTree(questionSchemaJson);
            if (root.isObject() && root.has("questions")) {
                JsonNode questionsNode = root.get("questions");
                if (questionsNode.isArray()) {
                    for (JsonNode qNode : questionsNode) {
                        if (qNode.isObject()) {
                            ObjectNode qObj = (ObjectNode) qNode;
                            qObj.remove("scoring");
                            qObj.remove("weight");
                            qObj.remove("skill");
                            if (qObj.has("options") && qObj.get("options").isArray()) {
                                for (JsonNode optNode : qObj.get("options")) {
                                    if (optNode.isObject()) {
                                        ((ObjectNode) optNode).remove("score");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return root;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.VALIDATION_FAILED, "Failed to sanitize question schema: " + e.getMessage());
        }
    }

    private void assertIsStudent(User user) {
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access restricted to students only");
        }
    }
}
