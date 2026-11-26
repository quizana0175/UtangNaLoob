package com.utangnaloob.controller;

import com.utangnaloob.dto.AuditLogDto;
import com.utangnaloob.dto.KycReviewDto;
import com.utangnaloob.dto.MessageResponse;
import com.utangnaloob.dto.UserProfileResponse;
import com.utangnaloob.service.AuditLogService;
import com.utangnaloob.service.DelinquencyService;
import com.utangnaloob.service.KycService;
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
@RequestMapping("/api/underwriter")
@PreAuthorize("hasRole('UNDERWRITER') or hasRole('ADMIN')")
public class UnderwriterController {

    @Autowired
    private KycService kycService;

    @Autowired
    private DelinquencyService delinquencyService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/queue")
    public ResponseEntity<List<UserProfileResponse>> getPendingKycQueue() {
        return ResponseEntity.ok(kycService.getPendingKycQueue());
    }

    @PostMapping("/review/{userId}")
    public ResponseEntity<MessageResponse> reviewKyc(
            @PathVariable Long userId,
            @Valid @RequestBody KycReviewDto reviewDto,
            Authentication authentication,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        MessageResponse response = kycService.reviewKyc(userId, reviewDto, authentication.getName(), clientIp);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/delinquencies")
    public ResponseEntity<List<Map<String, Object>>> getDelinquentAccounts() {
        return ResponseEntity.ok(delinquencyService.getDelinquentAccounts());
    }

    @GetMapping("/penalty-logs")
    public ResponseEntity<List<AuditLogDto>> getPenaltyLogs() {
        return ResponseEntity.ok(auditLogService.getLogsByAction("PENALTY_APPLIED"));
    }

    @PostMapping("/delinquencies/run-check")
    public ResponseEntity<Map<String, Object>> runDelinquencyCheck() {
        return ResponseEntity.ok(delinquencyService.evaluateOverdueAndPenalties());
    }
}
