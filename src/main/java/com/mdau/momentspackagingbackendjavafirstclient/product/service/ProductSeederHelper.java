package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.*;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductPricingTierRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductUomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared logic for all ProductSeeder1..10 classes.
 *
 * Guard: shouldSeed() returns false once any products exist in DB.
 *        resetSeedGuard() forces re-evaluation (called by SeededProductCleanupRunner).
 *
 * buildProduct()      – legacy builder, no description.
 * buildProductFull()  – full builder with description.
 * saveTiers()         – saves Packet / Carton / Bale pricing tiers for one product.
 *
 * Pricing convention passed to saveTiers():
 *   pktQty / pktUnitPrice  – pieces per packet, KES price per piece at packet rate
 *   ctnQty / ctnUnitPrice  – pieces per carton (0 = skip this tier)
 *   baleQty / baleUnitPrice – pieces per bale  (0 = skip this tier)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSeederHelper {

    private final ProductRepository            productRepository;
    private final ProductPricingTierRepository pricingTierRepository;
    private final ProductUomRepository         uomRepository;

    private Boolean seedingEnabled = null;

    // ── Seed guard ────────────────────────────────────────────────────────────

    public boolean shouldSeed() {
        if (seedingEnabled == null) {
            long count = productRepository.countActive();
            seedingEnabled = (count < 10);

            if (!seedingEnabled) {
                log.info("ProductSeederHelper: {} active products exist, seeders will skip.", count);
            }
        }
        return seedingEnabled;
    }

    /** Called by SeededProductCleanupRunner so seeders re-evaluate on the same startup. */
    public void resetSeedGuard() {
        this.seedingEnabled = null;
        log.info("ProductSeederHelper: seed guard reset — seeders will re-check.");
    }

    // ── Product builders ──────────────────────────────────────────────────────

    /** Legacy builder – no description. */
    public Product buildProduct(String code, String name, String uom,
                                String category, List<Product> pendingBatch) {
        return buildProductFull(code, name, category, null, pendingBatch);
    }

    /** Full builder including description. */
    public Product buildProductFull(String code, String name, String category,
                                    String description, List<Product> pendingBatch) {
        String slug = generateUniqueSlug(name, pendingBatch);

        Product p = Product.builder()
                .name(name)
                .slug(slug)
                .category(category)
                .description(description)
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
        return p;
    }

    // ── Pricing tiers ─────────────────────────────────────────────────────────

    /**
     * Saves Packet / Carton / Bale tiers for one product.
     * Pass qty=0 or unitPrice=0 to skip a tier.
     */
    public void saveTiers(Product product,
                          int pktQty,  double pktUnitPrice,
                          int ctnQty,  double ctnUnitPrice,
                          int baleQty, double baleUnitPrice) {

        ProductUom packetUom = uomRepository.findByCodeAndDeletedFalse("PACKET").orElse(null);
        ProductUom cartonUom = uomRepository.findByCodeAndDeletedFalse("CARTON").orElse(null);
        ProductUom baleUom   = uomRepository.findByCodeAndDeletedFalse("BALE").orElse(null);

        List<ProductPricingTier> tiers = new ArrayList<>();
        int order = 0;

        if (pktQty > 0 && pktUnitPrice > 0) {
            tiers.add(buildTier(product, packetUom,
                    "Packet of " + pktQty, pktQty, pktUnitPrice, order++));
        }
        if (ctnQty > 0 && ctnUnitPrice > 0) {
            tiers.add(buildTier(product, cartonUom,
                    "Carton of " + ctnQty, ctnQty, ctnUnitPrice, order++));
        }
        if (baleQty > 0 && baleUnitPrice > 0) {
            tiers.add(buildTier(product, baleUom,
                    "Bale of " + baleQty, baleQty, baleUnitPrice, order++));
        }

        if (!tiers.isEmpty()) {
            pricingTierRepository.saveAll(tiers);
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private ProductPricingTier buildTier(Product product, ProductUom uom,
                                         String collectionName, int qty,
                                         double unitPrice, int sortOrder) {
        BigDecimal price = BigDecimal.valueOf(unitPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal collectionPrice = price
                .multiply(BigDecimal.valueOf(qty))
                .setScale(2, RoundingMode.HALF_UP);

        return ProductPricingTier.builder()
                .product(product)
                .uom(uom)
                .collectionName(collectionName)
                .uomDescription("KES " + price + " per unit")
                .quantity(qty)
                .pricePerUnit(price)
                .collectionPrice(collectionPrice)
                .sortOrder(sortOrder)
                .enabled(true)
                .build();
    }

    private String generateUniqueSlug(String name, List<Product> pendingBatch) {
        String base = SlugUtil.toSlug(name);
        String slug = base;
        int counter = 1;
        String finalSlug = slug;
        while (productRepository.existsBySlug(slug)  // ← check ALL slugs, not just active
                || pendingBatch.stream().anyMatch(p -> finalSlug.equals(p.getSlug()))) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}