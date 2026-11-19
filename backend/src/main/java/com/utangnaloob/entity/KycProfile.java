package com.utangnaloob.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_profiles")
public class KycProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(length = 50)
    private String idType; // UMID, SSS, DRIVERS_LICENSE, PASSPORT, PRC_ID, VOTERS_ID

    @Column(length = 100)
    private String idNumber;

    @Column(length = 500)
    private String idDocumentUrl;

    @Column(length = 50)
    private String proofOfIncomeType; // PAYSLIP, CERTIFICATE_OF_EMPLOYMENT, ITR, BANK_STATEMENT

    @Column(length = 500)
    private String proofOfIncomeUrl;

    @Column(length = 30, nullable = false)
    private String kycStatus = "PENDING"; // PENDING, VERIFIED, REJECTED, NOT_SUBMITTED

    @Column(name = "verified_monthly_income")
    private Double verifiedMonthlyIncome;

    @Column(name = "recommended_credit_limit")
    private Double recommendedCreditLimit;

    @Column(length = 500)
    private String notesOrRejectionReason;

    @Column(length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    public KycProfile() {
    }

    public KycProfile(User user) {
        this.user = user;
        this.kycStatus = "NOT_SUBMITTED";
        this.submittedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getIdDocumentUrl() {
        return idDocumentUrl;
    }

    public void setIdDocumentUrl(String idDocumentUrl) {
        this.idDocumentUrl = idDocumentUrl;
    }

    public String getProofOfIncomeType() {
        return proofOfIncomeType;
    }

    public void setProofOfIncomeType(String proofOfIncomeType) {
        this.proofOfIncomeType = proofOfIncomeType;
    }

    public String getProofOfIncomeUrl() {
        return proofOfIncomeUrl;
    }

    public void setProofOfIncomeUrl(String proofOfIncomeUrl) {
        this.proofOfIncomeUrl = proofOfIncomeUrl;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public Double getVerifiedMonthlyIncome() {
        return verifiedMonthlyIncome;
    }

    public void setVerifiedMonthlyIncome(Double verifiedMonthlyIncome) {
        this.verifiedMonthlyIncome = verifiedMonthlyIncome;
    }

    public Double getRecommendedCreditLimit() {
        return recommendedCreditLimit;
    }

    public void setRecommendedCreditLimit(Double recommendedCreditLimit) {
        this.recommendedCreditLimit = recommendedCreditLimit;
    }

    public String getNotesOrRejectionReason() {
        return notesOrRejectionReason;
    }

    public void setNotesOrRejectionReason(String notesOrRejectionReason) {
        this.notesOrRejectionReason = notesOrRejectionReason;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
