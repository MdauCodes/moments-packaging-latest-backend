package com.mdau.momentspackagingbackendjavafirstclient.payment.service;

import com.mdau.momentspackagingbackendjavafirstclient.payment.config.PayHeroConfig;
import com.mdau.momentspackagingbackendjavafirstclient.payment.dto.PayHeroCallbackDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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

    private final PayHeroConfig payHeroConfig;
    private final RestTemplate  restTemplate = new RestTemplate();

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
                throw new RuntimeException("PayHero returned non-success response");
            }

            String checkoutRequestId = (String) response.getBody().get("CheckoutRequestID");
            log.info("STK push sent, checkoutRequestId={}", checkoutRequestId);
            return checkoutRequestId;

        } catch (Exception e) {
            log.error("PayHero STK push failed: {}", e.getMessage());
            throw new RuntimeException("Failed to initiate STK push: " + e.getMessage());
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
        if (digits.startsWith("0"))        digits = "254" + digits.substring(1);
        else if (digits.startsWith("254")) { /* already correct */ }
        else if (digits.startsWith("7") || digits.startsWith("1"))
            digits = "254" + digits;
        else throw new IllegalArgumentException("Invalid Kenyan phone: " + phone);
        return digits;
    }
}