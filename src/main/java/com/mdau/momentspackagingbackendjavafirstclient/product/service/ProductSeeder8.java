package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Product seeder part 8 of 10.
 * TODO: Replace the PRODUCTS array below with the actual product data for this batch.
 * Copy the exact product codes and names from the original seeder, then add:
 *   - A concise description (1-2 sentences describing the product and its use case)
 *   - pktQty  / pktUnitPrice  – pieces per packet and KES price per piece
 *   - ctnQty  / ctnUnitPrice  – pieces per carton (set to 0/0.0 if no carton tier)
 *   - baleQty / baleUnitPrice – pieces per bale   (set to 0/0.0 if no bale tier)
 *
 * For bale/KG products (sold by weight), use pktQty=1 and the KES bale price directly.
 */
@Slf4j
@Component
@Order(17)
@RequiredArgsConstructor
public class ProductSeeder8 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    // { code, name, category, description, pktQty, pktUnitPrice, ctnQty, ctnUnitPrice, baleQty, baleUnitPrice }
    private static final Object[][] PRODUCTS = {
            // TODO: populate with real data from the original ProductSeeder8
            // Example entry:
            // { "001", "Example Product Name", "Cups & Lids",
            //   "One or two sentences describing the product and its typical use case.",
            //   25, 15.00,   500, 13.50,   2500, 12.00 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        if (PRODUCTS.length == 0) {
            log.warn("ProductSeeder8: PRODUCTS array is empty — skipping. Please populate with real data.");
            return;
        }

        List<Product> batch = new ArrayList<>();
        for (Object[] row : PRODUCTS) {
            Product p = seederHelper.buildProductFull(
                    (String) row[0], (String) row[1], (String) row[2], (String) row[3], batch);
            batch.add(p);
        }
        productRepository.saveAll(batch);

        for (int i = 0; i < PRODUCTS.length; i++) {
            Object[] row = PRODUCTS[i];
            seederHelper.saveTiers(batch.get(i),
                    (int) row[4],  (double) row[5],
                    (int) row[6],  (double) row[7],
                    (int) row[8],  (double) row[9]);
        }

        log.info("ProductSeeder8: saved {} products with pricing tiers", batch.size());
    }
}