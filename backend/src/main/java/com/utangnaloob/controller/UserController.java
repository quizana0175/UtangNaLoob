package com.utangnaloob.controller;

import com.utangnaloob.dto.ChangePasswordRequest;
import com.utangnaloob.dto.MessageResponse;
import com.utangnaloob.dto.UpdateProfileRequest;
import com.utangnaloob.dto.UserProfileResponse;
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
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserProfileByEmail(authentication.getName()));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateUserProfile(@Valid @RequestBody UpdateProfileRequest updateRequest,
                                                                 Authentication authentication,
                                                                 HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        return ResponseEntity.ok(userService.updateUserProfile(authentication.getName(), updateRequest, clientIp));
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
