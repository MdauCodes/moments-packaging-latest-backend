package com.mdau.momentspackagingbackendjavafirstclient.cart.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductPricingTier;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductPricingTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final ProductPricingTierRepository pricingTierRepository;

    /**
     * Resolve unit price for a named collection tier.
     * The tier already carries pricePerUnit — this just validates and returns it.
     */
    public BigDecimal unitPriceForTier(ProductPricingTier tier) {
        return tier.getPricePerUnit();
    }

    /**
     * Collection line total = collectionPrice * numberOfCollections.
     * collectionPrice = pricePerUnit * unitsPerCollection.
     */
    public BigDecimal collectionLineTotal(ProductPricingTier tier, int numberOfCollections) {
        BigDecimal collectionPrice = tier.getPricePerUnit()
                .multiply(BigDecimal.valueOf(tier.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        return collectionPrice.multiply(BigDecimal.valueOf(numberOfCollections))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Individual unit line total (only valid when product.individualSalesEnabled=true).
     */
    public BigDecimal individualLineTotal(Product product, int units) {
        BigDecimal unitPrice = product.getBasePrice() != null
                ? product.getBasePrice()
                : BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(units))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Legacy method — kept so nothing outside cart breaks.
     * Resolves price by quantity range against existing tiers.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateUnitPrice(Product product, int quantity) {
        List<ProductPricingTier> tiers = pricingTierRepository.findByProductId(product.getId());
        return tiers.stream()
                .filter(t -> t.getQuantity() != null && t.getQuantity().equals(quantity))
                .map(ProductPricingTier::getPricePerUnit)
                .findFirst()
                .orElseGet(() -> tiers.stream()
                        .filter(t -> t.getMinQuantity() != null && quantity >= t.getMinQuantity())
                        .filter(t -> t.getMaxQuantity() == null || quantity <= t.getMaxQuantity())
                        .map(ProductPricingTier::getPricePerUnit)
                        .findFirst()
                        .orElse(product.getBasePrice() != null
                                ? product.getBasePrice()
                                : BigDecimal.ZERO));
    }

    public BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }
}