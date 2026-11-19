package com.utangnaloob.controller;

import com.utangnaloob.dto.*;
import com.utangnaloob.entity.LoanDisbursement;
import com.utangnaloob.entity.PaymentTransaction;
import com.utangnaloob.service.KycService;
import com.utangnaloob.service.LendingService;
import com.utangnaloob.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(originPatterns = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/borrower")
@PreAuthorize("hasRole('BORROWER') or hasRole('ADMIN')")
public class BorrowerController {

    @Autowired
    private UserService userService;

    @Autowired
    private KycService kycService;

    @Autowired
    private LendingService lendingService;

    @GetMapping("/dashboard")
    public ResponseEntity<UserProfileResponse> getBorrowerDashboard(Authentication authentication) {
        UserProfileResponse profile = userService.getUserProfileByEmail(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/kyc/submit")
    public ResponseEntity<MessageResponse> submitKyc(
            @Valid @RequestBody KycSubmissionDto kycSubmissionDto,
            Authentication authentication,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        MessageResponse response = kycService.submitKyc(authentication.getName(), kycSubmissionDto, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/loans/request")
    public ResponseEntity<LoanDisbursement> requestLoan(
            @Valid @RequestBody LoanRequestDto requestDto,
            Authentication authentication,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        LoanDisbursement loan = lendingService.requestLoan(authentication.getName(), requestDto, clientIp);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/loans")
    public ResponseEntity<List<LoanDisbursement>> getMyLoans(Authentication authentication) {
        return ResponseEntity.ok(lendingService.getLoansByUser(authentication.getName()));
    }

    @PostMapping("/loans/pay")
    public ResponseEntity<PaymentReceiptResponse> payInstallment(
            @Valid @RequestBody PaymentRequestDto paymentDto,
            Authentication authentication,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        PaymentReceiptResponse receipt = lendingService.processPayment(authentication.getName(), paymentDto, clientIp);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/loans/payments")
    public ResponseEntity<List<PaymentTransaction>> getPaymentHistory(Authentication authentication) {
        return ResponseEntity.ok(lendingService.getPaymentHistory(authentication.getName()));
    }
}
