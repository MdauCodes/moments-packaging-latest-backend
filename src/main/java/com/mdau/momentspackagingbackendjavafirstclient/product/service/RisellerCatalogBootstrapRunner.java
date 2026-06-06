package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs once on startup after all product seeders (order 100).
 * Populates risellerItemId on all existing products by matching
 * Riseller catalog codes to product keywords.
 * Safe to run repeatedly — skips products already mapped.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class RisellerCatalogBootstrapRunner implements ApplicationRunner {

    private final RisellerSyncService syncService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("RisellerCatalogBootstrapRunner: mapping Riseller IDs to products...");
        try {
            syncService.syncCatalog();
            log.info("RisellerCatalogBootstrapRunner: complete");
        } catch (Exception e) {
            log.error("RisellerCatalogBootstrapRunner failed — stock sync will still work once IDs are mapped: {}", e.getMessage(), e);
        }
    }
}