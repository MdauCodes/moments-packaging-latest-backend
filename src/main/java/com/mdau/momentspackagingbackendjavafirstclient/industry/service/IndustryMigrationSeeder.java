package com.mdau.momentspackagingbackendjavafirstclient.industry.service;

import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.industry.repository.IndustryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Runs before IndustrySeeder (@Order 2) to correct any stale / manually-added
 * industry records whose slug or name doesn't match the canonical taxonomy.
 *
 * Known issue this fixes: "Cosmetics" (or similar) was added manually via the
 * admin panel instead of the correct "Health & Beauty" / "health-and-beauty".
 * Because slug is a unique column, a wrong entry blocks the correct one.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class IndustryMigrationSeeder implements ApplicationRunner {

    private final IndustryRepository industryRepository;

    /** Canonical (slug → name, description) pairs the DB must contain. */
    private static final List<String[]> CANONICAL = List.of(
        new String[]{"food-and-beverage",  "Food & Beverage",
            "Packaging solutions for restaurants, cafés, cloud kitchens, food manufacturers and beverage brands."},
        new String[]{"retail-and-ecommerce", "Retail & E-Commerce",
            "Custom packaging for retail shops, online sellers, D2C brands and courier-ready businesses."},
        new String[]{"health-and-beauty",  "Health & Beauty",
            "Premium packaging for cosmetics, skincare, pharmaceuticals, clinics and personal care products."},
        new String[]{"agriculture",        "Agriculture",
            "Durable packaging for grains, fresh produce, seeds, farm inputs and agro-processed goods."},
        new String[]{"manufacturing",      "Manufacturing",
            "Industrial packaging solutions for manufactured goods, components and B2B wholesalers."},
        new String[]{"hospitality",        "Hospitality",
            "Branded packaging for hotels, restaurants, events, banquets and catering businesses."},
        new String[]{"fashion-and-apparel","Fashion & Apparel",
            "Stylish packaging for clothing brands, boutiques, tailors, shoes and fashion retail."},
        new String[]{"electronics",        "Electronics",
            "Protective and branded packaging for electronic devices, gadgets and accessories."}
    );

    /**
     * Old slugs or names that map to the canonical slug.
     * Key = legacy slug or lower-cased name found in DB, Value = canonical slug.
     */
    private static final Map<String, String> LEGACY_MAP = Map.of(
        "cosmetics",           "health-and-beauty",
        "beauty-personal-care","health-and-beauty",
        "pharma-health",       "health-and-beauty",
        "health-beauty",       "health-and-beauty",
        "textile-apparel",     "fashion-and-apparel",
        "ecommerce-mailers",   "retail-and-ecommerce",
        "gifting-events",      "hospitality",
        "industrial-hardware", "manufacturing",
        "food-beverage",       "food-and-beverage"
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Industry> all = industryRepository.findAll();
        if (all.isEmpty()) return; // nothing to migrate; IndustrySeeder will create them

        Map<String, Industry> bySlug = new LinkedHashMap<>();
        for (Industry i : all) bySlug.put(i.getSlug(), i);

        int fixed = 0;
        for (Industry industry : all) {
            String slug = industry.getSlug();
            String name = industry.getName();

            // Check if this slug needs to be migrated to a canonical slug
            String canonicalSlug = LEGACY_MAP.get(slug);
            if (canonicalSlug == null) {
                // Also check by lower-cased name (handles "Cosmetics" → health-and-beauty)
                canonicalSlug = LEGACY_MAP.get(name != null ? name.toLowerCase(Locale.ROOT) : "");
            }

            if (canonicalSlug != null && !bySlug.containsKey(canonicalSlug)) {
                // Find the canonical spec and update this entry
                for (String[] spec : CANONICAL) {
                    if (spec[0].equals(canonicalSlug)) {
                        log.info("IndustryMigrationSeeder: renaming '{}' ({}) → '{}' ({})",
                                name, slug, spec[1], spec[0]);
                        industry.setSlug(spec[0]);
                        industry.setName(spec[1]);
                        industry.setDescription(spec[2]);
                        industryRepository.save(industry);
                        bySlug.put(spec[0], industry);
                        bySlug.remove(slug);
                        fixed++;
                        break;
                    }
                }
            } else {
                // Slug is canonical — ensure the name and description are up to date
                for (String[] spec : CANONICAL) {
                    if (spec[0].equals(slug)) {
                        boolean dirty = false;
                        if (!spec[1].equals(name)) {
                            industry.setName(spec[1]);
                            dirty = true;
                        }
                        if (spec[2] != null && !spec[2].equals(industry.getDescription())) {
                            industry.setDescription(spec[2]);
                            dirty = true;
                        }
                        if (dirty) {
                            industryRepository.save(industry);
                            log.info("IndustryMigrationSeeder: updated name/description for '{}'", spec[0]);
                            fixed++;
                        }
                        break;
                    }
                }
            }
        }

        if (fixed > 0) {
            log.info("IndustryMigrationSeeder: corrected {} industry record(s).", fixed);
        } else {
            log.info("IndustryMigrationSeeder: all industry records are up to date.");
        }
    }
}
