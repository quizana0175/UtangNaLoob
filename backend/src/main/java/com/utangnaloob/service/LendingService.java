package com.utangnaloob.service;

import com.utangnaloob.dto.LoanRequestDto;
import com.utangnaloob.dto.PaymentReceiptResponse;
import com.utangnaloob.dto.PaymentRequestDto;
import com.utangnaloob.entity.*;
import com.utangnaloob.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LendingService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycProfileRepository kycProfileRepository;

    @Autowired
    private CreditLineRepository creditLineRepository;

    @Autowired
    private LoanDisbursementRepository loanDisbursementRepository;

    @Autowired
    private AmortizationScheduleRepository amortizationScheduleRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private BrevoEmailService brevoEmailService;

    @Transactional
    public LoanDisbursement requestLoan(String borrowerEmail, LoanRequestDto requestDto, String ipAddress) {
        User user = userRepository.findByEmail(borrowerEmail)
                .orElseThrow(() -> new RuntimeException("Borrower not found: " + borrowerEmail));

        // 1. Verify KYC status
        KycProfile kyc = kycProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("KYC Profile not found. Please submit your verification documents."));

        if (!"VERIFIED".equalsIgnoreCase(kyc.getKycStatus())) {
            throw new IllegalStateException("Your KYC status is " + kyc.getKycStatus() + ". You must be VERIFIED by an underwriter before cash loan disbursements can be processed.");
        }

        // 2. Verify Account & Credit Line status (FR-026: suspend drawdowns if DELINQUENT)
        if ("DELINQUENT".equalsIgnoreCase(user.getAccountStatus())) {
            throw new IllegalStateException("Your account is flagged as DELINQUENT. Please settle all overdue balances and penalties before requesting a new loan.");
        }

        CreditLine creditLine = creditLineRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Credit Line not found for user."));

        if (!"ACTIVE".equalsIgnoreCase(creditLine.getStatus())) {
            throw new IllegalStateException("Your credit line is currently " + creditLine.getStatus() + ". Disbursements are disabled until overdue balances are settled.");
        }

        BigDecimal requestedAmount = requestDto.getAmount();
        BigDecimal availableBalance = creditLine.getAvailableBalance();

        BigDecimal minLoanAmount = systemParameterRepository.findByParamKey("MIN_LOAN_AMOUNT")
                .map(p -> {
                    try {
                        return new BigDecimal(p.getParamValue().trim());
                    } catch (Exception e) {
                        return BigDecimal.valueOf(1000.00);
                    }
                })
                .orElse(BigDecimal.valueOf(1000.00));

        if (requestedAmount.compareTo(minLoanAmount) < 0) {
            throw new IllegalArgumentException("Minimum cash loan disbursement is PHP " + minLoanAmount + ".");
        }

        if (requestedAmount.compareTo(availableBalance) > 0) {
            throw new IllegalArgumentException("Requested amount (PHP " + requestedAmount + ") exceeds your available revolving credit balance (PHP " + availableBalance + ").");
        }

        // 3. Compute Interest, Fees and Terms from System Parameters
        BigDecimal monthlyRate = systemParameterRepository.findByParamKey("MONTHLY_INTEREST_RATE")
                .map(p -> {
                    try {
                        return new BigDecimal(p.getParamValue().trim());
                    } catch (Exception e) {
                        return new BigDecimal("0.02");
                    }
                })
                .orElse(new BigDecimal("0.02"));

        int termMonths = requestDto.getInstallmentTermMonths() != null ? requestDto.getInstallmentTermMonths() : 3;
        String frequency = "BI_WEEKLY".equalsIgnoreCase(requestDto.getRepaymentFrequency()) ? "BI_WEEKLY" : "MONTHLY";

        BigDecimal totalInterest = requestedAmount.multiply(monthlyRate)
                .multiply(BigDecimal.valueOf(termMonths))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal processingFee = requestedAmount.multiply(new BigDecimal("0.015")).max(new BigDecimal("150.00"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalRepayment = requestedAmount.add(totalInterest).add(processingFee);

        // 4. Update Credit Line: deduct available balance, add drawn balance
        creditLine.setAvailableBalance(creditLine.getAvailableBalance().subtract(requestedAmount));
        creditLine.setDrawnBalance(creditLine.getDrawnBalance().add(requestedAmount));
        creditLine.setUpdatedAt(LocalDateTime.now());
        creditLineRepository.save(creditLine);

        // 5. Create Loan Disbursement
        LoanDisbursement loan = new LoanDisbursement();
        loan.setUser(user);
        loan.setPrincipalAmount(requestedAmount);
        loan.setInstallmentTermMonths(termMonths);
        loan.setRepaymentFrequency(frequency);
        loan.setMonthlyInterestRate(monthlyRate);
        loan.setTotalInterestAmount(totalInterest);
        loan.setProcessingFee(processingFee);
        loan.setTotalRepaymentAmount(totalRepayment);
        loan.setOutstandingBalance(totalRepayment);
        loan.setStatus("ACTIVE");
        loan.setLoanPurpose(requestDto.getLoanPurpose() != null ? requestDto.getLoanPurpose() : "Personal / Emergency Cash Advance");
        loan.setDisbursedAt(LocalDateTime.now());

        // 6. Generate Amortization Schedule
        int totalInstallments = "BI_WEEKLY".equals(frequency) ? termMonths * 2 : termMonths;
        BigDecimal installmentTotal = totalRepayment.divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);
        BigDecimal installmentPrincipal = requestedAmount.divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);
        BigDecimal installmentInterest = totalInterest.divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);

        LocalDate startDate = LocalDate.now();
        for (int i = 1; i <= totalInstallments; i++) {
            AmortizationSchedule schedule = new AmortizationSchedule();
            schedule.setInstallmentNumber(i);
            if ("BI_WEEKLY".equals(frequency)) {
                schedule.setDueDate(startDate.plusDays(i * 15L));
            } else {
                schedule.setDueDate(startDate.plusMonths(i));
            }
            schedule.setPrincipalDue(installmentPrincipal);
            schedule.setInterestDue(installmentInterest);
            schedule.setTotalDue(installmentTotal);
            schedule.setAmountPaid(BigDecimal.ZERO);
            schedule.setStatus("PENDING");
            loan.addInstallment(schedule);
        }

        LoanDisbursement savedLoan = loanDisbursementRepository.save(loan);

        auditLogService.log(
                "LOAN_DISBURSED",
                borrowerEmail,
                borrowerEmail,
                "Disbursed loan of PHP " + requestedAmount + " (" + frequency + ", " + termMonths + " mos). Remaining available limit: PHP " + creditLine.getAvailableBalance(),
                ipAddress
        );

        brevoEmailService.sendLoanDisbursalEmail(
                user.getEmail(),
                user.getFirstName(),
                savedLoan.getId(),
                requestedAmount,
                totalRepayment,
                frequency,
                totalInstallments
        );

        return savedLoan;
    }

    public List<LoanDisbursement> getLoansByUser(String borrowerEmail) {
        User user = userRepository.findByEmail(borrowerEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + borrowerEmail));
        return loanDisbursementRepository.findByUserIdOrderByDisbursedAtDesc(user.getId());
    }

    public List<LoanDisbursement> getAllLoans() {
        return loanDisbursementRepository.findAll();
    }

    @Transactional
    public PaymentReceiptResponse processPayment(String borrowerEmail, PaymentRequestDto dto, String ipAddress) {
        User user = userRepository.findByEmail(borrowerEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + borrowerEmail));

        LoanDisbursement loan = loanDisbursementRepository.findById(dto.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + dto.getLoanId()));

        if (!loan.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized payment attempt for another user's loan.");
        }

        String txRef = "UNL-PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return reconcilePayment(user, loan, dto.getInstallmentId(), dto.getAmount(), dto.getPaymentMethod(), txRef, ipAddress);
    }

    /**
     * FR-019/020/022: Idempotent webhook payment reconciliation using externalTransactionId.
     */
    @Transactional
    public PaymentReceiptResponse processWebhookPayment(
            String externalTransactionId,
            Long loanId,
            Long installmentId,
            BigDecimal amount,
            String paymentMethod,
            String ipAddress) {

        if (externalTransactionId == null || externalTransactionId.isBlank()) {
            throw new IllegalArgumentException("externalTransactionId is required.");
        }

        // Idempotency check (FR-022 / NFR-R2)
        if (paymentTransactionRepository.existsByTransactionReference(externalTransactionId)) {
            LoanDisbursement loan = loanDisbursementRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with id: " + loanId));
            CreditLine creditLine = creditLineRepository.findByUserId(loan.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Credit Line not found for user."));
            return new PaymentReceiptResponse(
                    externalTransactionId,
                    amount,
                    paymentMethod != null ? paymentMethod : "GATEWAY_WEBHOOK",
                    LocalDateTime.now(),
                    creditLine.getAvailableBalance(),
                    creditLine.getDrawnBalance(),
                    loan.getOutstandingBalance(),
                    "Duplicate webhook event ignored idempotently.",
                    true
            );
        }

        LoanDisbursement loan = loanDisbursementRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + loanId));
        return reconcilePayment(
                loan.getUser(),
                loan,
                installmentId,
                amount,
                paymentMethod != null ? paymentMethod : "PAYMONGO_WEBHOOK",
                externalTransactionId,
                ipAddress
        );
    }

    private PaymentReceiptResponse reconcilePayment(
            User user,
            LoanDisbursement loan,
            Long installmentId,
            BigDecimal paymentAmount,
            String paymentMethod,
            String txRef,
            String ipAddress) {

        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        CreditLine creditLine = creditLineRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Credit Line not found for user."));

        // If specific installment selected, mark it paid
        if (installmentId != null) {
            AmortizationSchedule installment = amortizationScheduleRepository.findById(installmentId)
                    .orElse(null);
            if (installment != null && installment.getLoan().getId().equals(loan.getId())) {
                BigDecimal updatedPaid = installment.getAmountPaid().add(paymentAmount);
                installment.setAmountPaid(updatedPaid);
                BigDecimal fullRequired = installment.getTotalDue().add(installment.getPenaltyFee());
                if (updatedPaid.compareTo(fullRequired) >= 0) {
                    installment.setStatus("PAID");
                    installment.setPaidAt(LocalDateTime.now());
                } else {
                    installment.setStatus("PARTIALLY_PAID");
                }
                amortizationScheduleRepository.save(installment);
            }
        }

        // Deduct from outstanding loan balance
        BigDecimal newOutstanding = loan.getOutstandingBalance().subtract(paymentAmount);
        if (newOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setOutstandingBalance(BigDecimal.ZERO);
            loan.setStatus("PAID");
        } else {
            loan.setOutstandingBalance(newOutstanding);
        }
        loanDisbursementRepository.save(loan);

        // REVOLVING CREDIT REPLENISHMENT:
        BigDecimal principalReplenished = paymentAmount.min(creditLine.getDrawnBalance());
        creditLine.setAvailableBalance(creditLine.getAvailableBalance().add(principalReplenished).min(creditLine.getApprovedCreditLimit()));
        creditLine.setDrawnBalance(creditLine.getDrawnBalance().subtract(principalReplenished).max(BigDecimal.ZERO));
        creditLine.setUpdatedAt(LocalDateTime.now());
        creditLineRepository.save(creditLine);

        // Record Payment Transaction
        PaymentTransaction tx = new PaymentTransaction(
                user,
                loan,
                installmentId,
                paymentAmount,
                paymentMethod,
                txRef
        );
        paymentTransactionRepository.save(tx);

        String borrowerEmail = user.getEmail();
        auditLogService.log(
                "PAYMENT_RECONCILED",
                borrowerEmail,
                borrowerEmail,
                "Digital repayment of PHP " + paymentAmount + " via " + paymentMethod + ". Replenished available credit limit to PHP " + creditLine.getAvailableBalance(),
                ipAddress
        );

        // FR-026: Restore ACTIVE status if borrower has no remaining OVERDUE installments
        if (("DELINQUENT".equalsIgnoreCase(user.getAccountStatus()) || "DELINQUENT".equalsIgnoreCase(creditLine.getStatus()))
                && !amortizationScheduleRepository.existsByLoanUserIdAndStatus(user.getId(), "OVERDUE")) {
            user.setAccountStatus("ACTIVE");
            userRepository.save(user);
            creditLine.setStatus("ACTIVE");
            creditLine.setUpdatedAt(LocalDateTime.now());
            creditLineRepository.save(creditLine);
            auditLogService.log(
                    "DELINQUENCY_CLEARED",
                    borrowerEmail,
                    borrowerEmail,
                    "All overdue installments settled. Borrower profile and credit line restored to ACTIVE.",
                    ipAddress
            );
        }

        // FR-013: Dynamic Credit Limit Adjustment Engine (upgrade after 2 consecutive on-time PAID loans)
        if ("PAID".equalsIgnoreCase(loan.getStatus())) {
            evaluateDynamicCreditLimitUpgrade(user, creditLine, ipAddress);
        }

        // FR-027: Automated transactional email receipt via Brevo
        brevoEmailService.sendPaymentReceiptEmail(
                user.getEmail(),
                user.getFirstName(),
                txRef,
                paymentAmount,
                paymentMethod,
                loan.getOutstandingBalance(),
                creditLine.getAvailableBalance()
        );

        return new PaymentReceiptResponse(
                txRef,
                paymentAmount,
                paymentMethod,
                LocalDateTime.now(),
                creditLine.getAvailableBalance(),
                creditLine.getDrawnBalance(),
                loan.getOutstandingBalance(),
                "Payment successfully reconciled! Your revolving credit limit has been instantly replenished by PHP " + principalReplenished + ".",
                true
        );
    }

    /**
     * FR-013: Automatically increases a borrower's assigned credit limit by CREDIT_LIMIT_UPGRADE_PERCENT
     * (default 10%) upon the successful, on-time completion of two consecutive installment loans.
     */
    private void evaluateDynamicCreditLimitUpgrade(User user, CreditLine creditLine, String ipAddress) {
        List<LoanDisbursement> userLoans = loanDisbursementRepository.findByUserIdOrderByDisbursedAtDesc(user.getId());
        List<LoanDisbursement> paidLoans = userLoans.stream()
                .filter(l -> "PAID".equalsIgnoreCase(l.getStatus()))
                .toList();

        if (paidLoans.size() < 2 || paidLoans.size() % 2 != 0) {
            return;
        }

        boolean lastTwoOnTime = paidLoans.subList(0, 2).stream().allMatch(l ->
                l.getInstallments().stream().noneMatch(s ->
                        "OVERDUE".equalsIgnoreCase(s.getStatus())
                                || (s.getPenaltyFee() != null && s.getPenaltyFee().compareTo(BigDecimal.ZERO) > 0)
                )
        );
        if (!lastTwoOnTime) {
            return;
        }

        BigDecimal upgradePct = systemParameterRepository.findByParamKey("CREDIT_LIMIT_UPGRADE_PERCENT")
                .map(p -> {
                    try { return new BigDecimal(p.getParamValue().trim()); }
                    catch (Exception e) { return new BigDecimal("0.10"); }
                }).orElse(new BigDecimal("0.10"));

        BigDecimal oldLimit = creditLine.getApprovedCreditLimit();
        BigDecimal increase = oldLimit.multiply(upgradePct);
        // Round increase to nearest PHP 500.00 (minimum PHP 500.00)
        BigDecimal roundedIncrease = BigDecimal.valueOf(Math.max(500.0, Math.round(increase.doubleValue() / 500.0) * 500.0))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal newLimit = oldLimit.add(roundedIncrease);

        creditLine.setApprovedCreditLimit(newLimit);
        creditLine.setAvailableBalance(creditLine.getAvailableBalance().add(roundedIncrease));
        creditLine.setUpdatedAt(LocalDateTime.now());
        creditLineRepository.save(creditLine);

        auditLogService.log(
                "CREDIT_LIMIT_AUTO_UPGRADED",
                "SYSTEM_ENGINE",
                user.getEmail(),
                "Auto-upgraded credit limit from PHP " + oldLimit + " to PHP " + newLimit + " after 2 consecutive on-time paid loans.",
                ipAddress
        );

        brevoEmailService.sendCreditLimitUpgradeEmail(user.getEmail(), user.getFirstName(), oldLimit, newLimit);
    }

    public List<PaymentTransaction> getPaymentHistory(String borrowerEmail) {
        User user = userRepository.findByEmail(borrowerEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + borrowerEmail));
        return paymentTransactionRepository.findByUserIdOrderByPaidAtDesc(user.getId());
    }

    public Map<String, Object> getLoanConfig() {
        BigDecimal monthlyRate = systemParameterRepository.findByParamKey("MONTHLY_INTEREST_RATE")
                .map(p -> {
                    try { return new BigDecimal(p.getParamValue().trim()); }
                    catch (Exception e) { return new BigDecimal("0.02"); }
                }).orElse(new BigDecimal("0.02"));

        BigDecimal minLoanAmount = systemParameterRepository.findByParamKey("MIN_LOAN_AMOUNT")
                .map(p -> {
                    try { return new BigDecimal(p.getParamValue().trim()); }
                    catch (Exception e) { return new BigDecimal("1000.00"); }
                }).orElse(new BigDecimal("1000.00"));

        int gracePeriodDays = systemParameterRepository.findByParamKey("GRACE_PERIOD_DAYS")
                .map(p -> {
                    try { return Integer.parseInt(p.getParamValue().trim()); }
                    catch (Exception e) { return 3; }
                }).orElse(3);

        BigDecimal dailyPenaltyRate = systemParameterRepository.findByParamKey("DAILY_LATE_PENALTY_RATE")
                .map(p -> {
                    try { return new BigDecimal(p.getParamValue().trim()); }
                    catch (Exception e) { return new BigDecimal("0.005"); }
                }).orElse(new BigDecimal("0.005"));

        Map<String, Object> cfg = new HashMap<>();
        cfg.put("monthlyInterestRate", monthlyRate);
        cfg.put("minLoanAmount", minLoanAmount);
        cfg.put("gracePeriodDays", gracePeriodDays);
        cfg.put("dailyLatePenaltyRate", dailyPenaltyRate);
        return cfg;
    }
}
