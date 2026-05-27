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
 * Product seeder part 1 of 10 – products 1 to 41.
 * Includes descriptions and KES pricing tiers (Packet / Carton / Bale).
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class ProductSeeder1 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    /**
     * { code, name, category, description,
     *   pktQty, pktUnitPrice,
     *   ctnQty, ctnUnitPrice,   <- 0/0 = no carton tier
     *   baleQty, baleUnitPrice  <- 0/0 = no bale tier }
     */
    private static final Object[][] PRODUCTS = {
            { "529", "30*36 Bags 50S", "Bags",
                    "Heavy-duty 30x36 cm polythene bags in packs of 50. Ideal for general retail, grocery, and market use.",
                    50, 6.00,  1000, 5.50,  5000, 5.00 },

            { "527", "Allegro Extra Virgin Olive Oil 1Ltr", "Food & Condiments",
                    "Premium cold-pressed extra virgin olive oil in 1-litre bottles. Rich flavour suited for restaurants, caterers, and delis. Carton of 12.",
                    1, 850.00,  12, 800.00,  0, 0.0 },

            { "151", "Avery 57*40mm Thermal Roll", "Thermal Rolls",
                    "High-sensitivity 57x40 mm thermal rolls for POS and receipt printers. BPA-free, jam-resistant, clean white finish. Pack of 100.",
                    100, 18.00,  1000, 16.00,  5000, 14.50 },

            { "128", "Avery 79*80mm Thermal Roll", "Thermal Rolls",
                    "Wide-format 79x80 mm thermal rolls for kitchen printers and billing counters. Crisp output with reliable unwind. Pack of 50.",
                    50, 35.00,  500, 32.00,  2500, 29.00 },

            { "513", "Basmati Rice 25KG", "Food & Condiments",
                    "Long-grain aromatic basmati rice in 25 kg bags. Preferred by hotels, restaurants, and food service operations for its fragrance and texture.",
                    1, 3200.00,  4, 3100.00,  0, 0.0 },

            { "126", "Besa Salt Sachets", "Food & Condiments",
                    "Fine table salt in individual single-serve sachets. Ideal for restaurants, takeaways, cafes, and event catering. Pack of 1000.",
                    1000, 0.80,  5000, 0.70,  20000, 0.60 },

            { "116", "Besa Sugar Sachets White", "Food & Condiments",
                    "Refined white sugar in hygienic single-serve sachets for coffee stations and hospitality. Pack of 1000.",
                    1000, 0.90,  5000, 0.80,  20000, 0.70 },

            { "297", "Bio Blend Straws", "Straws & Stirrers",
                    "Eco-friendly bio-blend drinking straws made from plant-based materials. Biodegradable and compliant with green packaging standards. Pack of 250.",
                    250, 2.50,  2500, 2.20,  12500, 1.90 },

            { "220", "Black China Smart Bags", "Bags",
                    "Opaque black polythene smart bags with a smooth finish. Great for boutiques, salons, and retail shops seeking sleek, discreet packaging. Pack of 250.",
                    250, 3.50,  2500, 3.00,  12500, 2.60 },

            { "225", "Blue Smart Bag", "Bags",
                    "Vibrant blue polythene smart bags suitable for retail and gifting. Bright colour adds personality to every purchase. Pack of 250.",
                    250, 3.50,  2500, 3.00,  12500, 2.60 },

            { "227", "Chilli Sauce Sachets", "Food & Condiments",
                    "Spicy chilli sauce in single-serve sachets for fast food outlets, takeaways, and food courts. Consistent heat in every portion. Carton of 300.",
                    300, 2.50,  1500, 2.20,  0, 0.0 },

            { "214", "China Smart Bag Medium", "Bags",
                    "Medium-sized polythene smart bags with reinforced handles. Suitable for supermarkets, pharmacies, and general retail. Pack of 25.",
                    25, 8.00,  250, 7.00,  1250, 6.00 },

            { "240", "Click No.1/4 Bags", "General",
                    "Small quarter-size click polythene bags for retail, hardware, and packaging operations. Lightweight and versatile. Pack of 50.",
                    50, 5.00,  500, 4.50,  7500, 4.00 },

            { "505", "Clovers Cocoa Powder 400g", "Food & Condiments",
                    "Rich dark cocoa powder in 400g packs for baking, hot beverages, and desserts. Consistent quality for commercial kitchens and retail. Pack of 12.",
                    1, 380.00,  12, 360.00,  0, 0.0 },

            { "458", "DFAC 12oz Single Wall White Plain Cup", "Cups & Lids",
                    "White single-wall 12oz paper cups for hot beverages. Plain finish ideal for branded sleeves or custom printing. Pack of 50.",
                    50, 10.00,  1000, 9.00,  5000, 8.00 },

            { "461", "DFAC 45*300M Cling Film", "Wrapping & Foil",
                    "Professional-grade 45cm x 300m cling film for commercial kitchens. Excellent cling, moisture barrier, and freshness retention. Pack of 5.",
                    5, 750.00,  25, 720.00,  0, 0.0 },

            { "460", "DFAC 45*5 Cling Film", "Wrapping & Foil",
                    "Compact 45cm x 5m household cling film for everyday food wrapping. Clear, strong, and easy to dispense. Pack of 5.",
                    5, 180.00,  25, 165.00,  0, 0.0 },

            { "459", "DFAC 45*90 Aluminium Foil", "Wrapping & Foil",
                    "Heavy-duty 45cm x 90m aluminium foil for professional food service. Superior heat retention for baking and food preservation. Pack of 6.",
                    6, 600.00,  30, 570.00,  0, 0.0 },

            { "456", "DFAC 8oz Black Printed Ripple Cup", "Cups & Lids",
                    "Stylish black ripple-wall 8oz cups. Double-layer insulation keeps hands comfortable. Matte black finish elevates any café counter. Pack of 25.",
                    25, 18.00,  500, 16.50,  2500, 15.00 },

            { "248", "DFAC 850ml and 1000ml Lids", "Cups & Lids",
                    "Universal flat lids compatible with 850ml and 1000ml containers. Secure snap-fit and leak-resistant for hot or cold food service. Pack of 500.",
                    500, 5.00,  2500, 4.50,  0, 0.0 },

            { "538", "DFAC 8oz Single Wall Plain White Cup", "Cups & Lids",
                    "Classic white 8oz single-wall cups for hot beverages. Plain surface for custom printing. Cost-effective for high-volume operations. Pack of 50.",
                    50, 8.00,  1000, 7.20,  5000, 6.50 },

            { "455", "DFAC Biodegradable 2-Compartment Box 850ml", "Containers & Trays",
                    "Eco-friendly 850ml two-compartment food box. Keeps different foods separate — perfect for combo meals and takeaway portions. Pack of 500.",
                    500, 22.00,  2500, 20.00,  0, 0.0 },

            { "457", "DFAC Biodegradable 7-Inch Plate", "Tableware & Hygiene",
                    "7-inch round biodegradable plates for events and food service. Sturdy, grease-resistant, and environmentally responsible. Pack of 500.",
                    500, 12.00,  2500, 11.00,  0, 0.0 },

            { "247", "DFAC Biodegradable Box 1000ml", "Containers & Trays",
                    "Spacious 1000ml single-compartment biodegradable food box. Leak-resistant, ideal for rice, stew, or mixed meals in takeaway operations. Pack of 500.",
                    500, 24.00,  2500, 22.00,  0, 0.0 },

            { "249", "DFAC Clamshell Biodegradable Box 600ml", "Containers & Trays",
                    "Hinged-lid clamshell biodegradable container for 600ml servings. Self-closing design for burgers, snacks, and light meals. Pack of 250.",
                    250, 28.00,  1250, 26.00,  0, 0.0 },

            { "539", "DFAC Hanan Wet Wipes", "Tableware & Hygiene",
                    "Individually packaged moist hand wipes for restaurants and food service. Gentle, lightly fragranced, hygienic pre/post-meal use. Pack of 100.",
                    100, 4.50,  1000, 4.00,  5000, 3.50 },

            { "540", "DFAC Ripple 12oz Printed Paper Cup", "Cups & Lids",
                    "Branded ripple-wall 12oz cups with pre-printed design. Double-wall insulation without a sleeve. Great for specialty coffee and premium takeaways. Pack of 25.",
                    25, 22.00,  500, 20.00,  2500, 18.00 },

            { "251", "DFAC Wooden Forks", "Cutlery",
                    "Smooth birchwood disposable forks — eco-friendly alternative to plastic. Sturdy for salads and pasta at events and takeaways. Pack of 100.",
                    100, 3.00,  10000, 2.70,  50000, 2.40 },

            { "252", "DFAC Wooden Knife", "Cutlery",
                    "Disposable birchwood knives for spreading and light cutting. Biodegradable and ideal for catered events and food trucks. Pack of 100.",
                    100, 3.00,  10000, 2.70,  50000, 2.40 },

            { "250", "DFAC Wooden Spoons", "Cutlery",
                    "Smooth birchwood disposable spoons for soups and desserts. Natural look enhances presentation. Compostable sustainable cutlery. Pack of 100.",
                    100, 2.80,  10000, 2.50,  50000, 2.20 },

            { "531", "DFAC Cornstarch Cutlery Set", "Cutlery",
                    "Complete eco-friendly cutlery sets (fork, knife, spoon) made from plant-based cornstarch. Fully compostable, heat-resistant to 60°C. Pack of 500.",
                    500, 18.00,  2500, 16.50,  0, 0.0 },

            { "347", "Disposable Garbage Bags 50pcs", "Bags",
                    "Strong black garbage bags for offices, restaurants, and households. Thick gauge resists punctures and tears. Pack of 50.",
                    50, 12.00,  500, 11.00,  2500, 10.00 },

            { "362", "Dune No.1 Brown Handled Bags", "Bags",
                    "Kraft brown paper bags with twisted rope handles — premium retail packaging. Durable and stylish for boutiques, gift shops, and eco-conscious brands. Pack of 100.",
                    100, 25.00,  1000, 23.00,  5000, 21.00 },

            { "359", "Dune No.10 Khaki Bags 15KG", "General",
                    "No.10 khaki polythene bags sold by the 15 KG bale. Widely used in markets, hardware stores, and bulk packaging operations.",
                    1, 2200.00,  0, 0.0,  0, 0.0 },

            { "351", "Dune No.14 Khaki Bags 15KG", "General",
                    "Medium No.14 khaki bags in 15 KG bales. Reliable for agricultural produce, hardware supplies, and general goods distribution.",
                    1, 2400.00,  0, 0.0,  0, 0.0 },

            { "352", "Dune No.16 Khaki Bags 15KG", "General",
                    "Larger No.16 khaki bags in 15 KG bales for bulkier items. Common in grain markets, supermarkets, and wholesale distribution.",
                    1, 2600.00,  0, 0.0,  0, 0.0 },

            { "363", "Dune No.2 Brown Handled Bags", "Bags",
                    "Medium-large kraft paper bags with sturdy rope handles. Natural brown finish communicates quality and sustainability for retail and food gifting. Pack of 100.",
                    100, 30.00,  1000, 27.50,  5000, 25.00 },

            { "350", "Dune No.2 Khaki Bags 15KG", "General",
                    "Standard No.2 khaki bags in 15 KG bales. Versatile and economical bulk packaging for food to hardware industries.",
                    1, 1800.00,  0, 0.0,  0, 0.0 },

            { "364", "Dune No.3 Brown Handled Bags", "Bags",
                    "Large kraft paper bags with reinforced twisted handles. Strong enough for heavier items — ideal for shopping, gifting, and premium retail. Pack of 100.",
                    100, 35.00,  1000, 32.00,  5000, 29.00 },

            { "361", "Dune No.4 Brown Handled Bags", "Bags",
                    "Extra-large brown kraft paper bags with rope handles. Perfect choice for fashion retailers, shoe shops, and high-end gifting. Pack of 100.",
                    100, 40.00,  1000, 37.00,  5000, 34.00 },

            { "365", "Dune No.5 Brown Handled Bags", "Bags",
                    "Jumbo-size brown kraft handled bags for bulk retail and large product packaging. Premium finish with a natural aesthetic. Pack of 100.",
                    100, 45.00,  1000, 41.50,  5000, 38.00 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;

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

        log.info("ProductSeeder1: saved {} products with pricing tiers", batch.size());
    }
}