package com.utangnaloob.controller;

import com.utangnaloob.dto.ChangePasswordRequest;
import com.utangnaloob.dto.ForgotPasswordRequest;
import com.utangnaloob.dto.LoginRequest;
import com.utangnaloob.dto.MessageResponse;
import com.utangnaloob.dto.RegisterRequest;
import com.utangnaloob.dto.ResetPasswordRequest;
import com.utangnaloob.service.AuthService;
import com.utangnaloob.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(originPatterns = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        return authService.registerUser(registerRequest, clientIp);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        return authService.authenticateUser(loginRequest, clientIp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(Authentication authentication, HttpServletRequest request) {
        String email = authentication != null ? authentication.getName() : null;
        String clientIp = request.getRemoteAddr();
        return authService.logoutUser(email, clientIp);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new MessageResponse("User is not authenticated.", false));
        }
        return ResponseEntity.ok(userService.getUserProfileByEmail(authentication.getName()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        return authService.forgotPassword(forgotPasswordRequest, clientIp);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        return authService.resetPassword(resetPasswordRequest, clientIp);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest,
                                           Authentication authentication,
                                           HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new MessageResponse("User is not authenticated.", false));
        }
        String clientIp = request.getRemoteAddr();
        return authService.changePassword(authentication.getName(), changePasswordRequest, clientIp);
    }
}
