package com.utangnaloob.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class KycSubmissionDto {

    @NotBlank(message = "ID type is required")
    private String idType; // UMID, SSS, DRIVERS_LICENSE, PASSPORT, PRC_ID, VOTERS_ID

    @NotBlank(message = "ID number is required")
    private String idNumber;

    @NotBlank(message = "ID document image / URL is required")
    private String idDocumentUrl;

    @NotBlank(message = "Proof of income type is required")
    private String proofOfIncomeType; // PAYSLIP, COE, ITR, BANK_STATEMENT

    @NotBlank(message = "Proof of income document image / URL is required")
    private String proofOfIncomeUrl;

    @NotNull(message = "Declared monthly income is required")
    private Double monthlyIncome;

    public KycSubmissionDto() {
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

    public Double getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(Double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }
}
