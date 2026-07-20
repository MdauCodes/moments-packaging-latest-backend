package com.mdau.momentspackagingbackendjavafirstclient.documentbundle.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.dto.DocumentBundleAdminDto;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundle;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundleStatus;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.repository.DocumentBundleRepository;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.service.DocumentBundleService;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.repository.ReceiptRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.repository.TaxDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Admin "Documents/PDFs" tab — pending ETR-gated bundles, upload-and-send, and retry-on-failure. */
@RestController
@RequestMapping("/api/v1/admin/document-bundles")
@RequiredArgsConstructor
@IsAdmin
public class AdminDocumentBundleController {

    private final DocumentBundleRepository documentBundleRepository;
    private final DocumentBundleService    documentBundleService;
    private final ReceiptRepository        receiptRepository;
    private final TaxDocumentRepository    taxDocumentRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<DocumentBundleAdminDto> list(
            @RequestParam(required = false) DocumentBundleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<DocumentBundle> bundles = status != null
                ? documentBundleRepository.findByStatus(status, pageable)
                : documentBundleRepository.findAll(pageable);
        return bundles.map(this::toDto);
    }

    /** Shortcut lookup used by the Orders processing drawer — null if this order never opted into the ETR bundle. */
    @GetMapping("/by-order/{orderReference}")
    @Transactional(readOnly = true)
    public DocumentBundleAdminDto byOrder(@PathVariable String orderReference) {
        DocumentBundle bundle = documentBundleRepository.findByOrder_Reference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException("No document bundle for order: " + orderReference));
        return toDto(bundle);
    }

    @PostMapping(value = "/{id}/upload-etr", consumes = "multipart/form-data")
    public DocumentBundleAdminDto uploadEtr(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return toDto(documentBundleService.uploadEtrAndSend(id, file));
    }

    @PostMapping("/{id}/retry")
    public DocumentBundleAdminDto retry(@PathVariable UUID id) {
        return toDto(documentBundleService.retrySend(id));
    }

    private DocumentBundleAdminDto toDto(DocumentBundle bundle) {
        String receiptUrl = receiptRepository.findByOrder_Reference(bundle.getOrder().getReference())
                .map(r -> r.getCloudinaryUrl()).orElse(null);
        String taxInvoiceUrl = taxDocumentRepository.findByOrder_Reference(bundle.getOrder().getReference())
                .map(d -> d.getCloudinaryUrl()).orElse(null);
        return new DocumentBundleAdminDto(bundle, receiptUrl, taxInvoiceUrl);
    }
}
