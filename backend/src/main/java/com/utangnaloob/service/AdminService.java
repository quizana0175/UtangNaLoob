package com.utangnaloob.service;

import com.utangnaloob.dto.SystemParameterDto;
import com.utangnaloob.entity.ERole;
import com.utangnaloob.entity.SystemParameter;
import com.utangnaloob.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KycProfileRepository kycProfileRepository;

    @Autowired
    private CreditLineRepository creditLineRepository;

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    @Autowired
    private LoanDisbursementRepository loanDisbursementRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private AmortizationScheduleRepository amortizationScheduleRepository;

    @Autowired
    private AuditLogService auditLogService;

    public Map<String, Object> getPortfolioMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalBorrowers = userRepository.findByRoleName(ERole.ROLE_BORROWER).size();
        long totalUnderwriters = userRepository.findByRoleName(ERole.ROLE_UNDERWRITER).size();
        long pendingKycCount = kycProfileRepository.countByKycStatus("PENDING");
        long verifiedKycCount = kycProfileRepository.countByKycStatus("VERIFIED");

        BigDecimal totalApprovedCredit = creditLineRepository.sumTotalApprovedCredit();
        BigDecimal totalDrawnBalance = creditLineRepository.sumTotalDrawnBalance();
        long activeCreditLines = creditLineRepository.countByStatus("ACTIVE");

        BigDecimal totalDisbursedFunds = loanDisbursementRepository.sumTotalDisbursedPrincipal();
        BigDecimal totalOutstandingBalance = loanDisbursementRepository.sumTotalOutstandingBalance();
        BigDecimal totalCollectedPayments = paymentTransactionRepository.sumTotalCollected();
        long activeLoansCount = loanDisbursementRepository.findByStatusOrderByDisbursedAtDesc("ACTIVE").size();
        long overdueInstallmentsCount = amortizationScheduleRepository.findByStatusOrderByDueDateAsc("OVERDUE").size();
        long totalInstallmentsCount = amortizationScheduleRepository.count();

        double nplRatio = 0.0;
        double recoveryRate = 100.0;
        if (totalInstallmentsCount > 0) {
            nplRatio = BigDecimal.valueOf(overdueInstallmentsCount * 100.0 / totalInstallmentsCount)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
            recoveryRate = BigDecimal.valueOf(100.0 - nplRatio)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
        }

        metrics.put("totalUsers", totalUsers);
        metrics.put("totalBorrowers", totalBorrowers);
        metrics.put("totalUnderwriters", totalUnderwriters);
        metrics.put("pendingKycCount", pendingKycCount);
        metrics.put("verifiedKycCount", verifiedKycCount);
        metrics.put("totalApprovedCredit", totalApprovedCredit);
        metrics.put("totalDrawnBalance", totalDrawnBalance);
        metrics.put("activeCreditLines", activeCreditLines);
        metrics.put("totalDisbursedFunds", totalDisbursedFunds);
        metrics.put("totalOutstandingBalance", totalOutstandingBalance);
        metrics.put("totalCollectedPayments", totalCollectedPayments);
        metrics.put("activeLoansCount", activeLoansCount);
        metrics.put("overdueInstallmentsCount", overdueInstallmentsCount);
        metrics.put("repaymentRecoveryRate", recoveryRate);
        metrics.put("nonPerformingLoanRatio", nplRatio);

        return metrics;
    }

    public List<SystemParameterDto> getAllSystemParameters() {
        return systemParameterRepository.findAll().stream()
                .map(p -> new SystemParameterDto(
                        p.getId(),
                        p.getParamKey(),
                        p.getParamValue(),
                        p.getDescription(),
                        p.getUpdatedBy(),
                        p.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public SystemParameterDto updateParameter(String paramKey, String newValue, String adminEmail, String ipAddress) {
        SystemParameter param = systemParameterRepository.findByParamKey(paramKey)
                .orElseThrow(() -> new RuntimeException("System parameter not found: " + paramKey));

        String oldValue = param.getParamValue();
        param.setParamValue(newValue.trim());
        param.setUpdatedBy(adminEmail);
        param.setUpdatedAt(LocalDateTime.now());
        SystemParameter saved = systemParameterRepository.save(param);

        auditLogService.log(
                "SYSTEM_PARAMETER_UPDATED",
                adminEmail,
                paramKey,
                "Updated parameter '" + paramKey + "' from '" + oldValue + "' to '" + newValue + "'",
                ipAddress
        );

        return new SystemParameterDto(
                saved.getId(),
                saved.getParamKey(),
                saved.getParamValue(),
                saved.getDescription(),
                saved.getUpdatedBy(),
                saved.getUpdatedAt()
        );
    }
}
