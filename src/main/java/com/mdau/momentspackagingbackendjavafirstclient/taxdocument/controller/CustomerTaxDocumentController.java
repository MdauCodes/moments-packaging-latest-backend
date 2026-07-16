package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.dto.TaxDocumentCustomerDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.repository.TaxDocumentRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Powers the Documents tab on the customer dashboard — every tax invoice this customer has requested, across all their orders. */
@RestController
@RequestMapping("/api/v1/customer/tax-documents")
@RequiredArgsConstructor
public class CustomerTaxDocumentController {

    private final TaxDocumentRepository taxDocumentRepository;

    @GetMapping
    public PageResponse<TaxDocumentCustomerDto> getMine(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PageResponse<>(
                taxDocumentRepository.findByOrder_CustomerOrderByCreatedAtDesc(user, pageable)
                        .map(TaxDocumentCustomerDto::new));
    }
}
