package com.mdau.momentspackagingbackendjavafirstclient.business.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * Phase 1 of the trade-account roadmap — a self-service business profile
 * attached to a customer's User account. Opening one issues a single-use
 * welcome promo code (see BusinessAccountService), but carries no ongoing
 * discount or credit terms of its own; it exists to accumulate an order
 * history that later becomes the prerequisite for applying for actual
 * trade credit (a separate, future phase).
 */
@Entity
@Table(name = "business_accounts", indexes = {
        @Index(name = "idx_business_accounts_user_id", columnList = "user_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", length = 30)
    private BusinessType businessType;

    /** Optional at this stage — only required later when applying for trade
     *  credit, so opening a Business Account doesn't feel like disclosing
     *  formal registration paperwork just to get a profile. */
    @Column(name = "kra_pin", length = 20)
    private String kraPin;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(nullable = false, length = 150)
    private String road;

    @Column(name = "building_address", nullable = false, length = 255)
    private String buildingAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id")
    private Industry industry;

    @Column(name = "contact_person_name", nullable = false, length = 150)
    private String contactPersonName;

    @Column(name = "contact_person_role", length = 100)
    private String contactPersonRole;

    @Column(nullable = false, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BusinessAccountStatus status = BusinessAccountStatus.ACTIVE;

    /** Denormalized copy of the auto-issued PromoCode.code for this account —
     *  avoids a join on every read since it's set once and never changes. */
    @Column(name = "welcome_code", length = 50)
    private String welcomeCode;
}
