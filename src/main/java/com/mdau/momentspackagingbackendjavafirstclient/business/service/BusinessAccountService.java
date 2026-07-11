package com.mdau.momentspackagingbackendjavafirstclient.business.service;

import com.mdau.momentspackagingbackendjavafirstclient.business.dto.BusinessAccountCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.business.dto.BusinessAccountDto;
import com.mdau.momentspackagingbackendjavafirstclient.business.dto.CreditReadinessDto;
import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccount;
import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccountStatus;
import com.mdau.momentspackagingbackendjavafirstclient.business.repository.BusinessAccountRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.industry.repository.IndustryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.DiscountType;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PromoCode;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.PromoCodeRepository;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessAccountService {

    // Admin-editable via the generic /api/v1/admin/settings endpoint —
    // seeded by DiscountSettingsSeeder so they're visible there from boot.
    private static final String WELCOME_DISCOUNT_PERCENT_KEY = "discounts.welcomeCodePercent";
    private static final String WELCOME_DISCOUNT_MIN_ORDER_KEY = "discounts.welcomeCodeMinOrderAmount";
    private static final String WELCOME_DISCOUNT_VALID_DAYS_KEY = "discounts.welcomeCodeValidDays";
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BusinessAccountRepository businessAccountRepository;
    private final IndustryRepository industryRepository;
    private final OrderRepository orderRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final SettingsService settingsService;

    @Transactional
    public BusinessAccountDto create(User user, BusinessAccountCreateRequest request) {
        if (businessAccountRepository.existsByUserId(user.getId())) {
            throw new ConflictException("This account already has a business profile.");
        }
        BusinessAccount account = BusinessAccount.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .businessType(request.getBusinessType())
                .kraPin(request.getKraPin())
                .location(request.getLocation())
                .road(request.getRoad())
                .buildingAddress(request.getBuildingAddress())
                .industry(resolveIndustry(request.getIndustryId()))
                .contactPersonName(request.getContactPersonName())
                .contactPersonRole(request.getContactPersonRole())
                .phone(request.getPhone())
                .build();
        account.setWelcomeCode(issueWelcomeCode(user));
        return new BusinessAccountDto(businessAccountRepository.save(account));
    }

    /**
     * Every new Business Account earns a unique, single-use promo code
     * redeemable only by that account — percentage off, minimum order
     * amount, and validity window are all read from admin-editable
     * settings, never hardcoded per order.
     */
    private String issueWelcomeCode(User user) {
        String code = generateUniqueCode();
        BigDecimal percent = new BigDecimal(settingsService.getValue(WELCOME_DISCOUNT_PERCENT_KEY, "5"));
        BigDecimal minOrderAmount = new BigDecimal(settingsService.getValue(WELCOME_DISCOUNT_MIN_ORDER_KEY, "5000"));
        int validDays = Integer.parseInt(settingsService.getValue(WELCOME_DISCOUNT_VALID_DAYS_KEY, "30"));
        PromoCode promo = PromoCode.builder()
                .code(code)
                .discountType(DiscountType.PERCENT)
                .discountValue(percent)
                .minOrderAmount(minOrderAmount)
                .maxUses(1)
                .active(true)
                .expiresAt(Instant.now().plus(validDays, ChronoUnit.DAYS))
                .restrictedToUserId(user.getId())
                .build();
        promoCodeRepository.save(promo);
        return code;
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("WELCOME-");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (promoCodeRepository.existsByCodeIgnoreCase(code));
        return code;
    }

    @Transactional(readOnly = true)
    public BusinessAccountDto getMine(User user) {
        BusinessAccount account = businessAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No business account for this user."));
        return withOrderStats(new BusinessAccountDto(account), account);
    }

    @Transactional
    public BusinessAccountDto updateMine(User user, BusinessAccountCreateRequest request) {
        BusinessAccount account = businessAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No business account for this user."));
        account.setBusinessName(request.getBusinessName());
        account.setBusinessType(request.getBusinessType());
        account.setKraPin(request.getKraPin());
        account.setLocation(request.getLocation());
        account.setRoad(request.getRoad());
        account.setBuildingAddress(request.getBuildingAddress());
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
        return withOrderStats(new BusinessAccountDto(account), account);
    }

    /** Order count / lifetime spend / credit readiness — shown to the account
     *  owner and to admin alike, as an early signal toward future
     *  trade-credit eligibility. Never used to auto-approve anything. */
    private BusinessAccountDto withOrderStats(BusinessAccountDto dto, BusinessAccount account) {
        Object[] stats = orderRepository.getOrderStatsForCustomer(account.getUser()).get(0);
        Long orderCount = (Long) stats[0];
        BigDecimal totalSpend = (BigDecimal) stats[1];
        Instant lastOrderAt = (Instant) stats[2];
        dto.setOrderCount(orderCount);
        dto.setTotalSpend(totalSpend);
        dto.setCreditReadiness(computeReadiness(orderCount, totalSpend, account.getCreatedAt(), lastOrderAt));
        return dto;
    }

    /**
     * A deliberately simple, transparent scoring model — not a real credit
     * score. Four factors, each capped, so a business can see exactly what
     * moves the number: how often they order, how much, how long they've
     * held the account, and whether they've ordered recently.
     */
    private CreditReadinessDto computeReadiness(
            long orderCount, BigDecimal totalSpend, Instant accountCreatedAt, Instant lastOrderAt) {
        int orderCountPoints = (int) Math.round(Math.min(orderCount, 15) * 2.0);

        double spendRatio = Math.min(totalSpend.doubleValue() / 175_000.0, 1.0);
        int spendPoints = (int) Math.round(spendRatio * 35);

        long accountAgeDays = ChronoUnit.DAYS.between(accountCreatedAt, Instant.now());
        double agePeriod = Math.min(accountAgeDays / 90.0, 1.0);
        int accountAgePoints = (int) Math.round(agePeriod * 20);

        int recencyPoints;
        if (lastOrderAt == null) {
            recencyPoints = 0;
        } else {
            long daysSinceLastOrder = ChronoUnit.DAYS.between(lastOrderAt, Instant.now());
            if (daysSinceLastOrder <= 30) {
                recencyPoints = 15;
            } else if (daysSinceLastOrder >= 180) {
                recencyPoints = 0;
            } else {
                double decay = 1.0 - ((daysSinceLastOrder - 30) / 150.0);
                recencyPoints = (int) Math.round(decay * 15);
            }
        }

        return new CreditReadinessDto(orderCountPoints, spendPoints, accountAgePoints, recencyPoints);
    }

    /** Admin edit — corrects/completes a business's own profile on their behalf. */
    @Transactional
    public BusinessAccountDto adminUpdate(UUID id, BusinessAccountCreateRequest request) {
        BusinessAccount account = businessAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business account not found: " + id));
        account.setBusinessName(request.getBusinessName());
        account.setBusinessType(request.getBusinessType());
        account.setKraPin(request.getKraPin());
        account.setLocation(request.getLocation());
        account.setRoad(request.getRoad());
        account.setBuildingAddress(request.getBuildingAddress());
        account.setIndustry(resolveIndustry(request.getIndustryId()));
        account.setContactPersonName(request.getContactPersonName());
        account.setContactPersonRole(request.getContactPersonRole());
        account.setPhone(request.getPhone());
        return withOrderStats(new BusinessAccountDto(businessAccountRepository.save(account)), account);
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
