package com.mdau.momentspackagingbackendjavafirstclient.enquiry.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryCreateResponse;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryDto;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryStatusUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryItem;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository         enquiryRepository;
    private final EnquiryReferenceGenerator referenceGenerator;
    private final EmailService              emailService;

    @Transactional
    public EnquiryCreateResponse createEnquiry(EnquiryCreateRequest request) {
        String reference = referenceGenerator.generate();

        Enquiry enquiry = Enquiry.builder()
                .reference(reference)
                .persona(request.getPersona())
                .contactName(request.getContact().getName())
                .email(request.getContact().getEmail())
                .phone(request.getContact().getPhone())
                .company(request.getContact().getCompany())
                .message(request.getMessage())
                .source(request.getSource())
                .status(EnquiryStatus.NEW)
                .build();

        if (request.getItems() != null) {
            List<EnquiryItem> items = request.getItems().stream()
                    .map(itemReq -> EnquiryItem.builder()
                            .enquiry(enquiry)
                            .productId(itemReq.getProductId())
                            .productName(itemReq.getProductName())
                            .size(itemReq.getSize())
                            .material(itemReq.getMaterial())
                            .finish(itemReq.getFinish())
                            .quantity(itemReq.getQuantity())
                            .notes(itemReq.getNotes())
                            .build())
                    .collect(Collectors.toList());
            enquiry.setItems(items);
        }

        Enquiry saved = enquiryRepository.save(enquiry);
        log.info("Enquiry created: {}", reference);

        emailService.sendEnquiryReceivedToSales(saved);
        emailService.sendEnquiryConfirmationToCustomer(saved);

        return new EnquiryCreateResponse(saved.getId(), saved.getReference());
    }

    @Transactional(readOnly = true)
    public PageResponse<EnquiryDto> getAllEnquiries(EnquiryStatus status, Pageable pageable) {
        return new PageResponse<>(
                enquiryRepository.findAllWithFilters(status, pageable)
                        .map(EnquiryDto::new)
        );
    }

    @Transactional(readOnly = true)
    public EnquiryDto getById(UUID id) {
        return enquiryRepository.findById(id)
                .map(EnquiryDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found: " + id));
    }

    @Transactional
    public EnquiryDto updateStatus(UUID id, EnquiryStatusUpdateRequest request) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found: " + id));

        if (request.getStatus()        != null) enquiry.setStatus(request.getStatus());
        if (request.getAssignedTo()    != null) enquiry.setAssignedTo(request.getAssignedTo());
        if (request.getInternalNotes() != null) enquiry.setInternalNotes(request.getInternalNotes());

        return new EnquiryDto(enquiryRepository.save(enquiry));
    }
}