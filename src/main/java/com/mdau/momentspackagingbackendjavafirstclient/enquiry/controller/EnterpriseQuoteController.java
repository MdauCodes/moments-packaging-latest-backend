package com.mdau.momentspackagingbackendjavafirstclient.enquiry.controller;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnterpriseQuoteRequest;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryCreateResponse;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository.EnquiryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/enterprise-quote")
@RequiredArgsConstructor
public class EnterpriseQuoteController {

    private final EnquiryRepository enquiryRepository;
    private final EmailService      emailService;

    @PostMapping
    public ResponseEntity<EnquiryCreateResponse> submitQuote(
            @Valid @RequestBody EnterpriseQuoteRequest request) {

        Enquiry enquiry = Enquiry.builder()
                .contactName(request.getContactName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .company(request.getCompanyName())
                .message(buildMessage(request))
                .source("enterprise")
                .build();

        Enquiry saved = enquiryRepository.save(enquiry);
        emailService.sendEnterpriseQuoteToSales(saved);

        log.info("Enterprise quote submitted by {}", request.getEmail());
        return ResponseEntity.status(201).body(new EnquiryCreateResponse(saved.getId()));
    }

    private String buildMessage(EnterpriseQuoteRequest r) {
        return String.format(
                "Estimated Quantity: %d\nProduct Interest: %s\nMessage: %s",
                r.getEstimatedQuantity(),
                r.getProductInterest() != null ? r.getProductInterest() : "—",
                r.getMessage() != null ? r.getMessage() : "—");
    }
}