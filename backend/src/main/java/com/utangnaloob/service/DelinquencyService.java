package com.utangnaloob.service;

import com.utangnaloob.entity.AmortizationSchedule;
import com.utangnaloob.entity.CreditLine;
import com.utangnaloob.entity.LoanDisbursement;
import com.utangnaloob.entity.User;
import com.utangnaloob.repository.AmortizationScheduleRepository;
import com.utangnaloob.repository.CreditLineRepository;
import com.utangnaloob.repository.LoanDisbursementRepository;
import com.utangnaloob.repository.SystemParameterRepository;
import com.utangnaloob.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DelinquencyService {

    @Autowired
    private AmortizationScheduleRepository amortizationScheduleRepository;

    @Autowired
    private LoanDisbursementRepository loanDisbursementRepository;

    @Autowired
    private CreditLineRepository creditLineRepository;

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private BrevoEmailService brevoEmailService;

    private int getGracePeriodDays() {
        return systemParameterRepository.findByParamKey("GRACE_PERIOD_DAYS")
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getParamValue().trim());
                    } catch (Exception e) {
                        return 3;
                    }
                })
                .orElse(3);
    }

    private BigDecimal getDailyPenaltyRate() {
        return systemParameterRepository.findByParamKey("DAILY_LATE_PENALTY_RATE")
                .map(p -> {
                    try {
                        return new BigDecimal(p.getParamValue().trim());
                    } catch (Exception e) {
                        return new BigDecimal("0.005");
                    }
                })
                .orElse(new BigDecimal("0.005"));
    }

    /**
     * FR-023/024/025/029: Daily scheduled cron (00:01:00 UTC+8) that evaluates unpaid installments
     * past their due date + grace period, marks them OVERDUE, flags borrower & credit line DELINQUENT,
     * applies late penalties, and dispatches urgent overdue email alerts.
     */
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Manila")
    @Transactional
    public Map<String, Object> evaluateOverdueAndPenalties() {
        int graceDays = getGracePeriodDays();
        BigDecimal dailyRate = getDailyPenaltyRate();
        LocalDate today = LocalDate.now();

        List<AmortizationSchedule> candidates = amortizationScheduleRepository
                .findByStatusInAndDueDateBefore(Arrays.asList("PENDING", "OVERDUE"), today);

        int markedOverdueCount = 0;
        int penaltiesAppliedCount = 0;

        for (AmortizationSchedule inst : candidates) {
            long daysPastDue = ChronoUnit.DAYS.between(inst.getDueDate(), today);
            if (daysPastDue <= graceDays) {
                continue;
            }

            long penaltyDays = daysPastDue - graceDays;
            BigDecimal computedPenalty = inst.getTotalDue()
                    .multiply(dailyRate)
                    .multiply(BigDecimal.valueOf(penaltyDays))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal existingPenalty = inst.getPenaltyFee();
            boolean statusChanged = !"OVERDUE".equals(inst.getStatus());

            LoanDisbursement loan = inst.getLoan();
            User borrower = loan.getUser();

            if (statusChanged) {
                inst.setStatus("OVERDUE");
                markedOverdueCount++;

                // FR-025: Flag borrower profile and credit line as DELINQUENT
                if (!"DELINQUENT".equalsIgnoreCase(borrower.getAccountStatus())) {
                    borrower.setAccountStatus("DELINQUENT");
                    userRepository.save(borrower);
                }
                creditLineRepository.findByUserId(borrower.getId()).ifPresent(cl -> {
                    if (!"DELINQUENT".equalsIgnoreCase(cl.getStatus())) {
                        cl.setStatus("DELINQUENT");
                        cl.setUpdatedAt(LocalDateTime.now());
                        creditLineRepository.save(cl);
                    }
                });
            }

            if (computedPenalty.compareTo(existingPenalty) > 0) {
                BigDecimal delta = computedPenalty.subtract(existingPenalty);
                inst.setPenaltyFee(computedPenalty);

                loan.setOutstandingBalance(loan.getOutstandingBalance().add(delta));
                loanDisbursementRepository.save(loan);

                penaltiesAppliedCount++;
                auditLogService.log(
                        "PENALTY_APPLIED",
                        "SYSTEM_SCHEDULER",
                        borrower.getEmail(),
                        "Overdue penalty of PHP " + computedPenalty + " (+PHP " + delta + ") applied to Loan #" + loan.getId() + " Installment #" + inst.getInstallmentNumber() + " (" + daysPastDue + " days past due)",
                        "127.0.0.1"
                );
            }

            if (statusChanged) {
                // FR-029: Urgent overdue email alert
                brevoEmailService.sendOverdueAlertEmail(
                        borrower.getEmail(),
                        borrower.getFirstName(),
                        loan.getId(),
                        inst.getInstallmentNumber(),
                        inst.getDueDate(),
                        inst.getTotalDue(),
                        inst.getPenaltyFee()
                );
            }

            amortizationScheduleRepository.save(inst);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("evaluatedAt", LocalDate.now().toString());
        summary.put("gracePeriodDays", graceDays);
        summary.put("dailyPenaltyRate", dailyRate);
        summary.put("markedOverdue", markedOverdueCount);
        summary.put("penaltiesApplied", penaltiesAppliedCount);
        return summary;
    }

    /**
     * FR-028: Daily notification routine sending due-date reminder emails 3 days and 1 day prior.
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Manila")
    @Transactional(readOnly = true)
    public int dispatchDueDateReminders() {
        LocalDate today = LocalDate.now();
        List<LocalDate> targetDates = Arrays.asList(today.plusDays(1), today.plusDays(3));
        List<AmortizationSchedule> upcoming = amortizationScheduleRepository
                .findByStatusAndDueDateIn("PENDING", targetDates);

        int sent = 0;
        for (AmortizationSchedule inst : upcoming) {
            User borrower = inst.getLoan().getUser();
            long daysRemaining = ChronoUnit.DAYS.between(today, inst.getDueDate());
            boolean ok = brevoEmailService.sendDueDateReminderEmail(
                    borrower.getEmail(),
                    borrower.getFirstName(),
                    inst.getLoan().getId(),
                    inst.getInstallmentNumber(),
                    inst.getDueDate(),
                    inst.getTotalDue(),
                    daysRemaining
            );
            if (ok) {
                sent++;
            }
        }
        return sent;
    }

    /**
     * Returns all overdue installments with borrower details for underwriter monitoring.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDelinquentAccounts() {
        List<AmortizationSchedule> overdueList = amortizationScheduleRepository.findByStatusOrderByDueDateAsc("OVERDUE");
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();

        for (AmortizationSchedule inst : overdueList) {
            LoanDisbursement loan = inst.getLoan();
            User borrower = loan.getUser();
            long daysOverdue = Math.max(0, ChronoUnit.DAYS.between(inst.getDueDate(), today));
            BigDecimal penalty = inst.getPenaltyFee();
            BigDecimal totalWithPenalty = inst.getTotalDue().add(penalty);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("installmentId", inst.getId());
            item.put("loanId", loan.getId());
            item.put("installmentNumber", inst.getInstallmentNumber());
            item.put("borrowerId", borrower.getId());
            item.put("borrowerName", borrower.getFirstName() + " " + borrower.getLastName());
            item.put("borrowerEmail", borrower.getEmail());
            item.put("dueDate", inst.getDueDate().toString());
            item.put("daysOverdue", daysOverdue);
            item.put("baseDue", inst.getTotalDue());
            item.put("penaltyFee", penalty);
            item.put("totalDueWithPenalty", totalWithPenalty);
            item.put("loanOutstandingBalance", loan.getOutstandingBalance());
            result.add(item);
        }
        return result;
    }

    /**
     * Returns due-date reminders and overdue billing alerts for a specific borrower.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBorrowerBillingAlerts(String borrowerEmail) {
        User user = userRepository.findByEmail(borrowerEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + borrowerEmail));

        List<AmortizationSchedule> unpaid = amortizationScheduleRepository
                .findByLoanUserIdAndStatusInOrderByDueDateAsc(user.getId(), Arrays.asList("PENDING", "OVERDUE"));

        LocalDate today = LocalDate.now();
        LocalDate reminderCutoff = today.plusDays(7);
        List<Map<String, Object>> alerts = new ArrayList<>();

        for (AmortizationSchedule inst : unpaid) {
            if (inst.getDueDate().isAfter(reminderCutoff)) {
                continue;
            }

            long daysDiff = ChronoUnit.DAYS.between(today, inst.getDueDate());
            String severity;
            String title;
            if ("OVERDUE".equals(inst.getStatus()) || daysDiff < 0) {
                severity = "CRITICAL";
                title = "Overdue Installment (" + Math.abs(daysDiff) + " day(s) past due)";
            } else if (daysDiff == 0) {
                severity = "WARNING";
                title = "Payment Due Today";
            } else {
                severity = "INFO";
                title = "Upcoming Due in " + daysDiff + " day(s)";
            }

            BigDecimal totalWithPenalty = inst.getTotalDue().add(inst.getPenaltyFee());
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("installmentId", inst.getId());
            alert.put("loanId", inst.getLoan().getId());
            alert.put("installmentNumber", inst.getInstallmentNumber());
            alert.put("dueDate", inst.getDueDate().toString());
            alert.put("severity", severity);
            alert.put("title", title);
            alert.put("baseDue", inst.getTotalDue());
            alert.put("penaltyFee", inst.getPenaltyFee());
            alert.put("totalDueWithPenalty", totalWithPenalty);
            alert.put("status", inst.getStatus());
            alerts.add(alert);
        }
        return alerts;
    }
}
