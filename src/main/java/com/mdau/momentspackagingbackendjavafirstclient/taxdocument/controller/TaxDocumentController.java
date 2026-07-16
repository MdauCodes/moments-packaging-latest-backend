package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.controller;

import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.dto.CompleteUploadRequest;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service.TaxDocumentService;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadSignature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Unauthenticated (checkout is a guest flow) but token-gated — every call requires the one-time
 * uploadToken minted alongside the TaxDocument row at checkout, so only the browser that just
 * placed this specific order can request a signature or report a completed upload for it.
 */
@RestController
@RequestMapping("/api/v1/tax-documents")
@RequiredArgsConstructor
public class TaxDocumentController {

    private final TaxDocumentService taxDocumentService;

    @GetMapping("/{reference}/upload-signature")
    public UploadSignature getUploadSignature(@PathVariable String reference, @RequestParam String token) {
        return taxDocumentService.getUploadSignature(reference, token);
    }

    @PostMapping("/{reference}/complete")
    public void complete(@PathVariable String reference, @Valid @RequestBody CompleteUploadRequest request) {
        taxDocumentService.completeUpload(reference, request.getToken(), request.getCloudinaryUrl(), request.getCloudinaryPublicId());
    }
}
