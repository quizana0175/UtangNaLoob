package com.utangnaloob.service;

import com.utangnaloob.dto.KycReviewDto;
import com.utangnaloob.dto.KycSubmissionDto;
import com.utangnaloob.dto.MessageResponse;
import com.utangnaloob.dto.UserProfileResponse;
import com.utangnaloob.entity.CreditLine;
import com.utangnaloob.entity.KycProfile;
import com.utangnaloob.entity.User;
import com.utangnaloob.repository.CreditLineRepository;
import com.utangnaloob.repository.KycProfileRepository;
import com.utangnaloob.repository.SystemParameterRepository;
import com.utangnaloob.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KycService {

    @Autowired
    private KycProfileRepository kycProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CreditLineRepository creditLineRepository;

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private BrevoEmailService brevoEmailService;

    /**
     * SRS Appendix A: L_initial = min(L_max_tier, I_verified * DTI_max * mu_tier)
     * rounded to the nearest PHP 500.00 (minimum PHP 2,000.00).
     */
    private double computeAlgorithmicLimit(double monthlyIncome, String employmentStatus) {
        double maxInitialLimit = systemParameterRepository.findByParamKey("MAX_INITIAL_CREDIT_LIMIT")
                .map(p -> {
                    try { return Double.parseDouble(p.getParamValue().trim()); }
                    catch (Exception e) { return 50000.0; }
                })
                .orElse(50000.0);

        double dtiMax = systemParameterRepository.findByParamKey("MAX_DTI_RATIO")
                .map(p -> {
                    try { return Double.parseDouble(p.getParamValue().trim()); }
                    catch (Exception e) { return 0.30; }
                })
                .orElse(0.30);

        double muTier = 1.0;
        if (employmentStatus != null) {
            String emp = employmentStatus.toUpperCase();
            if (emp.contains("GOV") || emp.contains("MULTINATIONAL")) {
                muTier = 1.2;
            } else if (emp.contains("SELF") || emp.contains("FREELANCE")) {
                muTier = 0.8;
            }
        }

        double raw = monthlyIncome * dtiMax * muTier;
        double recommended = Math.min(Math.round(raw / 500.0) * 500.0, maxInitialLimit);
        return Math.max(recommended, 2000.0);
    }

    @Transactional
    public MessageResponse submitKyc(String email, KycSubmissionDto dto, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        // Update monthly income on user if changed
        if (dto.getMonthlyIncome() != null) {
            user.setMonthlyIncome(dto.getMonthlyIncome());
            userRepository.save(user);
        }

        KycProfile profile = kycProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> new KycProfile(user));

        profile.setIdType(dto.getIdType());
        profile.setIdNumber(dto.getIdNumber().trim());
        profile.setIdDocumentUrl(dto.getIdDocumentUrl().trim());
        profile.setProofOfIncomeType(dto.getProofOfIncomeType());
        profile.setProofOfIncomeUrl(dto.getProofOfIncomeUrl().trim());
        profile.setKycStatus("PENDING");
        profile.setSubmittedAt(LocalDateTime.now());
        profile.setNotesOrRejectionReason(null);

        double income = dto.getMonthlyIncome() != null ? dto.getMonthlyIncome() : 0.0;
        double recommended = computeAlgorithmicLimit(income, user.getEmploymentStatus());
        profile.setRecommendedCreditLimit(recommended);

        kycProfileRepository.save(profile);

        auditLogService.log(
                "KYC_SUBMITTED",
                email,
                email,
                "Submitted KYC documents (" + dto.getIdType() + ", " + dto.getProofOfIncomeType() + ") with declared income: PHP " + income + ", recommended limit: PHP " + recommended,
                ipAddress
        );

        return new MessageResponse("KYC documents submitted successfully! Your application is now in the underwriter review queue.", true);
    }

    public List<UserProfileResponse> getPendingKycQueue() {
        List<KycProfile> pending = kycProfileRepository.findByKycStatusOrderBySubmittedAtDesc("PENDING");

        return pending.stream().map(k -> {
            User u = k.getUser();
            UserProfileResponse resp = new UserProfileResponse();
            resp.setId(u.getId());
            resp.setFirstName(u.getFirstName());
            resp.setLastName(u.getLastName());
            resp.setEmail(u.getEmail());
            resp.setPhoneNumber(u.getPhoneNumber());
            resp.setDateOfBirth(u.getDateOfBirth());
            resp.setAddress(u.getAddress());
            resp.setEmploymentStatus(u.getEmploymentStatus());
            resp.setMonthlyIncome(u.getMonthlyIncome());
            resp.setAccountStatus(u.getAccountStatus());
            resp.setCreatedAt(k.getSubmittedAt());

            resp.setKycStatus(k.getKycStatus());
            resp.setIdType(k.getIdType());
            resp.setIdNumber(k.getIdNumber());
            resp.setIdDocumentUrl(k.getIdDocumentUrl());
            resp.setProofOfIncomeType(k.getProofOfIncomeType());
            resp.setProofOfIncomeUrl(k.getProofOfIncomeUrl());
            resp.setVerifiedMonthlyIncome(k.getVerifiedMonthlyIncome());
            resp.setNotesOrRejectionReason(k.getNotesOrRejectionReason());

            creditLineRepository.findByUserId(u.getId()).ifPresent(cl -> {
                resp.setApprovedCreditLimit(cl.getApprovedCreditLimit());
                resp.setAvailableBalance(cl.getAvailableBalance());
                resp.setDrawnBalance(cl.getDrawnBalance());
                resp.setCreditLineStatus(cl.getStatus());
            });

            return resp;
        }).collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse reviewKyc(Long userId, KycReviewDto dto, String underwriterEmail, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        KycProfile profile = kycProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("KYC profile not found for user: " + userId));

        String decision = dto.getDecision().toUpperCase();

        if ("VERIFIED".equals(decision) || "APPROVED".equals(decision)) {
            profile.setKycStatus("VERIFIED");
            profile.setReviewedBy(underwriterEmail);
            profile.setReviewedAt(LocalDateTime.now());
            if (dto.getVerifiedMonthlyIncome() != null) {
                profile.setVerifiedMonthlyIncome(dto.getVerifiedMonthlyIncome());
            }

            BigDecimal limitToAssign = dto.getApprovedCreditLimit();
            if (limitToAssign == null || limitToAssign.compareTo(BigDecimal.ZERO) <= 0) {
                if (dto.getVerifiedMonthlyIncome() != null && dto.getVerifiedMonthlyIncome() > 0) {
                    limitToAssign = BigDecimal.valueOf(computeAlgorithmicLimit(dto.getVerifiedMonthlyIncome(), user.getEmploymentStatus()));
                } else {
                    double rec = profile.getRecommendedCreditLimit() != null ? profile.getRecommendedCreditLimit() : 10000.0;
                    limitToAssign = BigDecimal.valueOf(rec);
                }
            }

            // Update or create CreditLine
            CreditLine creditLine = creditLineRepository.findByUserId(userId)
                    .orElseGet(() -> new CreditLine(user, BigDecimal.ZERO));

            creditLine.setApprovedCreditLimit(limitToAssign);
            creditLine.setAvailableBalance(limitToAssign);
            creditLine.setDrawnBalance(BigDecimal.ZERO);
            creditLine.setStatus("ACTIVE");
            creditLine.setUpdatedAt(LocalDateTime.now());
            creditLineRepository.save(creditLine);

            kycProfileRepository.save(profile);

            auditLogService.log(
                    "KYC_APPROVED",
                    underwriterEmail,
                    user.getEmail(),
                    "KYC approved with revolving credit line limit: PHP " + limitToAssign,
                    ipAddress
            );

            brevoEmailService.sendKycDecisionEmail(user.getEmail(), user.getFirstName(), true, limitToAssign, null);

            return new MessageResponse("Applicant KYC approved and revolving credit line of PHP " + limitToAssign + " activated successfully!", true);

        } else if ("REJECTED".equals(decision)) {
            profile.setKycStatus("REJECTED");
            profile.setReviewedBy(underwriterEmail);
            profile.setReviewedAt(LocalDateTime.now());
            profile.setNotesOrRejectionReason(dto.getNotesOrRejectionReason() != null ? dto.getNotesOrRejectionReason() : "Documentation does not meet credit underwriting requirements.");

            kycProfileRepository.save(profile);

            auditLogService.log(
                    "KYC_REJECTED",
                    underwriterEmail,
                    user.getEmail(),
                    "KYC rejected. Reason: " + profile.getNotesOrRejectionReason(),
                    ipAddress
            );

            brevoEmailService.sendKycDecisionEmail(user.getEmail(), user.getFirstName(), false, null, profile.getNotesOrRejectionReason());

            return new MessageResponse("Applicant KYC marked as REJECTED.", true);
        } else {
            throw new IllegalArgumentException("Invalid review decision: " + dto.getDecision());
        }
    }
}
