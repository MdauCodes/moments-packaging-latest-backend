package com.mdau.momentspackagingbackendjavafirstclient.receipt.service;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.payment.repository.PaymentRecordRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Renders the proof-of-payment receipt (distinct from the opt-in tax invoice) to a PDF byte
 * array. Same server-side rendering approach as TaxInvoicePdfService — generated the moment
 * payment is confirmed, never dependent on the customer's browser being open.
 */
@Service
@RequiredArgsConstructor
public class ReceiptPdfService {

    private static final DateTimeFormatter PAID_AT_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneId.of("Africa/Nairobi"));

    private final TemplateEngine templateEngine;
    private final PaymentRecordRepository paymentRecordRepository;

    public byte[] render(Order order) {
        Context ctx = new Context();
        ctx.setVariable("order", order);
        ctx.setVariable("paidAt", PAID_AT_FORMAT.format(order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt()));

        List<com.mdau.momentspackagingbackendjavafirstclient.payment.entity.PaymentRecord> records =
                paymentRecordRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
        String receiptNumber = records.isEmpty() ? null : records.get(0).getReceiptNumber();
        ctx.setVariable("receiptNumber", receiptNumber);

        String html = templateEngine.process("receipt", ctx);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Receipt PDF generation failed: " + e.getMessage(), e);
        }
    }
}
