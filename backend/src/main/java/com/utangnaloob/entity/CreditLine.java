package com.utangnaloob.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_lines")
public class CreditLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "approved_credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal approvedCreditLimit = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "drawn_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal drawnBalance = BigDecimal.ZERO;

    @Column(length = 30, nullable = false)
    private String status = "PENDING"; // PENDING, ACTIVE, SUSPENDED, FROZEN

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public CreditLine() {
    }

    public CreditLine(User user, BigDecimal approvedLimit) {
        this.user = user;
        this.approvedCreditLimit = approvedLimit;
        this.availableBalance = approvedLimit;
        this.drawnBalance = BigDecimal.ZERO;
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
