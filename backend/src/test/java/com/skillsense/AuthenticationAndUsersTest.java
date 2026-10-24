package com.skillsense;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsense.auth.LoginLockoutService;
import com.skillsense.auth.dto.ChangePasswordRequest;
import com.skillsense.auth.dto.LoginRequest;
import com.skillsense.term.AcademicTerm;
import com.skillsense.term.AcademicTermRepository;
import com.skillsense.term.dto.TermRequest;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import com.skillsense.user.dto.CreateUserRequest;
import com.skillsense.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationAndUsersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AcademicTermRepository termRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginLockoutService lockoutService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private User adminUser;
    private User studentUser;

    @BeforeEach
    void setUp() {
        lockoutService.clear();
        jdbcTemplate.execute("DELETE FROM student_results");
        jdbcTemplate.execute("DELETE FROM analytics_cache");
        jdbcTemplate.execute("DELETE FROM responses");
        jdbcTemplate.execute("DELETE FROM participation_log");
        jdbcTemplate.execute("DELETE FROM surveys");
        jdbcTemplate.execute("DELETE FROM survey_templates");
        userRepository.deleteAll();
        termRepository.deleteAll();

        // Create an admin user
        adminUser = userRepository.save(User.builder()
                .collegeId("admin_test")
                .name("Test Admin")
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("Admin@1234"))
                .role(Role.ADMIN)
                .isActive(true)
                .mustChangePassword(false)
                .build());

        // Create a student user
        studentUser = userRepository.save(User.builder()
                .collegeId("student_test")
                .name("Test Student")
                .email("student@test.com")
                .passwordHash(passwordEncoder.encode("Student@1234"))
                .role(Role.STUDENT)
                .branch("CSE")
                .studyYear(3)
                .section("B")
                .isActive(true)
                .mustChangePassword(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        lockoutService.clear();
        userRepository.deleteAll();
        termRepository.deleteAll();
    }

    private String obtainToken(String collegeId, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .collegeId(collegeId)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    @Test
    @DisplayName("Password hashing: Passwords are BCrypt-hashed and plaintext is not persisted")
    void testPasswordIsBcryptHashed() {
        User user = userRepository.findByCollegeId("admin_test").orElseThrow();
        assertNotEquals("Admin@1234", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2a$10$") || user.getPasswordHash().startsWith("$2b$10$"),
                "Password must be hashed with BCrypt strength 10");
        assertTrue(passwordEncoder.matches("Admin@1234", user.getPasswordHash()));
    }

    @Test
    @DisplayName("Login success: Valid credentials return token and user without password hash")
    void testLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .collegeId("admin_test")
                .password("Admin@1234")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.collegeId").value("admin_test"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("Login failure: Invalid password returns 401 UNAUTHENTICATED")
    void testLoginFailureInvalidPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .collegeId("admin_test")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Login failure: Inactive/disabled account returns 403 FORBIDDEN")
    void testLoginInactiveAccount() throws Exception {
        studentUser.setIsActive(false);
        userRepository.save(studentUser);

        LoginRequest request = LoginRequest.builder()
                .collegeId("student_test")
                .password("Student@1234")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Account lockout: 5 failed attempts lock the account for 5 minutes")
    void testAccountLockoutAfterFiveFailedAttempts() throws Exception {
        LoginRequest badRequest = LoginRequest.builder()
                .collegeId("student_test")
                .password("WrongPassword1")
                .build();

        // 4 failed attempts: returns UNAUTHENTICATED
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        // 5th failed attempt: triggers account lockout
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        // Even with the correct password, account remains locked
        LoginRequest correctRequest = LoginRequest.builder()
                .collegeId("student_test")
                .password("Student@1234")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    @DisplayName("Current user endpoint: GET /api/auth/me returns authenticated user details")
    void testGetMe() throws Exception {
        String token = obtainToken("admin_test", "Admin@1234");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collegeId").value("admin_test"))
                .andExpect(jsonPath("$.name").value("Test Admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("JWT validation: Missing or invalid token is rejected with 401")
    void testInvalidJwtRejected() throws Exception {
        // No header
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        // Malformed token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer bad.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Change password: Enforces minimum 8 chars with letter and digit")
    void testChangePassword() throws Exception {
        String token = obtainToken("student_test", "Student@1234");

        // Fails if less than 8 chars
        ChangePasswordRequest shortPwd = ChangePasswordRequest.builder()
                .oldPassword("Student@1234")
                .newPassword("Short1")
                .build();
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPwd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // Fails if no digit
        ChangePasswordRequest noDigit = ChangePasswordRequest.builder()
                .oldPassword("Student@1234")
                .newPassword("OnlyLettersPassword")
                .build();
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noDigit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // Valid change password succeeds
        ChangePasswordRequest validPwd = ChangePasswordRequest.builder()
                .oldPassword("Student@1234")
                .newPassword("NewValidPass2026")
                .build();
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPwd)))
                .andExpect(status().isOk());

        // Verify mustChangePassword is now false
        User updatedUser = userRepository.findByCollegeId("student_test").orElseThrow();
        assertFalse(updatedUser.getMustChangePassword());

        // Verify login with new password works
        obtainToken("student_test", "NewValidPass2026");
    }

    @Test
    @DisplayName("Role authorization: STUDENT calling /api/users gets 403, ADMIN succeeds")
    void testRoleAuthorization() throws Exception {
        String studentToken = obtainToken("student_test", "Student@1234");

        // Student accessing admin-only endpoint gets 403 FORBIDDEN
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Admin accessing gets 200 OK
        String adminToken = obtainToken("admin_test", "Admin@1234");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("User CRUD: Admin can create and update users, and reset passwords")
    void testUserCrudAndResetPassword() throws Exception {
        String adminToken = obtainToken("admin_test", "Admin@1234");

        // Create user
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .collegeId("FAC009")
                .name("Faculty Nine")
                .email("fac009@test.com")
                .role(Role.FACULTY)
                .branch("ECE")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.collegeId").value("FAC009"))
                .andExpect(jsonPath("$.temporaryPassword").isString())
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long createdUserId = createNode.get("user").get("userId").asLong();
        String tempPassword = createNode.get("temporaryPassword").asText();
        assertNotNull(tempPassword);

        // Verify newly created user can log in with temporary password
        obtainToken("FAC009", tempPassword);

        // Update user
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .name("Faculty Nine Updated")
                .email("fac009_new@test.com")
                .branch("EEE")
                .isActive(true)
                .build();

        mockMvc.perform(put("/api/users/" + createdUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Faculty Nine Updated"))
                .andExpect(jsonPath("$.branch").value("EEE"));

        // Reset password
        MvcResult resetResult = mockMvc.perform(post("/api/users/" + createdUserId + "/reset-password")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").isString())
                .andReturn();

        String resetTempPassword = objectMapper.readTree(resetResult.getResponse().getContentAsString())
                .get("temporaryPassword").asText();
        assertNotNull(resetTempPassword);

        // Verify login works with the reset temporary password
        obtainToken("FAC009", resetTempPassword);
    }

    @Test
    @DisplayName("CSV import: One valid, one duplicate, one invalid returns created, skipped, errors and credentials CSV")
    void testCsvImport() throws Exception {
        String adminToken = obtainToken("admin_test", "Admin@1234");

        // CSV content:
        // row 1: header
        // row 2: valid student 21CSE099
        // row 3: duplicate student_test (already exists)
        // row 4: invalid role UNKNOWN_ROLE
        String csvContent = "college_id,name,email,role,branch,study_year,section\n" +
                "21CSE099,Alice Smith,alice@test.com,STUDENT,CSE,3,A\n" +
                "student_test,Duplicate Student,dup@test.com,STUDENT,CSE,3,A\n" +
                "21IT088,Bob Jones,bob@test.com,INVALID_ROLE,IT,2,B\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/users/import")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.skipped").value(2))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.credentials").isString())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(2, responseNode.get("errors").size());
        assertTrue(responseNode.get("credentials").asText().contains("21CSE099"));

        // Verify the newly imported student exists in database and can log in
        User imported = userRepository.findByCollegeId("21CSE099").orElse(null);
        assertNotNull(imported);
        assertEquals("Alice Smith", imported.getName());
    }

    @Test
    @DisplayName("Terms endpoints: GET /api/terms, POST /api/terms, PUT /api/terms/{id}/current")
    void testTermsEndpoints() throws Exception {
        String adminToken = obtainToken("admin_test", "Admin@1234");
        String studentToken = obtainToken("student_test", "Student@1234");

        // 1. Create a term as Admin
        TermRequest termRequest1 = TermRequest.builder()
                .label("2025-26 Odd")
                .startDate(LocalDate.of(2025, 8, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .isCurrent(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/terms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(termRequest1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("2025-26 Odd"))
                .andReturn();

        Integer termId1 = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("termId").asInt();

        // 2. Student cannot create a term (403 FORBIDDEN)
        TermRequest termRequest2 = TermRequest.builder()
                .label("2026-27 Odd")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .isCurrent(true)
                .build();

        mockMvc.perform(post("/api/terms")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(termRequest2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // 3. Admin creates second term as current
        MvcResult createResult2 = mockMvc.perform(post("/api/terms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(termRequest2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isCurrent").value(true))
                .andReturn();

        Integer termId2 = objectMapper.readTree(createResult2.getResponse().getContentAsString())
                .get("termId").asInt();

        // 4. Student can view terms
        mockMvc.perform(get("/api/terms")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 5. Admin switches current term to termId1
        mockMvc.perform(put("/api/terms/" + termId1 + "/current")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termId").value(termId1))
                .andExpect(jsonPath("$.isCurrent").value(true));

        // Verify in DB that exactly one term is current
        AcademicTerm term1 = termRepository.findById(termId1).orElseThrow();
        AcademicTerm term2 = termRepository.findById(termId2).orElseThrow();
        assertTrue(term1.getIsCurrent());
        assertFalse(term2.getIsCurrent());
    }
}
