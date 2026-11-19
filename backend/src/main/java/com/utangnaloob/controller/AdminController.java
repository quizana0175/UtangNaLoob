package com.utangnaloob.controller;

import com.utangnaloob.dto.*;
import com.utangnaloob.service.AdminService;
import com.utangnaloob.service.AuditLogService;
import com.utangnaloob.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(originPatterns = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getPortfolioMetrics() {
        return ResponseEntity.ok(adminService.getPortfolioMetrics());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<UserDto> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody RoleUpdateRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String clientIp = servletRequest.getRemoteAddr();
        UserDto updated = userService.updateUserRoles(userId, request, authentication.getName(), clientIp);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/users/{userId}/toggle-status")
    public ResponseEntity<UserDto> toggleUserStatus(
            @PathVariable Long userId,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String clientIp = servletRequest.getRemoteAddr();
        UserDto updated = userService.toggleUserStatus(userId, authentication.getName(), clientIp);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/parameters")
    public ResponseEntity<List<SystemParameterDto>> getSystemParameters() {
        return ResponseEntity.ok(adminService.getAllSystemParameters());
    }

    @PutMapping("/parameters/{paramKey}")
    public ResponseEntity<SystemParameterDto> updateParameter(
            @PathVariable String paramKey,
            @RequestBody Map<String, String> body,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String newValue = body.get("paramValue");
        if (newValue == null || newValue.isBlank()) {
            throw new IllegalArgumentException("Parameter value cannot be blank");
        }
        String clientIp = servletRequest.getRemoteAddr();
        SystemParameterDto updated = adminService.updateParameter(paramKey, newValue, authentication.getName(), clientIp);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogDto>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getRecentLogs());
    }
}
