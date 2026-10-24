package com.skillsense.auth;

import com.skillsense.auth.dto.ChangePasswordRequest;
import com.skillsense.auth.dto.LoginRequest;
import com.skillsense.auth.dto.LoginResponse;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import com.skillsense.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginLockoutService lockoutService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       LoginLockoutService lockoutService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.lockoutService = lockoutService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String collegeId = request.getCollegeId() != null ? request.getCollegeId().trim() : "";

        if (lockoutService.isLocked(collegeId)) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCode.ACCOUNT_LOCKED,
                    "Account is temporarily locked for 5 minutes due to repeated failed login attempts"
            );
        }

        User user = userRepository.findByCollegeId(collegeId).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            lockoutService.recordFailedAttempt(collegeId);
            if (lockoutService.isLocked(collegeId)) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCode.ACCOUNT_LOCKED,
                        "Account is temporarily locked for 5 minutes due to repeated failed login attempts"
                );
            }
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCode.UNAUTHENTICATED,
                    "Invalid college ID or password"
            );
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "Account is disabled or inactive"
            );
        }

        lockoutService.recordSuccess(collegeId);

        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getCollegeId(),
                user.getRole(),
                user.getName()
        );

        return LoginResponse.builder()
                .token(token)
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(String collegeId) {
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "User not found"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public void changePassword(String collegeId, ChangePasswordRequest request) {
        User user = userRepository.findByCollegeId(collegeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "Current password does not match"
            );
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "New password cannot be the same as old password"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}
