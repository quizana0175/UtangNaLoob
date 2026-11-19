package com.utangnaloob.service;

import com.utangnaloob.dto.SystemParameterDto;
import com.utangnaloob.entity.ERole;
import com.utangnaloob.entity.SystemParameter;
import com.utangnaloob.repository.CreditLineRepository;
import com.utangnaloob.repository.KycProfileRepository;
import com.utangnaloob.repository.SystemParameterRepository;
import com.utangnaloob.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        metrics.put("totalUsers", totalUsers);
        metrics.put("totalBorrowers", totalBorrowers);
        metrics.put("totalUnderwriters", totalUnderwriters);
        metrics.put("pendingKycCount", pendingKycCount);
        metrics.put("verifiedKycCount", verifiedKycCount);
        metrics.put("totalApprovedCredit", totalApprovedCredit);
        metrics.put("totalDrawnBalance", totalDrawnBalance);
        metrics.put("activeCreditLines", activeCreditLines);

        // Performance ratios (simulated portfolio benchmarks)
        metrics.put("repaymentRecoveryRate", 98.4); // 98.4% recovery rate
        metrics.put("nonPerformingLoanRatio", 1.6); // 1.6% NPL ratio

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
