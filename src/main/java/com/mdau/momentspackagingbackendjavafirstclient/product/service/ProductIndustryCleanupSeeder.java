package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.industry.repository.IndustryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Removes incorrect industry associations written by the over-broad KRAFT /
 * SMART BAG patterns in ProductIndustryPatchSeeder.
 *
 * fashion-and-apparel should only cover genuinely boutique bags:
 *   ✓ Non-woven tote bags
 *   ✓ Twisted-handle / kraft-handle shopping bags
 *   ✓ Millinary / dune-style boutique bags
 *   ✗ Kraft ziplock bags, sauce cups, containers, generic smart bags, food packaging
 *
 * Sentinel: "industry-cleanup-v1"
 */
@Slf4j
@Component
@Order(33)
@RequiredArgsConstructor
public class ProductIndustryCleanupSeeder implements ApplicationRunner {

    private final ProductRepository  productRepository;
    private final IndustryRepository industryRepository;

    private static final String SENTINEL = "industry-cleanup-v1";
    private static final String FAS_SLUG = "fashion-and-apparel";

    @Override
    public void run(ApplicationArguments args) {
        try {
            runInternal();
        } catch (Exception e) {
            log.warn("ProductIndustryCleanupSeeder: aborted — {}. App startup continues normally.", e.getMessage());
        }
    }

    private void runInternal() {
        List<Product> allActive = productRepository.findAllActive();
        if (allActive.isEmpty()) return;

        boolean alreadyRun = allActive.stream()
                .anyMatch(p -> p.getKeywords() != null && p.getKeywords().contains(SENTINEL));
        if (alreadyRun) {
            log.info("ProductIndustryCleanupSeeder: already ran — skipping.");
            return;
        }

        Industry fasIndustry = industryRepository.findAll().stream()
                .filter(i -> FAS_SLUG.equals(i.getSlug()))
                .findFirst().orElse(null);
        if (fasIndustry == null) {
            log.warn("ProductIndustryCleanupSeeder: fashion-and-apparel not found in DB — skipping.");
            return;
        }

        int removed = 0;
        for (Product p : allActive) {
            try {
                boolean hasFas = p.getIndustries() != null &&
                        p.getIndustries().stream().anyMatch(i -> FAS_SLUG.equals(i.getSlug()));

                boolean changed = false;
                if (hasFas && !shouldHaveFas(p)) {
                    p.getIndustries().remove(fasIndustry);
                    changed = true;
                    removed++;
                }

                if (p.getKeywords() == null) p.setKeywords(new ArrayList<>());
                if (!p.getKeywords().contains(SENTINEL)) {
                    p.getKeywords().add(SENTINEL);
                    changed = true;
                }

                if (changed) productRepository.save(p);

            } catch (DataIntegrityViolationException e) {
                log.debug("ProductIndustryCleanupSeeder: skipping {} — concurrent write", p.getId());
            }
        }

        log.info("ProductIndustryCleanupSeeder: removed FAS from {} incorrectly-tagged products.", removed);
    }

    /**
     * Returns true only for bags that are genuinely boutique / fashion packaging.
     * Anything food-grade, bulk, or multi-purpose is excluded.
     */
    private boolean shouldHaveFas(Product p) {
        String cat  = p.getCategory() != null ? p.getCategory().toUpperCase(Locale.ROOT) : "";
        String name = p.getName()     != null ? p.getName().toUpperCase(Locale.ROOT)     : "";

        // Only BAGS category can ever belong to fashion-and-apparel
        if (!cat.equals("BAGS")) return false;

        // Exclude bulk, food, and non-boutique bag types
        if (hasAny(name,
                "ZIPLOCK", "ZIPPER", "ZIP LOCK",
                "SAUCE", "KETCHUP", "CONDIMENT",
                "PANEL SACK", "MAGUNIA", "WOVEN SACK",
                "V22", "V24", "V30", "50KG", "90KG", "100KG",
                "NET BAG", "RED NET", "PRODUCE NET",
                "GARBAGE", "REFUSE", "BIN LINER", "TRASH",
                "BREAD", "LOAF",
                "MEDICAL", "PHARMACY", "DISPENSARY",
                "KHAKI", "CLICK BAG", "SAWA", "MAFUCO",
                "FOOD", "TAKEAWAY")) {
            return false;
        }

        // Only keep FAS for bags that are explicitly boutique-styled
        return hasAny(name,
                "NON-WOVEN", "NON WOVEN", "NONWOVEN", "WOVEN TOTE",
                "TWISTED HANDLE", "TWISTED KRAFT", "KRAFT HANDLE", "HANDLED KRAFT",
                "MILLINARY", "DUNE NO.");
    }

    private boolean hasAny(String text, String... tokens) {
        for (String t : tokens) {
            if (text.contains(t)) return true;
        }
        return false;
    }
}
