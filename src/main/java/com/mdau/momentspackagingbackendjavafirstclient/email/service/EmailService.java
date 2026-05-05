package com.mdau.momentspackagingbackendjavafirstclient.email.service;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.lead.entity.Lead;
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

    @Async
    public void sendEnquiryReceivedToSales(Enquiry enquiry) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("enquiry", enquiry);
            String html = templateEngine.process("email/enquiry-received", ctx);
            sendHtml(salesAddress, "New Enquiry — " + enquiry.getReference(), html);
            log.info("Enquiry-received email sent for {}", enquiry.getReference());
        } catch (Exception e) {
            log.error("Failed to send enquiry-received email: {}", e.getMessage());
        }
    }

    @Async
    public void sendEnquiryConfirmationToCustomer(Enquiry enquiry) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("enquiry", enquiry);
            String html = templateEngine.process("email/enquiry-confirmation", ctx);
            sendHtml(enquiry.getEmail(),
                    "We received your enquiry — " + enquiry.getReference(), html);
            log.info("Enquiry-confirmation email sent to {}", enquiry.getEmail());
        } catch (Exception e) {
            log.error("Failed to send enquiry-confirmation email: {}", e.getMessage());
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
            sendHtml(salesAddress, "Lead Digest — " + digestPeriod + " (" + leads.size() + " new)", html);
            log.info("Lead digest sent for period: {}", digestPeriod);
        } catch (Exception e) {
            log.error("Failed to send lead digest email: {}", e.getMessage());
        }
    }

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