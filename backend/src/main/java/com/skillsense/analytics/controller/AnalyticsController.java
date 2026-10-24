package com.skillsense.analytics.controller;

import com.skillsense.analytics.dto.DashboardSummaryResponse;
import com.skillsense.analytics.dto.InsightResponse;
import com.skillsense.analytics.dto.SurveyAnalyticsResponse;
import com.skillsense.analytics.service.AnalyticsService;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    public AnalyticsController(AnalyticsService analyticsService, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/surveys/{id}/analytics")
    public ResponseEntity<SurveyAnalyticsResponse> getSurveyAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(analyticsService.getSurveyAnalytics(id, user));
    }

    @GetMapping("/surveys/{id}/trend")
    public ResponseEntity<Object> getSurveyTrend(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(analyticsService.getSurveyTrend(id, user));
    }

    @GetMapping("/surveys/{id}/insights")
    public ResponseEntity<InsightResponse> getSurveyInsights(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(analyticsService.getSurveyInsights(id, refresh, user));
    }

    @GetMapping("/surveys/{id}/texts")
    public ResponseEntity<List<String>> getSurveyTexts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestParam(name = "key") String key
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(analyticsService.getSurveyTexts(id, key, user));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(analyticsService.getDashboardSummary(user));
    }

    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "User is not authenticated");
        }
        return userRepository.findByCollegeId(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.NOT_FOUND,
                        "User account not found: " + userDetails.getUsername()));
    }
}
