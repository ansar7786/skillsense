package com.skillsense.seed;

import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/seed")
public class SeederController {

    private final DemoDataSeeder demoDataSeeder;
    private final PerformanceDataSeeder performanceDataSeeder;
    private final UserRepository userRepository;

    public SeederController(DemoDataSeeder demoDataSeeder,
                            PerformanceDataSeeder performanceDataSeeder,
                            UserRepository userRepository) {
        this.demoDataSeeder = demoDataSeeder;
        this.performanceDataSeeder = performanceDataSeeder;
        this.userRepository = userRepository;
    }

    @PostMapping("/demo")
    public ResponseEntity<Map<String, Object>> seedDemo(@AuthenticationPrincipal UserDetails userDetails) {
        assertAdmin(userDetails);
        Map<String, Object> result = demoDataSeeder.seedDemoData();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/perf")
    public ResponseEntity<Map<String, Object>> seedPerformance(@AuthenticationPrincipal UserDetails userDetails) {
        assertAdmin(userDetails);
        Map<String, Object> result = performanceDataSeeder.seedAndBenchmark();
        return ResponseEntity.ok(result);
    }

    private void assertAdmin(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Authentication required");
        }
        User user = userRepository.findByCollegeId(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "User not found"));
        if (user.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Only ADMIN can trigger seeder operations");
        }
    }
}
