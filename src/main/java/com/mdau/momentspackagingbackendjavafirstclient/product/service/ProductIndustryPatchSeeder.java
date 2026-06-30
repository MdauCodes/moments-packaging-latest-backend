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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Patch seeder that re-applies industry-association rules to ALL active products.
 * Runs AFTER IndustryMigrationSeeder (@Order 1) ensures correct slugs exist.
 * Only ADDS missing industries — never removes existing ones.
 *
 * Idempotency: guarded by sentinel keyword "industry-patch-v1". Products that
 * already have this sentinel are skipped, so the seeder is safe to redeploy.
 *
 * Covers customer personas that need specific industries:
 *   - Pharmaceutical attendant → health-and-beauty (pharmacy bags, gloves, masks)
 *   - Poultry / crops farmer   → agriculture (sacks, punnets, cling film, net bags)
 *   - Café / restaurant        → food-and-beverage (cups, straws, boxes, thermal rolls)
 *   - Hotel / events caterer   → hospitality (plates, cutlery, PPE, foil)
 *   - Boutique / fashion shop  → fashion-and-apparel (kraft bags, smart bags)
 *   - Supermarket / duka       → retail-and-ecommerce (thermal rolls, shopping bags)
 *   - Manufacturer / workshop  → manufacturing (tapes, foil, sacks, PPE)
 */
@Slf4j
@Component
@Order(32)
@RequiredArgsConstructor
public class ProductIndustryPatchSeeder implements ApplicationRunner {

    private final ProductRepository  productRepository;
    private final IndustryRepository industryRepository;

    private static final String SENTINEL = "industry-patch-v1";

    // ── Industry slug constants (must match IndustrySeeder + IndustryMigrationSeeder) ──
    private static final String FAB = "food-and-beverage";
    private static final String RET = "retail-and-ecommerce";
    private static final String HAB = "health-and-beauty";
    private static final String AGR = "agriculture";
    private static final String MFG = "manufacturing";
    private static final String HOS = "hospitality";
    private static final String FAS = "fashion-and-apparel";

    private Map<String, Industry> slugToIndustry;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> allActive = productRepository.findAllActive();
        if (allActive.isEmpty()) {
            log.info("ProductIndustryPatchSeeder: no active products — skipping.");
            return;
        }

        // Idempotency: if any product already has the sentinel, the patch already ran
        boolean alreadyPatched = allActive.stream()
                .anyMatch(p -> p.getKeywords() != null && p.getKeywords().contains(SENTINEL));
        if (alreadyPatched) {
            log.info("ProductIndustryPatchSeeder: already patched — skipping.");
            return;
        }

        slugToIndustry = industryRepository.findAll().stream()
                .collect(Collectors.toMap(Industry::getSlug, i -> i));
        if (slugToIndustry.isEmpty()) {
            log.warn("ProductIndustryPatchSeeder: no industries in DB — skipping.");
            return;
        }

        int patched = 0;
        for (Product p : allActive) {
            String cat  = p.getCategory() != null ? p.getCategory().toUpperCase(Locale.ROOT) : "";
            String name = p.getName()     != null ? p.getName().toUpperCase(Locale.ROOT)     : "";

            Set<String> required = resolveIndustrySlugs(cat, name);

            // Existing industries on this product
            Set<String> existing = (p.getIndustries() == null) ? new HashSet<>()
                    : p.getIndustries().stream()
                        .map(Industry::getSlug)
                        .collect(Collectors.toSet());

            boolean changed = false;
            for (String slug : required) {
                if (!existing.contains(slug)) {
                    Industry ind = slugToIndustry.get(slug);
                    if (ind != null) {
                        if (p.getIndustries() == null) p.setIndustries(new HashSet<>());
                        p.getIndustries().add(ind);
                        changed = true;
                    }
                }
            }

            // Stamp the sentinel keyword so we don't re-run
            if (p.getKeywords() == null) p.setKeywords(new ArrayList<>());
            if (!p.getKeywords().contains(SENTINEL)) {
                p.getKeywords().add(SENTINEL);
                changed = true;
            }

            if (changed) {
                productRepository.save(p);
                patched++;
            }
        }

