package com.mdau.momentspackagingbackendjavafirstclient.receipt.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.entity.Receipt;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.entity.ReceiptStatus;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.repository.ReceiptRepository;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadResponse;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Every order gets a Receipt automatically the moment payment is confirmed successful —
 * unlike TaxDocument (opt-in, requested at checkout), this is universal and has no
 * customer-facing request step. Generated server-side only, same pattern as
 * TaxDocumentService.generateAndUpload, so it never depends on the customer's browser
 * being open at the moment of payment confirmation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptPdfService receiptPdfService;
    private final UploadService uploadService;

    /**
     * Generates, uploads, and returns the Receipt for an order that has just been paid.
     * Called unconditionally from PaymentService — never before payment succeeds.
     * Resilient by design: a failure here must never block the payment flow itself.
     */
    @Transactional
    public Receipt generateForPaidOrder(Order order) {
        Receipt receipt = receiptRepository.findByOrder_Reference(order.getReference())
                .orElseGet(() -> Receipt.builder()
                        .order(order)
                        .recipientEmail(order.getEmail())
                        .status(ReceiptStatus.PENDING)
                        .build());
        receipt.setStatus(ReceiptStatus.GENERATING);
        receipt.setFailureReason(null);
        receiptRepository.save(receipt);

        try {
            byte[] pdfBytes = receiptPdfService.render(order);
            String filename = "receipt-" + order.getReference() + ".pdf";
            UploadResponse uploaded = uploadService.uploadRaw(pdfBytes, "receipts", filename);
            receipt.setCloudinaryUrl(uploaded.getUrl());
            receipt.setCloudinaryPublicId(uploaded.getPublicId());
            receipt.setStatus(ReceiptStatus.SENT);
            receipt.setSentAt(java.time.Instant.now());
            receiptRepository.save(receipt);
            log.info("Receipt generated for order {}: {}", order.getReference(), uploaded.getUrl());
        } catch (Exception e) {
            receipt.setStatus(ReceiptStatus.FAILED);
            receipt.setFailureReason(e.getMessage());
            receiptRepository.save(receipt);
            log.error("Receipt generation failed for order {}: {}", order.getReference(), e.getMessage(), e);
        }
        return receipt;
    }

    /** Admin retry — regenerates from the order's current data, same as a first-time generation. */
    @Transactional
    public Receipt retry(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));
        return generateForPaidOrder(receipt.getOrder());
    }
}
