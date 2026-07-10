package com.mdau.momentspackagingbackendjavafirstclient.business.service;

import com.mdau.momentspackagingbackendjavafirstclient.business.dto.BusinessAccountCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.business.dto.BusinessAccountDto;
import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccount;
import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccountStatus;
import com.mdau.momentspackagingbackendjavafirstclient.business.repository.BusinessAccountRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.industry.repository.IndustryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessAccountService {

    private final BusinessAccountRepository businessAccountRepository;
    private final IndustryRepository industryRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public BusinessAccountDto create(User user, BusinessAccountCreateRequest request) {
        if (businessAccountRepository.existsByUserId(user.getId())) {
            throw new ConflictException("This account already has a business profile.");
        }
        BusinessAccount account = BusinessAccount.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .kraPin(request.getKraPin())
                .businessRegNumber(request.getBusinessRegNumber())
                .industry(resolveIndustry(request.getIndustryId()))
                .contactPersonName(request.getContactPersonName())
                .contactPersonRole(request.getContactPersonRole())
                .phone(request.getPhone())
                .build();
        return new BusinessAccountDto(businessAccountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public BusinessAccountDto getMine(User user) {
        return businessAccountRepository.findByUserId(user.getId())
                .map(BusinessAccountDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("No business account for this user."));
    }

    @Transactional
    public BusinessAccountDto updateMine(User user, BusinessAccountCreateRequest request) {
        BusinessAccount account = businessAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No business account for this user."));
        account.setBusinessName(request.getBusinessName());
        account.setKraPin(request.getKraPin());
        account.setBusinessRegNumber(request.getBusinessRegNumber());
        account.setIndustry(resolveIndustry(request.getIndustryId()));
        account.setContactPersonName(request.getContactPersonName());
        account.setContactPersonRole(request.getContactPersonRole());
        account.setPhone(request.getPhone());
        return new BusinessAccountDto(businessAccountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public PageResponse<BusinessAccountDto> listAll(String search, Pageable pageable) {
        String q = search != null ? search : "";
        return new PageResponse<>(
                businessAccountRepository
                        .findByBusinessNameContainingIgnoreCaseOrKraPinContainingIgnoreCase(q, q, pageable)
                        .map(BusinessAccountDto::new));
    }

    @Transactional(readOnly = true)
    public BusinessAccountDto getById(UUID id) {
        BusinessAccount account = businessAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business account not found: " + id));
        BusinessAccountDto dto = new BusinessAccountDto(account);
        Object[] stats = orderRepository.getOrderStatsForCustomer(account.getUser()).get(0);
        dto.setOrderCount((Long) stats[0]);
        dto.setTotalSpend((BigDecimal) stats[1]);
        return dto;
    }

    @Transactional
    public BusinessAccountDto setStatus(UUID id, BusinessAccountStatus status) {
        BusinessAccount account = businessAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business account not found: " + id));
        account.setStatus(status);
        return new BusinessAccountDto(businessAccountRepository.save(account));
    }

    private Industry resolveIndustry(UUID industryId) {
        if (industryId == null) return null;
        return industryRepository.findById(industryId)
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found: " + industryId));
    }
}
