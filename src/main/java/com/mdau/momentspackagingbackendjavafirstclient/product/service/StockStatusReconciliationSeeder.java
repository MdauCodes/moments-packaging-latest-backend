package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
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
 * One-time (and self-healing on every restart) correction for products whose
 * stockStatus drifted out of sync with stockCount — e.g. "0 units" shown in
 * green because stockStatus was left at IN_STOCK. Root cause fixed in
 * ProductService (updateProduct/createProduct now recompute stockStatus
 * whenever stockCount changes and no explicit stockStatus is supplied); this
 * seeder repairs rows that already drifted before that fix shipped.
 * MADE_TO_ORDER is intentionally left untouched — it's independent of count.
 */
@Slf4j
@Component
@Order(41)
@RequiredArgsConstructor
public class StockStatusReconciliationSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> mismatched = new ArrayList<>();
        for (Product product : productRepository.findByDeletedFalse()) {
            if (product.getStockStatus() == StockStatus.MADE_TO_ORDER) continue;
            StockStatus derived = deriveStockStatus(product.getStockCount(), product.getLowStockThreshold());
            if (derived != product.getStockStatus()) {
                product.setStockStatus(derived);
                mismatched.add(product);
            }
        }
        if (!mismatched.isEmpty()) {
            productRepository.saveAll(mismatched);
            log.info("Reconciled stockStatus for {} products whose status had drifted from stockCount.", mismatched.size());
        }
    }

    private StockStatus deriveStockStatus(int stockCount, int lowStockThreshold) {
        if (stockCount <= 0) return StockStatus.OUT_OF_STOCK;
        if (stockCount <= lowStockThreshold) return StockStatus.LOW_STOCK;
        return StockStatus.IN_STOCK;
    }
}
