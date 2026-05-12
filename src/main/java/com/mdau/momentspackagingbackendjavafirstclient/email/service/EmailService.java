package com.mdau.momentspackagingbackendjavafirstclient.email.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.lead.entity.Lead;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender  mailSender;
    private final TemplateEngine  templateEngine;
    private final ObjectMapper    objectMapper;

    @Value("${app.email.from}")
    private String fromAddress;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.email.sales-address:pkihara2008@gmail.com}")
    private String salesAddress;

    @Value("${app.email.notify-addresses:}")
    private String notifyAddresses;

    @Value("${app.email.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${app.email.brevo-api-url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    @Value("${app.email.use-brevo-api:true}")
    private boolean useBrevoApi;

    @Async
    public void sendOtpEmail(User user, String otp) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("firstName", user.getFirstName());
            ctx.setVariable("otp", otp);
            String html = templateEngine.process("email/otp-verification", ctx);
            sendHtml(user.getEmail(), "Your Moments Packaging verification code", html);
            log.info("OTP email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetOtpEmail(User user, String otp) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("firstName", user.getFirstName());
            ctx.setVariable("otp", otp);
            String html = templateEngine.process("email/password-reset", ctx);
            sendHtml(user.getEmail(), "Your Moments Packaging password reset code", html);
            log.info("Password reset OTP email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(User user) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("firstName", user.getFirstName());
            String html = templateEngine.process("email/welcome", ctx);
            sendHtml(user.getEmail(), "Welcome to Moments Packaging Kenya!", html);
            log.info("Welcome email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendEnterpriseQuoteToSales(Enquiry enquiry) {
        try {
            String subject = "New Enquiry - " + enquiry.getContactName();
            String body = "<h2>New Enquiry</h2>"
                    + "<p><strong>Name:</strong> " + enquiry.getContactName() + "</p>"
                    + "<p><strong>Email:</strong> " + enquiry.getEmail() + "</p>"
                    + "<p><strong>Phone:</strong> " + (enquiry.getPhone() != null ? enquiry.getPhone() : "-") + "</p>"
                    + "<p><strong>Company:</strong> " + (enquiry.getCompany() != null ? enquiry.getCompany() : "-") + "</p>"
                    + "<p><strong>Message:</strong><br>" + (enquiry.getMessage() != null ? enquiry.getMessage() : "-") + "</p>";
            for (String addr : notifyAddresses.split(",")) {
                String trimmed = addr.trim();
                if (!trimmed.isBlank()) sendHtml(trimmed, subject, body);
            }
        } catch (Exception e) {
            log.error("Failed to send enquiry notification email: {}", e.getMessage());
        }
    }

    @Async
    public void sendEnquiryAcknowledgement(Enquiry enquiry) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("name", enquiry.getContactName());
            String html = templateEngine.process("email/enquiry-received", ctx);
            sendHtml(enquiry.getEmail(), "We received your enquiry - Moments Packaging Kenya", html);
            log.info("Enquiry acknowledgement sent to {}", enquiry.getEmail());
        } catch (Exception e) {
            log.error("Failed to send enquiry acknowledgement to {}: {}", enquiry.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendLeadDigest(List<Lead> leads, String digestPeriod) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("leads", leads);
            ctx.setVariable("digestPeriod", digestPeriod);
            ctx.setVariable("count", leads.size());
            String html = templateEngine.process("email/lead-digest", ctx);
            for (String addr : notifyAddresses.split(",")) {
                String trimmed = addr.trim();
                if (!trimmed.isBlank()) sendHtml(trimmed, "Lead Digest - " + digestPeriod + " (" + leads.size() + " new)", html);
            }
        } catch (Exception e) {
            log.error("Failed to send lead digest email: {}", e.getMessage());
        }
    }

    @Async
    public void sendLowStockAlert(List<Product> products) {
        try {
            StringBuilder rows = new StringBuilder();
            for (Product p : products) {
                rows.append("<tr><td>").append(p.getName()).append("</td>")
                    .append("<td>").append(p.getStockCount()).append("</td>")
                    .append("<td>").append(p.getLowStockThreshold()).append("</td></tr>");
            }
            String body = "<h2>Low Stock Alert - Moments Packaging</h2>"
                    + "<p>The following products are running low on stock:</p>"
                    + "<table border=\"1\" cellpadding=\"8\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%\">"
                    + "<tr style=\"background:#2d5016;color:#fff\"><th>Product</th><th>Stock</th><th>Threshold</th></tr>"
                    + rows.toString()
                    + "</table><p>Please restock soon.</p>";
            for (String addr : notifyAddresses.split(",")) {
                String trimmed = addr.trim();
                if (!trimmed.isBlank()) sendHtml(trimmed, "Low Stock Alert - " + products.size() + " product(s)", body);
            }
            log.info("Low stock alert sent for {} products", products.size());
        } catch (Exception e) {
            log.error("Failed to send low stock alert: {}", e.getMessage());
        }
    }

    @Async
    public void sendOrderConfirmedEmail(Order order) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("order", order);
            String html = templateEngine.process("email/order-confirmed", ctx);
            sendHtml(order.getEmail(), "Order Confirmed - " + order.getReference(), html);
            log.info("Order confirmed email sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order confirmed email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOrderPaidEmail(Order order) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("order", order);
            ctx.setVariable("receiptNumber", null);
            String html = templateEngine.process("email/order-paid", ctx);
            sendHtml(order.getEmail(), "Payment Received - " + order.getReference(), html);
            log.info("Order paid email sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order paid email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOrderInProductionEmail(Order order) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("order", order);
            String html = templateEngine.process("email/order-in-production", ctx);
            sendHtml(order.getEmail(), "Your Order is In Production - " + order.getReference(), html);
            log.info("Order in production email sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order in production email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOrderDispatchedEmail(Order order) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("order", order);
            String html = templateEngine.process("email/order-dispatched", ctx);
            sendHtml(order.getEmail(), "Your Order is On Its Way - " + order.getReference(), html);
            log.info("Order dispatched email sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order dispatched email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOrderDeliveredEmail(Order order) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("order", order);
            String html = templateEngine.process("email/order-delivered", ctx);
            sendHtml(order.getEmail(), "Order Delivered - " + order.getReference(), html);
            log.info("Order delivered email sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order delivered email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOrderReadyForDispatchEmail(Order order) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("order", order);
            String html = templateEngine.process("email/order-ready-for-dispatch", ctx);
            sendHtml(order.getEmail(), "Your Order is Ready for Dispatch - " + order.getReference(), html);
            log.info("Order ready for dispatch email sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order ready for dispatch email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOrderCancelledEmail(Order order) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("order", order);
            String html = templateEngine.process("email/order-cancelled", ctx);
            sendHtml(order.getEmail(), "Order Cancelled - " + order.getReference(), html);
            log.info("Order cancelled email sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order cancelled email: {}", e.getMessage());
        }
    }

    private void sendHtml(String to, String subject, String htmlBody) throws Exception {
        if (useBrevoApi && brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                sendViaBrevoApi(to, subject, htmlBody);
                log.debug("Email sent via Brevo API to {}", to);
                return;
            } catch (Exception e) {
                log.warn("Brevo API failed for {}, falling back to SMTP: {}", to, e.getMessage());
            }
        }
        sendViaSMTP(to, subject, htmlBody);
        log.debug("Email sent via SMTP to {}", to);
    }

    private void sendViaBrevoApi(String to, String subject, String htmlBody) throws Exception {
        Map<String, Object> sender = new HashMap<>();
        sender.put("name", fromName);
        sender.put("email", fromAddress);

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("email", to);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("to", List.of(recipient));
        payload.put("subject", subject);
        payload.put("htmlContent", htmlBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        String json = objectMapper.writeValueAsString(payload);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(brevoApiUrl, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Brevo API error " + response.getStatusCode() + ": " + response.getBody());
        }
    }

    private void sendViaSMTP(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }
}