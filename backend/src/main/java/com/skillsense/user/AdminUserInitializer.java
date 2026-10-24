package com.skillsense.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_PASSWORD:Demo@1234}")
    private String adminPassword;

    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            log.info("No users found in database. Initializing default admin user...");
            User admin = User.builder()
                    .collegeId("admin")
                    .name("System Administrator")
                    .email("admin@skillsense.edu")
                    .role(Role.ADMIN)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .isActive(true)
                    .mustChangePassword(true)
                    .build();

            userRepository.save(admin);
            log.info("Admin account initialized successfully (college_id: admin).");
        }
    }
}
