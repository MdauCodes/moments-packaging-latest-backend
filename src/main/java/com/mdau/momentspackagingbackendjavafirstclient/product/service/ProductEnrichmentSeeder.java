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
 * Runs once at startup (after all ProductSeeders and IndustrySeeder) to enrich every product with:
 *   - Industry associations (which business sectors use this product)
 *   - Search keywords (what customers type to find it)
 *   - isFastMoving flag (high-volume everyday commodity)
 *   - Tags (Featured / Trending where applicable)
 *
 * Logic is category + name-pattern based so it automatically covers Riseller-synced products too.
 * Guard: skips entirely if any product already has industries set (idempotent).
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class ProductEnrichmentSeeder implements ApplicationRunner {

    private final ProductRepository  productRepository;
    private final IndustryRepository industryRepository;

    // Industry slug constants (must match IndustrySeeder)
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
            log.info("ProductEnrichmentSeeder: no active products — skipping.");
            return;
        }

        long alreadyEnriched = allActive.stream()
                .filter(p -> p.getIndustries() != null && !p.getIndustries().isEmpty())
                .count();
        if (alreadyEnriched > 0) {
            log.info("ProductEnrichmentSeeder: {} products already have industries — skipping.", alreadyEnriched);
            return;
        }

        slugToIndustry = industryRepository.findAll().stream()
                .collect(Collectors.toMap(Industry::getSlug, i -> i));

        if (slugToIndustry.isEmpty()) {
            log.warn("ProductEnrichmentSeeder: no industries in DB — skipping (run IndustrySeeder first).");
            return;
        }

        int enriched = 0;
        for (Product p : allActive) {
            String cat  = p.getCategory() != null ? p.getCategory().toUpperCase(Locale.ROOT) : "";
            String name = p.getName()     != null ? p.getName().toUpperCase(Locale.ROOT)     : "";

            // Industries
            Set<String> slugs = resolveIndustrySlugs(cat, name);
            slugs.stream()
                    .map(slugToIndustry::get)
                    .filter(Objects::nonNull)
                    .forEach(p.getIndustries()::add);

            // Keywords — merge without duplicating existing ones
            List<String> kws = generateKeywords(cat, name, p.getName());
            if (p.getKeywords() == null) p.setKeywords(new ArrayList<>());
            kws.stream()
                    .filter(k -> k != null && !k.isBlank() && !p.getKeywords().contains(k))
                    .forEach(p.getKeywords()::add);

            // isFastMoving
            p.setIsFastMoving(resolveFastMoving(cat, name));

            // Tags
            List<String> newTags = generateTags(cat, name);
            if (p.getTags() == null) p.setTags(new ArrayList<>());
            newTags.stream()
                    .filter(t -> !p.getTags().contains(t))
                    .forEach(p.getTags()::add);

            productRepository.save(p);
            enriched++;
        }

        log.info("ProductEnrichmentSeeder: enriched {} products with industries, keywords, tags.", enriched);
    }

    // ── Industry rules ────────────────────────────────────────────────────────────

    private Set<String> resolveIndustrySlugs(String cat, String name) {
        Set<String> s = new LinkedHashSet<>();

        switch (cat) {
            // ── Bags ────────────────────────────────────────────────────────
            case "BAGS" -> {
                s.addAll(List.of(RET, FAB));
                // Kraft / paper handled bags → fashion & apparel (boutiques, shoe shops)
                if (hasAny(name, "KRAFT", "BROWN HANDLED", "TWISTED", "MILLINARY", "DUNE NO.")) s.add(FAS);
                // Printed smart bags (China, Mrembo, themed prints) → fashion
                if (hasAny(name, "SMART BAG", "CHINA", "MREMBO", "GUCCI", "KITENGE",
                        "SAFARI", "PREMIER LEAGUE", "MATATU", "GIRAFFE", "HUMMING",
                        "CHERRY", "ROSE", "FLOWERED", "3D", "SHINY", "LAMINATED",
                        "NIGERIAN", "FNA")) s.add(FAS);
                // Non-woven reusable bags → fashion + health/beauty (pharmacy, cosmetics)
                if (hasAny(name, "NON-WOVEN", "NON WOVEN", "WOVEN")) { s.add(FAS); s.add(HAB); }
                // Medical/pharmacy bags → health & beauty
                if (hasAny(name, "MEDICAL", "PHARMACY")) s.add(HAB);
                // Agricultural sacks (panel, box-bottom, large-volume bales) → agriculture + manufacturing
                if (hasAny(name, "PANEL SACK", "BOX BOTTOM", "V22", "V24", "V30", "50KG", "90KG", "JUTE"))
                    s.addAll(List.of(AGR, MFG));
                // Garbage / refuse → hospitality + manufacturing
                if (hasAny(name, "GARBAGE", "REFUSE")) s.addAll(List.of(HOS, MFG));
            }

            // ── Food & Condiments ───────────────────────────────────────────
            case "FOOD & CONDIMENTS" -> s.addAll(List.of(FAB, HOS));

            // ── Thermal Rolls ───────────────────────────────────────────────
            case "THERMAL ROLLS" -> s.addAll(List.of(RET, FAB, HOS, MFG));

            // ── Cups & Lids ─────────────────────────────────────────────────
            case "CUPS & LIDS", "CUPS", "LIDS" -> {
                s.addAll(List.of(FAB, HOS));
                // Cold beverage cups (bubble tea, slushies, juice bars) → retail
                if (hasAny(name, "ICE CREAM", "BUBBLE", "PET U", "COLD", "SMOOTHIE",
                        "JUICE", "CLEAR CUP", "PET CUP")) s.add(RET);
            }

            // ── Wrapping & Foil ─────────────────────────────────────────────
            case "WRAPPING & FOIL", "FOIL", "FILM & WRAP", "FOIL TINS" -> {
                s.addAll(List.of(FAB, HOS));
                // Cling film and stretch wrap → agriculture (farm produce, cold chain) + manufacturing
                if (hasAny(name, "CLING", "FILM", "STRETCH WRAP", "JUMBO CLING")) s.addAll(List.of(AGR, MFG));
            }

            // ── Containers & Trays ──────────────────────────────────────────
            case "CONTAINERS & TRAYS", "CONTAINERS", "BOXES & BAKING", "TRAYS" -> {
                s.addAll(List.of(FAB, HOS));
                // Punnets → agriculture (fresh produce) + retail (supermarket display)
                if (hasAny(name, "PUNNET")) s.addAll(List.of(AGR, RET));
                // Cake boxes → retail (bakery customers)
                if (hasAny(name, "CAKE", "CUPCAKE", "MUFFIN", "BAKING")) s.add(RET);
                // Aluminium trays → manufacturing (industrial heat use)
                if (hasAny(name, "ALUMINIUM")) s.add(MFG);
            }

            // ── Tableware & Hygiene ─────────────────────────────────────────
            case "TABLEWARE & HYGIENE", "PLATES", "TABLEWARE" -> {
                s.addAll(List.of(FAB, HOS));
                // Wipes / towels → health & beauty too
                if (hasAny(name, "WIPE", "TOWEL", "TISSUE", "NAPKIN")) s.add(HAB);
            }

            // ── Cutlery ─────────────────────────────────────────────────────
            case "CUTLERY" -> s.addAll(List.of(FAB, HOS));

            // ── Straws & Stirrers ────────────────────────────────────────────
            case "STRAWS & STIRRERS", "STRAWS", "ACCESSORIES" -> s.addAll(List.of(FAB, HOS));

            // ── Jars & Bottles ──────────────────────────────────────────────
            case "JARS & BOTTLES", "JARS", "BOTTLES" -> {
                s.addAll(List.of(FAB, RET));
                // Honey jars → agriculture (honey producers) + health (natural foods)
                if (hasAny(name, "HONEY")) s.addAll(List.of(AGR, HAB));
                // Polypots / condiment jars → health & beauty (also used for spices, supplements)
                if (hasAny(name, "POLYPOT", "POT")) s.add(HAB);
            }

            // ── Hygiene & PPE ───────────────────────────────────────────────
            case "HYGIENE & PPE", "HYGIENE" -> {
                s.addAll(List.of(HAB, FAB, MFG));
                // Gloves, masks, hairnets → hospitality (kitchens, hotels)
                if (hasAny(name, "GLOVE", "MASK", "HAIRNET", "APRON")) s.add(HOS);
            }

            // ── Tapes ───────────────────────────────────────────────────────
            case "TAPES", "TAPE" -> s.addAll(List.of(MFG, RET));

            // ── General ─────────────────────────────────────────────────────
            case "GENERAL" -> {
                s.addAll(List.of(RET, FAB));
                // Khaki/market bags → agriculture (farmers market)
                if (hasAny(name, "KHAKI", "SAWA", "MAFUCO", "MEGA", "ECOLAB", "CLICK", "KINGS", "PYRAMID"))
                    s.add(AGR);
                // Manila envelopes → manufacturing / office
                if (hasAny(name, "MANILA", "ENVELOPE")) s.add(MFG);
                // Net bags (produce) → agriculture
                if (hasAny(name, "NET BAG", "RED NET")) s.add(AGR);
                // Kraft containers listed as General
                if (hasAny(name, "SALAD BOWL", "DELI BOX", "KRAFT")) s.add(HOS);
            }

            // ── Miscellaneous ────────────────────────────────────────────────
            case "MISCELLANEOUS" -> s.addAll(List.of(FAB, HOS));

            // ── Riseller-inferred categories ────────────────────────────────
            case "BOARDS" -> s.addAll(List.of(FAB, HOS));

            default -> s.addAll(List.of(RET, FAB));
        }

        return s;
    }

    // ── Keyword generation ────────────────────────────────────────────────────────

    private List<String> generateKeywords(String cat, String name, String originalName) {
        List<String> kws = new ArrayList<>();

        // Words from product name (meaningful, ≥3 chars)
        if (originalName != null) {
            Arrays.stream(originalName.toLowerCase(Locale.ROOT)
                            .replaceAll("[^a-z0-9\\s]", " ").trim().split("\\s+"))
                    .filter(w -> w.length() >= 3)
                    .forEach(kws::add);
        }

        // Category base keywords (English + Swahili)
        switch (cat) {
            case "BAGS" ->
                kws.addAll(List.of("mfuko", "bag", "polythene bag", "carrier bag", "packaging bag",
                        "plastic bag", "mfuko wa plastiki", "shopping bag", "retail bag"));
            case "FOOD & CONDIMENTS" ->
                kws.addAll(List.of("food", "catering", "restaurant supplies", "condiment",
                        "kitchen", "food service", "hotel supply", "chakula"));
            case "THERMAL ROLLS" ->
                kws.addAll(List.of("thermal paper", "receipt roll", "POS roll", "till roll",
                        "thermal roll", "printer paper", "karatasi ya POS", "receipt paper",
                        "cashier roll"));
            case "CUPS & LIDS", "CUPS", "LIDS" ->
                kws.addAll(List.of("disposable cup", "paper cup", "kikombe", "coffee cup",
                        "takeaway cup", "cup lid", "hot cup", "cold cup", "beverage cup"));
            case "WRAPPING & FOIL", "FOIL", "FILM & WRAP", "FOIL TINS" ->
                kws.addAll(List.of("cling film", "aluminium foil", "food wrap", "wrapping",
                        "karatasi ya kufunika", "plastic wrap", "kitchen foil",
                        "food packaging film"));
            case "CONTAINERS & TRAYS", "CONTAINERS", "BOXES & BAKING", "TRAYS" ->
                kws.addAll(List.of("food container", "takeaway box", "food box", "chombo",
                        "packaging container", "tray", "deli box", "meal box",
                        "mkebe wa chakula"));
            case "TABLEWARE & HYGIENE", "PLATES", "TABLEWARE" ->
                kws.addAll(List.of("disposable plate", "catering plate", "event plate",
                        "sahani ya plastiki", "buffet plate", "tableware", "hygiene supply"));
            case "CUTLERY" ->
                kws.addAll(List.of("disposable cutlery", "spoon", "fork", "knife",
                        "kijiko", "uma", "wooden cutlery", "eco cutlery", "catering cutlery"));
            case "STRAWS & STIRRERS", "STRAWS", "ACCESSORIES" ->
                kws.addAll(List.of("drinking straw", "coffee stirrer", "mkongojo",
                        "straw", "stirrer", "bubble tea straw", "biodegradable straw"));
            case "JARS & BOTTLES", "JARS", "BOTTLES" ->
                kws.addAll(List.of("jar", "PET jar", "food jar", "chupa", "packaging jar",
                        "storage jar", "honey jar", "condiment jar"));
            case "HYGIENE & PPE", "HYGIENE" ->
                kws.addAll(List.of("PPE", "gloves", "food safety", "hairnet", "face mask",
                        "hygiene", "disposable gloves", "kitchen safety", "ulinzi"));
            case "TAPES", "TAPE" ->
                kws.addAll(List.of("packing tape", "adhesive tape", "sellotape", "masking tape",
                        "packaging tape", "carton tape", "tape roll"));
            default ->
                kws.addAll(List.of("packaging", "wholesale", "Nairobi supplier", "packaging Kenya"));
        }

        // Product-specific context keywords from name patterns
        if (hasAny(name, "BIODEGRADABLE", "ECO", "BIO BLEND"))
            kws.addAll(List.of("eco friendly packaging", "biodegradable", "green packaging", "sustainable packaging"));
        if (hasAny(name, "BAMBOO", "WOODEN"))
            kws.addAll(List.of("wooden cutlery", "bamboo packaging", "eco cutlery", "natural packaging"));
        if (hasAny(name, "KRAFT"))
            kws.addAll(List.of("kraft paper", "brown paper packaging", "kraft bag", "eco packaging"));
        if (hasAny(name, "PIZZA"))
            kws.addAll(List.of("pizza box", "pizza delivery box", "pizza packaging", "sanduku la pizza"));
        if (hasAny(name, "CAKE", "CUPCAKE"))
            kws.addAll(List.of("cake box", "bakery box", "cake packaging", "sanduku la keki", "bakery packaging"));
        if (hasAny(name, "SALAD", "SALAD BOWL"))
            kws.addAll(List.of("salad bowl", "kraft salad bowl", "salad packaging", "salad container"));
        if (hasAny(name, "TAKEAWAY", "DELI BOX"))
            kws.addAll(List.of("takeaway packaging", "food delivery box", "restaurant packaging",
                    "takeaway container", "delivery packaging"));
        if (hasAny(name, "HONEY"))
            kws.addAll(List.of("honey jar", "asali jar", "honey packaging", "PET honey jar", "honey container"));
        if (hasAny(name, "RIPPLE"))
            kws.addAll(List.of("ripple cup", "double wall cup", "insulated coffee cup", "premium cup"));
        if (hasAny(name, "BUBBLE TEA", "BOBA"))
            kws.addAll(List.of("bubble tea cup", "boba cup", "wide straw", "bubble tea straw"));
        if (hasAny(name, "ICE CREAM"))
            kws.addAll(List.of("ice cream cup", "gelato cup", "ice cream packaging", "aiskrimu cup",
                    "frozen dessert cup", "ice cream tub"));
        if (hasAny(name, "PANEL SACK", "50KG", "90KG", "BOX BOTTOM"))
            kws.addAll(List.of("grain sack", "magunia", "gunia", "farm sack", "produce bag",
                    "bulk sack", "agricultural packaging"));
        if (hasAny(name, "CLING"))
            kws.addAll(List.of("cling wrap", "food wrap film", "plastic wrap", "cling film Kenya", "polythene wrap"));
        if (hasAny(name, "PUNNET"))
            kws.addAll(List.of("punnet", "fruit punnet", "produce punnet", "berry container",
                    "fresh produce packaging", "supermarket punnet"));
        if (hasAny(name, "SHAWARMA", "WRAP BOX"))
            kws.addAll(List.of("shawarma box", "wrap box", "street food packaging", "wrap packaging"));
        if (hasAny(name, "POPCORN"))
            kws.addAll(List.of("popcorn tub", "popcorn box", "cinema packaging", "snack tub"));
        if (hasAny(name, "SACHET", "SALT SACHET", "SUGAR SACHET", "SAUCE SACHET"))
            kws.addAll(List.of("sachet", "single serve sachet", "condiment sachet", "restaurant sachet",
                    "table sachet", "portion sachet"));
        if (hasAny(name, "SMART BAG"))
            kws.addAll(List.of("smart bag", "printed polythene bag", "branded bag", "retail smart bag",
                    "coloured bag", "shopping bag"));
        if (hasAny(name, "NON-WOVEN", "NON WOVEN"))
            kws.addAll(List.of("non woven bag", "reusable bag", "eco bag", "tote bag", "cloth bag",
                    "mfuko wa kitambaa"));
        if (hasAny(name, "THERMAL", "RECEIPT", "TILL ROLL", "POS"))
            kws.addAll(List.of("thermal receipt", "POS paper", "till roll", "cash register paper",
                    "billing paper", "karatasi ya risiti"));
        if (hasAny(name, "MICROWAVE"))
            kws.addAll(List.of("microwave container", "meal prep container", "reheatable container",
                    "food prep box", "microwave safe box"));

        // Remove duplicates (maintain insertion order)
        return kws.stream().distinct().collect(Collectors.toList());
    }

    // ── isFastMoving rules ────────────────────────────────────────────────────────

    private boolean resolveFastMoving(String cat, String name) {
        return switch (cat) {
            case "BAGS" ->
                // High-volume everyday bags: coloured polythene smart bags, khaki, garbage
                hasAny(name, "SMART BAG", "CHINA", "KHAKI", "GARBAGE", "REFUSE", "SAWA",
                        "MEGA", "MAFUCO", "ECOLAB", "CLICK", "DUNE", "THANK YOU", "FNA",
                        "PYRAMID", "KINGS", "FLOWERED", "NIGERIAN");
            case "THERMAL ROLLS"      -> true;  // always high turnover
            case "CUPS & LIDS"        ->
                hasAny(name, "8OZ", "12OZ", "8 OZ", "12 OZ", "5OZ", "SLEEVE");
            case "WRAPPING & FOIL"    ->
                hasAny(name, "CLING", "30*30", "30X30", "30*300", "45*5", "GREASEPROOF");
            case "TABLEWARE & HYGIENE"->
                hasAny(name, "PLATE", "WIPE", "NAPKIN");
            case "CUTLERY"            ->
                hasAny(name, "SPOON", "FORK", "TEA SPOON");
            case "STRAWS & STIRRERS"  ->
                hasAny(name, "STRAW", "FLEXIBLE", "CLEAR STRAW", "COLOURED STRAW");
            case "FOOD & CONDIMENTS"  ->
                hasAny(name, "SACHET", "SALT", "SUGAR", "SAUCE", "CHILLI SAUCE", "TOMATO SAUCE");
            case "GENERAL"            ->
                hasAny(name, "KHAKI", "SAWA", "MEGA", "MAFUCO", "CLICK", "PYRAMID", "KINGS");
            default -> false;
        };
    }

    // ── Tag generation ────────────────────────────────────────────────────────────

    private List<String> generateTags(String cat, String name) {
        List<String> tags = new ArrayList<>();

        // Trending: eco/sustainable packaging — strong consumer preference in Kenya 2025+
        if (hasAny(name, "BIODEGRADABLE", "BIO BLEND", "BAMBOO", "WOODEN", "WOOD",
                "CORNSTARCH", "KRAFT", "NON-WOVEN", "NON WOVEN", "ECO HAPA",
                "GREASEPROOF", "BIO"))
            tags.add("Trending");

        // Featured: flagship high-volume products every business type uses
        if (hasAny(name, "SMART BAG", "THANK YOU") ||
            (cat.equals("CUPS & LIDS") && hasAny(name, "8OZ", "12OZ", "RIPPLE")) ||
            (cat.equals("THERMAL ROLLS") && hasAny(name, "57*40", "57X40")))
            tags.add("Featured");

        return tags;
    }

    // ── Helper ────────────────────────────────────────────────────────────────────

    private boolean hasAny(String text, String... tokens) {
        for (String t : tokens) {
            if (text.contains(t)) return true;
        }
        return false;
    }
}
