package com.utangnaloob.service;

import com.utangnaloob.config.JwtUtils;
import com.utangnaloob.config.UserDetailsImpl;
import com.utangnaloob.dto.AuthResponse;
import com.utangnaloob.dto.ChangePasswordRequest;
import com.utangnaloob.dto.ForgotPasswordRequest;
import com.utangnaloob.dto.ForgotPasswordResponse;
import com.utangnaloob.dto.LoginRequest;
import com.utangnaloob.dto.MessageResponse;
import com.utangnaloob.dto.RegisterRequest;
import com.utangnaloob.dto.ResetPasswordRequest;
import com.utangnaloob.entity.*;
import com.utangnaloob.repository.CreditLineRepository;
import com.utangnaloob.repository.KycProfileRepository;
import com.utangnaloob.repository.PasswordResetTokenRepository;
import com.utangnaloob.repository.RoleRepository;
import com.utangnaloob.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private KycProfileRepository kycProfileRepository;

    @Autowired
    private CreditLineRepository creditLineRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public ResponseEntity<?> registerUser(RegisterRequest registerRequest, String ipAddress) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: An account with email '" + registerRequest.getEmail() + "' already exists! Please log in or use a different email.", false));
        }

        // Create new user entity with securely hashed password
        User user = new User(
                registerRequest.getFirstName().trim(),
                registerRequest.getLastName().trim(),
                registerRequest.getEmail().trim().toLowerCase(),
                encoder.encode(registerRequest.getPassword()),
                registerRequest.getPhoneNumber(),
                registerRequest.getDateOfBirth(),
                registerRequest.getAddress(),
                registerRequest.getEmploymentStatus(),
                registerRequest.getMonthlyIncome()
        );

        // All public registrations strictly create standard consumer accounts (ROLE_BORROWER)
        Role borrowerRole = roleRepository.findByName(ERole.ROLE_BORROWER)
                .orElseThrow(() -> new RuntimeException("Error: Default ROLE_BORROWER is not initialized in the database."));
        Set<Role> roles = new HashSet<>();
        roles.add(borrowerRole);

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Initialize empty KYC Profile and Credit Line for borrower
        boolean isBorrower = roles.stream().anyMatch(r -> r.getName() == ERole.ROLE_BORROWER);
        if (isBorrower) {
            KycProfile kycProfile = new KycProfile(savedUser);
            kycProfileRepository.save(kycProfile);

            CreditLine creditLine = new CreditLine(savedUser, BigDecimal.ZERO);
            creditLineRepository.save(creditLine);
        }

        auditLogService.log(
                "USER_REGISTERED",
                savedUser.getEmail(),
                savedUser.getEmail(),
                "New user registered with roles: " + roles.stream().map(r -> r.getName().name()).collect(Collectors.joining(", ")),
                ipAddress
        );

        return ResponseEntity.ok(new MessageResponse("User account created successfully! You can now log in.", true));
    }

    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest, String ipAddress) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail().trim().toLowerCase(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        auditLogService.log(
                "USER_LOGIN",
                userDetails.getUsername(),
                userDetails.getUsername(),
                "Successful login authentication",
                ipAddress
        );

        return ResponseEntity.ok(new AuthResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getAccountStatus(),
                roles
        ));
    }

    public ResponseEntity<?> logoutUser(String email, String ipAddress) {
        SecurityContextHolder.clearContext();
        auditLogService.log(
                "USER_LOGOUT",
                email != null ? email : "Anonymous",
                email,
                "User logged out successfully and session terminated",
                ipAddress
        );
        return ResponseEntity.ok(new MessageResponse("User session terminated successfully.", true));
    }

    @Transactional
    public ResponseEntity<?> forgotPassword(ForgotPasswordRequest request, String ipAddress) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: No account found associated with email '" + request.getEmail() + "'. Please check the address or register.", false));
        }

        User user = userOpt.get();

        // Invalidate or remove existing active tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate a cryptographically secure, expiring single-use reset token
        String token = UUID.randomUUID().toString();
        int expiryMinutes = 15;
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryMinutes);
        passwordResetTokenRepository.save(resetToken);

        auditLogService.log(
                "PASSWORD_RESET_REQUESTED",
                user.getEmail(),
                user.getEmail(),
                "Password reset token generated (valid for " + expiryMinutes + " minutes)",
                ipAddress
        );

        return ResponseEntity.ok(new ForgotPasswordResponse(
                true,
                "A password reset token has been generated successfully. For development and evaluation, use the token provided below or proceed with the reset link.",
                token,
                expiryMinutes
        ));
    }

    @Transactional
    public ResponseEntity<?> resetPassword(ResetPasswordRequest request, String ipAddress) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: New password and confirmation password do not match.", false));
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(request.getToken().trim());
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Invalid or unrecognized password reset token.", false));
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isUsed()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: This password reset token has already been used. Please request a new one.", false));
        }

        if (resetToken.isExpired()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: This password reset token has expired. Tokens are valid for 15 minutes. Please request a new one.", false));
        }

        User user = resetToken.getUser();

        // Securely hash the new password using BCrypt
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate the token immediately so it cannot be reused (single-use token)
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        auditLogService.log(
                "PASSWORD_RESET_COMPLETED",
                user.getEmail(),
                user.getEmail(),
                "Password was successfully reset using single-use verification token",
                ipAddress
        );

        return ResponseEntity.ok(new MessageResponse("Your password has been reset successfully! You can now log in with your new password.", true));
    }

    @Transactional
    public ResponseEntity<?> changePassword(String email, ChangePasswordRequest request, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        // 1. Verify current password
        if (!encoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: The current password you entered is incorrect.", false));
        }

        // 2. Validate new password and confirmation match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: New password and confirmation password do not match.", false));
        }

        // 3. Prevent reusing the exact same password
        if (encoder.matches(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: New password cannot be identical to your current password. Please choose a different password.", false));
        }

        // 4. Securely hash and update
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log(
                "PASSWORD_CHANGED",
                user.getEmail(),
                user.getEmail(),
                "Authenticated user updated their account password",
                ipAddress
        );

        return ResponseEntity.ok(new MessageResponse("Your password has been changed successfully. Your previous password can no longer be used for authentication.", true));
    }
}
