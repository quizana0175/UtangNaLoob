package com.utangnaloob.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentReceiptResponse {

    private String transactionReference;
    private BigDecimal amountPaid;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private BigDecimal newAvailableBalance;
    private BigDecimal newDrawnBalance;
    private BigDecimal remainingLoanBalance;
    private String message;
    private boolean success;

    public PaymentReceiptResponse() {
    }

    public PaymentReceiptResponse(String transactionReference, BigDecimal amountPaid, String paymentMethod, LocalDateTime paidAt, BigDecimal newAvailableBalance, BigDecimal newDrawnBalance, BigDecimal remainingLoanBalance, String message, boolean success) {
        this.transactionReference = transactionReference;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.paidAt = paidAt;
        this.newAvailableBalance = newAvailableBalance;
        this.newDrawnBalance = newDrawnBalance;
        this.remainingLoanBalance = remainingLoanBalance;
        this.message = message;
        this.success = success;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public BigDecimal getNewAvailableBalance() {
        return newAvailableBalance;
    }

    public void setNewAvailableBalance(BigDecimal newAvailableBalance) {
        this.newAvailableBalance = newAvailableBalance;
    }

    public BigDecimal getNewDrawnBalance() {
        return newDrawnBalance;
    }

    public void setNewDrawnBalance(BigDecimal newDrawnBalance) {
        this.newDrawnBalance = newDrawnBalance;
    }

    public BigDecimal getRemainingLoanBalance() {
        return remainingLoanBalance;
    }

    public void setRemainingLoanBalance(BigDecimal remainingLoanBalance) {
        this.remainingLoanBalance = remainingLoanBalance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
