package com.mdau.momentspackagingbackendjavafirstclient.cart.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductPricingTier;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductPricingTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final ProductPricingTierRepository pricingTierRepository;

    @Transactional(readOnly = true)
    public BigDecimal calculateUnitPrice(Product product, int quantity) {
        List<ProductPricingTier> tiers = pricingTierRepository.findByProductId(product.getId());
        return tiers.stream()
                .filter(t -> quantity >= t.getMinQuantity())
                .filter(t -> t.getMaxQuantity() == null || quantity <= t.getMaxQuantity())
                .map(ProductPricingTier::getPricePerUnit)
                .findFirst()
                .orElse(product.getBasePrice() != null ? product.getBasePrice() : BigDecimal.ZERO);
    }

    public BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}