package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The `category` column is free text, not an enum, so manually-seeded products
 * (Title Case, e.g. "Wrapping & Foil") and Riseller-synced products
 * (UPPER_CASE with different wording, e.g. "FILM & WRAP") ended up with
 * spelling variants of the same conceptual category. The public product
 * filter does an exact case-insensitive match, so a frontend dropdown could
 * only ever match ONE of these variants at a time — most category filter
 * selections silently returned zero or incomplete results.
 *
 * This seeder rewrites every product's category to one of 15 canonical
 * values, using the same variant groupings already established by
 * {@link ProductIndustryPatchSeeder}'s resolveIndustrySlugs() switch.
 *
 * Naturally idempotent: only writes when the value actually changes, so
 * it's safe to run on every startup without a sentinel keyword.
 */
@Slf4j
@Component
@Order(34)
@RequiredArgsConstructor
public class ProductCategoryNormalizationSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;

    /** Canonical category → every raw spelling variant that should map to it. */
    private static final Map<String, List<String>> CANONICAL_TO_VARIANTS = Map.ofEntries(
            Map.entry("Bags", List.of("BAGS")),
            Map.entry("Food & Condiments", List.of("FOOD & CONDIMENTS")),
            Map.entry("Thermal Rolls", List.of("THERMAL ROLLS")),
            Map.entry("Cups & Lids", List.of("CUPS & LIDS", "CUPS", "LIDS")),
            Map.entry("Wrapping & Foil", List.of("WRAPPING & FOIL", "FOIL", "FILM & WRAP", "FOIL TINS")),
            Map.entry("Containers & Trays", List.of("CONTAINERS & TRAYS", "CONTAINERS", "BOXES & BAKING", "TRAYS")),
            Map.entry("Tableware & Hygiene", List.of("TABLEWARE & HYGIENE", "PLATES", "TABLEWARE")),
            Map.entry("Cutlery", List.of("CUTLERY")),
            Map.entry("Straws & Stirrers", List.of("STRAWS & STIRRERS", "STRAWS", "ACCESSORIES")),
            Map.entry("Jars & Bottles", List.of("JARS & BOTTLES", "JARS", "BOTTLES")),
            Map.entry("Hygiene & PPE", List.of("HYGIENE & PPE", "HYGIENE")),
            Map.entry("Tapes", List.of("TAPES", "TAPE")),
            Map.entry("General", List.of("GENERAL")),
            Map.entry("Miscellaneous", List.of("MISCELLANEOUS")),
            Map.entry("Boards", List.of("BOARDS")),
            // Found via the "unmapped" log line on first deploy — real values on real
            // products, not covered by the seeder-file scan this map was built from.
            Map.entry("Gifting & Retail", List.of("GIFTING")),
            Map.entry("Mailers & Shipping", List.of("MAILERS"))
    );

    /** Reverse-indexed for O(1) lookup: uppercased raw value → canonical value. */
    private static final Map<String, String> RAW_TO_CANONICAL = buildReverseIndex();

    private static Map<String, String> buildReverseIndex() {
        Map<String, String> index = new java.util.HashMap<>();
        CANONICAL_TO_VARIANTS.forEach((canonical, variants) -> {
            index.put(canonical.toUpperCase(Locale.ROOT), canonical);
            variants.forEach(v -> index.put(v.toUpperCase(Locale.ROOT), canonical));
        });
        return index;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            runInternal();
        } catch (Exception e) {
            log.warn("ProductCategoryNormalizationSeeder: aborted — {}. App startup continues normally.", e.getMessage());
        }
    }

    private void runInternal() {
        List<Product> allActive = productRepository.findAllActive();
        if (allActive.isEmpty()) return;

        int normalized = 0;
        int unmapped = 0;
        for (Product p : allActive) {
            try {
                String raw = p.getCategory();
                if (raw == null || raw.isBlank()) continue;

                String canonical = RAW_TO_CANONICAL.get(raw.trim().toUpperCase(Locale.ROOT));
                if (canonical == null) {
                    unmapped++;
                    log.debug("ProductCategoryNormalizationSeeder: no canonical mapping for category '{}' on product {}",
                            raw, p.getId());
                    continue;
                }

                if (!canonical.equals(raw)) {
                    p.setCategory(canonical);
                    productRepository.save(p);
                    normalized++;
                }
            } catch (DataIntegrityViolationException e) {
                log.debug("ProductCategoryNormalizationSeeder: skipping product {} — concurrent write", p.getId());
            }
        }

        log.info("ProductCategoryNormalizationSeeder: normalized {} products to canonical categories ({} unmapped).",
                normalized, unmapped);
    }
}
