package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.PriceUnit;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shared logic for all ProductSeeder1..10 classes.
 * Guard: shouldSeed() returns false once any products exist in DB.
 * buildProduct(): constructs a Product from seeder row data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSeederHelper {

    private final ProductRepository productRepository;

    private Boolean seedingEnabled = null;

    public boolean shouldSeed() {
        if (seedingEnabled == null) {
            long count = productRepository.countActive();
            seedingEnabled = (count == 0);
            if (!seedingEnabled) {
                log.info("ProductSeederHelper: {} products already exist, all seeders will skip", count);
            }
        }
        return seedingEnabled;
    }

    public Product buildProduct(String code, String name, String uom,
                                String category, List<Product> pendingBatch) {
        String slug = generateUniqueSlug(name, pendingBatch);

        Product p = Product.builder()
                .name(name)
                .slug(slug)
                .category(category)
                .moq(1)
                .individualSalesEnabled(false)
                .stockStatus(StockStatus.MADE_TO_ORDER)
                .stockCount(0)
                .lowStockThreshold(10)
                .leadTimeDays(14)
                .priceUnit(PriceUnit.PER_UNIT)
                .isDiscount(false)
                .isNewArrival(false)
                .isFastMoving(false)
                .customizable(false)
                .vatRate(new BigDecimal("0.1600"))
                .vatExempt(false)
                .deleted(false)
                .build();

        p.getKeywords().add(code);
        p.getTags().add("UOM:" + uom);

        return p;
    }

    private String generateUniqueSlug(String name, List<Product> pendingBatch) {
        String base = SlugUtil.toSlug(name);
        String slug = base;
        int counter = 1;
        String finalSlug = slug;
        while (productRepository.existsBySlugAndDeletedFalse(slug)
                || pendingBatch.stream().anyMatch(p -> finalSlug.equals(p.getSlug()))) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}