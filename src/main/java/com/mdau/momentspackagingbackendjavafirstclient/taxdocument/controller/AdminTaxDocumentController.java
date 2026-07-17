package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.dto.TaxDocumentAdminDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocument;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocumentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.repository.TaxDocumentRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service.TaxDocumentService;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service.TaxInvoicePdfService;
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

/** Phase 3 — the admin's view of every requested tax invoice: status, a retry button, and enough detail to build a WhatsApp helper message. */
@RestController
@RequestMapping("/api/v1/admin/tax-documents")
@RequiredArgsConstructor
@IsAdmin
public class AdminTaxDocumentController {

    private final TaxDocumentRepository taxDocumentRepository;
    private final TaxDocumentService    taxDocumentService;
    private final TaxInvoicePdfService  taxInvoicePdfService;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<TaxDocumentAdminDto> list(
            @RequestParam(required = false) TaxDocumentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocument> docs =
                status != null ? taxDocumentRepository.findByStatus(status, pageable) : taxDocumentRepository.findAll(pageable);
        return docs.map(TaxDocumentAdminDto::new);
    }

    @PostMapping("/{id}/retry")
    public TaxDocumentAdminDto retry(@PathVariable java.util.UUID id) {
        return new TaxDocumentAdminDto(taxDocumentService.retry(id));
    }

    /**
     * Regenerates the tax invoice PDF straight from the order's current data, bypassing
     * Cloudinary entirely — works even for EXPIRED documents whose Cloudinary asset has already
     * been deleted by the weekly cleanup job, since it never depends on that asset existing.
     */
    @GetMapping("/{id}/preview")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> preview(@PathVariable UUID id) {
        TaxDocument doc = taxDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax document not found: " + id));
        byte[] pdf = taxInvoicePdfService.render(doc.getOrder());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tax-invoice-" + doc.getOrder().getReference() + ".pdf\"")
                .body(pdf);
    }
}
