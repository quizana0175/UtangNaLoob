package com.utangnaloob.dto;

public class ForgotPasswordResponse {
    private boolean success;
    private String message;
    private String token;
    private int expiryMinutes;
    private boolean emailSent;

    public ForgotPasswordResponse() {
    }

    public ForgotPasswordResponse(boolean success, String message, String token, int expiryMinutes) {
        this(success, message, token, expiryMinutes, false);
    }

    public ForgotPasswordResponse(boolean success, String message, String token, int expiryMinutes, boolean emailSent) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.expiryMinutes = expiryMinutes;
        this.emailSent = emailSent;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(int expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }
}
