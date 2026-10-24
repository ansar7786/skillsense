package com.skillsense.survey.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.survey.dto.CreateSurveyRequest;
import com.skillsense.survey.dto.EligibleCountResponse;
import com.skillsense.survey.dto.NewVersionRequest;
import com.skillsense.survey.dto.ProgressResponse;
import com.skillsense.survey.dto.SurveyResponse;
import com.skillsense.survey.dto.UpdateSurveyRequest;
import com.skillsense.survey.entity.ParticipationLog;
import com.skillsense.survey.entity.Survey;
import com.skillsense.survey.entity.SurveyTemplate;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.model.TargetFilter;
import com.skillsense.survey.model.schema.SurveyQuestionSchema;
import com.skillsense.survey.repository.ParticipationLogRepository;
import com.skillsense.survey.repository.SurveyRepository;
import com.skillsense.survey.repository.SurveyTemplateRepository;
import com.skillsense.survey.validation.QuestionSchemaValidator;
import com.skillsense.term.AcademicTerm;
import com.skillsense.term.AcademicTermRepository;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyTemplateRepository templateRepository;
    private final AcademicTermRepository termRepository;
    private final ParticipationLogRepository participationLogRepository;
    private final QuestionSchemaValidator schemaValidator;
    private final TargetingService targetingService;
    private final ObjectMapper objectMapper;

    public SurveyService(SurveyRepository surveyRepository,
                         SurveyTemplateRepository templateRepository,
                         AcademicTermRepository termRepository,
                         ParticipationLogRepository participationLogRepository,
                         QuestionSchemaValidator schemaValidator,
                         TargetingService targetingService,
                         ObjectMapper objectMapper) {
        this.surveyRepository = surveyRepository;
        this.templateRepository = templateRepository;
        this.termRepository = termRepository;
        this.participationLogRepository = participationLogRepository;
        this.schemaValidator = schemaValidator;
        this.targetingService = targetingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SurveyResponse createSurvey(CreateSurveyRequest request, User currentUser) {
        if (currentUser.getRole() == Role.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Students cannot create surveys");
        }

        // Section 5: WELLBEING category must be anonymous
        boolean isAnonymous = Boolean.TRUE.equals(request.getIsAnonymous());
        if (request.getCategory() == SurveyCategory.WELLBEING) {
            isAnonymous = true;
        }

        // Validate and serialize question schema
        String schemaJson = serializeJson(request.getQuestionSchema());
        schemaValidator.parseAndValidate(schemaJson);

        // Normalize and validate target filter
        TargetFilter filter = targetingService.normalizeAndValidateFilter(
                request.getTargetFilter(),
                currentUser,
                request.getCategory()
        );
        String targetFilterJson = serializeJson(filter);

        String skillTargetsJson = request.getSkillTargets() != null ? serializeJson(request.getSkillTargets()) : null;

        AcademicTerm term = termRepository.findById(request.getTermId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Academic term not found: " + request.getTermId()));

        SurveyTemplate template;
        int version;
        if (request.getTemplateId() == null) {
            template = SurveyTemplate.builder()
                    .name(request.getTitle())
                    .category(request.getCategory())
                    .createdBy(currentUser)
                    .build();
            template = templateRepository.save(template);
            version = 1;
        } else {
            template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey template not found: " + request.getTemplateId()));
            version = surveyRepository.findMaxVersionByTemplateId(template.getTemplateId()) + 1;
        }

        Survey survey = Survey.builder()
                .template(template)
                .version(version)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .term(term)
                .questionSchema(schemaJson)
                .skillTargets(skillTargetsJson)
                .isAnonymous(isAnonymous)
                .targetFilter(targetFilterJson)
                .status(SurveyStatus.DRAFT)
                .opensAt(request.getOpensAt())
                .closesAt(request.getClosesAt())
                .eligibleCount(0)
                .createdBy(currentUser)
                .build();

        survey = surveyRepository.save(survey);
        return SurveyResponse.fromEntity(survey, 0L, objectMapper);
    }

    @Transactional(readOnly = true)
    public SurveyResponse getSurvey(Long id, User currentUser) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + id));

        assertCanViewSurvey(survey, currentUser);

        long responseCount = participationLogRepository.countRespondedBySurveyId(id);
        return SurveyResponse.fromEntity(survey, responseCount, objectMapper);
    }

    @Transactional(readOnly = true)
    public List<SurveyResponse> listSurveys(SurveyStatus status, SurveyCategory category, Integer termId, User currentUser) {
        if (currentUser.getRole() == Role.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Students must access assigned surveys via student endpoints");
        }

        List<Survey> all = surveyRepository.searchSurveys(status, category, termId);
        List<SurveyResponse> result = new ArrayList<>();

        for (Survey s : all) {
            if (canViewSurvey(s, currentUser)) {
                long responseCount = participationLogRepository.countRespondedBySurveyId(s.getSurveyId());
                result.add(SurveyResponse.fromEntity(s, responseCount, objectMapper));
            }
        }

        return result;
    }

    @Transactional
    public SurveyResponse updateSurvey(Long id, UpdateSurveyRequest request, User currentUser) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + id));

        assertCanEditSurvey(survey, currentUser);

        if (survey.getStatus() == SurveyStatus.CLOSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Closed surveys cannot be edited. Create a new version instead.");
        }

        if (survey.getStatus() == SurveyStatus.ACTIVE) {
            // Immutability rule: ACTIVE surveys ONLY allow title, description, closesAt
            if (request.getCategory() != null && request.getCategory() != survey.getCategory()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Cannot change category on an ACTIVE survey");
            }
            if (request.getTermId() != null && !request.getTermId().equals(survey.getTerm().getTermId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Cannot change academic term on an ACTIVE survey");
            }
            if (request.getQuestionSchema() != null) {
                String newSchemaJson = serializeJson(request.getQuestionSchema());
                if (!newSchemaJson.equals(survey.getQuestionSchema())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Questions cannot change after activation");
                }
            }
            if (request.getIsAnonymous() != null && !request.getIsAnonymous().equals(survey.getIsAnonymous())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Anonymity cannot change after activation");
            }
            if (request.getTargetFilter() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Targeting cannot change after activation");
            }

            if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
                survey.setTitle(request.getTitle().trim());
            }
            if (request.getDescription() != null) {
                survey.setDescription(request.getDescription());
            }
            if (request.getClosesAt() != null) {
                survey.setClosesAt(request.getClosesAt());
            }

            survey = surveyRepository.save(survey);
            long count = participationLogRepository.countRespondedBySurveyId(id);
            return SurveyResponse.fromEntity(survey, count, objectMapper);
        }

        // DRAFT updates: all editable fields allowed
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            survey.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            survey.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            survey.setCategory(request.getCategory());
            if (request.getCategory() == SurveyCategory.WELLBEING) {
                survey.setIsAnonymous(true);
            }
        }
        if (request.getTermId() != null) {
            AcademicTerm term = termRepository.findById(request.getTermId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Academic term not found: " + request.getTermId()));
            survey.setTerm(term);
        }
        if (request.getQuestionSchema() != null) {
            String schemaJson = serializeJson(request.getQuestionSchema());
            schemaValidator.parseAndValidate(schemaJson);
            survey.setQuestionSchema(schemaJson);
        }
        if (request.getSkillTargets() != null) {
            survey.setSkillTargets(serializeJson(request.getSkillTargets()));
        }
        if (request.getIsAnonymous() != null) {
            if (survey.getCategory() == SurveyCategory.WELLBEING) {
                survey.setIsAnonymous(true);
            } else {
                survey.setIsAnonymous(request.getIsAnonymous());
            }
        }
        if (request.getTargetFilter() != null) {
            TargetFilter filter = targetingService.normalizeAndValidateFilter(request.getTargetFilter(), currentUser, survey.getCategory());
            survey.setTargetFilter(serializeJson(filter));
        }
        if (request.getOpensAt() != null) {
            survey.setOpensAt(request.getOpensAt());
        }
        if (request.getClosesAt() != null) {
            survey.setClosesAt(request.getClosesAt());
        }

        survey = surveyRepository.save(survey);
        return SurveyResponse.fromEntity(survey, 0L, objectMapper);
    }

    @Transactional
    public void deleteSurvey(Long id, User currentUser) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + id));

        assertCanEditSurvey(survey, currentUser);

        if (survey.getStatus() != SurveyStatus.DRAFT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Only DRAFT surveys can be deleted");
        }

        long responsesCount = surveyRepository.countResponsesBySurveyId(id);
        if (responsesCount > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Cannot delete survey with existing responses");
        }

        participationLogRepository.deleteById_SurveyId(id);
        surveyRepository.delete(survey);
    }

    @Transactional
    public SurveyResponse activateSurvey(Long id, User currentUser) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + id));

        assertCanEditSurvey(survey, currentUser);

        if (survey.getStatus() != SurveyStatus.DRAFT) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "Only DRAFT surveys can be activated (current status: " + survey.getStatus() + ")"
            );
        }

        // 1. Validate question schema
        schemaValidator.parseAndValidate(survey.getQuestionSchema());

        // 2. Parse target filter and select all active eligible students
        TargetFilter filter;
        try {
            filter = objectMapper.readValue(survey.getTargetFilter(), TargetFilter.class);
        } catch (Exception e) {
            filter = new TargetFilter("ALL", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        List<User> eligibleStudents = targetingService.findEligibleStudents(filter);

        // 3. Write one participation_log row per eligible student
        List<ParticipationLog> logs = new ArrayList<>();
        for (User student : eligibleStudents) {
            logs.add(new ParticipationLog(survey.getSurveyId(), student.getUserId(), false));
        }
        participationLogRepository.saveAll(logs);

        // 4. Update survey runtime state
        survey.setEligibleCount(eligibleStudents.size());
        survey.setStatus(SurveyStatus.ACTIVE);
        survey.setActivatedAt(LocalDateTime.now());
        if (survey.getOpensAt() == null) {
            survey.setOpensAt(LocalDateTime.now().minusSeconds(1));
        }

        survey = surveyRepository.save(survey);
        return SurveyResponse.fromEntity(survey, 0L, objectMapper);
    }

    @Transactional
    public SurveyResponse closeSurvey(Long id, User currentUser) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + id));

        assertCanEditSurvey(survey, currentUser);

        if (survey.getStatus() != SurveyStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "Only ACTIVE surveys can be closed (current status: " + survey.getStatus() + ")"
            );
        }

        survey.setStatus(SurveyStatus.CLOSED);
        survey = surveyRepository.save(survey);

        long count = participationLogRepository.countRespondedBySurveyId(id);
        return SurveyResponse.fromEntity(survey, count, objectMapper);
    }

    @Transactional
    public SurveyResponse createNextVersion(Long id, NewVersionRequest request, User currentUser) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + id));

        assertCanEditSurvey(survey, currentUser);

        if (survey.getStatus() != SurveyStatus.CLOSED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "Only CLOSED surveys can have a new version created (current status: " + survey.getStatus() + ")"
            );
        }

        AcademicTerm term = termRepository.findById(request.getTermId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Academic term not found: " + request.getTermId()));

        int nextVersion = surveyRepository.findMaxVersionByTemplateId(survey.getTemplate().getTemplateId()) + 1;

        Survey newSurvey = Survey.builder()
                .template(survey.getTemplate())
                .version(nextVersion)
                .title(survey.getTitle())
                .description(survey.getDescription())
                .category(survey.getCategory())
                .term(term)
                .questionSchema(survey.getQuestionSchema()) // Exact question schema and keys preserved
                .skillTargets(survey.getSkillTargets())
                .isAnonymous(survey.getIsAnonymous())
                .targetFilter(survey.getTargetFilter())
                .status(SurveyStatus.DRAFT)
                .opensAt(null)
                .closesAt(null)
                .eligibleCount(0)
                .createdBy(currentUser)
                .build();

        newSurvey = surveyRepository.save(newSurvey);
        return SurveyResponse.fromEntity(newSurvey, 0L, objectMapper);
    }

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(Long id, User currentUser) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + id));

        assertCanViewSurvey(survey, currentUser);

        long responded = participationLogRepository.countRespondedBySurveyId(id);
        int eligible = survey.getEligibleCount() != null ? survey.getEligibleCount() : 0;
        double rate = eligible > 0 ? (double) responded / eligible : 0.0;

        return new ProgressResponse(responded, eligible, rate);
    }

    @Transactional(readOnly = true)
    public EligibleCountResponse countEligible(TargetFilter filter, User currentUser) {
        TargetFilter normalized = targetingService.normalizeAndValidateFilter(filter, currentUser, null);
        long count = targetingService.countEligibleStudents(normalized);
        return new EligibleCountResponse(count);
    }

    private void assertCanViewSurvey(Survey s, User u) {
        if (!canViewSurvey(s, u)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied for this survey");
        }
    }

    private boolean canViewSurvey(Survey s, User u) {
        Role role = u.getRole();
        if (role == Role.ADMIN) return true;
        if (s.getCreatedBy() != null && s.getCreatedBy().getUserId().equals(u.getUserId())) return true;
        if (role == Role.PLACEMENT_OFFICER && s.getCategory() == SurveyCategory.PLACEMENT) return true;
        if (role == Role.HOD) {
            return isSurveyTargetingBranch(s, u.getBranch());
        }
        return false;
    }

    private void assertCanEditSurvey(Survey s, User u) {
        if (u.getRole() == Role.ADMIN) return;
        if (s.getCreatedBy() != null && s.getCreatedBy().getUserId().equals(u.getUserId())) return;
        throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Only the creator or an Administrator can modify this survey");
    }

    private boolean isSurveyTargetingBranch(Survey s, String branch) {
        if (branch == null || branch.trim().isEmpty()) return false;
        try {
            TargetFilter filter = objectMapper.readValue(s.getTargetFilter(), TargetFilter.class);
            if ("ALL".equalsIgnoreCase(filter.getScope())) return true;
            if (filter.getBranches() == null || filter.getBranches().isEmpty()) return true;
            return filter.getBranches().stream().anyMatch(b -> b.equalsIgnoreCase(branch));
        } catch (Exception e) {
            return false;
        }
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String str) return str;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Failed to serialize JSON: " + e.getMessage());
        }
    }
}
