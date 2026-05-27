package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductPricingTierRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ONE-TIME cleanup runner (Order 5 – runs before seeders).
 *
 * Deletes all seeded products that have NO primary image AND no image URLs attached.
 * Products manually created by admin (e.g. "Flexible Plates Plastic", "Kraft Cups",
 * "Single Wall 12oz Coffee Cup", "toothpicks and holders") have images and are kept.
 *
 * After the clean slate this runner soft-deletes the empty products so the
 * seeders (Order 10+) can re-seed fresh data with descriptions and pricing.
 *
 * SAFE TO KEEP PERMANENTLY: once products exist the seeders won't re-seed,
 * and this runner only acts when imageless seeded products are detected.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class SeededProductCleanupRunner implements ApplicationRunner {

    private final ProductRepository            productRepository;
    private final ProductPricingTierRepository pricingTierRepository;
    private final ProductSeederHelper          seederHelper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> allActive = productRepository.findAllActive();

        // Identify imageless products (no primaryImageUrl AND no imageUrls)
        List<Product> imageless = allActive.stream()
                .filter(p -> isNullOrBlank(p.getPrimaryImageUrl())
                        && (p.getImageUrls() == null || p.getImageUrls().isEmpty()))
                .toList();

        if (imageless.isEmpty()) {
            log.info("SeededProductCleanupRunner: no imageless products found — skipping cleanup.");
            return;
        }

        log.info("SeededProductCleanupRunner: found {} imageless products — deleting pricing tiers and soft-deleting products.",
                imageless.size());

        for (Product p : imageless) {
            // Delete all pricing tiers first (FK constraint)
            pricingTierRepository.deleteByProductId(p.getId());
            // Soft-delete the product
            p.setDeleted(true);
        }

        productRepository.saveAll(imageless);

        // Reset the seeder guard so seeders run on this startup
        seederHelper.resetSeedGuard();

        log.info("SeededProductCleanupRunner: soft-deleted {} imageless products. Seeders will now re-seed.",
                imageless.size());
    }

    private boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }
}