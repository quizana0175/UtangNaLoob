package com.utangnaloob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService {

    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:noreply@utangnaloob.ph}")
    private String senderEmail;

    @Value("${brevo.sender.name:Utang na Loob}")
    private String senderName;

    @Value("${utangnaloob.app.frontendResetUrl:http://localhost:3000/standalone.html#/reset-password}")
    private String frontendResetUrl;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isConfigured() {
        return brevoApiKey != null && !brevoApiKey.isBlank();
    }

    /**
     * Core helper to dispatch an HTML email via Brevo Transactional Email API.
     * Gracefully returns false without throwing if Brevo is unconfigured or unreachable (NFR-R3).
     */
    public boolean sendHtmlEmail(String toEmail, String toName, String subject, String htmlContent) {
        if (!isConfigured()) {
            logger.warn("Brevo API key is not configured (BREVO_API_KEY). Skipping email '{}' for {}", subject, toEmail);
            return false;
        }
        if (toEmail == null || toEmail.isBlank()) {
            return false;
        }

        try {
            String safeName = (toName != null && !toName.isBlank()) ? toName : "Valued Borrower";

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", Map.of("name", senderName, "email", senderEmail));
            payload.put("to", List.of(Map.of("email", toEmail, "name", safeName)));
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", brevoApiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Brevo email '{}' sent successfully to {} (status {})", subject, toEmail, response.statusCode());
                return true;
            } else {
                logger.error("Brevo API returned error status {} for '{}' to {}: {}", response.statusCode(), subject, toEmail, response.body());
                return false;
            }
        } catch (Exception e) {
            logger.error("Failed to send Brevo email '{}' to {}: {}", subject, toEmail, e.getMessage());
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String toEmail, String toName, String token, int expiryMinutes) {
        String resetLink = frontendResetUrl + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String body = "<h2 style=\"color: #10b981; margin-top: 0;\">Utang na Loob &mdash; Password Reset</h2>"
                + "<p>Hi " + escapeHtml(safeName(toName)) + ",</p>"
                + "<p>We received a request to reset your password. Click the button below to set a new password (valid for <strong>" + expiryMinutes + " minutes</strong>):</p>"
                + "<p style=\"margin: 24px 0;\"><a href=\"" + resetLink + "\" style=\"background: #10b981; color: #020617; padding: 12px 20px; text-decoration: none; font-weight: bold; border-radius: 8px; display: inline-block;\">Reset Password</a></p>"
                + "<p style=\"font-size: 13px; color: #94a3b8;\">Or copy and paste this verification token into the Reset Password form:</p>"
                + "<p style=\"background: #020617; padding: 10px 14px; border-radius: 6px; font-family: monospace; color: #fbbf24; word-break: break-all;\">" + escapeHtml(token) + "</p>"
                + "<p style=\"font-size: 12px; color: #64748b; margin-top: 24px;\">If you did not request this, you can safely ignore this email.</p>";
        return sendHtmlEmail(toEmail, toName, "Reset Your Utang na Loob Password", wrapLayout(body));
    }

    /**
     * FR-027: Automated transactional email receipt immediately following installment repayment.
     */
    public boolean sendPaymentReceiptEmail(String toEmail, String toName, String txRef, BigDecimal amountPaid,
                                           String paymentMethod, BigDecimal remainingLoanBalance, BigDecimal availableCredit) {
        String body = "<h2 style=\"color: #10b981; margin-top: 0;\">Payment Receipt Confirmed</h2>"
                + "<p>Hi " + escapeHtml(safeName(toName)) + ",</p>"
                + "<p>Thank you! Your installment repayment has been reconciled and your revolving credit limit has been replenished.</p>"
                + "<div style=\"background: #020617; padding: 16px; border-radius: 8px; margin: 16px 0; font-size: 14px;\">"
                + "<div><strong>Reference No:</strong> " + escapeHtml(txRef) + "</div>"
                + "<div><strong>Amount Paid:</strong> PHP " + amountPaid + "</div>"
                + "<div><strong>Payment Method:</strong> " + escapeHtml(paymentMethod) + "</div>"
                + "<div><strong>Remaining Loan Balance:</strong> PHP " + remainingLoanBalance + "</div>"
                + "<div><strong>Available Credit Limit:</strong> PHP " + availableCredit + "</div>"
                + "</div>"
                + "<p style=\"font-size: 12px; color: #64748b;\">Utang na Loob Automated Ledger System</p>";
        return sendHtmlEmail(toEmail, toName, "Payment Receipt (" + txRef + ") - Utang na Loob", wrapLayout(body));
    }

    /**
     * FR-028: Daily due-date reminder email (3 days and 1 day prior to due date).
     */
    public boolean sendDueDateReminderEmail(String toEmail, String toName, Long loanId, int installmentNumber,
                                            LocalDate dueDate, BigDecimal totalDue, long daysRemaining) {
        String body = "<h2 style=\"color: #38bdf8; margin-top: 0;\">Upcoming Payment Reminder</h2>"
                + "<p>Hi " + escapeHtml(safeName(toName)) + ",</p>"
                + "<p>This is a friendly reminder that Installment #" + installmentNumber + " for Loan #" + loanId
                + " is due in <strong>" + daysRemaining + " day(s)</strong> (on <strong>" + dueDate + "</strong>).</p>"
                + "<div style=\"background: #020617; padding: 16px; border-radius: 8px; margin: 16px 0; font-size: 14px;\">"
                + "<div><strong>Installment Due:</strong> PHP " + totalDue + "</div>"
                + "<div><strong>Due Date:</strong> " + dueDate + "</div>"
                + "</div>"
                + "<p>Please settle on or before the due date to avoid late penalties and keep your credit standing active.</p>";
        return sendHtmlEmail(toEmail, toName, "Payment Due in " + daysRemaining + " Day(s) - Utang na Loob", wrapLayout(body));
    }

    /**
     * FR-029: Urgent overdue alert email when an installment enters OVERDUE status.
     */
    public boolean sendOverdueAlertEmail(String toEmail, String toName, Long loanId, int installmentNumber,
                                         LocalDate dueDate, BigDecimal principalAndInterestDue, BigDecimal penaltyFee) {
        BigDecimal totalPayable = principalAndInterestDue.add(penaltyFee);
        String body = "<h2 style=\"color: #f43f5e; margin-top: 0;\">Urgent: Overdue Installment Notice</h2>"
                + "<p>Hi " + escapeHtml(safeName(toName)) + ",</p>"
                + "<p>Installment #" + installmentNumber + " for Loan #" + loanId + " (originally due on <strong>" + dueDate + "</strong>) is now <strong>OVERDUE</strong>.</p>"
                + "<div style=\"background: #020617; padding: 16px; border-radius: 8px; border: 1px solid #f43f5e; margin: 16px 0; font-size: 14px;\">"
                + "<div><strong>Installment Balance:</strong> PHP " + principalAndInterestDue + "</div>"
                + "<div><strong>Late Penalty Applied:</strong> PHP " + penaltyFee + "</div>"
                + "<div><strong>Total Amount to Settle:</strong> PHP " + totalPayable + "</div>"
                + "</div>"
                + "<p>Your revolving credit line is temporarily suspended for new drawdowns until overdue balances and penalties are settled in full. Please log in to your Borrower Portal to pay via GCash, Maya, or Card.</p>";
        return sendHtmlEmail(toEmail, toName, "URGENT: Overdue Payment Notice (Loan #" + loanId + ") - Utang na Loob", wrapLayout(body));
    }

    /**
     * Activity Diagram: Dispatch KYC approval or rejection notification email.
     */
    public boolean sendKycDecisionEmail(String toEmail, String toName, boolean approved, BigDecimal creditLimit, String reason) {
        String body;
        String subject;
        if (approved) {
            subject = "KYC Verified & Credit Line Activated - Utang na Loob";
            body = "<h2 style=\"color: #10b981; margin-top: 0;\">KYC Verification Approved!</h2>"
                    + "<p>Hi " + escapeHtml(safeName(toName)) + ",</p>"
                    + "<p>Your identity and income documents have been verified. Your revolving credit line is now active!</p>"
                    + "<div style=\"background: #020617; padding: 16px; border-radius: 8px; margin: 16px 0; font-size: 14px;\">"
                    + "<div><strong>Assigned Revolving Credit Limit:</strong> PHP " + creditLimit + "</div>"
                    + "</div>"
                    + "<p>You can now simulate and request cash advance drawdowns directly from your Borrower Dashboard.</p>";
        } else {
            subject = "KYC Application Update - Utang na Loob";
            body = "<h2 style=\"color: #f59e0b; margin-top: 0;\">KYC Application Requires Resubmission</h2>"
                    + "<p>Hi " + escapeHtml(safeName(toName)) + ",</p>"
                    + "<p>Your KYC submission was not approved at this time.</p>"
                    + "<div style=\"background: #020617; padding: 16px; border-radius: 8px; margin: 16px 0; font-size: 14px;\">"
                    + "<div><strong>Underwriter Feedback:</strong> " + escapeHtml(reason != null ? reason : "Please submit clearer documents.") + "</div>"
                    + "</div>"
                    + "<p>Please log in to your Borrower Dashboard to update and resubmit your documents.</p>";
        }
        return sendHtmlEmail(toEmail, toName, subject, wrapLayout(body));
    }

    /**
     * Sequence Diagram: Dispatch Loan Disbursal Statement email.
     */
    public boolean sendLoanDisbursalEmail(String toEmail, String toName, Long loanId, BigDecimal principal,
                                          BigDecimal totalRepayment, String frequency, int terms) {
        String body = "<h2 style=\"color: #10b981; margin-top: 0;\">Loan Disbursed Successfully</h2>"
                + "<p>Hi " + escapeHtml(safeName(toName)) + ",</p>"
                + "<p>Your cash advance request (Loan #" + loanId + ") has been disbursed.</p>"
                + "<div style=\"background: #020617; padding: 16px; border-radius: 8px; margin: 16px 0; font-size: 14px;\">"
                + "<div><strong>Principal Disbursed:</strong> PHP " + principal + "</div>"
                + "<div><strong>Total Repayment (incl. interest &amp; fees):</strong> PHP " + totalRepayment + "</div>"
                + "<div><strong>Schedule:</strong> " + terms + " term(s) (" + escapeHtml(frequency) + ")</div>"
                + "</div>"
                + "<p>Log in anytime to view your itemized amortization schedule or make early repayments.</p>";
        return sendHtmlEmail(toEmail, toName, "Loan Disbursal Statement (Loan #" + loanId + ") - Utang na Loob", wrapLayout(body));
    }

    /**
     * FR-013: Notify borrower when their credit limit is automatically upgraded after consecutive on-time loans.
     */
    public boolean sendCreditLimitUpgradeEmail(String toEmail, String toName, BigDecimal oldLimit, BigDecimal newLimit) {
        String body = "<h2 style=\"color: #10b981; margin-top: 0;\">Credit Limit Upgraded!</h2>"
                + "<p>Hi " + escapeHtml(safeName(toName)) + ",</p>"
                + "<p>Congratulations on completing two consecutive installment loans on time! As a reward for your strong repayment history, your revolving credit limit has been automatically increased.</p>"
                + "<div style=\"background: #020617; padding: 16px; border-radius: 8px; margin: 16px 0; font-size: 14px;\">"
                + "<div><strong>Previous Limit:</strong> PHP " + oldLimit + "</div>"
                + "<div><strong>New Credit Limit:</strong> PHP " + newLimit + "</div>"
                + "</div>";
        return sendHtmlEmail(toEmail, toName, "Your Credit Limit Has Increased! - Utang na Loob", wrapLayout(body));
    }

    private String wrapLayout(String innerHtml) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 24px; background: #0f172a; color: #e2e8f0; border-radius: 12px;\">"
                + innerHtml
                + "</div>";
    }

    private String safeName(String toName) {
        return (toName != null && !toName.isBlank()) ? toName : "Valued Borrower";
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
