package com.mdau.momentspackagingbackendjavafirstclient.order.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.DiscountType;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PromoCode;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> validateAndCalculate(String code, BigDecimal subtotal) {
        return validateAndCalculate(code, subtotal, null);
    }

    /**
     * @param requestingUserId the customer attempting to redeem the code, or null for a
     *                         guest checkout. Codes with {@code restrictedToUserId} set
     *                         (e.g. a Business Account's auto-issued welcome code) are
     *                         only valid when this matches.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateAndCalculate(String code, BigDecimal subtotal, UUID requestingUserId) {
        PromoCode promo = promoCodeRepository
                .findByCodeIgnoreCaseAndActiveTrue(code).orElse(null);

        if (promo == null) {
            return Map.of("valid", false, "message", "Invalid or expired promo code",
                    "discountAmount", BigDecimal.ZERO);
        }

        if (promo.getRestrictedToUserId() != null && !promo.getRestrictedToUserId().equals(requestingUserId)) {
            return Map.of("valid", false, "message", "This code is not valid for your account",
                    "discountAmount", BigDecimal.ZERO);
        }

        if (promo.getExpiresAt() != null && promo.getExpiresAt().isBefore(Instant.now())) {
            return Map.of("valid", false, "message", "Promo code has expired",
                    "discountAmount", BigDecimal.ZERO);
        }

        if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            return Map.of("valid", false, "message", "Promo code usage limit reached",
                    "discountAmount", BigDecimal.ZERO);
        }

        if (subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            return Map.of("valid", false,
                    "message", "Minimum order amount is KES " + promo.getMinOrderAmount(),
                    "discountAmount", BigDecimal.ZERO);
        }

        BigDecimal discount;
        if (promo.getDiscountType() == DiscountType.PERCENT) {
            discount = subtotal.multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = promo.getDiscountValue().min(subtotal);
        }

        BigDecimal finalTotal = subtotal.subtract(discount);

        return Map.of("valid", true, "message", "Promo code applied",
                "discountAmount", discount, "finalTotal", finalTotal,
                "code", promo.getCode());
    }

    @Transactional
    public void incrementUsedCount(String code) {
        promoCodeRepository.findByCodeIgnoreCaseAndActiveTrue(code)
                .ifPresent(promo -> {
                    promo.setUsedCount(promo.getUsedCount() + 1);
                    promoCodeRepository.save(promo);
                });
    }

    @Transactional(readOnly = true)
    public List<PromoCode> getAll() {
        return promoCodeRepository.findAll();
    }

    @Transactional
    public PromoCode create(PromoCode promo) {
        promo.setCode(promo.getCode().toUpperCase());
        return promoCodeRepository.save(promo);
    }

    @Transactional
    public PromoCode update(UUID id, PromoCode updates) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));
        if (updates.getDiscountValue()  != null) promo.setDiscountValue(updates.getDiscountValue());
        if (updates.getMinOrderAmount() != null) promo.setMinOrderAmount(updates.getMinOrderAmount());
        if (updates.getMaxUses()        != null) promo.setMaxUses(updates.getMaxUses());
        if (updates.getExpiresAt()      != null) promo.setExpiresAt(updates.getExpiresAt());
        if (updates.getActive()         != null) promo.setActive(updates.getActive());
        return promoCodeRepository.save(promo);
    }

    @Transactional
    public void delete(UUID id) {
        promoCodeRepository.deleteById(id);
    }
}