        log.info("ProductIndustryPatchSeeder: patched {} products with missing industry associations.", patched);
    }

    // ── Comprehensive industry resolution covering all customer personas ──────────

    private Set<String> resolveIndustrySlugs(String cat, String name) {
        Set<String> s = new LinkedHashSet<>();

        switch (cat) {
            // ── Bags ──────────────────────────────────────────────────────────────
            case "BAGS" -> {
                s.addAll(List.of(RET, FAB));

                // Kraft / paper handled bags → fashion boutiques, shoe shops, gift shops
                if (hasAny(name, "KRAFT", "BROWN HANDLED", "TWISTED HANDLE", "MILLINARY", "DUNE NO."))
                    s.add(FAS);

                // Printed / branded smart bags → fashion & retail
                if (hasAny(name, "SMART BAG", "CHINA", "MREMBO", "GUCCI", "KITENGE", "SAFARI",
                        "PREMIER LEAGUE", "MATATU", "GIRAFFE", "HUMMING", "CHERRY", "ROSE",
                        "FLOWERED", "3D", "SHINY", "LAMINATED", "NIGERIAN", "FNA"))
                    s.add(FAS);

                // Non-woven reusable bags → fashion boutiques + health & beauty (spas, salons, pharmacies)
                if (hasAny(name, "NON-WOVEN", "NON WOVEN", "WOVEN"))
                    s.addAll(List.of(FAS, HAB));

                // Medical / pharmacy bags → health & beauty (pharmaceutical attendants)
                if (hasAny(name, "MEDICAL", "PHARMACY", "DISPENSARY"))
                    s.add(HAB);

                // Agricultural / farm sacks → poultry farmers, crops farmers
                if (hasAny(name, "PANEL SACK", "BOX BOTTOM", "V22", "V24", "V30",
                        "50KG", "90KG", "100KG", "JUTE", "WOVEN SACK"))
                    s.addAll(List.of(AGR, MFG));

                // Garbage / bin liners → hospitality (hotels, kitchens) + manufacturing (factories)
                if (hasAny(name, "GARBAGE", "REFUSE", "BIN LINER", "TRASH"))
                    s.addAll(List.of(HOS, MFG));

                // Net bags → fresh produce farmers
                if (hasAny(name, "NET BAG", "RED NET", "PRODUCE NET"))
                    s.add(AGR);

                // Bread / bakery bags → food & beverage
                if (hasAny(name, "BREAD", "LOAF BAG"))
                    s.add(FAB);

                // Hospitality also uses bags
                s.add(HOS);
            }

            // ── Food & Condiments ─────────────────────────────────────────────────
            case "FOOD & CONDIMENTS" ->
                s.addAll(List.of(FAB, HOS, RET));   // cafés, hotels, AND retail supermarkets

            // ── Thermal Rolls ─────────────────────────────────────────────────────
            // Used by: supermarkets (RET), cafés/restaurants (FAB), hotels (HOS),
            // factories (MFG), pharmacies (HAB), agro-dealers (AGR)
            case "THERMAL ROLLS" ->
                s.addAll(List.of(RET, FAB, HOS, MFG, HAB, AGR));

            // ── Cups & Lids ───────────────────────────────────────────────────────
            case "CUPS & LIDS", "CUPS", "LIDS" -> {
                s.addAll(List.of(FAB, HOS));
                // Cold drink / juice bar cups → retail + health (wellness juice bars)
                if (hasAny(name, "ICE CREAM", "BUBBLE", "PET U", "COLD", "SMOOTHIE",
                        "JUICE", "CLEAR CUP", "PET CUP", "COLD DRINK"))
                    s.addAll(List.of(RET, HAB));
                // Sleeves & generic → retail too
                s.add(RET);
            }

            // ── Wrapping & Foil ───────────────────────────────────────────────────
            case "WRAPPING & FOIL", "FOIL", "FILM & WRAP", "FOIL TINS" -> {
                s.addAll(List.of(FAB, HOS));
                // Cling / stretch / cold-chain film → crops farmers, cold stores
                if (hasAny(name, "CLING", "FILM", "STRETCH WRAP", "JUMBO CLING", "PRODUCE"))
                    s.addAll(List.of(AGR, MFG));
                // Aluminium foil tins → catering + retail + manufacturing
                if (hasAny(name, "ALUMINIUM", "FOIL TIN"))
                    s.addAll(List.of(RET, MFG));
                // Greaseproof / baking paper → bakeries (FAB + RET)
                if (hasAny(name, "GREASEPROOF", "BAKING PAPER", "PARCHMENT"))
                    s.add(RET);
            }

            // ── Containers & Trays ────────────────────────────────────────────────
            case "CONTAINERS & TRAYS", "CONTAINERS", "BOXES & BAKING", "TRAYS" -> {
                s.addAll(List.of(FAB, HOS));
                // Punnets → crops farmers, supermarkets (for fresh produce display)
                if (hasAny(name, "PUNNET"))
                    s.addAll(List.of(AGR, RET));
                // Cake / bakery boxes → retail + FAB
                if (hasAny(name, "CAKE", "CUPCAKE", "MUFFIN", "BAKING", "PASTRY"))
                    s.addAll(List.of(RET, FAB));
                // Aluminium trays → manufacturing + hospitality
                if (hasAny(name, "ALUMINIUM", "FOIL TRAY"))
                    s.add(MFG);
                // Microwave containers → retail (office workers, supermarkets)
                if (hasAny(name, "MICROWAVE"))
                    s.add(RET);
            }

            // ── Tableware & Hygiene ───────────────────────────────────────────────
            case "TABLEWARE & HYGIENE", "PLATES", "TABLEWARE" -> {
                s.addAll(List.of(FAB, HOS, RET));   // events / caterers / supermarkets
                // Wipes, tissue, towels → health & beauty (salons, clinics) + retail
                if (hasAny(name, "WIPE", "TOWEL", "TISSUE", "NAPKIN", "SERVIETTE"))
                    s.add(HAB);
            }

            // ── Cutlery ───────────────────────────────────────────────────────────
            case "CUTLERY" ->
                s.addAll(List.of(FAB, HOS, RET));

            // ── Straws & Stirrers ─────────────────────────────────────────────────
            case "STRAWS & STIRRERS", "STRAWS", "ACCESSORIES" ->
                s.addAll(List.of(FAB, HOS, RET));

            // ── Jars & Bottles ────────────────────────────────────────────────────
            case "JARS & BOTTLES", "JARS", "BOTTLES" -> {
                s.addAll(List.of(FAB, RET));
                // Honey jars → beekeepers / agro-processors
                if (hasAny(name, "HONEY"))
                    s.addAll(List.of(AGR, HAB));
                // Condiment / spice jars → health & retail
                if (hasAny(name, "POLYPOT", "POT", "SPICE", "CONDIMENT", "SAUCE"))
                    s.addAll(List.of(HAB, FAB));
                // Cosmetic jars / lotion bottles → health & beauty
                if (hasAny(name, "CREAM", "LOTION", "COSMETIC", "BEAUTY"))
                    s.add(HAB);
                // Pharmaceutical bottles
                if (hasAny(name, "MEDICINE", "PHARMACY", "SUPPLEMENT", "SYRUP"))
                    s.add(HAB);
            }

            // ── Hygiene & PPE ─────────────────────────────────────────────────────
            case "HYGIENE & PPE", "HYGIENE" -> {
                // Pharmaceutical attendants, salon workers, food handlers, factory workers
                s.addAll(List.of(HAB, FAB, MFG, HOS));
                // All PPE serves multiple sectors
                s.add(RET);   // pharmacies / supermarkets stock PPE for sale
            }

            // ── Tapes ─────────────────────────────────────────────────────────────
            case "TAPES", "TAPE" ->
                s.addAll(List.of(MFG, RET, FAB));

            // ── General ───────────────────────────────────────────────────────────
            case "GENERAL" -> {
                s.addAll(List.of(RET, FAB));
                // Large-volume / market bags → agro dealers, mama mboga
                if (hasAny(name, "KHAKI", "SAWA", "MAFUCO", "MEGA", "ECOLAB",
                        "CLICK", "KINGS", "PYRAMID"))
                    s.addAll(List.of(AGR, HOS));
                // Manila / envelopes → office / manufacturing
                if (hasAny(name, "MANILA", "ENVELOPE"))
                    s.add(MFG);
                // Net bags → agro fresh produce
                if (hasAny(name, "NET BAG", "RED NET"))
                    s.add(AGR);
                // Kraft bowls, deli boxes → hospitality
                if (hasAny(name, "SALAD BOWL", "DELI BOX", "KRAFT"))
                    s.add(HOS);
            }

            // ── Miscellaneous & Boards ────────────────────────────────────────────
            case "MISCELLANEOUS" -> s.addAll(List.of(FAB, HOS, RET));
            case "BOARDS"        -> s.addAll(List.of(FAB, HOS));

            // ── Riseller extra categories ─────────────────────────────────────────
            case "PLATES"       -> s.addAll(List.of(FAB, HOS, RET));
            case "BOXES & BAKING" -> s.addAll(List.of(FAB, HOS, RET));
            case "FILM & WRAP"  -> s.addAll(List.of(FAB, HOS, AGR, MFG));

            default -> s.addAll(List.of(RET, FAB));
        }

        return s;
    }

    private boolean hasAny(String text, String... tokens) {
        for (String t : tokens) {
            if (text.contains(t)) return true;
        }
        return false;
    }
}
