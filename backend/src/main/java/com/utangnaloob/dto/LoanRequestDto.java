package com.utangnaloob.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class LoanRequestDto {

    @NotNull(message = "Requested loan disbursement amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan disbursement is PHP 1,000.00")
    private BigDecimal amount;

    @NotNull(message = "Installment duration is required")
    private Integer installmentTermMonths; // 1, 2, 3, 6, 12

    @NotBlank(message = "Repayment frequency is required (BI_WEEKLY or MONTHLY)")
    private String repaymentFrequency; // BI_WEEKLY, MONTHLY

    private String loanPurpose;

    public LoanRequestDto() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getInstallmentTermMonths() {
        return installmentTermMonths;
    }

    public void setInstallmentTermMonths(Integer installmentTermMonths) {
        this.installmentTermMonths = installmentTermMonths;
    }

    public String getRepaymentFrequency() {
        return repaymentFrequency;
    }

    public void setRepaymentFrequency(String repaymentFrequency) {
        this.repaymentFrequency = repaymentFrequency;
    }

    public String getLoanPurpose() {
        return loanPurpose;
    }

    public void setLoanPurpose(String loanPurpose) {
        this.loanPurpose = loanPurpose;
    }
}
