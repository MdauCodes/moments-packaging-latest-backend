package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.dto.TaxDocumentAdminDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocumentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.repository.TaxDocumentRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service.TaxDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/** Phase 3 — the admin's view of every requested tax invoice: status, a retry button, and enough detail to build a WhatsApp helper message. */
@RestController
@RequestMapping("/api/v1/admin/tax-documents")
@RequiredArgsConstructor
@IsAdmin
public class AdminTaxDocumentController {

    private final TaxDocumentRepository taxDocumentRepository;
    private final TaxDocumentService    taxDocumentService;

    @GetMapping
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
}
