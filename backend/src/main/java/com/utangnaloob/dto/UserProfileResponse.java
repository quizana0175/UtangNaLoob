package com.utangnaloob.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class UserProfileResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String dateOfBirth;
    private String address;
    private String employmentStatus;
    private Double monthlyIncome;
    private String accountStatus;
    private List<String> roles;
    private LocalDateTime createdAt;

    // Associated KYC info
    private String kycStatus;
    private String idType;
    private String idNumber;
    private String idDocumentUrl;
    private String proofOfIncomeType;
    private String proofOfIncomeUrl;
    private Double verifiedMonthlyIncome;
    private String notesOrRejectionReason;

    // Associated Credit line info
    private BigDecimal approvedCreditLimit;
    private BigDecimal availableBalance;
    private BigDecimal drawnBalance;
    private String creditLineStatus;

    public UserProfileResponse() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public Double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(Double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
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

    public Double getVerifiedMonthlyIncome() {
        return verifiedMonthlyIncome;
    }

    public void setVerifiedMonthlyIncome(Double verifiedMonthlyIncome) {
        this.verifiedMonthlyIncome = verifiedMonthlyIncome;
    }

    public String getNotesOrRejectionReason() {
        return notesOrRejectionReason;
    }

    public void setNotesOrRejectionReason(String notesOrRejectionReason) {
        this.notesOrRejectionReason = notesOrRejectionReason;
    }

    public BigDecimal getApprovedCreditLimit() {
        return approvedCreditLimit;
    }

    public void setApprovedCreditLimit(BigDecimal approvedCreditLimit) {
        this.approvedCreditLimit = approvedCreditLimit;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getDrawnBalance() {
        return drawnBalance;
    }

    public void setDrawnBalance(BigDecimal drawnBalance) {
        this.drawnBalance = drawnBalance;
    }

    public String getCreditLineStatus() {
        return creditLineStatus;
    }

    public void setCreditLineStatus(String creditLineStatus) {
        this.creditLineStatus = creditLineStatus;
    }
}
