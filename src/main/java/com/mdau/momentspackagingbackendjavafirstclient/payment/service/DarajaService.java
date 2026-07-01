package com.mdau.momentspackagingbackendjavafirstclient.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.PaymentGatewayException;
import com.mdau.momentspackagingbackendjavafirstclient.payment.config.DarajaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Safaricom M-Pesa Daraja API — STK Push (Lipa Na M-Pesa Online).
 * Runs alongside PayHero. PaymentMethod.MPESA routes here.
 * PaymentMethod.PAYHERO still routes to PayHeroService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DarajaService {

    private final DarajaConfig darajaConfig;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String OAUTH_URL_SUFFIX  = "/oauth/v1/generate?grant_type=client_credentials";
    private static final String STK_PUSH_URL_SUFFIX = "/mpesa/stkpush/v1/processrequest";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // -- Public API -------------------------------------------------------

    /**
     * Initiates STK Push via Daraja.
     * Returns the CheckoutRequestID which is stored as checkoutRequestId
     * on PaymentRecord — same field used by PayHero for callback matching.
     */
    public String initiateSTKPush(String phone, BigDecimal amount, String externalReference) {
        log.info("Daraja STK push: phone={} amount={} ref={}", phone, amount, externalReference);

        String token     = fetchAccessToken();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        String password  = buildPassword(timestamp);
        String normalized = normalizePhone(phone);

        Map<String, Object> payload = new HashMap<>();
        payload.put("BusinessShortCode", darajaConfig.getShortcode());
        payload.put("Password",          password);
        payload.put("Timestamp",         timestamp);
        payload.put("TransactionType",   darajaConfig.getTransactionType());
        payload.put("Amount",            amount.toBigInteger().toString());
        payload.put("PartyA",            normalized);
        payload.put("PartyB",            darajaConfig.getShortcode());
        payload.put("PhoneNumber",       normalized);
        payload.put("CallBackURL",       buildCallbackUrl());
        payload.put("AccountReference",  darajaConfig.getAccountReference());
        payload.put("TransactionDesc",   darajaConfig.getTransactionDesc());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String url = darajaConfig.getBaseUrl() + STK_PUSH_URL_SUFFIX;

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(payload, headers), Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new PaymentGatewayException(
                        "M-Pesa service returned an unexpected response. Please try again.",
                        "DARAJA_INVALID_RESPONSE");
            }

            String responseCode = String.valueOf(response.getBody().get("ResponseCode"));
            if (!"0".equals(responseCode)) {
                String desc = String.valueOf(response.getBody().getOrDefault("ResponseDescription", "Unknown error"));
                log.error("Daraja STK push rejected: code={} desc={}", responseCode, desc);
                throw new PaymentGatewayException(
                        "M-Pesa payment request rejected. Please try again.", "DARAJA_REJECTED");
            }

            String checkoutRequestId = (String) response.getBody().get("CheckoutRequestID");
            log.info("Daraja STK push accepted, checkoutRequestId={}", checkoutRequestId);
            return checkoutRequestId;

        } catch (PaymentGatewayException e) {
            throw e;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Daraja STK push HTTP error: {}", e.getResponseBodyAsString());
            String userMessage = parseDarajaError(e.getResponseBodyAsString());
            throw new PaymentGatewayException(userMessage, "DARAJA_HTTP_ERROR");
        } catch (Exception e) {
            log.error("Unexpected Daraja STK push error", e);
            throw new PaymentGatewayException(
                    "Unexpected M-Pesa error. Please try again.", "DARAJA_UNEXPECTED");
        }
    }

    // -- Helpers ----------------------------------------------------------

    String fetchAccessToken() {
        String credentials = darajaConfig.getConsumerKey() + ":" + darajaConfig.getConsumerSecret();
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);

        String url = darajaConfig.getBaseUrl() + OAUTH_URL_SUFFIX;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new PaymentGatewayException(
                        "Failed to connect to M-Pesa. Please try again.", "DARAJA_AUTH_FAILED");
            }
            String token = (String) response.getBody().get("access_token");
            if (token == null || token.isBlank()) {
                throw new PaymentGatewayException(
                        "M-Pesa authentication failed. Please try again.", "DARAJA_AUTH_EMPTY");
            }
            return token;
        } catch (PaymentGatewayException e) {
            throw e;
        } catch (Exception e) {
            log.error("Daraja OAuth failed", e);
            throw new PaymentGatewayException(
                    "M-Pesa authentication error. Please try again.", "DARAJA_AUTH_ERROR");
        }
    }

    /**
     * Safaricom's STK Push callback carries no signature Daraja lets us verify
     * against the payload, unlike PayHero (which echoes back a Basic Auth
     * header we can compare — see PayHeroService.isValidCallback()). The
     * callback URL is sent fresh with every STK push request rather than
     * fixed via Safaricom's portal, so embedding a secret in the URL path
     * itself is the practical equivalent: only someone who received our STK
     * push request (i.e. Safaricom) — or has access to our environment
     * config — could know that path segment. Without this, anyone who
     * guesses/observes a CheckoutRequestID could POST a fake "payment
     * succeeded" callback and get an order marked paid for free.
     */
    String buildCallbackUrl() {
        String base = darajaConfig.getCallbackUrl();
        if (base == null || base.isBlank()) return base;
        return base.replaceAll("/+$", "") + "/" + resolveCallbackSecret();
    }

    public boolean isValidCallbackSecret(String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        return MessageDigest.isEqual(
                resolveCallbackSecret().getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Explicit app.daraja.callback-secret wins if set; otherwise derives a
     * stable secret from config that's already required for STK push to
     * work at all (consumerSecret + passkey), so this never introduces a
     * new required env var that could block a deploy.
     */
    private String resolveCallbackSecret() {
        String explicit = darajaConfig.getCallbackSecret();
        if (explicit != null && !explicit.isBlank()) return explicit;
        try {
            String raw = darajaConfig.getConsumerSecret() + ":" + darajaConfig.getPasskey() + ":daraja-callback";
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on the JVM; this is unreachable in practice.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String buildPassword(String timestamp) {
        String raw = darajaConfig.getShortcode() + darajaConfig.getPasskey() + timestamp;
        return Base64.getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public String normalizePhone(String phone) {
        if (phone == null || phone.isBlank())
            throw new IllegalArgumentException("Phone number is required");
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("0"))          digits = "254" + digits.substring(1);
        else if (digits.startsWith("254"))   { /* ok */ }
        else if (digits.startsWith("7") || digits.startsWith("1"))
            digits = "254" + digits;
        else throw new IllegalArgumentException("Invalid Kenyan phone: " + phone);
        return digits;
    }

    private String parseDarajaError(String rawBody) {
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            String msg = node.path("errorMessage").asText("");
            if (!msg.isBlank()) return msg;
            msg = node.path("ResultDesc").asText("");
            if (!msg.isBlank()) return msg;
        } catch (Exception ignored) {}
        return "M-Pesa payment error. Please try again.";
    }
}