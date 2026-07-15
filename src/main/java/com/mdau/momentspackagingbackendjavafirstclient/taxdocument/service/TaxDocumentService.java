package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
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

import java.util.UUID;

/**
 * Phase 1 scope: create the TaxDocument row and get the PDF generated + uploaded to Cloudinary.
 * Emailing it (Phase 2) and the admin retry/WhatsApp-helper UI (Phase 3) build on top of this —
 * generateAndUpload() is deliberately re-triggerable from just the TaxDocument/Order (not a
 * one-shot tied to checkout) since Phase 3's retry needs to call the exact same path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxDocumentService {

    private final TaxDocumentRepository taxDocumentRepository;
    private final TaxInvoicePdfService taxInvoicePdfService;
    private final UploadService uploadService;

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
}
