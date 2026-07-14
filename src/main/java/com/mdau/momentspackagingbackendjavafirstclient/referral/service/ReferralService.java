package com.mdau.momentspackagingbackendjavafirstclient.referral.service;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralCodeRepository       referralCodeRepo;
    private final ReferralEventRepository      referralEventRepo;
    private final CreditWalletRepository       walletRepo;
    private final CreditTransactionRepository  txRepo;
    private final ReferralTierConfigRepository tierRepo;
    private final RewardsTierConfigRepository  rewardsTierRepo;
    private final UserRepository               userRepository;
    private final SettingsService              settingsService;
    private final com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository productRepository;

    @Value("${app.frontend.url:https://moments-connect-hub.lovable.app}")
    private String frontendUrl;

    // ── Settings keys ─────────────────────────────────────────────────────────
    public  static final String KEY_FEATURE_UNLOCKED    = "referral.feature.unlocked";
    public  static final String KEY_PROGRAM_ENABLED     = "referral.program.enabled";
    private static final String KEY_CREDITS_PER_KES     = "referral.credits.per.kes";
    private static final String KEY_MAX_REDEMPTION_PCT  = "referral.max.redemption.percent";
    private static final String KEY_MAX_ACTIVE_REFERRALS= "referral.max.active.referrals.per.user";
    /** Redemptions allowed before the customer must verify their email to redeem again. */
    public  static final int    FREE_REDEMPTION_LIMIT    = 3;
    private static final String KEY_WELCOME_POINTS      = "rewards.welcome.points";
    private static final String KEY_REVIEW_POINTS       = "rewards.review.points";
    private static final String KEY_POINTS_PER_100_KES  = "rewards.points.per.100kes";

    // ── Feature status (public — for frontend) ────────────────────────────────

    public ReferralFeatureStatusDto getFeatureStatus() {
        boolean unlocked = isFeatureUnlocked();
        boolean enabled  = unlocked && isProgramEnabled();
        int creditsPerKes = Integer.parseInt(
                settingsService.getValue(KEY_CREDITS_PER_KES, "10"));
        int maxRedemptionPct = Integer.parseInt(
                settingsService.getValue(KEY_MAX_REDEMPTION_PCT, "20"));
        return new ReferralFeatureStatusDto(unlocked, enabled, creditsPerKes, maxRedemptionPct);
    }

    // ── On registration: create wallet (always) + referral code (if unlocked) ─

    @Transactional
    public void initializeNewUser(User user) {
        CreditWallet wallet = CreditWallet.builder().user(user).build();
        walletRepo.save(wallet);

        if (!isFeatureUnlocked()) {
            log.debug("Referral feature locked — skipping code creation for {}", user.getEmail());
            return;
        }
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

    // ── Rewards program — wallet/ledger are the same tables the referral
    //    system already uses; these are the general (non-referral) earning
    //    triggers: welcome bonus, review bonus, spend-based accrual. Earned
    //    by every customer regardless of account type (Individual Shopper or
    //    Business) — the two account types differ in what other features
    //    they get, not in whether they participate in rewards.

    @Transactional
    public void awardWelcomeBonus(User user) {
        int points = Integer.parseInt(settingsService.getValue(KEY_WELCOME_POINTS, "100"));
        if (points <= 0) return;
        awardCredits(user, points, CreditTransactionType.EARNED_SIGNUP,
                "Welcome bonus for opening your account", null, null);
    }

    @Transactional
    public void awardReviewBonus(User user) {
        int points = Integer.parseInt(settingsService.getValue(KEY_REVIEW_POINTS, "50"));
        if (points <= 0) return;
        awardCredits(user, points, CreditTransactionType.EARNED_REVIEW,
                "Points earned for submitting a product review", null, null);
    }

    /**
     * General points accrual on any paid order — every customer earns these
     * regardless of referral status, unlike EARNED_PURCHASE (which is
     * specifically "purchase made by a referred buyer", see
     * processOrderForReferral). Both can fire on the same order — a
     * deliberate stack, matching how reference loyalty programs (Smile.io,
     * Rothy's) run spend-based points and referral rewards as two
     * independent, coexisting mechanics rather than one blocking the other.
     */
    @Transactional
    public void awardOrderPoints(User buyer, Order order) {
        int pointsPer100Kes = Integer.parseInt(settingsService.getValue(KEY_POINTS_PER_100_KES, "1"));
        int points = order.getTotalAmount()
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.valueOf(pointsPer100Kes))
                .intValue();
        if (points <= 0) return;
        awardCredits(buyer, points, CreditTransactionType.EARNED_ORDER,
                "Points earned for order of KES " + order.getTotalAmount(), null, order.getId().toString());
    }

    // ── On registration with referral code ────────────────────────────────────

    @Transactional
    public void recordReferralSignup(User referee, String referralCode) {
        if (!isFeatureUnlocked() || !isProgramEnabled()) return;

        ReferralCode rc = referralCodeRepo.findByCode(referralCode.toUpperCase()).orElse(null);
        if (rc == null || !rc.getIsActive()) {
            log.warn("Invalid or inactive referral code: {}", referralCode);
            return;
        }

        int maxActive = Integer.parseInt(
                settingsService.getValue(KEY_MAX_ACTIVE_REFERRALS, "50"));
        long activeCount = referralEventRepo.countByReferrerAndStatus(
                rc.getUser(), ReferralEventStatus.PENDING);

        if (rc.getMaxReferrals() != null && rc.getTotalReferrals() >= rc.getMaxReferrals()) {
            log.warn("Referral code {} has reached its cap", referralCode);
            return;
        }
        if (activeCount >= maxActive) {
            log.warn("Referrer {} has too many pending referrals", rc.getUser().getEmail());
            return;
        }

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

        log.info("Referral event: {} referred {}", rc.getUser().getEmail(), referee.getEmail());
    }

    // ── On order completion ───────────────────────────────────────────────────

    @Transactional
    public void processOrderForReferral(User buyer, Order order) {
        if (!isFeatureUnlocked() || !isProgramEnabled()) return;

        // Check for a pending referral FIRST — this is a rare, cheap lookup that only
        // matches actually-referred buyers, so logging past this point is meaningful
        // rather than spamming every ordinary order that was never referred at all.
        ReferralEvent event = referralEventRepo
                .findByRefereeAndStatus(buyer, ReferralEventStatus.PENDING)
                .orElse(null);
        if (event == null) return;

        BigDecimal orderTotal = order.getTotalAmount();
        ReferralTierConfig tier = tierRepo.findMatchingTier(orderTotal).orElse(null);
        if (tier == null) {
            log.warn("Referral for {} (order {}) NOT confirmed — no active tier band covers order " +
                            "amount KES {}. The referral event stays PENDING; check for gaps in Referral Payout Tiers.",
                    buyer.getEmail(), order.getReference(), orderTotal);
            return;
        }

        if (tier.getRefereeCredits() > 0) {
            awardCredits(buyer, tier.getRefereeCredits(),
                    CreditTransactionType.EARNED_PURCHASE,
                    "Credits earned for qualifying purchase of KES " + orderTotal,
                    event, order.getId().toString());
        }
        if (tier.getReferrerCredits() > 0) {
            awardCredits(event.getReferrer(), tier.getReferrerCredits(),
                    CreditTransactionType.EARNED_REFERRAL,
                    "Credits earned — your referral " + buyer.getFirstName() + " made a purchase",
                    event, order.getId().toString());
        }

        event.setStatus(ReferralEventStatus.CONFIRMED);
        event.setOrder(order);
        event.setQualifyingAmount(orderTotal);
        event.setRefereeCreditsAwarded(tier.getRefereeCredits());
        event.setReferrerCreditsAwarded(tier.getReferrerCredits());
        referralEventRepo.save(event);

        log.info("Referral confirmed: {} credits → referrer, {} credits → referee",
                tier.getReferrerCredits(), tier.getRefereeCredits());
    }

    // ── Credit redemption ─────────────────────────────────────────────────────

    public BigDecimal calculateMaxRedeemableKes(BigDecimal orderTotal) {
        int maxPct = Integer.parseInt(
                settingsService.getValue(KEY_MAX_REDEMPTION_PCT, "20"));
        return orderTotal.multiply(BigDecimal.valueOf(maxPct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.FLOOR);
    }

    @Transactional
    public BigDecimal redeemCredits(User user, int creditsToRedeem, Order order) {
        assertFeatureAvailable();

        CreditWallet wallet = getOrCreateWallet(user);
        if (wallet.getBalance() < creditsToRedeem) {
            throw new IllegalArgumentException("Insufficient credits. Balance: " + wallet.getBalance());
        }

        BigDecimal discountKes = creditsToKes(creditsToRedeem);

        BigDecimal maxRedeemable = calculateMaxRedeemableKes(order.getTotalAmount());
        if (discountKes.compareTo(maxRedeemable) > 0) {
            throw new IllegalArgumentException(
                    "Maximum redeemable for this order is KES " + maxRedeemable);
        }

        wallet.setBalance(wallet.getBalance() - creditsToRedeem);
        wallet.setLifetimeRedeemed(wallet.getLifetimeRedeemed() + creditsToRedeem);
        walletRepo.save(wallet);

        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(CreditTransactionType.REDEEMED)
                .amount(-creditsToRedeem)
                .balanceAfter(wallet.getBalance())
                .description("Redeemed " + creditsToRedeem + " credits for KES " + discountKes + " discount")
                .orderId(order.getId().toString())
                .build();
        txRepo.save(tx);

        log.info("User {} redeemed {} credits = KES {} on order {}",
                user.getEmail(), creditsToRedeem, discountKes, order.getId());
        return discountKes;
    }

    /**
     * Pure calculation, no mutation — used both by the checkout-preview
     * endpoint and by CheckoutService while computing an order's total
     * (the order doesn't exist yet at that point, so redeemCredits/its
     * order-bound cap check can't run until after the order is saved).
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRedemptionDiscount(User user, int creditsToRedeem) {
        assertFeatureAvailable();
        CreditWallet wallet = getOrCreateWallet(user);
        if (wallet.getBalance() < creditsToRedeem) {
            throw new IllegalArgumentException("Insufficient credits. Balance: " + wallet.getBalance());
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified()) && wallet.getRedemptionCount() >= FREE_REDEMPTION_LIMIT) {
            throw new IllegalArgumentException(
                    "You've used your " + FREE_REDEMPTION_LIMIT + " free redemptions — verify your email " +
                    "in your account dashboard to keep redeeming points.");
        }
        BigDecimal discount = creditsToKes(creditsToRedeem);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            int creditsPerKes = Integer.parseInt(settingsService.getValue(KEY_CREDITS_PER_KES, "10"));
            throw new IllegalArgumentException(
                    "That many points round to a KES 0 discount — redeem at least " + creditsPerKes + " points.");
        }
        return discount;
    }

    /**
     * Commits a redemption whose discount amount was already computed and
     * folded into the order's total at checkout time (see CheckoutService) —
     * re-validates balance only (defends against a race between preview and
     * commit), does not re-derive the discount or the order-value cap, since
     * those were already fixed at checkout time and re-deriving them off the
     * now-discounted order total would produce a smaller, inconsistent cap.
     */
    // REQUIRES_NEW: CheckoutService already validates the balance (via
    // calculateRedemptionDiscount, before the order is ever built) and calls this only
    // after the order is saved. If this still somehow throws — a genuine race between
    // that check and this commit — it must NOT mark the outer checkout() transaction
    // rollback-only, which would otherwise destroy the whole order over a redemption
    // problem alone. Running in its own transaction means a failure here rolls back
    // only the wallet debit, and CheckoutService's existing catch-and-log is what
    // actually then applies, as originally intended.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void commitRedemption(User user, int creditsToRedeem, BigDecimal discountKes, Order order) {
        CreditWallet wallet = getOrCreateWallet(user);
        if (wallet.getBalance() < creditsToRedeem) {
            throw new IllegalArgumentException("Insufficient credits. Balance: " + wallet.getBalance());
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified()) && wallet.getRedemptionCount() >= FREE_REDEMPTION_LIMIT) {
            throw new IllegalArgumentException("Free redemption limit reached — email verification required.");
        }
        wallet.setBalance(wallet.getBalance() - creditsToRedeem);
        wallet.setLifetimeRedeemed(wallet.getLifetimeRedeemed() + creditsToRedeem);
        wallet.setRedemptionCount(wallet.getRedemptionCount() + 1);
        walletRepo.save(wallet);

        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(CreditTransactionType.REDEEMED)
                .amount(-creditsToRedeem)
                .balanceAfter(wallet.getBalance())
                .description("Redeemed " + creditsToRedeem + " credits for KES " + discountKes + " discount")
                .orderId(order.getId().toString())
                .build();
        txRepo.save(tx);

        log.info("User {} redeemed {} credits = KES {} on order {}",
                user.getEmail(), creditsToRedeem, discountKes, order.getId());
    }

    // ── Customer endpoints ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReferralCodeDto getMyReferralCode(User user) {
        assertFeatureAvailable();
        ReferralCode rc = referralCodeRepo.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Referral code not found"));
        return new ReferralCodeDto(rc, frontendUrl);
    }

    @Transactional(readOnly = true)
    public CreditWalletDto getMyWallet(User user) {
        CreditWallet wallet = getOrCreateWallet(user);
        int creditsPerKes = Integer.parseInt(
                settingsService.getValue(KEY_CREDITS_PER_KES, "10"));
        return new CreditWalletDto(wallet, creditsPerKes,
                Boolean.TRUE.equals(user.getEmailVerified()), FREE_REDEMPTION_LIMIT);
    }

    @Transactional(readOnly = true)
    public Page<CreditTransactionDto> getMyTransactions(User user, Pageable pageable) {
        return txRepo.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(CreditTransactionDto::new);
    }

    @Transactional(readOnly = true)
    public List<ReferralEventDto> getMyReferrals(User user) {
        assertFeatureAvailable();
        return referralEventRepo.findByReferrerOrderByCreatedAtDesc(user)
                .stream().map(ReferralEventDto::new).collect(Collectors.toList());
    }

    /** An Individual Shopper's resolved VIP tier — computed from lifetime points earned, never stored. */
    @Transactional(readOnly = true)
    public RewardsTierConfigDto getMyRewardsTier(User user) {
        CreditWallet wallet = getOrCreateWallet(user);
        return rewardsTierRepo.findCurrentTier(wallet.getLifetimeEarned())
                .map(RewardsTierConfigDto::new)
                .orElse(null);
    }

    /** Public, non-personal config for the cart/checkout "spend X more to unlock Y" FAB banner. */
    @Transactional(readOnly = true)
    public RewardsProgressConfigDto getRewardsProgressConfig() {
        int pointsPer100Kes = Integer.parseInt(settingsService.getValue(KEY_POINTS_PER_100_KES, "1"));
        List<RewardsTierConfigDto> tiers = rewardsTierRepo.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(RewardsTierConfigDto::new).collect(Collectors.toList());
        return new RewardsProgressConfigDto(pointsPer100Kes, tiers);
    }

    // ── Admin: VIP rewards tier CRUD ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RewardsTierConfigDto> getAllRewardsTiers() {
        return rewardsTierRepo.findAll().stream()
                .map(RewardsTierConfigDto::new).collect(Collectors.toList());
    }

    @Transactional
    public RewardsTierConfigDto createRewardsTier(RewardsTierConfigDto req) {
        RewardsTierConfig tier = RewardsTierConfig.builder()
                .tierName(req.getTierName())
                .minLifetimePoints(req.getMinLifetimePoints())
                .discountPercent(req.getDiscountPercent())
                .perkDescription(req.getPerkDescription())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();
        return new RewardsTierConfigDto(rewardsTierRepo.save(tier));
    }

    @Transactional
    public RewardsTierConfigDto updateRewardsTier(UUID id, RewardsTierConfigDto req) {
        RewardsTierConfig tier = rewardsTierRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found: " + id));
        if (req.getTierName()          != null) tier.setTierName(req.getTierName());
        if (req.getMinLifetimePoints() != null) tier.setMinLifetimePoints(req.getMinLifetimePoints());
        if (req.getDiscountPercent()   != null) tier.setDiscountPercent(req.getDiscountPercent());
        if (req.getPerkDescription()   != null) tier.setPerkDescription(req.getPerkDescription());
        if (req.getIsActive()          != null) tier.setIsActive(req.getIsActive());
        if (req.getSortOrder()         != null) tier.setSortOrder(req.getSortOrder());
        return new RewardsTierConfigDto(rewardsTierRepo.save(tier));
    }

    @Transactional
    public void deleteRewardsTier(UUID id) {
        if (!rewardsTierRepo.existsById(id))
            throw new ResourceNotFoundException("Tier not found: " + id);
        rewardsTierRepo.deleteById(id);
    }

    // ── Admin: tier CRUD ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReferralTierConfigDto> getAllTiers() {
        return tierRepo.findAll().stream()
                .map(ReferralTierConfigDto::new).collect(Collectors.toList());
    }

    @Transactional
    public ReferralTierConfigDto createTier(ReferralTierConfigDto req) {
        ReferralTierConfig tier = ReferralTierConfig.builder()
                .tierName(req.getTierName())
                .minOrderAmount(req.getMinOrderAmount())
                .maxOrderAmount(req.getMaxOrderAmount())
                .referrerCredits(req.getReferrerCredits())
                .refereeCredits(req.getRefereeCredits())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();
        return new ReferralTierConfigDto(tierRepo.save(tier));
    }

    @Transactional
    public ReferralTierConfigDto updateTier(UUID id, ReferralTierConfigDto req) {
        ReferralTierConfig tier = tierRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found: " + id));
        if (req.getTierName()        != null) tier.setTierName(req.getTierName());
        if (req.getMinOrderAmount()  != null) tier.setMinOrderAmount(req.getMinOrderAmount());
        if (req.getMaxOrderAmount()  != null) tier.setMaxOrderAmount(req.getMaxOrderAmount());
        if (req.getReferrerCredits() != null) tier.setReferrerCredits(req.getReferrerCredits());
        if (req.getRefereeCredits()  != null) tier.setRefereeCredits(req.getRefereeCredits());
        if (req.getIsActive()        != null) tier.setIsActive(req.getIsActive());
        if (req.getSortOrder()       != null) tier.setSortOrder(req.getSortOrder());
        return new ReferralTierConfigDto(tierRepo.save(tier));
    }

    @Transactional
    public void deleteTier(UUID id) {
        if (!tierRepo.existsById(id))
            throw new ResourceNotFoundException("Tier not found: " + id);
        tierRepo.deleteById(id);
    }

    /**
     * One-shot snapshot for the frontend's margin-aware tier calculator —
     * everything the Auto/Manual Mode UI needs to run entirely client-side
     * until the admin explicitly seeds tiers.
     */
    @Transactional(readOnly = true)
    public MarginSummaryDto getMarginSummary() {
        BigDecimal blendedGp = productRepository.averageGrossProfitPercent();
        long withCost  = productRepository.countWithCostData();
        long totalActive = productRepository.countActiveNotSuspended();
        int creditsPerKes = Integer.parseInt(
                settingsService.getValue(KEY_CREDITS_PER_KES, "10"));
        List<ReferralTierConfigDto> existing = getAllTiers();
        return new MarginSummaryDto(blendedGp, withCost, totalActive, creditsPerKes, existing);
    }

    /**
     * Replaces every existing referral tier with the given list in one shot —
     * the "Seed into system" action from Auto Mode. Caller (controller) is
     * responsible for requiring an explicit admin confirmation before calling
     * this, since it is destructive to whatever tiers currently exist.
     */
    @Transactional
    public List<ReferralTierConfigDto> seedTiers(List<ReferralTierConfigDto> tiers) {
        validateNoOverlaps(tiers);
        tierRepo.deleteAll();
        List<ReferralTierConfig> saved = tiers.stream()
                .map(req -> ReferralTierConfig.builder()
                        .tierName(req.getTierName())
                        .minOrderAmount(req.getMinOrderAmount())
                        .maxOrderAmount(req.getMaxOrderAmount())
                        .referrerCredits(req.getReferrerCredits())
                        .refereeCredits(req.getRefereeCredits())
                        .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                        .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                        .build())
                .map(tierRepo::save)
                .collect(Collectors.toList());
        return saved.stream().map(ReferralTierConfigDto::new).collect(Collectors.toList());
    }

    /**
     * Rejects a tier list with two active bands whose order-value ranges overlap —
     * that's silent, ambiguous config (findMatchingTier deterministically picks the
     * highest-minOrderAmount match with no warning), not a legitimate "gap is fine"
     * case. Gaps between bands are allowed (an admin may deliberately exclude very
     * small orders) but overlaps are always a mistake.
     */
    private void validateNoOverlaps(List<ReferralTierConfigDto> tiers) {
        List<ReferralTierConfigDto> active = tiers.stream()
                .filter(t -> t.getIsActive() == null || t.getIsActive())
                .sorted(Comparator.comparing(ReferralTierConfigDto::getMinOrderAmount))
                .collect(Collectors.toList());
        for (int i = 0; i < active.size() - 1; i++) {
            ReferralTierConfigDto a = active.get(i);
            ReferralTierConfigDto b = active.get(i + 1);
            BigDecimal aMax = a.getMaxOrderAmount();
            if (aMax == null || aMax.compareTo(b.getMinOrderAmount()) > 0) {
                throw new IllegalArgumentException(
                        "Tiers \"" + a.getTierName() + "\" and \"" + b.getTierName() + "\" overlap — " +
                        "an order could match both, and only the higher-minimum one would ever apply. " +
                        "Fix the ranges before seeding.");
            }
        }
    }

    // ── Admin: credit adjustment ──────────────────────────────────────────────

    @Transactional
    public CreditWalletDto adminAdjustCredits(AdminCreditAdjustRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CreditWallet wallet = getOrCreateWallet(user);

        int newBalance = wallet.getBalance() + request.getAmount();
        if (newBalance < 0)
            throw new IllegalArgumentException("Adjustment would result in negative balance");

        wallet.setBalance(newBalance);
        if (request.getAmount() > 0)
            wallet.setLifetimeEarned(wallet.getLifetimeEarned() + request.getAmount());
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
        return new CreditWalletDto(wallet, creditsPerKes,
                Boolean.TRUE.equals(user.getEmailVerified()), FREE_REDEMPTION_LIMIT);
    }

    @Transactional(readOnly = true)
    public Page<CreditTransactionDto> getAllTransactions(Pageable pageable) {
        return txRepo.findAllByOrderByCreatedAtDesc(pageable).map(CreditTransactionDto::new);
    }

    @Transactional(readOnly = true)
    public Page<ReferralEventDto> getAllReferralEvents(Pageable pageable) {
        return referralEventRepo.findAllByOrderByCreatedAtDesc(pageable).map(ReferralEventDto::new);
    }

    @Transactional(readOnly = true)
    public RewardsSummaryDto getRewardsSummary() {
        List<Object[]> rows = txRepo.sumEarnedAndRedeemed();
        Object[] row = rows.isEmpty() ? new Object[]{0L, 0L} : rows.get(0);
        long totalEarned = ((Number) row[0]).longValue();
        long totalRedeemed = ((Number) row[1]).longValue();
        long netOutstanding = totalEarned - totalRedeemed;

        int creditsPerKes = Integer.parseInt(
                settingsService.getValue(KEY_CREDITS_PER_KES, "10"));
        BigDecimal kesValueRedeemed = creditsToKes((int) totalRedeemed);
        BigDecimal kesValueOutstanding = creditsToKes((int) netOutstanding);

        return new RewardsSummaryDto(
                totalEarned, totalRedeemed, netOutstanding,
                kesValueRedeemed, kesValueOutstanding, creditsPerKes);
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    public boolean isFeatureUnlocked() {
        return Boolean.parseBoolean(
                settingsService.getValue(KEY_FEATURE_UNLOCKED, "false"));
    }

    public boolean isProgramEnabled() {
        return Boolean.parseBoolean(
                settingsService.getValue(KEY_PROGRAM_ENABLED, "false"));
    }

    private void assertFeatureAvailable() {
        if (!isFeatureUnlocked())
            throw new IllegalStateException("Referral feature is not available");
        if (!isProgramEnabled())
            throw new IllegalStateException("Referral program is currently disabled");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal creditsToKes(int credits) {
        int creditsPerKes = Integer.parseInt(
                settingsService.getValue(KEY_CREDITS_PER_KES, "10"));
        return BigDecimal.valueOf(credits)
                .divide(BigDecimal.valueOf(creditsPerKes), 2, RoundingMode.FLOOR);
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
                .user(user).type(type).amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .referralEvent(event).orderId(orderId)
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
            if (++attempts > 20) code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (referralCodeRepo.existsByCode(code));
        return code;
    }
}