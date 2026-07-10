package com.mdau.momentspackagingbackendjavafirstclient.business.dto;

import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccount;
import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccountStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class BusinessAccountDto {

    private final UUID id;
    private final String businessName;
    private final String kraPin;
    private final String businessRegNumber;
    private final UUID industryId;
    private final String industryName;
    private final String contactPersonName;
    private final String contactPersonRole;
    private final String phone;
    private final BusinessAccountStatus status;
    private final Instant createdAt;

    /** Admin-only enrichment — null on the customer's own "/me" view. */
    @Setter
    private Long orderCount;
    @Setter
    private java.math.BigDecimal totalSpend;

    public BusinessAccountDto(BusinessAccount account) {
        this.id = account.getId();
        this.businessName = account.getBusinessName();
        this.kraPin = account.getKraPin();
        this.businessRegNumber = account.getBusinessRegNumber();
        this.industryId = account.getIndustry() != null ? account.getIndustry().getId() : null;
        this.industryName = account.getIndustry() != null ? account.getIndustry().getName() : null;
        this.contactPersonName = account.getContactPersonName();
        this.contactPersonRole = account.getContactPersonRole();
        this.phone = account.getPhone();
        this.status = account.getStatus();
        this.createdAt = account.getCreatedAt();
    }
}
