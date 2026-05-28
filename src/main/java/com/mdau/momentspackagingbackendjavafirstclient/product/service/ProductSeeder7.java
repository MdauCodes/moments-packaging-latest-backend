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

@Slf4j
@Component
@Order(16)
@RequiredArgsConstructor
public class ProductSeeder7 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final Object[][] PRODUCTS = {
            { "436", "Pyramid No.15 Assorted Bags", "General",
                    "Assorted colour No.15 polythene bags. Pack of 50, 20 packs per carton.",
                    50, 4.50,  1000, 4.00,  0, 0.0 },
            { "413", "Pyramid No.22 Assorted Bags", "General",
                    "Assorted colour No.22 bags for medium retail. Pack of 50, 20 packs per carton.",
                    50, 6.50,  1000, 5.80,  0, 0.0 },
            { "474", "Pyramid No.25 Assorted Bags", "General",
                    "Assorted colour No.25 bags for large retail packaging. Pack of 50, 20 packs per carton.",
                    50, 8.00,  1000, 7.20,  0, 0.0 },
            { "254", "Rasmy 45x90 Aluminium Foil", "Wrapping & Foil",
                    "Professional 45cm x 90m aluminium foil for commercial food service. Consistent quality for baking and preservation. Pack of 12.",
                    12, 620.00,  60, 590.00,  0, 0.0 },
            { "160", "Rasmy Adhesive Tape 60mm x100", "Tapes",
                    "60mm x 100m adhesive clear tape for carton sealing. Pack of 5 rolls, 12 packs per carton.",
                    5, 280.00,  60, 260.00,  0, 0.0 },
            { "161", "Rasmy Masking Tape 8-Pack", "Tapes",
                    "General-purpose masking tape in 8-roll packs for painting, labelling, and packaging. Pack of 8, 24 packs per carton.",
                    8, 180.00,  192, 165.00,  0, 0.0 },
            { "226", "Red Smart Bag", "Bags",
                    "Bold red polythene smart bags for high-visibility retail and gifting. Pack of 250.",
                    250, 3.50,  2500, 3.00,  12500, 2.60 },
            { "515", "Red Tabasco 60ml", "Food & Condiments",
                    "Iconic Red Tabasco hot sauce in 60ml bottles. A table condiment staple for restaurants, hotels, and grills. Carton of 48.",
                    48, 180.00,  0, 0.0,  0, 0.0 },
            { "519", "Rosemary Leaves 1KG", "Food & Condiments",
                    "Dried rosemary leaves in 1 KG packs for commercial kitchens, bakeries, and spice retail. Intense aromatic flavour.",
                    1, 950.00,  0, 0.0,  0, 0.0 },
            { "523", "SAF Instant Yeast 500g", "Food & Condiments",
                    "Professional instant yeast in 500g packs for bakeries, pizzerias, and pastry shops. Fast-acting and reliable. Carton of 20.",
                    20, 280.00,  0, 0.0,  0, 0.0 },
            { "477", "Sawa No.1/4 Bags", "General",
                    "Smallest Sawa No.1/4 bags for single-item retail. Pack of 50, 120 packs per carton.",
                    50, 4.00,  6000, 3.50,  0, 0.0 },
            { "237", "Sawa No.5 Bags", "General",
                    "Sawa No.5 bags for larger retail items. Pack of 50, 40 packs per carton.",
                    50, 8.00,  2000, 7.20,  0, 0.0 },
            { "235", "Sawa No.3 Bags", "General",
                    "Sawa No.3 bags for medium retail packaging. Pack of 50, 56 packs per carton.",
                    50, 6.00,  2800, 5.50,  0, 0.0 },
            { "236", "Sawa No.4 Bags", "General",
                    "Sawa No.4 bags for larger retail and grocery use. Pack of 50, 48 packs per carton.",
                    50, 7.00,  2400, 6.30,  0, 0.0 },
            { "217", "Shiny Bag Large", "Bags",
                    "Glossy shiny bags for premium gifting and luxury retail. Eye-catching finish for boutiques. Pack of 25.",
                    25, 12.00,  250, 11.00,  0, 0.0 },
            { "216", "Shiny Bag Medium", "Bags",
                    "Medium glossy shiny bags for gifts, cosmetics, and fashion retail. Pack of 25.",
                    25, 9.00,  250, 8.20,  0, 0.0 },
            { "215", "Shiny Bag Small", "Bags",
                    "Small glossy shiny bags for jewellery, accessories, and small gifts. Pack of 25.",
                    25, 7.00,  250, 6.30,  0, 0.0 },
            { "447", "Skewers", "Miscellaneous",
                    "Bamboo skewers for BBQ, kebabs, and food displays. Heat-resistant and splinter-free. Pack of 100.",
                    100, 5.00,  1000, 4.50,  0, 0.0 },
            { "222", "Small China Smart Bag", "Bags",
                    "Small China-style polythene smart bags for retail and pharmacy. Pack of 250.",
                    250, 2.80,  2500, 2.50,  12500, 2.20 },
            { "524", "Spice Black Peppercorn 10KG", "Food & Condiments",
                    "Whole black peppercorns in 10 KG packs for commercial kitchens and spice distributors. Bold, aromatic flavour.",
                    1, 4500.00,  0, 0.0,  0, 0.0 },
            { "525", "Spice Black Pepper Powder 10KG", "Food & Condiments",
                    "Ground black pepper in 10 KG packs for food production and catering operations. Consistent grind and heat.",
                    1, 4800.00,  0, 0.0,  0, 0.0 },
            { "526", "Spice Pine Nuts 1KG", "Food & Condiments",
                    "Premium pine nuts sold by the kilogram for pesto, salads, and bakery use. Rich buttery flavour.",
                    1, 2800.00,  0, 0.0,  0, 0.0 },
            { "504", "Spice T/Heat Oregano Leaves 20g", "Food & Condiments",
                    "Dried oregano leaves in 20g packs for pizzas, pasta, and Mediterranean dishes. Pack of 6.",
                    6, 80.00,  0, 0.0,  0, 0.0 },
            { "292", "Stat 1200cc Aluminium Container", "Containers & Trays",
                    "1200cc aluminium containers for large meal portions and oven use. Pack of 400.",
                    400, 14.00,  0, 0.0,  0, 0.0 },
            { "293", "Stat 1900cc Aluminium Container", "Containers & Trays",
                    "Extra-large 1900cc aluminium containers for family meals and catering trays. Pack of 300.",
                    300, 18.00,  0, 0.0,  0, 0.0 },
            { "231", "Stat 30x15 Cling Film", "Wrapping & Foil",
                    "Narrow 30cm x 15m cling film for small food items and light wrapping. Pack of 24.",
                    24, 80.00,  120, 73.00,  0, 0.0 },
            { "101", "Stat 30x30 Cling Film", "Wrapping & Foil",
                    "Standard 30cm x 30m cling film for everyday kitchen food wrapping. Pack of 24.",
                    24, 100.00,  120, 92.00,  0, 0.0 },
            { "117", "Stat 30x300 Cling Film with Blade", "Wrapping & Foil",
                    "Long 30cm x 300m cling film with built-in blade dispenser for easy cutting. Pack of 12.",
                    12, 480.00,  60, 455.00,  0, 0.0 },
            { "141", "Stat 30x300 Cling Film No Blade", "Wrapping & Foil",
                    "Long 30cm x 300m cling film without blade for catering and commercial use. Pack of 12.",
                    12, 460.00,  60, 435.00,  0, 0.0 },
            { "104", "Stat 45x5 Aluminium Foil", "Wrapping & Foil",
                    "Compact 45cm x 5m aluminium foil for everyday use. Lightweight and easy to handle. Pack of 24.",
                    24, 95.00,  120, 87.00,  0, 0.0 },
            { "481", "Sumo Clear Tape 12-Pack", "Tapes",
                    "Clear office tape in 12-roll packs for everyday stationery and packaging. Pack of 12, 24 packs per carton.",
                    12, 120.00,  288, 110.00,  0, 0.0 },
            { "480", "Sumo Clear Tape 8-Pack", "Tapes",
                    "Clear tape in convenient 8-roll packs for offices and light packaging. Pack of 8, 24 packs per carton.",
                    8, 85.00,  192, 77.00,  0, 0.0 },
            { "479", "Sumo Masking Tape 3-Pack", "Tapes",
                    "Masking tape in 3-roll packs for painting and light packaging. Pack of 3, 24 packs per carton.",
                    3, 45.00,  72, 40.00,  0, 0.0 },
            { "478", "Sumo Masking Tape 8-Pack", "Tapes",
                    "Masking tape in 8-roll packs for painting, labelling, and surface protection. Pack of 8, 24 packs per carton.",
                    8, 110.00,  192, 100.00,  0, 0.0 },
            { "118", "Sunfresh Tomato Sauce Sachets", "Food & Condiments",
                    "Tomato sauce in single-serve sachets for restaurants, takeaways, and fast-food outlets. Carton of 300.",
                    300, 1.50,  1500, 1.30,  0, 0.0 },
            { "371", "Tech 120mm Flat Lid", "Cups & Lids",
                    "120mm flat lids for containers and cups. Secure snap-fit for cold and ambient food service. Pack of 40, 15 packs per carton.",
                    40, 5.00,  600, 4.50,  0, 0.0 },
            { "450", "Tech 12oz Single Wall White Cup", "Cups & Lids",
                    "Classic white 12oz single-wall Tech cups for hot beverages. Plain finish for custom branding. Pack of 50, 20 packs per carton.",
                    50, 10.00,  1000, 9.00,  5000, 8.00 },
            { "288", "Tech Food and Dessert BS3 Tray", "Containers & Trays",
                    "BS3 black presentation trays for desserts, sushi, and food displays. Pack of 1000.",
                    1000, 6.00,  0, 0.0,  0, 0.0 },
            { "112", "Tech 100 Dome Lids", "Cups & Lids",
                    "Dome lids for 100-series cups. Allows toppings and whipped cream to sit above the rim. Pack of 50, 50 packs per carton.",
                    50, 6.00,  2500, 5.50,  0, 0.0 },
            { "155", "Tech 100/500 Clear Cup", "Cups & Lids",
                    "Clear 500ml cups in the Tech 100 series for cold drinks and smoothies. Pack of 25, 50 packs per carton.",
                    25, 7.00,  1250, 6.30,  0, 0.0 },
            { "106", "Tech 1000cc Clear Punnets", "Containers & Trays",
                    "Clear 1000cc punnets for fresh produce, berries, and deli items. Pack of 20, 10 packs per carton.",
                    20, 12.00,  200, 11.00,  0, 0.0 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        if (PRODUCTS.length == 0) { log.warn("ProductSeeder7: empty"); return; }
        List<Product> batch = new ArrayList<>();
        for (Object[] row : PRODUCTS) {
            Product p = seederHelper.buildProductFull((String)row[0],(String)row[1],(String)row[2],(String)row[3],batch);
            batch.add(p);
        }
        productRepository.saveAll(batch);
        for (int i = 0; i < PRODUCTS.length; i++) {
            Object[] row = PRODUCTS[i];
            seederHelper.saveTiers(batch.get(i),(int)row[4],(double)row[5],(int)row[6],(double)row[7],(int)row[8],(double)row[9]);
        }
        log.info("ProductSeeder7: saved {} products with pricing tiers", batch.size());
    }
}