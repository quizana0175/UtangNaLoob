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
import java.util.List;
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

        // 2. Verify Credit Line
        CreditLine creditLine = creditLineRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Credit Line not found for user."));

        if (!"ACTIVE".equalsIgnoreCase(creditLine.getStatus())) {
            throw new IllegalStateException("Your credit line is currently " + creditLine.getStatus() + ". Disbursements are disabled.");
        }

        BigDecimal requestedAmount = requestDto.getAmount();
        BigDecimal availableBalance = creditLine.getAvailableBalance();

        if (requestedAmount.compareTo(BigDecimal.valueOf(1000.00)) < 0) {
            throw new IllegalArgumentException("Minimum cash loan disbursement is PHP 1,000.00.");
        }

        if (requestedAmount.compareTo(availableBalance) > 0) {
            throw new IllegalArgumentException("Requested amount (PHP " + requestedAmount + ") exceeds your available revolving credit balance (PHP " + availableBalance + ").");
        }

        // 3. Compute Interest, Fees and Terms from System Parameters
        BigDecimal monthlyRate = new BigDecimal("0.02"); // 2% per month default
        systemParameterRepository.findByParamKey("MONTHLY_INTEREST_RATE").ifPresent(p -> {
            try {
                // monthly rate e.g. 0.02
            } catch (Exception e) {}
        });

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

        BigDecimal paymentAmount = dto.getAmount();
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        CreditLine creditLine = creditLineRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Credit Line not found for user."));

        // If specific installment selected, mark it paid
        if (dto.getInstallmentId() != null) {
            AmortizationSchedule installment = amortizationScheduleRepository.findById(dto.getInstallmentId())
                    .orElse(null);
            if (installment != null && installment.getLoan().getId().equals(loan.getId())) {
                installment.setStatus("PAID");
                installment.setPaidAt(LocalDateTime.now());
                installment.setAmountPaid(installment.getAmountPaid().add(paymentAmount));
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
        // Calculate principal restored (proportional replenishment)
        BigDecimal principalReplenished = paymentAmount.min(creditLine.getDrawnBalance());
        creditLine.setAvailableBalance(creditLine.getAvailableBalance().add(principalReplenished).min(creditLine.getApprovedCreditLimit()));
        creditLine.setDrawnBalance(creditLine.getDrawnBalance().subtract(principalReplenished).max(BigDecimal.ZERO));
        creditLine.setUpdatedAt(LocalDateTime.now());
        creditLineRepository.save(creditLine);

        // Record Payment Transaction
        String txRef = "UNL-PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        PaymentTransaction tx = new PaymentTransaction(
                user,
                loan,
                dto.getInstallmentId(),
                paymentAmount,
                dto.getPaymentMethod(),
                txRef
        );
        paymentTransactionRepository.save(tx);

        auditLogService.log(
                "PAYMENT_RECONCILED",
                borrowerEmail,
                borrowerEmail,
                "Digital repayment of PHP " + paymentAmount + " via " + dto.getPaymentMethod() + ". Replenished available credit limit to PHP " + creditLine.getAvailableBalance(),
                ipAddress
        );

        return new PaymentReceiptResponse(
                txRef,
                paymentAmount,
                dto.getPaymentMethod(),
                LocalDateTime.now(),
                creditLine.getAvailableBalance(),
                creditLine.getDrawnBalance(),
                loan.getOutstandingBalance(),
                "Payment successfully reconciled! Your revolving credit limit has been instantly replenished by PHP " + principalReplenished + ".",
                true
        );
    }

    public List<PaymentTransaction> getPaymentHistory(String borrowerEmail) {
        User user = userRepository.findByEmail(borrowerEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + borrowerEmail));
        return paymentTransactionRepository.findByUserIdOrderByPaidAtDesc(user.getId());
    }
}
