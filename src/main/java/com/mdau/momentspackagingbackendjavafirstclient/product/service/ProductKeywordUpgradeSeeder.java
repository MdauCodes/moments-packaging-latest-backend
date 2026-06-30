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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Second-pass keyword enrichment seeder.
 * Adds customer-centric, persona-based, and Swahili keywords that complement the
 * base keywords added by ProductEnrichmentSeeder (@Order 30).
 *
 * Idempotency: guarded by a sentinel keyword ("packaging kenya v2") that is added
 * to every product on the first run and detected on subsequent runs to skip.
 * Only runs after ProductEnrichmentSeeder has populated industries.
 */
@Slf4j
@Component
@Order(31)
@RequiredArgsConstructor
public class ProductKeywordUpgradeSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;

    private static final String V2_SENTINEL = "packaging kenya v2";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> allActive = productRepository.findAllActive();
        if (allActive.isEmpty()) {
            log.info("ProductKeywordUpgradeSeeder: no active products — skipping.");
            return;
        }

        // Only run after ProductEnrichmentSeeder has associated industries
        List<Product> enriched = allActive.stream()
                .filter(p -> p.getIndustries() != null && !p.getIndustries().isEmpty())
                .collect(Collectors.toList());

        if (enriched.isEmpty()) {
            log.info("ProductKeywordUpgradeSeeder: no industry-enriched products yet — skipping.");
            return;
        }

        // Idempotency check: skip if any product already has the V2 sentinel
        boolean alreadyUpgraded = enriched.stream()
                .anyMatch(p -> p.getKeywords() != null && p.getKeywords().contains(V2_SENTINEL));
        if (alreadyUpgraded) {
            log.info("ProductKeywordUpgradeSeeder: already upgraded — skipping.");
            return;
        }

        int upgraded = 0;
        for (Product p : enriched) {
            String cat  = p.getCategory() != null ? p.getCategory().toUpperCase(Locale.ROOT) : "";
            String name = p.getName()     != null ? p.getName().toUpperCase(Locale.ROOT)     : "";

            List<String> extras = generateEnhancedKeywords(cat, name);
            extras.add(V2_SENTINEL);

            if (p.getKeywords() == null) p.setKeywords(new ArrayList<>());
            extras.stream()
                    .filter(k -> k != null && !k.isBlank() && !p.getKeywords().contains(k))
                    .forEach(p.getKeywords()::add);

            productRepository.save(p);
            upgraded++;
        }

        log.info("ProductKeywordUpgradeSeeder: enhanced keywords for {} products.", upgraded);
    }

    // ── Enhanced keyword generation ───────────────────────────────────────────────

    private List<String> generateEnhancedKeywords(String cat, String name) {
        List<String> kws = new ArrayList<>();

        // ── Category-level persona & context keywords ──────────────────────────
        switch (cat) {
            case "BAGS" -> kws.addAll(List.of(
                    "mifuko", "mifuko ya plastiki", "polythene", "polybag",
                    "bags wholesale nairobi", "bags kenya", "packaging bags nairobi",
                    "mama mboga bag", "duka bag", "kiosk bag", "grocery bag",
                    "market bag", "shop bag", "daily bag", "kadama",
                    "vegetable bag", "nyama bag", "butchery bag",
                    "carrier bag kenya", "plastic carrier bag"
            ));
            case "FOOD & CONDIMENTS" -> kws.addAll(List.of(
                    "restaurant supplies nairobi", "catering supplies kenya",
                    "hotel kitchen supplies", "food service packaging",
                    "chakula packaging", "condiment supply", "table sachet",
                    "single portion", "fast food supply", "kikombe cha chakula"
            ));
            case "THERMAL ROLLS" -> kws.addAll(List.of(
                    "thermal paper roll", "POS paper roll", "receipt printer paper",
                    "till receipt paper", "cashier paper", "billing paper",
                    "karatasi ya POS machine", "karatasi ya machini ya risiti",
                    "M-Pesa receipt paper", "supermarket receipt paper",
                    "register paper kenya", "57mm roll", "79mm roll", "80mm roll",
                    "ATM paper roll", "petrol station receipt"
            ));
            case "CUPS & LIDS", "CUPS", "LIDS" -> kws.addAll(List.of(
                    "vikombe", "vikombe vya plastiki", "vikombe vya karatasi",
                    "cup wholesale nairobi", "take-away cup kenya",
                    "kopo la kahawa", "kopo la chai", "disposable kikombe",
                    "coffee shop cup", "barista cup", "cafe packaging",
                    "espresso cup", "latte cup", "hot chocolate cup",
                    "5oz cup", "6oz cup", "8oz cup", "10oz cup", "12oz cup", "16oz cup",
                    "small cup", "medium cup", "large cup",
                    "milkshake cup", "slushie cup", "frappe cup", "iced drink cup"
            ));
            case "WRAPPING & FOIL", "FOIL", "FILM & WRAP", "FOIL TINS" -> kws.addAll(List.of(
                    "food safe wrap", "covering food", "keep food fresh",
                    "silver foil kenya", "cooking foil", "baking foil",
                    "BBQ foil", "grilling foil", "foil paper kenya",
                    "buffet wrap", "catering wrap", "karatasi ya kufunika chakula",
                    "fresh produce film", "export packaging film", "cold storage film",
                    "flower wrap", "farm packaging", "stretch film"
            ));
            case "CONTAINERS & TRAYS", "CONTAINERS", "BOXES & BAKING", "TRAYS" -> kws.addAll(List.of(
                    "food delivery packaging", "delivery box nairobi",
                    "restaurant takeaway packaging", "mkandamizo wa chakula",
                    "mkebe wa kutumia mara moja", "food box kenya",
                    "aluminium tray kenya", "foil tray catering",
                    "serving tray", "buffet tray", "chafing tray liner",
                    "bakery packaging nairobi", "pastry box", "bread box",
                    "microwave safe container", "reheatable packaging",
                    "meal prep container kenya", "hermetically sealed container"
            ));
            case "TABLEWARE & HYGIENE", "PLATES", "TABLEWARE" -> kws.addAll(List.of(
                    "disposable tableware kenya", "event plates nairobi",
                    "buffet supplies", "catering plates wholesale",
                    "sahani za plastiki", "sahani za karatasi",
                    "party supplies nairobi", "wedding disposables",
                    "conference catering supplies", "tissue paper kenya",
                    "wet wipe", "hand wipe", "table napkin", "serviette"
            ));
            case "CUTLERY" -> kws.addAll(List.of(
                    "vyombo vya kula", "vikinga vya chakula",
                    "disposable spoon fork knife", "party cutlery set",
                    "event cutlery nairobi", "wedding cutlery", "catering cutlery set",
                    "buffet cutlery", "street food cutlery", "takeaway fork spoon",
                    "soup spoon disposable", "compostable cutlery", "plant-based cutlery",
                    "eco friendly cutlery kenya"
            ));
            case "STRAWS & STIRRERS", "STRAWS", "ACCESSORIES" -> kws.addAll(List.of(
                    "paper straws kenya", "straw wholesale nairobi",
                    "drinking tube", "mkongojo wa kunywa", "bomba la kunywa",
                    "fresh juice straw", "smoothie straw", "fat straw", "wide straw",
                    "cocktail stirrer", "coffee stirrer", "hot drink stirrer",
                    "bamboo stirrer", "biodegradable straw kenya",
                    "compostable straw", "eco straw", "jumbo straw"
            ));
            case "JARS & BOTTLES", "JARS", "BOTTLES" -> kws.addAll(List.of(
                    "PET packaging kenya", "food grade bottle", "airtight jar kenya",
                    "sauce bottle", "ketchup bottle", "hot sauce bottle", "dressing bottle",
                    "pickle jar", "spice jar", "seasoning jar", "herbs jar",
                    "spice packaging kenya", "condiment jar", "chupa ya plastiki",
                    "cosmetic jar", "lotion bottle", "cream jar", "beauty packaging jar"
            ));
            case "HYGIENE & PPE", "HYGIENE" -> kws.addAll(List.of(
                    "food safety equipment", "kitchen safety gear", "HACCP packaging",
                    "food grade gloves", "latex free gloves", "nitrile gloves kenya",
                    "vinyl gloves", "disposable hair cap", "chef hair cover", "food prep hat",
                    "kitchen PPE nairobi", "hospitality gloves", "food handler gloves",
                    "medical gloves", "clinic supplies", "health facility packaging",
                    "food safety compliance", "food hygiene supplies"
            ));
            case "TAPES", "TAPE" -> kws.addAll(List.of(
                    "packing tape kenya", "carton sealing tape", "box tape",
                    "tape wholesale nairobi", "brown tape", "clear tape",
                    "masking tape kenya", "double sided tape", "fragile tape",
                    "warehouse tape", "logistics tape", "e-commerce packaging tape"
            ));
            default -> kws.addAll(List.of(
                    "packaging supplier nairobi", "packaging wholesale kenya",
                    "packaging materials kenya", "packaging shop nairobi OTC"
            ));
        }

        // ── Name-pattern persona keywords ─────────────────────────────────────
        if (hasAny(name, "GARBAGE", "REFUSE", "BIN LINER"))
            kws.addAll(List.of("bin liner", "rubbish bag", "trash bag", "taka bag",
                    "garbage liner kenya", "refuse collection bag", "skip bag"));

        if (hasAny(name, "PANEL SACK", "BOX BOTTOM", "50KG", "90KG", "100KG", "JUTE", "V22", "V24", "V30"))
            kws.addAll(List.of("magunia ya unga", "gunia ya unga", "sako ya unga",
                    "farm sack", "grain bag kenya", "produce sack", "woven sack",
                    "animal feed bag", "flour bag", "sugar sack"));

        if (hasAny(name, "KHAKI", "SAWA", "MAFUCO", "MEGA", "ECOLAB", "CLICK", "KINGS", "PYRAMID", "FNA"))
            kws.addAll(List.of("khaki bag kenya", "market bag nairobi", "mama mboga packaging",
                    "daily use bag", "kiosk polythene bag", "duka packaging",
                    "retail shop bag nairobi", "high-street bag", "open market bag"));

        if (hasAny(name, "SMART BAG", "CHINA", "MREMBO", "GUCCI", "KITENGE", "SAFARI",
                "PREMIER LEAGUE", "MATATU", "GIRAFFE", "HUMMING", "CHERRY", "ROSE",
                "FLOWERED", "3D", "SHINY", "LAMINATED", "NIGERIAN"))
            kws.addAll(List.of("branded polythene bag", "printed plastic bag", "fancy bag kenya",
                    "coloured shopping bag", "mfuko wa rangi", "gift bag plastic",
                    "boutique bag", "premium shopping bag", "attractive carry bag"));

        if (hasAny(name, "NON-WOVEN", "NON WOVEN"))
            kws.addAll(List.of("reusable shopping bag", "eco tote bag", "cloth carry bag",
                    "mfuko wa kitambaa", "non-woven tote", "washable bag",
                    "green bag", "eco-friendly carrier", "sustainable bag kenya"));

        if (hasAny(name, "KRAFT"))
            kws.addAll(List.of("premium paper bag", "boutique paper bag", "gift paper bag",
                    "brown kraft bag", "paper carry bag", "retail gift bag",
                    "eco paper bag kenya", "handle paper bag"));

        if (hasAny(name, "PIZZA"))
            kws.addAll(List.of("pizza restaurant packaging", "delivery pizza box",
                    "pizza chain supplies", "italian restaurant box",
                    "fast food pizza packaging", "pizza outlet supplies",
                    "sanduku la pizza nairobi", "pizza box wholesale kenya"));

        if (hasAny(name, "CAKE", "CUPCAKE", "MUFFIN"))
            kws.addAll(List.of("bakery shop packaging", "cake shop box", "patisserie packaging",
                    "birthday cake box", "wedding cake packaging",
                    "cupcake packaging kenya", "pastry shop supplies",
                    "sanduku la keki nairobi", "celebration cake packaging"));

        if (hasAny(name, "SALAD", "SALAD BOWL"))
            kws.addAll(List.of("healthy food packaging", "salad restaurant packaging",
                    "health food bowl", "quinoa bowl packaging",
                    "meal kit packaging", "clean eating packaging"));

        if (hasAny(name, "SHAWARMA"))
            kws.addAll(List.of("shawarma restaurant packaging", "wrap shop supplies",
                    "street food packaging nairobi", "fast food wrap packaging",
                    "Lebanese food packaging", "doner kebab box"));

        if (hasAny(name, "RIPPLE"))
            kws.addAll(List.of("premium coffee cup", "double wall cup", "insulated paper cup",
                    "barista quality cup", "specialty coffee packaging",
                    "artisan coffee cup", "take-away premium cup",
                    "anti-heat cup", "branded coffee cup"));

        if (hasAny(name, "BUBBLE TEA", "BOBA"))
            kws.addAll(List.of("bubble tea shop packaging", "boba tea cup",
                    "tapioca drink cup", "asian drink packaging", "milk tea packaging",
                    "matcha bubble tea cup", "passion fruit bubble tea"));

        if (hasAny(name, "ICE CREAM", "GELATO"))
            kws.addAll(List.of("gelato shop packaging", "ice cream parlour cup",
                    "frozen yoghurt cup", "aiskrimu packaging nairobi",
                    "sorbet cup", "ice cream kiosk supply", "frozen dessert packaging",
                    "soft serve cup", "ice cream tub wholesale"));

        if (hasAny(name, "HONEY"))
            kws.addAll(List.of("beekeeping packaging", "honey producer jar",
                    "organic honey jar kenya", "asali packaging nairobi",
                    "honey export packaging", "apiary packaging",
                    "natural honey bottle", "raw honey container"));

        if (hasAny(name, "PUNNET"))
            kws.addAll(List.of("strawberry punnet", "blueberry punnet", "grape punnet",
                    "cherry tomato punnet", "supermarket fresh produce",
                    "fruit tray packaging", "export horticulture packaging",
                    "supermarket display tray", "fresh berry packaging",
                    "green grocer packaging"));

        if (hasAny(name, "CLING", "CLING FILM"))
            kws.addAll(List.of("food storage film", "keep food fresh film",
                    "fridge wrap", "kitchen cling film kenya",
                    "catering cling wrap", "jumbo cling film wholesale",
                    "polythene food wrap", "PVC cling film"));

        if (hasAny(name, "GREASEPROOF", "BAKING PAPER"))
            kws.addAll(List.of("baking paper kenya", "parchment paper nairobi",
                    "anti-stick baking sheet", "oven paper", "non-stick paper",
                    "bakery parchment", "cookie sheet liner", "kitchen baking paper"));

        if (hasAny(name, "MICROWAVE"))
            kws.addAll(List.of("microwave meal box", "ready meal packaging",
                    "reheatable food packaging", "meal prep packaging kenya",
                    "microwave lunch box", "office meal container"));

        if (hasAny(name, "POPCORN"))
            kws.addAll(List.of("popcorn shop packaging", "cinema snack tub",
                    "movie popcorn bucket", "event popcorn packaging",
                    "snack tub wholesale", "party popcorn box"));

        if (hasAny(name, "GLOVE", "GLOVES"))
            kws.addAll(List.of("food handler gloves kenya", "kitchen work gloves",
                    "disposable examination gloves", "blue nitrile gloves",
                    "vinyl gloves catering", "protective work gloves",
                    "food processing gloves", "HACCP compliant gloves"));

        if (hasAny(name, "HAIRNET", "HAIR NET"))
            kws.addAll(List.of("chef hairnet", "food preparation hairnet",
                    "kitchen hair cover", "catering hair cover",
                    "food factory headcover", "HACCP hairnet kenya"));

        if (hasAny(name, "STRAW"))
            kws.addAll(List.of("drinking straw wholesale nairobi", "paper straw supplier",
                    "biodegradable straw supplier", "straw for juice bar",
                    "restaurant straw supply", "cocktail straw kenya"));

        if (hasAny(name, "SACHET", "SALT SACHET", "SUGAR SACHET", "SAUCE SACHET"))
            kws.addAll(List.of("condiment sachet supplier", "single serve packaging",
                    "fast food sachet", "restaurant table sachet",
                    "hotel room condiment", "airline catering sachet",
                    "portion control packaging", "individual serving sachet"));

        if (hasAny(name, "ALUMINIUM", "FOIL TIN"))
            kws.addAll(List.of("aluminium foil tray", "foil container catering",
                    "BBQ aluminium tray", "roasting tray kenya",
                    "takeaway foil container", "oven-ready foil tray",
                    "buffet foil chafing dish", "baking foil tray"));

        if (hasAny(name, "BIODEGRADABLE", "BIO BLEND", "BAMBOO", "WOODEN", "CORNSTARCH",
                "ECO HAPA", "BIO", "COMPOSTABLE"))
            kws.addAll(List.of("sustainable packaging kenya", "eco friendly packaging nairobi",
                    "green packaging supplier", "compostable packaging",
                    "environmentally friendly packaging", "zero waste packaging",
                    "packaging ya mazingira", "biodegradable packaging supplier kenya",
                    "plastic alternative packaging", "eco conscious packaging"));

        // Remove duplicates
        return kws.stream().distinct().collect(Collectors.toList());
    }

    private boolean hasAny(String text, String... tokens) {
        for (String t : tokens) {
            if (text.contains(t)) return true;
        }
        return false;
    }
}
