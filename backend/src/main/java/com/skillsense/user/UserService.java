package com.skillsense.user;

import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.user.dto.CreateUserRequest;
import com.skillsense.user.dto.CreateUserResponse;
import com.skillsense.user.dto.CsvImportResponse;
import com.skillsense.user.dto.ResetPasswordResponse;
import com.skillsense.user.dto.UpdateUserRequest;
import com.skillsense.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom RANDOM = new SecureRandom();

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Role role, String branch, String q, Pageable pageable) {
        String query = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        String br = (branch != null && !branch.trim().isEmpty()) ? branch.trim() : null;

        return userRepository.searchUsers(role, br, query, pageable)
                .map(UserResponse::fromEntity);
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        String collegeId = request.getCollegeId().trim();

        if (userRepository.existsByCollegeId(collegeId)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "User with college ID already exists: " + collegeId
            );
        }

        String tempPassword = generateTemporaryPassword();

        User user = User.builder()
                .collegeId(collegeId)
                .name(request.getName().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .role(request.getRole())
                .branch(request.getBranch() != null ? request.getBranch().trim() : null)
                .studyYear(request.getStudyYear())
                .section(request.getSection() != null ? request.getSection().trim() : null)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .isActive(true)
                .mustChangePassword(true)
                .build();

        User saved = userRepository.save(user);

        return CreateUserResponse.builder()
                .user(UserResponse.fromEntity(saved))
                .temporaryPassword(tempPassword)
                .build();
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "User not found: " + userId));

        user.setName(request.getName().trim());
        user.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        user.setBranch(request.getBranch() != null ? request.getBranch().trim() : null);
        user.setStudyYear(request.getStudyYear());
        user.setSection(request.getSection() != null ? request.getSection().trim() : null);

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "User not found: " + userId));

        String newTempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(newTempPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);

        return ResetPasswordResponse.builder()
                .temporaryPassword(newTempPassword)
                .build();
    }

    @Transactional
    public CsvImportResponse importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "CSV file is empty or missing");
        }

        int created = 0;
        int skipped = 0;
        List<CsvImportResponse.RowError> errors = new ArrayList<>();
        StringBuilder credentialsCsv = new StringBuilder("college_id,temporary_password\n");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "CSV file has no content");
            }

            String line;
            int rowNum = 1;

            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                // CSV format: college_id,name,email,role,branch,study_year,section
                String[] tokens = line.split(",", -1);
                if (tokens.length < 4) {
                    errors.add(new CsvImportResponse.RowError(rowNum, "Missing required columns (at least college_id, name, email, role)"));
                    skipped++;
                    continue;
                }

                String collegeId = tokens[0].trim();
                String name = tokens[1].trim();
                String email = tokens.length > 2 ? tokens[2].trim() : null;
                String roleStr = tokens.length > 3 ? tokens[3].trim() : "";
                String branch = tokens.length > 4 ? tokens[4].trim() : null;
                String yearStr = tokens.length > 5 ? tokens[5].trim() : null;
                String section = tokens.length > 6 ? tokens[6].trim() : null;

                if (collegeId.isEmpty() || name.isEmpty() || roleStr.isEmpty()) {
                    errors.add(new CsvImportResponse.RowError(rowNum, "college_id, name, and role cannot be empty"));
                    skipped++;
                    continue;
                }

                if (userRepository.existsByCollegeId(collegeId)) {
                    errors.add(new CsvImportResponse.RowError(rowNum, "Duplicate college_id: " + collegeId));
                    skipped++;
                    continue;
                }

                Role role;
                try {
                    role = Role.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    errors.add(new CsvImportResponse.RowError(rowNum, "Invalid role: " + roleStr));
                    skipped++;
                    continue;
                }

                Integer studyYear = null;
                if (yearStr != null && !yearStr.isEmpty()) {
                    try {
                        studyYear = Integer.parseInt(yearStr);
                    } catch (NumberFormatException e) {
                        errors.add(new CsvImportResponse.RowError(rowNum, "Invalid study_year: " + yearStr));
                        skipped++;
                        continue;
                    }
                }

                String tempPassword = generateTemporaryPassword();

                User user = User.builder()
                        .collegeId(collegeId)
                        .name(name)
                        .email(email != null && !email.isEmpty() ? email : null)
                        .role(role)
                        .branch(branch != null && !branch.isEmpty() ? branch : null)
                        .studyYear(studyYear)
                        .section(section != null && !section.isEmpty() ? section : null)
                        .passwordHash(passwordEncoder.encode(tempPassword))
                        .isActive(true)
                        .mustChangePassword(true)
                        .build();

                userRepository.save(user);
                created++;
                credentialsCsv.append(collegeId).append(",").append(tempPassword).append("\n");
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Failed to parse CSV: " + e.getMessage());
        }

        return CsvImportResponse.builder()
                .created(created)
                .skipped(skipped)
                .errors(errors)
                .credentials(credentialsCsv.toString().trim())
                .build();
    }

    public static String generateTemporaryPassword() {
        // Generates an 8-character temporary password satisfying letter + digit rules
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";

        StringBuilder sb = new StringBuilder("Temp");
        for (int i = 0; i < 3; i++) {
            sb.append(letters.charAt(RANDOM.nextInt(letters.length())));
        }
        for (int i = 0; i < 3; i++) {
            sb.append(digits.charAt(RANDOM.nextInt(digits.length())));
        }
        return sb.toString();
    }
}
