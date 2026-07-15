package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders the tax-invoice Thymeleaf template (same engine used for emails) to a PDF byte array
 * via openhtmltopdf. Server-side only — never touches the customer's browser, so it can be
 * generated and emailed without them being present (e.g. from a payment webhook in Phase 2).
 */
@Service
@RequiredArgsConstructor
public class TaxInvoicePdfService {

    private static final String KEY_BUSINESS_KRA_PIN = "business.kraPin";
    private static final DateTimeFormatter ISSUED_AT_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneId.of("Africa/Nairobi"));

    private final TemplateEngine templateEngine;
    private final SettingsService settingsService;

    public byte[] render(Order order) {
        Context ctx = new Context();
        ctx.setVariable("order", order);
        ctx.setVariable("issuedAt", ISSUED_AT_FORMAT.format(order.getCreatedAt()));
        ctx.setVariable("kraPin", settingsService.getValue(KEY_BUSINESS_KRA_PIN, ""));

        String html = templateEngine.process("tax-invoice", ctx);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Tax invoice PDF generation failed: " + e.getMessage(), e);
        }
    }
}
