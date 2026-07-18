package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocument;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocumentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.repository.TaxDocumentRepository;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadResponse;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadService;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadSignature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Phase 1: create the TaxDocument row with a one-time uploadToken. The PDF itself is generated
 * client-side (checkout.tsx / lib/pdf.ts) while the customer's browser is still present — the
 * frontend calls getUploadSignature() then completeUpload() once it's uploaded the file to
 * Cloudinary directly. generateAndUpload() (the original server-side Thymeleaf/openhtmltopdf
 * renderer) is kept as the fallback for cases with no browser attached: the payment webhook path
 * (sendIfRequested, below) still calls it if the frontend never completed the upload — e.g. a
 * delayed bank-transfer/COD confirmation, or the customer closing the tab right after checkout.
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
                .uploadToken(UUID.randomUUID().toString())
                .build();
        return taxDocumentRepository.save(doc);
    }

    /**
     * Issues a signed Cloudinary upload for the frontend to use directly — the backend never
     * touches the PDF bytes for this path. Scoped to a dedicated folder ("tax-invoices", distinct
     * from the "tax-documents" folder the server-side fallback renderer uploads to) so the two
     * paths are easy to tell apart in Cloudinary if needed, and gated by the one-time token so
     * only the browser that just placed this order can request one.
     */
    @Transactional(readOnly = true)
    public UploadSignature getUploadSignature(String orderReference, String token) {
        TaxDocument doc = requireByReferenceAndToken(orderReference, token);
        if (doc.getCloudinaryUrl() != null) {
            throw new IllegalStateException("Tax invoice already uploaded for this order");
        }
        String filename = "tax-invoice-" + orderReference + ".pdf";
        return uploadService.signRawUpload("tax-invoices", filename);
    }

    /**
     * Records the frontend-uploaded PDF's Cloudinary location. First writer wins — if the
     * payment-webhook fallback (generateAndUpload) already produced a URL by the time this
     * arrives (a race between the two paths), the frontend's own upload is deleted instead of
     * silently orphaning a Cloudinary asset nothing points to.
     */
    @Transactional
    public void completeUpload(String orderReference, String token, String cloudinaryUrl, String cloudinaryPublicId) {
        TaxDocument doc = requireByReferenceAndToken(orderReference, token);
        if (doc.getCloudinaryUrl() != null) {
            log.warn("Tax invoice for order {} already generated (likely webhook fallback beat the frontend upload); discarding client-uploaded copy", orderReference);
            uploadService.deleteRaw(cloudinaryPublicId);
            return;
        }
        doc.setCloudinaryUrl(cloudinaryUrl);
        doc.setCloudinaryPublicId(cloudinaryPublicId);
        taxDocumentRepository.save(doc);
        log.info("Tax invoice uploaded client-side for order {}: {}", orderReference, cloudinaryUrl);
    }

    /** Admin retry button (Phase 3) — re-runs the exact same regenerate-then-email path sendIfRequested uses. */
    @Transactional
    public TaxDocument retry(UUID taxDocumentId) {
        TaxDocument doc = taxDocumentRepository.findById(taxDocumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax document not found: " + taxDocumentId));
        sendIfRequested(doc.getOrder());
        return taxDocumentRepository.findById(taxDocumentId).orElseThrow();
    }

    /**
     * Phase 4 — called weekly by TaxDocumentCleanupJob. Deletes the Cloudinary asset for every
     * SENT document whose email went out more than 2 weeks ago (matching the "available for 2
     * weeks" promise in the email itself) and flips it to EXPIRED. Recoverable afterwards only
     * via the admin retry button, which regenerates a fresh PDF from the order.
     */
    @Transactional
    public int expireOldSentDocuments() {
        Instant cutoff = Instant.now().minus(14, java.time.temporal.ChronoUnit.DAYS);
        List<TaxDocument> due = taxDocumentRepository.findByStatusAndSentAtBefore(TaxDocumentStatus.SENT, cutoff);
        for (TaxDocument doc : due) {
            try {
                if (doc.getCloudinaryPublicId() != null) {
                    uploadService.deleteRaw(doc.getCloudinaryPublicId());
                }
                doc.setCloudinaryUrl(null);
                doc.setCloudinaryPublicId(null);
                doc.setStatus(TaxDocumentStatus.EXPIRED);
                taxDocumentRepository.save(doc);
            } catch (Exception e) {
                log.error("Failed to expire tax document {} (order {}): {}", doc.getId(), doc.getOrder().getReference(), e.getMessage(), e);
            }
        }
        return due.size();
    }

    private TaxDocument requireByReferenceAndToken(String orderReference, String token) {
        TaxDocument doc = taxDocumentRepository.findByOrder_Reference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException("No tax document requested for order: " + orderReference));
        if (token == null || !token.equals(doc.getUploadToken())) {
            throw new IllegalArgumentException("Invalid upload token");
        }
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
            String filename = "tax-invoice-" + doc.getOrder().getReference() + ".pdf";
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

        // Always regenerate here, even if a Cloudinary asset already exists — the frontend's
        // own checkout-time upload (see checkout.tsx uploadTaxInvoicePdf) necessarily fires
        // before payment is confirmed, so it captures a PENDING/pre-discount snapshot that can
        // never be corrected client-side. This runs once payment actually succeeds and has the
        // real, final order data, so it overwrites that early snapshot with an accurate one.
        generateAndUpload(doc.getId());
        doc = taxDocumentRepository.findById(doc.getId()).orElseThrow();
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
