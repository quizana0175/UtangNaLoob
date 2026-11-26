package com.utangnaloob.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utangnaloob.dto.MessageResponse;
import com.utangnaloob.dto.PaymentReceiptResponse;
import com.utangnaloob.service.LendingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * FR-019 / FR-020 / FR-022 / NFR-S4:
 * Public payment gateway webhook endpoint protected by HMAC-SHA256 signature verification
 * and idempotent external_transaction_id reconciliation.
 */
@CrossOrigin(originPatterns = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentWebhookController {

    @Value("${payment.webhook.secret:utangnaloob-sandbox-webhook-secret}")
    private String webhookSecret;

    @Autowired
    private LendingService lendingService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<?> handlePaymentWebhook(
            @RequestHeader(value = "X-Signature", required = false) String signatureHeader,
            @RequestBody String rawPayload,
            HttpServletRequest request) {

        if (signatureHeader == null || !verifyHmacSha256(rawPayload, signatureHeader)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Invalid or missing HMAC-SHA256 webhook signature.", false));
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventType = root.path("event").asText("payment.paid");

            if (!"checkout.session.completed".equalsIgnoreCase(eventType)
                    && !"payment.paid".equalsIgnoreCase(eventType)) {
                return ResponseEntity.ok(new MessageResponse("Webhook event acknowledged: " + eventType, true));
            }

            JsonNode data = root.has("data") ? root.get("data") : root;
            String externalPaymentId = data.path("externalTransactionId").asText("");
            long loanId = data.path("loanId").asLong(0);
            Long installmentId = data.hasNonNull("installmentId") ? data.get("installmentId").asLong() : null;
            BigDecimal amount = new BigDecimal(data.path("amount").asText("0"));
            String paymentMethod = data.path("paymentMethod").asText("PAYMONGO_WEBHOOK");

            PaymentReceiptResponse receipt = lendingService.processWebhookPayment(
                    externalPaymentId,
                    loanId,
                    installmentId,
                    amount,
                    paymentMethod,
                    request.getRemoteAddr()
            );

            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Webhook processing error: " + e.getMessage(), false));
        }
    }

    private boolean verifyHmacSha256(String payload, String providedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedHex = HexFormat.of().formatHex(hash);
            String cleanProvided = providedSignature.trim().replaceFirst("^sha256=", "");
            return MessageDigest.isEqual(
                    expectedHex.getBytes(StandardCharsets.UTF_8),
                    cleanProvided.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
