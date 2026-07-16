package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocument;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocumentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.repository.TaxDocumentRepository;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadResponse;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Phase 1: create the TaxDocument row and get the PDF generated + uploaded to Cloudinary.
 * Phase 2 (this file's sendIfRequested): triggered from PaymentService once payment succeeds —
 * emails the Cloudinary link and flips status to SENT/FAILED. The admin retry/WhatsApp-helper UI
 * (Phase 3) builds on top of this — generateAndUpload() and sendIfRequested() are both
 * deliberately re-triggerable from just the Order/TaxDocument, not one-shot, since Phase 3's
 * retry button needs to call the exact same paths.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxDocumentService {

    private final TaxDocumentRepository taxDocumentRepository;
    private final TaxInvoicePdfService taxInvoicePdfService;
    private final UploadService uploadService;
    private final EmailService emailService;

    @Transactional
    public TaxDocument requestForOrder(Order order) {
        TaxDocument doc = TaxDocument.builder()
                .order(order)
                .recipientEmail(order.getTaxInvoiceEmail() != null ? order.getTaxInvoiceEmail() : order.getEmail())
                .status(TaxDocumentStatus.PENDING)
                .build();
        doc = taxDocumentRepository.save(doc);
        generateAndUpload(doc.getId());
        return doc;
    }

    /**
     * Renders the PDF and uploads it to Cloudinary. Resilient by design — a failure here sets
     * status=FAILED with a reason rather than throwing, since order creation (and later, the
     * payment webhook) must never fail because a tax document couldn't be produced.
     */
    @Transactional
    public void generateAndUpload(UUID taxDocumentId) {
        TaxDocument doc = taxDocumentRepository.findById(taxDocumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax document not found: " + taxDocumentId));
        doc.setStatus(TaxDocumentStatus.GENERATING);
        doc.setFailureReason(null);
        taxDocumentRepository.save(doc);

        try {
            byte[] pdfBytes = taxInvoicePdfService.render(doc.getOrder());
            String filename = "tax-invoice-" + doc.getOrder().getReference();
            UploadResponse uploaded = uploadService.uploadRaw(pdfBytes, "tax-documents", filename);
            doc.setCloudinaryUrl(uploaded.getUrl());
            doc.setCloudinaryPublicId(uploaded.getPublicId());
            // Generated and stored, not yet emailed — Phase 2 flips this to SENT once the
            // email actually goes out. Staying at PENDING here (not a new "GENERATED" state)
            // deliberately keeps the state model to the 5 states already agreed on.
            doc.setStatus(TaxDocumentStatus.PENDING);
            taxDocumentRepository.save(doc);
            log.info("Tax invoice generated for order {}: {}", doc.getOrder().getReference(), uploaded.getUrl());
        } catch (Exception e) {
            doc.setStatus(TaxDocumentStatus.FAILED);
            doc.setFailureReason(e.getMessage());
            taxDocumentRepository.save(doc);
            log.error("Tax invoice generation failed for order {}: {}", doc.getOrder().getReference(), e.getMessage(), e);
        }
    }

    /**
     * Called from PaymentService once an order's payment succeeds. If checkout's initial
     * generateAndUpload() failed (e.g. a transient Cloudinary hiccup), retries it here — payment
     * confirmation often follows checkout by only seconds to minutes, so this is a natural
     * second chance before giving up and requiring an admin to notice and retry manually.
     */
    @Transactional
    public void sendIfRequested(Order order) {
        Optional<TaxDocument> maybeDoc = taxDocumentRepository.findByOrder_Reference(order.getReference());
        if (maybeDoc.isEmpty()) {
            log.warn("Order {} has taxInvoiceRequested=true but no TaxDocument row exists", order.getReference());
            return;
        }
        TaxDocument doc = maybeDoc.get();

        if (doc.getCloudinaryUrl() == null) {
            generateAndUpload(doc.getId());
            doc = taxDocumentRepository.findById(doc.getId()).orElseThrow();
        }
        if (doc.getCloudinaryUrl() == null) {
            // generateAndUpload already recorded the failure reason on the doc — nothing more to do here.
            return;
        }

        try {
            emailService.sendTaxInvoiceReadyEmail(doc);
            doc.setStatus(TaxDocumentStatus.SENT);
            doc.setSentAt(Instant.now());
            doc.setFailureReason(null);
            taxDocumentRepository.save(doc);
            log.info("Tax invoice emailed for order {} to {}", order.getReference(), doc.getRecipientEmail());
        } catch (Exception e) {
            doc.setStatus(TaxDocumentStatus.FAILED);
            doc.setFailureReason("Email send failed: " + e.getMessage());
            taxDocumentRepository.save(doc);
            log.error("Tax invoice email failed for order {}: {}", order.getReference(), e.getMessage(), e);
        }
    }
}
