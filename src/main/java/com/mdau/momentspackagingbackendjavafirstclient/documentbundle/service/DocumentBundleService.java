package com.mdau.momentspackagingbackendjavafirstclient.documentbundle.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundle;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundleStatus;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.repository.DocumentBundleRepository;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.repository.ReceiptRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.repository.TaxDocumentRepository;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadResponse;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Every order where the customer checked "Send me my ETR & tax documents" (Order.etrRequested)
 * gets a DocumentBundle the moment payment succeeds, sitting PENDING until an admin uploads the
 * ETR scan — see AdminDocumentBundleController. Uploading immediately triggers the send (receipt
 * + tax invoice + ETR emailed together) since "manually initiate sending" is the one act of
 * uploading, not a separate step; retrySend exists only for a FAILED send after the ETR is
 * already on file.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentBundleService {

    private static final int MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final byte[] PDF_MAGIC  = {0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC  = {(byte) 0x89, 0x50, 0x4E, 0x47};

    private final DocumentBundleRepository documentBundleRepository;
    private final ReceiptRepository        receiptRepository;
    private final TaxDocumentRepository    taxDocumentRepository;
    private final UploadService            uploadService;
    private final EmailService             emailService;

    /** Called from PaymentService once a bundle-opted-in order's payment succeeds. */
    @Transactional
    public DocumentBundle createPendingForOrder(Order order) {
        return documentBundleRepository.findByOrder_Reference(order.getReference())
                .orElseGet(() -> documentBundleRepository.save(DocumentBundle.builder()
                        .order(order)
                        .recipientEmail(order.getDocumentsEmail())
                        .status(DocumentBundleStatus.PENDING)
                        .build()));
    }

    /**
     * Admin upload — validates, uploads the ETR to Cloudinary, then immediately sends the bundle.
     * Works regardless of the bundle's current status, so it doubles as the "re-upload & resend"
     * action for a FAILED send or an EXPIRED (auto-deleted after 2 months) ETR — see
     * DocumentBundleCleanupJob. A prior ETR asset is deleted first so re-uploads don't orphan
     * old Cloudinary files.
     */
    @Transactional
    public DocumentBundle uploadEtrAndSend(UUID bundleId, MultipartFile file) {
        DocumentBundle bundle = documentBundleRepository.findById(bundleId)
                .orElseThrow(() -> new ResourceNotFoundException("Document bundle not found: " + bundleId));
        if (bundle.getEtrCloudinaryPublicId() != null) {
            try {
                uploadService.deleteRaw(bundle.getEtrCloudinaryPublicId());
            } catch (Exception e) {
                log.warn("Could not delete previous ETR asset for order {}: {}", bundle.getOrder().getReference(), e.getMessage());
            }
        }
        byte[] bytes = validateAndReadEtrFile(file);
        String filename = "etr-" + bundle.getOrder().getReference() + "-" + System.currentTimeMillis();
        UploadResponse uploaded = uploadService.uploadRaw(bytes, "etr-documents", filename);
        bundle.setEtrCloudinaryUrl(uploaded.getUrl());
        bundle.setEtrCloudinaryPublicId(uploaded.getPublicId());
        bundle.setEtrUploadedAt(Instant.now());
        documentBundleRepository.save(bundle);
        return sendBundle(bundle);
    }

    /** Admin retry — only valid once an ETR is already on file; re-attempts the email send alone. */
    @Transactional
    public DocumentBundle retrySend(UUID bundleId) {
        DocumentBundle bundle = documentBundleRepository.findById(bundleId)
                .orElseThrow(() -> new ResourceNotFoundException("Document bundle not found: " + bundleId));
        if (bundle.getEtrCloudinaryUrl() == null) {
            throw new IllegalStateException("Upload the ETR document before sending.");
        }
        return sendBundle(bundle);
    }

    /**
     * Called monthly by DocumentBundleCleanupJob. Deletes the Cloudinary ETR asset for every SENT
     * bundle whose ETR was uploaded more than 2 months ago and flips it to EXPIRED. Deliberately
     * a longer window than the tax invoice's 2-week retention (see TaxDocumentCleanupJob) — ETRs
     * are the KRA-compliance document customers are most likely to need to re-download later, so
     * they get more time before deletion, while still being minimized against Cloudinary cost.
     * Recoverable afterwards only via the admin re-upload action, same as an expired tax invoice.
     */
    @Transactional
    public int expireOldEtrs() {
        Instant cutoff = Instant.now().minus(60, java.time.temporal.ChronoUnit.DAYS);
        var due = documentBundleRepository.findByStatusAndEtrUploadedAtBefore(DocumentBundleStatus.SENT, cutoff);
        for (DocumentBundle bundle : due) {
            try {
                if (bundle.getEtrCloudinaryPublicId() != null) {
                    uploadService.deleteRaw(bundle.getEtrCloudinaryPublicId());
                }
                bundle.setEtrCloudinaryUrl(null);
                bundle.setEtrCloudinaryPublicId(null);
                bundle.setStatus(DocumentBundleStatus.EXPIRED);
                documentBundleRepository.save(bundle);
            } catch (Exception e) {
                log.error("Failed to expire ETR for bundle {} (order {}): {}",
                        bundle.getId(), bundle.getOrder().getReference(), e.getMessage(), e);
            }
        }
        return due.size();
    }

    private DocumentBundle sendBundle(DocumentBundle bundle) {
        Order order = bundle.getOrder();
        String receiptUrl = receiptRepository.findByOrder_Reference(order.getReference())
                .map(r -> r.getCloudinaryUrl()).orElse(null);
        String taxInvoiceUrl = taxDocumentRepository.findByOrder_Reference(order.getReference())
                .map(d -> d.getCloudinaryUrl()).orElse(null);
        try {
            emailService.sendDocumentBundleEmail(bundle, receiptUrl, taxInvoiceUrl);
            bundle.setStatus(DocumentBundleStatus.SENT);
            bundle.setSentAt(Instant.now());
            bundle.setFailureReason(null);
            log.info("Document bundle sent for order {} to {}", order.getReference(), bundle.getRecipientEmail());
        } catch (Exception e) {
            bundle.setStatus(DocumentBundleStatus.FAILED);
            bundle.setFailureReason(e.getMessage());
            log.error("Document bundle send failed for order {}: {}", order.getReference(), e.getMessage(), e);
        }
        return documentBundleRepository.save(bundle);
    }

    /** Accepts a scanned ETR as PDF, JPEG or PNG — magic-byte checked, same 5MB cap as other uploads. */
    private byte[] validateAndReadEtrFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }
        byte[] bytes;
        try (InputStream is = file.getInputStream()) {
            bytes = is.readAllBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read file: " + e.getMessage());
        }
        if (bytes.length < 4 || !(startsWith(bytes, PDF_MAGIC) || startsWith(bytes, JPEG_MAGIC) || startsWith(bytes, PNG_MAGIC))) {
            throw new IllegalArgumentException("Invalid file type. Only PDF, JPEG and PNG are allowed.");
        }
        return bytes;
    }

    private boolean startsWith(byte[] bytes, byte[] magic) {
        for (int i = 0; i < magic.length; i++) {
            if (bytes[i] != magic[i]) return false;
        }
        return true;
    }
}
