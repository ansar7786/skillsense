package com.skillsense.report;

import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/surveys/{id}/export")
@PreAuthorize("hasAnyRole('FACULTY', 'HOD', 'PLACEMENT_OFFICER', 'ADMIN')")
public class ReportExportController {

    private final ReportExportService reportExportService;
    private final UserRepository userRepository;

    public ReportExportController(ReportExportService reportExportService, UserRepository userRepository) {
        this.reportExportService = reportExportService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<byte[]> exportSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestParam(name = "format", defaultValue = "pdf") String format
    ) {
        if (userDetails == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "Authentication required");
        }

        User user = userRepository.findByCollegeId(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED, "User not found"));

        byte[] content = reportExportService.exportReport(id, format, user);

        HttpHeaders headers = new HttpHeaders();
        if ("xlsx".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "survey_" + id + "_report.xlsx");
        } else {
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "survey_" + id + "_report.pdf");
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }
}
