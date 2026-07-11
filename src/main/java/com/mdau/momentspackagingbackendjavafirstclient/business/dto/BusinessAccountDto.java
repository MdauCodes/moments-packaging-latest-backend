package com.mdau.momentspackagingbackendjavafirstclient.business.dto;

import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccount;
import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccountStatus;
import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class BusinessAccountDto {

    private final UUID id;
    private final String businessName;
    private final BusinessType businessType;
    private final String kraPin;
    private final String location;
    private final String road;
    private final String buildingAddress;
    private final UUID industryId;
    private final String industryName;
    private final String contactPersonName;
    private final String contactPersonRole;
    private final String phone;
    private final BusinessAccountStatus status;
    private final String welcomeCode;
    private final Instant createdAt;

    /** Set on both the customer's own "/me" view and the admin detail view —
     *  an early signal toward future trade-credit eligibility. */
    @Setter
    private Long orderCount;
    @Setter
    private java.math.BigDecimal totalSpend;
    @Setter
    private CreditReadinessDto creditReadiness;

    public BusinessAccountDto(BusinessAccount account) {
        this.id = account.getId();
        this.businessName = account.getBusinessName();
        this.businessType = account.getBusinessType();
        this.kraPin = account.getKraPin();
        this.location = account.getLocation();
        this.road = account.getRoad();
        this.buildingAddress = account.getBuildingAddress();
        this.industryId = account.getIndustry() != null ? account.getIndustry().getId() : null;
        this.industryName = account.getIndustry() != null ? account.getIndustry().getName() : null;
        this.contactPersonName = account.getContactPersonName();
        this.contactPersonRole = account.getContactPersonRole();
        this.phone = account.getPhone();
        this.status = account.getStatus();
        this.welcomeCode = account.getWelcomeCode();
        this.createdAt = account.getCreatedAt();
    }
}
