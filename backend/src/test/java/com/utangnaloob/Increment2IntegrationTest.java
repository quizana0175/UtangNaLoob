package com.utangnaloob;

import com.utangnaloob.dto.*;
import com.utangnaloob.entity.PasswordResetToken;
import com.utangnaloob.entity.User;
import com.utangnaloob.repository.PasswordResetTokenRepository;
import com.utangnaloob.repository.UserRepository;
import com.utangnaloob.service.AuthService;
import com.utangnaloob.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public class Increment2IntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "borrower@utangnaloob.ph";
    private static final String ORIGINAL_PASS = "Borrower@123";

    @BeforeEach
    public void setup() {
        Optional<User> userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertTrue(userOpt.isPresent(), "Seed borrower account must exist");
    }

    @Test
    @DisplayName("Requirement 1: Authenticated User Profile Management - View & Edit Profile")
    public void testUserProfileViewAndEdit() {
        UserProfileResponse profileBefore = userService.getUserProfileByEmail(TEST_EMAIL);
        assertNotNull(profileBefore);
        assertEquals(TEST_EMAIL, profileBefore.getEmail());

        UpdateProfileRequest updateReq = new UpdateProfileRequest(
                "Juan Updated",
                "Dela Cruz",
                "09189876543",
                "1994-05-15",
                "123 Ayala Avenue, Makati City",
                "SELF_EMPLOYED",
                45000.0
        );

        UserProfileResponse profileAfter = userService.updateUserProfile(TEST_EMAIL, updateReq, "127.0.0.1");

        assertEquals("Juan Updated", profileAfter.getFirstName());
        assertEquals("Dela Cruz", profileAfter.getLastName());
        assertEquals("09189876543", profileAfter.getPhoneNumber());
        assertEquals("123 Ayala Avenue, Makati City", profileAfter.getAddress());
        assertEquals("SELF_EMPLOYED", profileAfter.getEmploymentStatus());
        assertEquals(45000.0, profileAfter.getMonthlyIncome());

        // Verify in database
        User dbUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertEquals("Juan Updated", dbUser.getFirstName());
        assertEquals(45000.0, dbUser.getMonthlyIncome());
    }

    @Test
    @DisplayName("Requirement 2: Forgot Password & Single-Use Reset Token Lifecycle")
    public void testForgotPasswordAndResetPassword() {
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest(TEST_EMAIL);
        ResponseEntity<?> forgotRes = authService.forgotPassword(forgotReq, "127.0.0.1");
        assertEquals(200, forgotRes.getStatusCode().value());
        assertTrue(forgotRes.getBody() instanceof ForgotPasswordResponse);

        ForgotPasswordResponse tokenData = (ForgotPasswordResponse) forgotRes.getBody();
        String token = tokenData.getToken();
        assertNotNull(token);

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        assertTrue(tokenOpt.isPresent());
        assertFalse(tokenOpt.get().isUsed());
        assertFalse(tokenOpt.get().isExpired());

        // Reset password with mismatched confirmation should fail
        ResetPasswordRequest mismatchReq = new ResetPasswordRequest(token, "NewSecret@999", "MismatchSecret@999");
        ResponseEntity<?> mismatchRes = authService.resetPassword(mismatchReq, "127.0.0.1");
        assertEquals(400, mismatchRes.getStatusCode().value());

        // Reset password with valid credentials
        ResetPasswordRequest validReset = new ResetPasswordRequest(token, "NewSecurePass@2026", "NewSecurePass@2026");
        ResponseEntity<?> resetRes = authService.resetPassword(validReset, "127.0.0.1");
        assertEquals(200, resetRes.getStatusCode().value());

        // Verify user password changed in database
        User updatedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecurePass@2026", updatedUser.getPassword()));
        assertFalse(passwordEncoder.matches(ORIGINAL_PASS, updatedUser.getPassword()));

        // Single-use guarantee: attempt to reuse the same token must fail
        ResetPasswordRequest reuseReq = new ResetPasswordRequest(token, "AnotherPass@333", "AnotherPass@333");
        ResponseEntity<?> reuseRes = authService.resetPassword(reuseReq, "127.0.0.1");
        assertEquals(400, reuseRes.getStatusCode().value());
        assertTrue(((MessageResponse) reuseRes.getBody()).getMessage().contains("already been used"));
    }

    @Test
    @DisplayName("Requirement 3: Authenticated Change Password & Current Password Verification")
    public void testChangePasswordFlow() {
        // 1. Invalid current password must be rejected
        ChangePasswordRequest wrongCurrentReq = new ChangePasswordRequest(
                "WrongCurrentPassword@999",
                "BrandNewPass@456",
                "BrandNewPass@456"
        );
        ResponseEntity<?> wrongRes = authService.changePassword(TEST_EMAIL, wrongCurrentReq, "127.0.0.1");
        assertEquals(400, wrongRes.getStatusCode().value());
        assertTrue(((MessageResponse) wrongRes.getBody()).getMessage().contains("current password you entered is incorrect"));

        // 2. Mismatched new password must be rejected
        ChangePasswordRequest mismatchReq = new ChangePasswordRequest(
                ORIGINAL_PASS,
                "BrandNewPass@456",
                "DifferentPass@789"
        );
        ResponseEntity<?> mismatchRes = authService.changePassword(TEST_EMAIL, mismatchReq, "127.0.0.1");
        assertEquals(400, mismatchRes.getStatusCode().value());

        // 3. New password identical to current password must be rejected
        ChangePasswordRequest identicalReq = new ChangePasswordRequest(
                ORIGINAL_PASS,
                ORIGINAL_PASS,
                ORIGINAL_PASS
        );
        ResponseEntity<?> identicalRes = authService.changePassword(TEST_EMAIL, identicalReq, "127.0.0.1");
        assertEquals(400, identicalRes.getStatusCode().value());
        assertTrue(identicalRes.getBody().toString().contains("cannot be identical"));

        // 4. Valid password change succeeds
        ChangePasswordRequest validChange = new ChangePasswordRequest(
                ORIGINAL_PASS,
                "UpdatedPassword@789",
                "UpdatedPassword@789"
        );
        ResponseEntity<?> validRes = authService.changePassword(TEST_EMAIL, validChange, "127.0.0.1");
        assertEquals(200, validRes.getStatusCode().value());

        // 5. Verify old password no longer works and new password works
        User userInDb = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertTrue(passwordEncoder.matches("UpdatedPassword@789", userInDb.getPassword()));
        assertFalse(passwordEncoder.matches(ORIGINAL_PASS, userInDb.getPassword()));
    }
}
