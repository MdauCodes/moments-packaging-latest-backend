package com.mdau.momentspackagingbackendjavafirstclient.enquiry.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryCreateResponse;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryDto;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final EmailService      emailService;

    private static final DateTimeFormatter NOTE_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                             .withZone(ZoneId.of("Africa/Nairobi"));

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
                .status(EnquiryStatus.NEW)
                .build();

        Enquiry saved = enquiryRepository.save(enquiry);
        log.info("Enquiry saved: {}", saved.getId());

        emailService.sendEnterpriseQuoteToSales(saved);
        emailService.sendEnquiryAcknowledgement(saved);

        return new EnquiryCreateResponse(saved.getId());
    }

    @Transactional
    public EnquiryDto updateEnquiry(UUID id, EnquiryUpdateRequest request, String updatedByEmail) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found: " + id));

        if (request.getStatus() != null) {
            EnquiryStatus prev = enquiry.getStatus();
            enquiry.setStatus(request.getStatus());
            // Record first contact timestamp
            if (prev == EnquiryStatus.NEW
                    && request.getStatus() == EnquiryStatus.CONTACTED
                    && enquiry.getFirstContactedAt() == null) {
                enquiry.setFirstContactedAt(Instant.now());
            }
        }

        if (request.getAssignedToId() != null) {
            enquiry.setAssignedToId(request.getAssignedToId());
        }
        if (request.getAssignedToName() != null) {
            enquiry.setAssignedToName(request.getAssignedToName());
        }
        if (request.getFollowUpAt() != null) {
            enquiry.setFollowUpAt(request.getFollowUpAt());
        }
        if (request.getEstimatedValue() != null) {
            enquiry.setEstimatedValue(request.getEstimatedValue());
        }
        if (request.getProductInterest() != null) {
            enquiry.setProductInterest(request.getProductInterest());
        }

        // Append note with timestamp + author
        if (request.getNote() != null && !request.getNote().isBlank()) {
            String stamp = "[" + NOTE_TS.format(Instant.now()) + " – " + updatedByEmail + "] ";
            String existing = enquiry.getInternalNotes();
            enquiry.setInternalNotes(
                    existing == null || existing.isBlank()
                            ? stamp + request.getNote()
                            : existing + "\n" + stamp + request.getNote());
        }

        return new EnquiryDto(enquiryRepository.save(enquiry));
    }

    @Transactional(readOnly = true)
    public EnquiryDto getById(UUID id) {
        return enquiryRepository.findById(id)
                .map(EnquiryDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found: " + id));
    }
}