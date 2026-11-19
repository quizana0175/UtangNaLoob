package com.utangnaloob.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan_disbursements")
public class LoanDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "principal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "installment_term_months", nullable = false)
    private Integer installmentTermMonths; // 1, 2, 3, 6, 12

    @Column(name = "repayment_frequency", nullable = false, length = 30)
    private String repaymentFrequency; // BI_WEEKLY, MONTHLY

    @Column(name = "monthly_interest_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal monthlyInterestRate; // e.g. 0.0200 (2.0%)

    @Column(name = "total_interest_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalInterestAmount;

    @Column(name = "processing_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal processingFee;

    @Column(name = "total_repayment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRepaymentAmount;

    @Column(name = "outstanding_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(length = 30, nullable = false)
    private String status = "ACTIVE"; // ACTIVE, PAID, OVERDUE

    @Column(length = 255)
    private String loanPurpose;

    @Column(name = "disbursed_at", nullable = false)
    private LocalDateTime disbursedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("installmentNumber ASC")
    private List<AmortizationSchedule> installments = new ArrayList<>();

    public LoanDisbursement() {
    }

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

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
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

    public BigDecimal getMonthlyInterestRate() {
        return monthlyInterestRate;
    }

    public void setMonthlyInterestRate(BigDecimal monthlyInterestRate) {
        this.monthlyInterestRate = monthlyInterestRate;
    }

    public BigDecimal getTotalInterestAmount() {
        return totalInterestAmount;
    }

    public void setTotalInterestAmount(BigDecimal totalInterestAmount) {
        this.totalInterestAmount = totalInterestAmount;
    }

    public BigDecimal getProcessingFee() {
        return processingFee;
    }

    public void setProcessingFee(BigDecimal processingFee) {
        this.processingFee = processingFee;
    }

    public BigDecimal getTotalRepaymentAmount() {
        return totalRepaymentAmount;
    }

    public void setTotalRepaymentAmount(BigDecimal totalRepaymentAmount) {
        this.totalRepaymentAmount = totalRepaymentAmount;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLoanPurpose() {
        return loanPurpose;
    }

    public void setLoanPurpose(String loanPurpose) {
        this.loanPurpose = loanPurpose;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }

    public List<AmortizationSchedule> getInstallments() {
        return installments;
    }

    public void setInstallments(List<AmortizationSchedule> installments) {
        this.installments = installments;
    }

    public void addInstallment(AmortizationSchedule schedule) {
        this.installments.add(schedule);
        schedule.setLoan(this);
    }
}
