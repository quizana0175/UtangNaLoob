package com.utangnaloob.controller;

import com.utangnaloob.dto.LoginRequest;
import com.utangnaloob.dto.MessageResponse;
import com.utangnaloob.dto.RegisterRequest;
import com.utangnaloob.service.AuthService;
import com.utangnaloob.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
}
