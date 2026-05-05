package com.mdau.momentspackagingbackendjavafirstclient.email.service;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.lead.entity.Lead;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromAddress;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.email.sales-address}")
    private String salesAddress;

    // ── Auth ──────────────────────────────────────────────────────────────────

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
            log.error("Failed to send OTP email: {}", e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(User user, String resetLink) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("firstName", user.getFirstName());
            ctx.setVariable("resetLink", resetLink);
            String html = templateEngine.process("email/password-reset", ctx);
            sendHtml(user.getEmail(), "Reset your Moments Packaging password", html);
            log.info("Password reset email sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
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
            log.error("Failed to send welcome email: {}", e.getMessage());
        }
    }

    // ── Enquiry ───────────────────────────────────────────────────────────────

    @Async
    public void sendEnterpriseQuoteToSales(Enquiry enquiry) {
        try {
            String subject = "New Enterprise Enquiry — " + enquiry.getContactName();
            String body = """
                    <h2>New Enterprise Quote Request</h2>
                    <p><strong>Name:</strong> %s</p>
                    <p><strong>Email:</strong> %s</p>
                    <p><strong>Phone:</strong> %s</p>
                    <p><strong>Company:</strong> %s</p>
                    <p><strong>Message:</strong><br>%s</p>
                    """.formatted(
                    enquiry.getContactName(), enquiry.getEmail(),
                    enquiry.getPhone()   != null ? enquiry.getPhone()   : "—",
                    enquiry.getCompany() != null ? enquiry.getCompany() : "—",
                    enquiry.getMessage() != null ? enquiry.getMessage() : "—");
            sendHtml(salesAddress, subject, body);
        } catch (Exception e) {
            log.error("Failed to send enterprise quote email: {}", e.getMessage());
        }
    }

    // ── Lead digest ───────────────────────────────────────────────────────────

    @Async
    public void sendLeadDigest(List<Lead> leads, String digestPeriod) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("leads", leads);
            ctx.setVariable("digestPeriod", digestPeriod);
            ctx.setVariable("count", leads.size());
            String html = templateEngine.process("email/lead-digest", ctx);
            sendHtml(salesAddress,
                    "Lead Digest — " + digestPeriod + " (" + leads.size() + " new)", html);
        } catch (Exception e) {
            log.error("Failed to send lead digest email: {}", e.getMessage());
        }
    }

    // ── Low stock alert ───────────────────────────────────────────────────────

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
                    <h2>Low Stock Alert — Moments Packaging</h2>
                    <p>The following products are running low on stock:</p>
                    <table border="1" cellpadding="8" cellspacing="0"
                           style="border-collapse:collapse;width:100%%">
                      <tr style="background:#2d5016;color:#fff">
                        <th>Product</th><th>Stock</th><th>Threshold</th>
                      </tr>
                      %s
                    </table>
                    <p>Please restock soon.</p>
                    """.formatted(rows.toString());
            sendHtml(salesAddress,
                    "Low Stock Alert — " + products.size() + " product(s)", body);
            log.info("Low stock alert sent for {} products", products.size());
        } catch (Exception e) {
            log.error("Failed to send low stock alert: {}", e.getMessage());
        }
    }

    // ── Order notifications ───────────────────────────────────────────────────

    @Async
    public void sendOrderConfirmedEmail(Order order) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("order", order);
            String html = templateEngine.process("email/order-confirmed", ctx);
            sendHtml(order.getEmail(), "Order Confirmed — " + order.getReference(), html);
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
            sendHtml(order.getEmail(), "Payment Received — " + order.getReference(), html);
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
            sendHtml(order.getEmail(),
                    "Your Order is In Production — " + order.getReference(), html);
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
            sendHtml(order.getEmail(),
                    "Your Order is On Its Way — " + order.getReference(), html);
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
            sendHtml(order.getEmail(),
                    "Order Delivered — " + order.getReference(), html);
            log.info("Order delivered email sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order delivered email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOrderCancelledEmail(Order order) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("order", order);
            String html = templateEngine.process("email/order-cancelled", ctx);
            sendHtml(order.getEmail(),
                    "Order Cancelled — " + order.getReference(), html);
            log.info("Order cancelled email sent for {}", order.getReference());
        } catch (Exception e) {
            log.error("Failed to send order cancelled email: {}", e.getMessage());
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }
}