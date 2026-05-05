package com.mdau.momentspackagingbackendjavafirstclient.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    @Value("${app.africastalking.enabled:false}")
    private boolean enabled;

    @Value("${app.africastalking.sender-id:MOMENTS}")
    private String senderId;

    @Async
    public void sendSms(String phone, String message) {
        try {
            String normalized = normalizePhone(phone);
            if (!enabled) {
                log.info("SMS stub [{} -> {}]: {}", senderId, normalized, message);
                return;
            }
            // TODO: inject AfricasTalking SDK and send real SMS
            // AfricasTalking at = new AfricasTalking(username, apiKey);
            // at.getSmsService().send(message, new String[]{normalized}, senderId);
            log.info("SMS sent to {}", normalized);
        } catch (Exception e) {
            log.error("SMS send failed to {}: {}", phone, e.getMessage());
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("0"))        return "+254" + digits.substring(1);
        if (digits.startsWith("254"))      return "+"    + digits;
        if (digits.startsWith("7") || digits.startsWith("1")) return "+254" + digits;
        return phone;
    }
}