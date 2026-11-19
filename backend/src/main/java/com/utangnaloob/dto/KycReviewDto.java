package com.utangnaloob.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class KycReviewDto {

    @NotBlank(message = "Decision status is required (VERIFIED or REJECTED)")
    private String decision; // VERIFIED, REJECTED

    private Double verifiedMonthlyIncome;

    private BigDecimal approvedCreditLimit; // Custom or adjusted limit decided by Underwriter

    private String notesOrRejectionReason;

    public KycReviewDto() {
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public Double getVerifiedMonthlyIncome() {
        return verifiedMonthlyIncome;
    }

    public void setVerifiedMonthlyIncome(Double verifiedMonthlyIncome) {
        this.verifiedMonthlyIncome = verifiedMonthlyIncome;
    }

    public BigDecimal getApprovedCreditLimit() {
        return approvedCreditLimit;
    }

    public void setApprovedCreditLimit(BigDecimal approvedCreditLimit) {
        this.approvedCreditLimit = approvedCreditLimit;
    }

    public String getNotesOrRejectionReason() {
        return notesOrRejectionReason;
    }

    public void setNotesOrRejectionReason(String notesOrRejectionReason) {
        this.notesOrRejectionReason = notesOrRejectionReason;
    }
}
