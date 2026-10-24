package com.skillsense.survey;

import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.survey.dto.CreateSurveyRequest;
import com.skillsense.survey.dto.EligibleCountResponse;
import com.skillsense.survey.dto.NewVersionRequest;
import com.skillsense.survey.dto.ProgressResponse;
import com.skillsense.survey.dto.SurveyResponse;
import com.skillsense.survey.dto.UpdateSurveyRequest;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.SurveyStatus;
import com.skillsense.survey.model.TargetFilter;
import com.skillsense.survey.service.SurveyService;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@PreAuthorize("hasAnyRole('FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN')")
public class SurveyController {

    private final SurveyService surveyService;
    private final UserRepository userRepository;

    public SurveyController(SurveyService surveyService, UserRepository userRepository) {
        this.surveyService = surveyService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<SurveyResponse> createSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSurveyRequest request
    ) {
        User user = getCurrentUser(userDetails);
        SurveyResponse response = surveyService.createSurvey(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SurveyResponse>> getSurveys(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "status", required = false) SurveyStatus status,
            @RequestParam(name = "category", required = false) SurveyCategory category,
            @RequestParam(name = "termId", required = false) Integer termId
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(surveyService.listSurveys(status, category, termId, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurveyResponse> getSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(surveyService.getSurvey(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SurveyResponse> updateSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateSurveyRequest request
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(surveyService.updateSurvey(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        User user = getCurrentUser(userDetails);
        surveyService.deleteSurvey(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<SurveyResponse> activateSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(surveyService.activateSurvey(id, user));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<SurveyResponse> closeSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(surveyService.closeSurvey(id, user));
    }

    @PostMapping("/{id}/new-version")
    public ResponseEntity<SurveyResponse> createNewVersion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @Valid @RequestBody NewVersionRequest request
    ) {
        User user = getCurrentUser(userDetails);
        SurveyResponse response = surveyService.createNextVersion(id, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<ProgressResponse> getProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(surveyService.getProgress(id, user));
    }

    @PostMapping("/eligible-count")
    public ResponseEntity<EligibleCountResponse> getEligibleCount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) TargetFilter filter
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(surveyService.countEligible(filter, user));
    }

    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "User is not authenticated");
        }
        return userRepository.findByCollegeId(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.NOT_FOUND, "User account not found: " + userDetails.getUsername()));
    }
}
