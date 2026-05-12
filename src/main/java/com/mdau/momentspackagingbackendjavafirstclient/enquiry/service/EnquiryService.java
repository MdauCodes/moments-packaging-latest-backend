package com.mdau.momentspackagingbackendjavafirstclient.enquiry.service;

import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryCreateResponse;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final EmailService      emailService;

    @Transactional
    public EnquiryCreateResponse createEnquiry(EnquiryCreateRequest request) {
        Enquiry enquiry = Enquiry.builder()
                .persona(request.getPersona())
                .contactName(request.getContact().getName())
                .email(request.getContact().getEmail())
                .phone(request.getContact().getPhone())
                .company(request.getContact().getCompany())
                .message(request.getMessage())
                .source(request.getSource())
                .build();

        Enquiry saved = enquiryRepository.save(enquiry);
        log.info("Enterprise enquiry saved: {}", saved.getId());

        emailService.sendEnterpriseQuoteToSales(saved);
        emailService.sendEnquiryAcknowledgement(saved);

        return new EnquiryCreateResponse(saved.getId());
    }
}
