package com.skillsense.response;

import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.response.dto.StudentSurveyDetailResponse;
import com.skillsense.response.dto.StudentSurveySummaryResponse;
import com.skillsense.response.dto.SubmitResponseRequest;
import com.skillsense.response.dto.SubmitResponseResult;
import com.skillsense.response.service.ResponseService;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my/surveys")
@PreAuthorize("hasRole('STUDENT')")
public class StudentSurveyController {

    private final ResponseService responseService;
    private final UserRepository userRepository;
    private final com.skillsense.analytics.service.StudentResultService studentResultService;

    public StudentSurveyController(ResponseService responseService, UserRepository userRepository,
                                   com.skillsense.analytics.service.StudentResultService studentResultService) {
        this.responseService = responseService;
        this.userRepository = userRepository;
        this.studentResultService = studentResultService;
    }

    @GetMapping
    public ResponseEntity<List<StudentSurveySummaryResponse>> getMySurveys(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User student = getCurrentStudent(userDetails);
        return ResponseEntity.ok(responseService.getMySurveys(student));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentSurveyDetailResponse> getMySurveyDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        User student = getCurrentStudent(userDetails);
        return ResponseEntity.ok(responseService.getMySurveyDetail(id, student));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<com.skillsense.analytics.dto.StudentResultResponse> getMySurveyResult(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        User student = getCurrentStudent(userDetails);
        return ResponseEntity.ok(studentResultService.getMyResult(id, student));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmitResponseResult> submitSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @Valid @RequestBody SubmitResponseRequest request
    ) {
        User student = getCurrentStudent(userDetails);
        SubmitResponseResult result = responseService.submitResponse(id, request, student);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    private User getCurrentStudent(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "User is not authenticated");
        }
        return userRepository.findByCollegeId(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.NOT_FOUND, "User account not found: " + userDetails.getUsername()));
    }
}
