package com.mdau.momentspackagingbackendjavafirstclient.referral.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.referral.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.*;
import com.mdau.momentspackagingbackendjavafirstclient.referral.repository.*;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralCodeRepository      referralCodeRepo;
    private final ReferralEventRepository     referralEventRepo;
    private final CreditWalletRepository      walletRepo;
    private final CreditTransactionRepository txRepo;
    private final ReferralTierConfigRepository tierRepo;
    private final UserRepository              userRepository;
    private final SettingsService             settingsService;

    @Value("${app.frontend.url:https://moments-connect-hub.lovable.app}")
    private String frontendUrl;

    // ── Settings keys ─────────────────────────────────────────────────────────
    private static final String KEY_PROGRAM_ENABLED      = "referral.program.enabled";
    private static final String KEY_CREDITS_PER_KES      = "referral.credits.per.kes";
    private static final String KEY_MAX_REDEMPTION_PCT   = "referral.max.redemption.percent";
    private static final String KEY_MAX_ACTIVE_REFERRALS = "referral.max.active.referrals.per.user";

    // ── On registration: create referral code + wallet ────────────────────────

    @Transactional
    public void initializeNewUser(User user) {
        // Create wallet
        CreditWallet wallet = CreditWallet.builder().user(user).build();
        walletRepo.save(wallet);

        // Create referral code only if program is enabled
        if (!isProgramEnabled()) {
            log.debug("Referral program disabled — skipping code creation for {}", user.getEmail());
            return;
        }

        String code = generateUniqueCode(user);
        ReferralCode rc = ReferralCode.builder()
                .user(user)
                .code(code)
                .isActive(true)
                .build();
        referralCodeRepo.save(rc);
        log.info("Referral code {} created for {}", code, user.getEmail());
    }

    // ── On registration with referral code ────────────────────────────────────

    @Transactional
    public void recordReferralSignup(User referee, String referralCode) {
        if (!isProgramEnabled()) return;

        ReferralCode rc = referralCodeRepo.findByCode(referralCode.toUpperCase()).orElse(null);
        if (rc == null || !rc.getIsActive()) {
            log.warn("Invalid or inactive referral code: {}", referralCode);
            return;
        }

        // Check referrer cap
        int maxActive = Integer.parseInt(
                settingsService.getValue(KEY_MAX_ACTIVE_REFERRALS, "50"));
        long activeCount = referralEventRepo.countByReferrerAndStatus(
                rc.getUser(), ReferralEventStatus.PENDING);
        if (rc.getMaxReferrals() != null && rc.getTotalReferrals() >= rc.getMaxReferrals()) {
            log.warn("Referral code {} has reached its max referrals cap", referralCode);
            return;
        }
        if (activeCount >= maxActive) {
            log.warn("Referrer {} has too many pending referrals", rc.getUser().getEmail());
            return;
        }

        // Check referee hasn't already been referred
        boolean alreadyReferred = referralEventRepo
                .findByRefereeAndStatus(referee, ReferralEventStatus.PENDING).isPresent();
        if (alreadyReferred) return;

        ReferralEvent event = ReferralEvent.builder()
                .referrer(rc.getUser())
                .referee(referee)
                .referralCode(rc.getCode())
                .status(ReferralEventStatus.PENDING)
                .build();
        referralEventRepo.save(event);

        rc.setTotalReferrals(rc.getTotalReferrals() + 1);
        referralCodeRepo.save(rc);

        log.info("Referral event created: {} referred {}", rc.getUser().getEmail(), referee.getEmail());
    }

    // ── On order completion: confirm referral + award credits ─────────────────

    @Transactional
    public void processOrderForReferral(User buyer, Order order) {
        if (!isProgramEnabled()) return;

        BigDecimal orderTotal = order.getTotalAmount();

        // Find matching tier
        ReferralTierConfig tier = tierRepo.findMatchingTier(orderTotal).orElse(null);
        if (tier == null) {
            log.debug("No matching referral tier for order amount {}", orderTotal);
            return;
        }

        // Find pending referral event for this buyer
        ReferralEvent event = referralEventRepo
                .findByRefereeAndStatus(buyer, ReferralEventStatus.PENDING)
                .orElse(null);
        if (event == null) return;

        // Award referee credits
        if (tier.getRefereeCredits() > 0) {
            awardCredits(buyer, tier.getRefereeCredits(),
                    CreditTransactionType.EARNED_PURCHASE,
                    "Credits earned for qualifying purchase of KES " + orderTotal,
                    event, order.getId().toString());
        }

        // Award referrer credits
        if (tier.getReferrerCredits() > 0) {
            awardCredits(event.getReferrer(), tier.getReferrerCredits(),
                    CreditTransactionType.EARNED_REFERRAL,
                    "Credits earned — your referral " + buyer.getFirstName() + " made a purchase",
                    event, order.getId().toString());
        }

        // Confirm the event
        event.setStatus(ReferralEventStatus.CONFIRMED);
        event.setOrder(order);
        event.setQualifyingAmount(orderTotal);
        event.setRefereeCreditsAwarded(tier.getRefereeCredits());
        event.setReferrerCreditsAwarded(tier.getReferrerCredits());
        referralEventRepo.save(event);

        log.info("Referral confirmed: {} credits to referrer {}, {} credits to referee {}",
                tier.getReferrerCredits(), event.getReferrer().getEmail(),
                tier.getRefereeCredits(), buyer.getEmail());
    }

    // ── Calculate max credits redeemable for an order ─────────────────────────

    public BigDecimal calculateMaxRedeemableKes(BigDecimal orderTotal) {
        int maxPct = Integer.parseInt(
                settingsService.getValue(KEY_MAX_REDEMPTION_PCT, "20"));
        return orderTotal.multiply(BigDecimal.valueOf(maxPct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.FLOOR);
    }

    // ── Redeem credits on an order ────────────────────────────────────────────

    @Transactional
    public BigDecimal redeemCredits(User user, int creditsToRedeem, Order order) {
        if (!isProgramEnabled()) return BigDecimal.ZERO;

        CreditWallet wallet = getOrCreateWallet(user);
        if (wallet.getBalance() < creditsToRedeem) {
            throw new IllegalArgumentException("Insufficient credits. Balance: " + wallet.getBalance());
        }

        int creditsPerKes = Integer.parseInt(
                settingsService.getValue(KEY_CREDITS_PER_KES, "10"));
        BigDecimal discountKes = BigDecimal.valueOf(creditsToRedeem)
                .divide(BigDecimal.valueOf(creditsPerKes), 2, RoundingMode.FLOOR);

        BigDecimal maxRedeemable = calculateMaxRedeemableKes(order.getTotalAmount());
        if (discountKes.compareTo(maxRedeemable) > 0) {
            throw new IllegalArgumentException(
                    "Maximum redeemable for this order is KES " + maxRedeemable);
        }

        // Deduct from wallet
        wallet.setBalance(wallet.getBalance() - creditsToRedeem);
        wallet.setLifetimeRedeemed(wallet.getLifetimeRedeemed() + creditsToRedeem);
        walletRepo.save(wallet);

        // Record transaction
        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(CreditTransactionType.REDEEMED)
                .amount(-creditsToRedeem)
                .balanceAfter(wallet.getBalance())
                .description("Redeemed " + creditsToRedeem + " credits for KES " + discountKes + " discount")
                .orderId(order.getId().toString())
                .build();
        txRepo.save(tx);

        log.info("User {} redeemed {} credits = KES {} discount on order {}",
                user.getEmail(), creditsToRedeem, discountKes, order.getId());
        return discountKes;
    }

    // ── Customer: get my referral code ────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReferralCodeDto getMyReferralCode(User user) {
        ReferralCode rc = referralCodeRepo.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Referral code not found"));
        return new ReferralCodeDto(rc, frontendUrl);
    }

    // ── Customer: get my wallet ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CreditWalletDto getMyWallet(User user) {
        CreditWallet wallet = getOrCreateWallet(user);
        int creditsPerKes = Integer.parseInt(
                settingsService.getValue(KEY_CREDITS_PER_KES, "10"));
        return new CreditWalletDto(wallet, creditsPerKes);
    }

    // ── Customer: get my transaction history ──────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CreditTransactionDto> getMyTransactions(User user, Pageable pageable) {
        return txRepo.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(CreditTransactionDto::new);
    }

    // ── Customer: get my referral events ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReferralEventDto> getMyReferrals(User user) {
        return referralEventRepo.findByReferrerOrderByCreatedAtDesc(user)
                .stream()
                .map(ReferralEventDto::new)
                .collect(Collectors.toList());
    }

    // ── Admin: CRUD tiers ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReferralTierConfigDto> getAllTiers() {
        return tierRepo.findAll().stream()
                .map(ReferralTierConfigDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReferralTierConfigDto createTier(ReferralTierConfigDto request) {
        ReferralTierConfig tier = ReferralTierConfig.builder()
                .tierName(request.getTierName())
                .minOrderAmount(request.getMinOrderAmount())
                .maxOrderAmount(request.getMaxOrderAmount())
                .referrerCredits(request.getReferrerCredits())
                .refereeCredits(request.getRefereeCredits())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return new ReferralTierConfigDto(tierRepo.save(tier));
    }

    @Transactional
    public ReferralTierConfigDto updateTier(UUID id, ReferralTierConfigDto request) {
        ReferralTierConfig tier = tierRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found: " + id));
        if (request.getTierName()       != null) tier.setTierName(request.getTierName());
        if (request.getMinOrderAmount() != null) tier.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getMaxOrderAmount() != null) tier.setMaxOrderAmount(request.getMaxOrderAmount());
        if (request.getReferrerCredits()!= null) tier.setReferrerCredits(request.getReferrerCredits());
        if (request.getRefereeCredits() != null) tier.setRefereeCredits(request.getRefereeCredits());
        if (request.getIsActive()       != null) tier.setIsActive(request.getIsActive());
        if (request.getSortOrder()      != null) tier.setSortOrder(request.getSortOrder());
        return new ReferralTierConfigDto(tierRepo.save(tier));
    }

    @Transactional
    public void deleteTier(UUID id) {
        if (!tierRepo.existsById(id))
            throw new ResourceNotFoundException("Tier not found: " + id);
        tierRepo.deleteById(id);
    }

    // ── Admin: manual credit adjustment ──────────────────────────────────────

    @Transactional
    public CreditWalletDto adminAdjustCredits(AdminCreditAdjustRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CreditWallet wallet = getOrCreateWallet(user);

        int newBalance = wallet.getBalance() + request.getAmount();
        if (newBalance < 0) throw new IllegalArgumentException("Adjustment would result in negative balance");

        wallet.setBalance(newBalance);
        if (request.getAmount() > 0) {
            wallet.setLifetimeEarned(wallet.getLifetimeEarned() + request.getAmount());
        }
        walletRepo.save(wallet);

        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(CreditTransactionType.ADJUSTED)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .description("Admin adjustment: " + request.getReason())
                .build();
        txRepo.save(tx);

        int creditsPerKes = Integer.parseInt(
                settingsService.getValue(KEY_CREDITS_PER_KES, "10"));
        return new CreditWalletDto(wallet, creditsPerKes);
    }

    // ── Admin: all transactions ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CreditTransactionDto> getAllTransactions(Pageable pageable) {
        return txRepo.findAllByOrderByCreatedAtDesc(pageable)
                .map(CreditTransactionDto::new);
    }

    // ── Admin: all referral events ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ReferralEventDto> getAllReferralEvents(Pageable pageable) {
        return referralEventRepo.findAllByOrderByCreatedAtDesc(pageable)
                .map(ReferralEventDto::new);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isProgramEnabled() {
        return Boolean.parseBoolean(
                settingsService.getValue(KEY_PROGRAM_ENABLED, "true"));
    }

    private CreditWallet getOrCreateWallet(User user) {
        return walletRepo.findByUser(user).orElseGet(() -> {
            CreditWallet w = CreditWallet.builder().user(user).build();
            return walletRepo.save(w);
        });
    }

    private void awardCredits(User user, int amount, CreditTransactionType type,
                               String description, ReferralEvent event, String orderId) {
        CreditWallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setLifetimeEarned(wallet.getLifetimeEarned() + amount);
        walletRepo.save(wallet);

        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .referralEvent(event)
                .orderId(orderId)
                .build();
        txRepo.save(tx);
    }

    private String generateUniqueCode(User user) {
        String base = (user.getFirstName() != null
                ? user.getFirstName().toUpperCase().replaceAll("[^A-Z0-9]", "") : "USER");
        if (base.length() > 6) base = base.substring(0, 6);
        String code;
        int attempts = 0;
        do {
            String suffix = String.format("%04d", new SecureRandom().nextInt(10000));
            code = base + suffix;
            attempts++;
            if (attempts > 20) code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (referralCodeRepo.existsByCode(code));
        return code;
    }
}