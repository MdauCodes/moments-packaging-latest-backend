package com.mdau.momentspackagingbackendjavafirstclient.lead.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.lead.dto.LeadCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.lead.dto.LeadCreateResponse;
import com.mdau.momentspackagingbackendjavafirstclient.lead.dto.LeadDto;
import com.mdau.momentspackagingbackendjavafirstclient.lead.dto.LeadUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.lead.entity.Lead;
import com.mdau.momentspackagingbackendjavafirstclient.lead.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;

    @Transactional
    public LeadCreateResponse createLead(LeadCreateRequest request) {
        return leadRepository.findByEmail(request.getEmail())
                .map(existing -> {
                    log.info("Duplicate lead, returning existing: {}", request.getEmail());
                    return new LeadCreateResponse(existing.getId());
                })
                .orElseGet(() -> {
                    Lead lead = Lead.builder()
                            .email(request.getEmail())
                            .persona(request.getPersona())
                            .source(request.getSource())
                            .trigger(request.getTrigger())
                            .contacted(false)
                            .build();
                    Lead saved = leadRepository.save(lead);
                    log.info("New lead captured: {}", saved.getEmail());
                    return new LeadCreateResponse(saved.getId());
                });
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadDto> getAllLeads(Pageable pageable) {
        Page<Lead> page = leadRepository.findAll(pageable);
        return new PageResponse<>(page.map(LeadDto::new));
    }

    @Transactional
    public LeadDto updateLead(UUID id, LeadUpdateRequest request) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
        if (request.getContacted() != null) {
            lead.setContacted(request.getContacted());
        }
        return new LeadDto(leadRepository.save(lead));
    }

    @Transactional(readOnly = true)
    public List<LeadDto> getRecentUncontacted(int days) {
        Instant since = Instant.now().minusSeconds((long) days * 24 * 60 * 60);
        return leadRepository.findRecentUncontacted(since)
                .stream()
                .map(LeadDto::new)
                .collect(Collectors.toList());
    }
}