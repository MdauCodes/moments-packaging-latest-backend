package com.mdau.momentspackagingbackendjavafirstclient.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.PaymentGatewayException;
import com.mdau.momentspackagingbackendjavafirstclient.payment.config.PayHeroConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayHeroService {

    private final PayHeroConfig  payHeroConfig;
    private final ObjectMapper   objectMapper;
    private final RestTemplate   restTemplate = new RestTemplate();

    public String initiateSTKPush(String phone, BigDecimal amount,
                                  String externalReference) {
        log.info("Initiating PayHero STK push: phone={}, amount={}, ref={}",
                phone, amount, externalReference);

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount",             amount.intValue());
        payload.put("phone_number",       normalizePhone(phone));
        payload.put("channel_id",         Integer.parseInt(payHeroConfig.getChannelId()));
        payload.put("provider",           "m-pesa");
        payload.put("external_reference", externalReference);
        payload.put("callback_url",       payHeroConfig.getCallbackUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", buildBasicAuth());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    payHeroConfig.getBaseUrl(), request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new PaymentGatewayException(
                        "Payment service returned an unexpected response. Please try again.",
                        "GATEWAY_INVALID_RESPONSE"
                );
            }

            String checkoutRequestId = (String) response.getBody().get("CheckoutRequestID");
            log.info("STK push sent, checkoutRequestId={}", checkoutRequestId);
            return checkoutRequestId;

        } catch (PaymentGatewayException e) {
            throw e;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String rawBody = e.getResponseBodyAsString();
            log.error("PayHero STK push failed: {}", rawBody);

            String code        = "GATEWAY_ERROR";
            String userMessage = "Payment service error. Please try again.";

            try {
                JsonNode node         = objectMapper.readTree(rawBody);
                String  gatewayCode   = node.path("error_code").asText("");
                String  gatewayMsg    = node.path("error_message").asText("");

                if ("BAD_REQUEST".equals(gatewayCode) &&
                        gatewayMsg.toLowerCase().contains("insufficient balance")) {
                    code        = "MERCHANT_UNAVAILABLE";
                    userMessage = "Payment is temporarily unavailable. Please try again later or contact support.";
                } else if (!gatewayCode.isBlank()) {
                    code = gatewayCode;
                }
            } catch (Exception ignored) {}

            throw new PaymentGatewayException(userMessage, code);

        } catch (Exception e) {
            log.error("Unexpected error during STK push", e);
            throw new PaymentGatewayException(
                    "Unexpected payment error. Please try again.",
                    "GATEWAY_UNEXPECTED"
            );
        }
    }

    public boolean isValidCallback(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false;
        String expected = buildBasicAuth();
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                authHeader.getBytes(StandardCharsets.UTF_8));
    }

    private String buildBasicAuth() {
        String credentials = payHeroConfig.getApiUsername() + ":"
                + payHeroConfig.getApiPassword();
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public String normalizePhone(String phone) {
        if (phone == null || phone.isBlank())
            throw new IllegalArgumentException("Phone number is required");
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("0"))         digits = "254" + digits.substring(1);
        else if (digits.startsWith("254"))  { /* already correct */ }
        else if (digits.startsWith("7") || digits.startsWith("1"))
            digits = "254" + digits;
        else throw new IllegalArgumentException("Invalid Kenyan phone: " + phone);
        return digits;
    }
}