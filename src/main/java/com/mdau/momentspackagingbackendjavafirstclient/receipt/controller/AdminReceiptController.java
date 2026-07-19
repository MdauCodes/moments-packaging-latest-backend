package com.mdau.momentspackagingbackendjavafirstclient.receipt.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.dto.ReceiptAdminDto;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.entity.Receipt;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.entity.ReceiptStatus;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.repository.ReceiptRepository;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.service.ReceiptPdfService;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Admin visibility for the automatic, universal payment receipt — mirrors AdminTaxDocumentController. */
@RestController
@RequestMapping("/api/v1/admin/receipts")
@RequiredArgsConstructor
@IsAdmin
public class AdminReceiptController {

    private final ReceiptRepository receiptRepository;
    private final ReceiptService    receiptService;
    private final ReceiptPdfService receiptPdfService;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<ReceiptAdminDto> list(
            @RequestParam(required = false) ReceiptStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Receipt> receipts = status != null
                ? receiptRepository.findByStatus(status, pageable)
                : receiptRepository.findAll(pageable);
        return receipts.map(ReceiptAdminDto::new);
    }

    @PostMapping("/{id}/retry")
    public ReceiptAdminDto retry(@PathVariable UUID id) {
        return new ReceiptAdminDto(receiptService.retry(id));
    }

    /** Regenerates the receipt PDF straight from the order's current data, bypassing Cloudinary entirely. */
    @GetMapping("/{id}/preview")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> preview(@PathVariable UUID id) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + id));
        byte[] pdf = receiptPdfService.render(receipt.getOrder());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"receipt-" + receipt.getOrder().getReference() + ".pdf\"")
                .body(pdf);
    }
}
