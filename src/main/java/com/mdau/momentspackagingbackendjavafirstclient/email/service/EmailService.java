package com.mdau.momentspackagingbackendjavafirstclient.email.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.lead.entity.Lead;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.payment.repository.PaymentRecordRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocument;
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
    private final PaymentRecordRepository paymentRecordRepository;

    @Value("${app.email.from}")
    private String fromAddress;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.email.sales-address:pkihara2008@gmail.com}")
    private String salesAddress;

    @Value("${app.email.notify-addresses:}")
    private String notifyAddresses;

    @Value("${app.email.ops-address:mdaucodes@gmail.com}")
    private String opsAddress;

    @Value("${app.email.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${app.email.brevo-api-url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    @Value("${app.email.use-brevo-api:true}")
    private boolean useBrevoApi;

    // Same values the frontend footer/WhatsApp button use (app.company.*), so every email's
    // footer matches what a customer already sees on the site instead of a separately
    // hardcoded (and easily stale) number/address per template.
    @Value("${app.company.email}")
    private String companyEmail;

    @Value("${app.company.phone}")
    private String companyPhone;

    @Value("${app.company.whatsapp-number}")
    private String companyWhatsapp;

    /** Sets the footer contact variables every customer-facing email template reads. */
    private void addCompanyContext(Context ctx) {
        ctx.setVariable("companyEmail", companyEmail);
        ctx.setVariable("companyPhone", companyPhone);
        ctx.setVariable("companyWhatsapp", companyWhatsapp);
    }

    // ---- Staff invite & password reset ----

    @Async
    public void sendStaffInviteEmail(User user, String tempPassword) {
        try {
            String roleName = user.getStaffRole() != null
                    ? user.getStaffRole().getDisplayName() : "Staff";
            String body = """
                    <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                      <h2 style="color:#1a472a">Welcome to Moments Packaging Kenya</h2>
                      <p>Hi %s,</p>
                      <p>You have been added to the Moments Packaging team as <strong>%s</strong>.</p>
                      <p>Your login details:</p>
                      <table style="border-collapse:collapse;width:100%%">
                        <tr><td style="padding:8px;font-weight:bold">Email:</td>
                            <td style="padding:8px;font-family:monospace">%s</td></tr>
                        <tr style="background:#f5f5f5">
                            <td style="padding:8px;font-weight:bold">Temporary password:</td>
                            <td style="padding:8px;font-family:monospace;font-size:18px;color:#1a472a">
                              <strong>%s</strong></td></tr>
                      </table>
                      <p style="color:#dc2626;margin-top:16px">
                        <strong>Warning: this password expires in 48 hours.</strong>
                        You will be prompted to change it on first login.
                      </p>
                      <p>If you do not log in within 48 hours, your account will be removed
                         and you will need to be re-invited by the administrator.</p>
                      <p style="margin-top:24px;color:#666;font-size:12px">
                        Moments Packaging Kenya - Internal staff access only.
                      </p>
                    </div>
                    """.formatted(
                    user.getFirstName(), roleName, user.getEmail(), tempPassword);

            sendHtml(user.getEmail(),
                    "You have been invited to Moments Packaging - " + roleName, body);
            log.info("Staff invite email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send staff invite email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendStaffPasswordResetEmail(User user, String tempPassword) {
        try {
            String body = """
                    <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                      <h2 style="color:#1a472a">Password Reset - Moments Packaging</h2>
                      <p>Hi %s,</p>
                      <p>Your password has been reset by the administrator.</p>
                      <p>Your new temporary password:</p>
                      <p style="font-family:monospace;font-size:24px;color:#1a472a;
                                background:#f0fdf4;padding:16px;border-radius:8px;
                                text-align:center">
                        <strong>%s</strong>
                      </p>
                      <p style="color:#dc2626">
                        <strong>Warning: this password expires in 48 hours.</strong>
                        You will be prompted to set a new password on login.
                      </p>
                    </div>
                    """.formatted(user.getFirstName(), tempPassword);

            sendHtml(user.getEmail(), "Your Moments Packaging password has been reset", body);
            log.info("Staff password reset email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send staff password reset email to {}: {}",
                    user.getEmail(), e.getMessage());
        }
    }

    // ---- Auth ----

    @Async
    public void sendOtpEmail(User user, String otp) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            addCompanyContext(ctx);
            ctx.setVariable("firstName", user.getFirstName());
            ctx.setVariable("otp", otp);
            String html = templateEngine.process("email/otp-verification", ctx);
            sendHtml(user.getEmail(), "Your Moments Packaging verification code", html);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetOtpEmail(User user, String otp) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            addCompanyContext(ctx);
            ctx.setVariable("firstName", user.getFirstName());
            ctx.setVariable("otp", otp);
            String html = templateEngine.process("email/password-reset", ctx);
            sendHtml(user.getEmail(), "Your Moments Packaging password reset code", html);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(User user) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            addCompanyContext(ctx);
            ctx.setVariable("firstName", user.getFirstName());
            String html = templateEngine.process("email/welcome", ctx);
            sendHtml(user.getEmail(), "Welcome to Moments Packaging Kenya!", html);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // ---- Enquiry ----

    @Async
    public void sendEnterpriseQuoteToSales(Enquiry enquiry) {
        try {
            String subject = "New Enquiry - " + enquiry.getContactName();
            String body = """
                    <h2>New Enquiry</h2>
                    <p><strong>Name:</strong> %s</p>
                    <p><strong>Email:</strong> %s</p>
                    <p><strong>Phone:</strong> %s</p>
                    <p><strong>Company:</strong> %s</p>
                    <p><strong>Message:</strong><br>%s</p>
                    """.formatted(
                    enquiry.getContactName(), enquiry.getEmail(),
                    enquiry.getPhone()   != null ? enquiry.getPhone()   : "-",
                    enquiry.getCompany() != null ? enquiry.getCompany() : "-",
                    enquiry.getMessage() != null ? enquiry.getMessage() : "-");
            for (String addr : notifyAddresses.split(",")) {
                String t = addr.trim();
                if (!t.isBlank()) sendHtml(t, subject, body);
            }
        } catch (Exception e) {
            log.error("Failed to send enquiry notification: {}", e.getMessage());
        }
    }

    @Async
    public void sendEnquiryAcknowledgement(Enquiry enquiry) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            addCompanyContext(ctx);
            ctx.setVariable("name", enquiry.getContactName());
            String html = templateEngine.process("email/enquiry-received", ctx);
            sendHtml(enquiry.getEmail(),
                    "We received your enquiry - Moments Packaging Kenya", html);
        } catch (Exception e) {
            log.error("Failed to send enquiry acknowledgement: {}", e.getMessage());
        }
    }

    // ---- Lead digest ----

    @Async
    public void sendLeadDigest(List<Lead> leads, String digestPeriod) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("leads", leads);
            ctx.setVariable("digestPeriod", digestPeriod);
            ctx.setVariable("count", leads.size());
            String html = templateEngine.process("email/lead-digest", ctx);
            for (String addr : notifyAddresses.split(",")) {
                String t = addr.trim();
                if (!t.isBlank()) sendHtml(t,
                        "Lead Digest - " + digestPeriod + " (" + leads.size() + " new)", html);
            }
        } catch (Exception e) {
            log.error("Failed to send lead digest: {}", e.getMessage());
        }
    }

    // ---- Low stock ----

    @Async
    public void sendLowStockAlert(List<Product> products) {
        try {
            StringBuilder rows = new StringBuilder();
            for (Product p : products) {
                rows.append("<tr><td>").append(p.getName()).append("</td>")
                        .append("<td>").append(p.getStockCount()).append("</td>")
                        .append("<td>").append(p.getLowStockThreshold()).append("</td></tr>");
            }
            String body = """
                    <h2>Low Stock Alert</h2>
                    <table border="1" cellpadding="8" style="border-collapse:collapse">
                      <tr style="background:#2d5016;color:#fff">
                        <th>Product</th><th>Stock</th><th>Threshold</th></tr>
                      %s
                    </table>
                    """.formatted(rows.toString());
            for (String addr : notifyAddresses.split(",")) {
                String t = addr.trim();
                if (!t.isBlank()) sendHtml(t,
                        "Low Stock Alert - " + products.size() + " product(s)", body);
            }
        } catch (Exception e) {
            log.error("Failed to send low stock alert: {}", e.getMessage());
        }
    }

    // ---- Order notifications ----

    @Async public void sendOrderConfirmedEmail(Order order) {
        sendOrderEmail(order, "email/order-confirmed",
                "Order Confirmed - " + order.getReference());
    }

    @Async
    public void sendOrderPaidEmail(Order order, String receiptUrl) {
        try {
            if (order.getItems() != null) org.hibernate.Hibernate.initialize(order.getItems());
            if (order.getStatusHistory() != null) org.hibernate.Hibernate.initialize(order.getStatusHistory());
            if (order.getCustomer() != null) org.hibernate.Hibernate.initialize(order.getCustomer());
            Context ctx = new Context(Locale.ENGLISH);
            addCompanyContext(ctx);
            ctx.setVariable("order", order);
            ctx.setVariable("receiptUrl", receiptUrl);
            List<com.mdau.momentspackagingbackendjavafirstclient.payment.entity.PaymentRecord> records =
                    paymentRecordRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
            ctx.setVariable("receiptNumber", records.isEmpty() ? null : records.get(0).getReceiptNumber());
            String html = templateEngine.process("email/order-paid", ctx);
            sendHtml(order.getEmail(), "Payment Received - " + order.getReference(), html);
            log.info("Order email [email/order-paid] sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order-paid email for {}: {}", order.getReference(), e.getMessage());
        }
    }

    @Async public void sendOrderInProductionEmail(Order order) {
        sendOrderEmail(order, "email/order-in-production",
                "Your Order is In Production - " + order.getReference());
    }

    @Async public void sendOrderDispatchedEmail(Order order) {
        sendOrderEmail(order, "email/order-dispatched",
                "Your Order is On Its Way - " + order.getReference());
    }

    @Async public void sendOrderDeliveredEmail(Order order) {
        sendOrderEmail(order, "email/order-delivered",
                "Order Delivered - " + order.getReference());
    }

    @Async public void sendOrderReadyForDispatchEmail(Order order) {
        sendOrderEmail(order, "email/order-ready-for-dispatch",
                "Your Order is Ready for Dispatch - " + order.getReference());
    }

    @Async public void sendOrderCancelledEmail(Order order) {
        sendOrderEmail(order, "email/order-cancelled",
                "Order Cancelled - " + order.getReference());
    }

    @Async
    public void sendPaymentFailedEmail(Order order, String failureReason) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            addCompanyContext(ctx);
            ctx.setVariable("order", order);
            ctx.setVariable("failureReason", failureReason);
            String html = templateEngine.process("email/payment-failed", ctx);
            sendHtml(order.getEmail(),
                    "Payment unsuccessful - " + order.getReference(), html);
        } catch (Exception e) {
            log.error("Failed to send payment failed email for {}: {}",
                    order.getReference(), e.getMessage());
        }
    }

    /**
     * Synchronous (not @Async) and throws on failure, unlike the order-status emails above —
     * TaxDocumentService needs to know whether the send actually succeeded so it can set the
     * TaxDocument's status to SENT or FAILED accordingly, rather than fire-and-forget.
     */
    public void sendTaxInvoiceReadyEmail(TaxDocument doc, String receiptUrl) throws Exception {
        Context ctx = new Context(Locale.ENGLISH);
        addCompanyContext(ctx);
        ctx.setVariable("order", doc.getOrder());
        ctx.setVariable("pdfUrl", doc.getCloudinaryUrl());
        ctx.setVariable("receiptUrl", receiptUrl);
        String html = templateEngine.process("email/tax-invoice-ready", ctx);
        sendHtml(doc.getRecipientEmail(), "Your tax invoice - " + doc.getOrder().getReference(), html);
    }

    /**
     * Synchronous and throws on failure, same reasoning as sendTaxInvoiceReadyEmail —
     * DocumentBundleService needs to know whether the send actually succeeded so it can set the
     * bundle's status to SENT or FAILED (with reason) rather than fire-and-forget.
     */
    public void sendDocumentBundleEmail(
            com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundle bundle,
            String receiptUrl, String taxInvoiceUrl) throws Exception {
        Context ctx = new Context(Locale.ENGLISH);
        addCompanyContext(ctx);
        ctx.setVariable("order", bundle.getOrder());
        ctx.setVariable("receiptUrl", receiptUrl);
        ctx.setVariable("taxInvoiceUrl", taxInvoiceUrl);
        ctx.setVariable("etrUrl", bundle.getEtrCloudinaryUrl());
        String html = templateEngine.process("email/document-bundle-ready", ctx);
        sendHtml(bundle.getRecipientEmail(), "Your receipt, tax invoice & ETR - " + bundle.getOrder().getReference(), html);
    }

    private void sendOrderEmail(Order order, String template, String subject) {
        try {
            // Eagerly initialize ALL lazy collections before Thymeleaf accesses them
            // Must be done here - async thread has no active Hibernate session
            if (order.getItems() != null) {
                org.hibernate.Hibernate.initialize(order.getItems());
            }
            if (order.getStatusHistory() != null) {
                org.hibernate.Hibernate.initialize(order.getStatusHistory());
            }
            if (order.getCustomer() != null) {
                org.hibernate.Hibernate.initialize(order.getCustomer());
            }
            Context ctx = new Context(Locale.ENGLISH);
            addCompanyContext(ctx);
            ctx.setVariable("order", order);
            String html = templateEngine.process(template, ctx);
            sendHtml(order.getEmail(), subject, html);
            log.info("Order email [{}] sent for {}", template, order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order email [{}] for {}: {}",
                    template, order.getReference(), e.getMessage());
        }
    }


    @Async
    public void sendStaffPasswordResetOtpEmail(User user, String otp) {
        try {
            String body = """
                    <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                      <h2 style="color:#1a472a">Password Reset - Moments Packaging</h2>
                      <p>Hi %s,</p>
                      <p>Use the 6-digit code below to reset your password.
                         It expires in <strong>15 minutes</strong>.</p>
                      <div style="text-align:center;margin:32px 0">
                        <p style="font-size:40px;font-weight:bold;letter-spacing:10px;color:#1a472a;
                                  background:#f0fdf4;padding:20px;border-radius:8px">%s</p>
                      </div>
                      <p style="color:#dc2626;font-size:12px">
                        If you did not request this, ignore this email - your password will not change.
                      </p>
                    </div>
                    """.formatted(user.getFirstName(), otp);
            sendHtml(user.getEmail(), "Reset your Moments Packaging password", body);
            log.info("Password reset OTP email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // ---- Core sender ----

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
            throw new RuntimeException("Brevo API error " + response.getStatusCode());
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

    @Async
    public void sendSyncFailureAlert(String message) {
        try {
            String body = "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto\">"
                    + "<h2 style=\"color:#dc2626\">Riseller Sync Alert</h2>"
                    + "<p>" + message + "</p>"
                    + "<p style=\"color:#666;font-size:12px\">Moments Packaging Kenya - Automated alert</p>"
                    + "</div>";
            for (String addr : opsAddress.split(",")) {
                String t = addr.trim();
                if (!t.isBlank()) sendHtml(t, "Riseller Sync Failure Alert", body);
            }
        } catch (Exception e) {
            log.error("Failed to send sync failure alert: {}", e.getMessage());
        }
    }
}
