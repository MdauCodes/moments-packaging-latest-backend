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
@Order(14)
@RequiredArgsConstructor
public class ProductSeeder5 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final Object[][] PRODUCTS = {
            { "211", "Moonray 12oz Single Wall White Cup", "Cups & Lids",
                    "Classic white 12oz single-wall cups for hot beverages. Plain finish for custom printing or sleeve branding. Pack of 50, 20 packs per carton.",
                    50, 10.00,  1000, 9.00,  5000, 8.00 },
            { "209", "Moonray 12oz Sleeves", "Cups & Lids",
                    "Corrugated paper sleeves for 12oz cups. Insulates hands from heat and enhances premium drink presentation. Pack of 25, 40 packs per carton.",
                    25, 6.00,  1000, 5.50,  0, 0.0 },
            { "210", "Moonray 8oz Sleeves", "Cups & Lids",
                    "Corrugated sleeves for 8oz cups. Keeps hands comfortable with a professional finish. Pack of 25, 40 packs per carton.",
                    25, 5.50,  1000, 5.00,  0, 0.0 },
            { "119", "Moonray Nitrile Black Gloves", "Hygiene & PPE",
                    "Black nitrile gloves — powder-free and chemical resistant. Preferred for tattooing, auto work, and food service. Pack of 100, 10 packs per carton.",
                    100, 18.00,  1000, 16.50,  0, 0.0 },
            { "134", "Moonray Nitrile Blue Gloves", "Hygiene & PPE",
                    "Blue nitrile gloves for medical, food handling, and lab use. Excellent sensitivity and puncture resistance. Pack of 100, 10 packs per carton.",
                    100, 16.00,  1000, 14.50,  0, 0.0 },
            { "202", "Mr. Toothpick Jar", "Cutlery",
                    "Toothpick jars with individually wrapped toothpicks for restaurant tables and catering events. Pack of 10 jars.",
                    10, 120.00,  0, 0.0,  0, 0.0 },
            { "167", "Mrembo No.20 Red Net Bags", "General",
                    "Red No.20 net bags for fresh produce, onions, and agricultural packaging. Breathable mesh for air circulation. Pack of 50, 100 packs per carton.",
                    50, 7.00,  5000, 6.20,  0, 0.0 },
            { "453", "Net Face Mask Black 3PLY", "Hygiene & PPE",
                    "Black 3-ply disposable face masks with comfortable ear loops. For food service, healthcare, and public spaces. Pack of 50, 80 packs per carton.",
                    50, 3.50,  4000, 3.00,  0, 0.0 },
            { "454", "Net Face Mask Blue 3PLY", "Hygiene & PPE",
                    "Blue 3-ply disposable face masks for healthcare, catering, and general hygiene. Pack of 50, 80 packs per carton.",
                    50, 3.00,  4000, 2.60,  0, 0.0 },
            { "448", "Net Wire Mesh", "Miscellaneous",
                    "Wire mesh netting for produce displays, shelving, and general store use. Pack of 5.",
                    5, 350.00,  0, 0.0,  0, 0.0 },
            { "473", "Newdawn Disposable Forks", "Cutlery",
                    "White disposable plastic forks for takeaway and events. Sturdy enough for solid foods. Pack of 100, 10 packs per carton.",
                    100, 3.00,  1000, 2.70,  5000, 2.40 },
            { "472", "Newdawn Table Spoons", "Cutlery",
                    "White disposable table spoons for soups and desserts. Strong for single-use food service. Pack of 100, 10 packs per carton.",
                    100, 3.00,  1000, 2.70,  5000, 2.40 },
            { "392", "Nezo 30x30 Cling Film", "Wrapping & Foil",
                    "Nezo 30cm x 30m cling film for everyday food wrapping. Crystal clear and microwave-safe. Pack of 24.",
                    24, 100.00,  120, 92.00,  0, 0.0 },
            { "391", "Nezo 30x300 Cling Film", "General",
                    "Long 30cm x 300m commercial cling film for high-frequency catering. Fewer roll changes. Pack of 12.",
                    12, 480.00,  60, 455.00,  0, 0.0 },
            { "393", "Nezo 45x5 Aluminium Foil", "Wrapping & Foil",
                    "Compact 45cm x 5m foil rolls for home and small catering. Lightweight and economical. Pack of 36.",
                    36, 95.00,  180, 87.00,  0, 0.0 },
            { "137", "Nice 5x8 Non-Woven Bags", "Bags",
                    "Small 5x8 inch non-woven reusable bags for pharmacies and cosmetics. Eco-friendly. Pack of 50, 100 packs per carton.",
                    50, 8.00,  5000, 7.20,  0, 0.0 },
            { "136", "Nice 6x9 Non-Woven Bags", "Bags",
                    "6x9 inch non-woven bags for boutiques and gift shops. Reusable and eco-conscious. Pack of 50, 100 packs per carton.",
                    50, 10.00,  5000, 9.00,  0, 0.0 },
            { "102", "Nice 7x12 Non-Woven Bags", "Bags",
                    "Medium 7x12 inch non-woven bags for clothing and books. Durable handles and neat stitching. Pack of 50, 100 packs per carton.",
                    50, 13.00,  5000, 11.50,  0, 0.0 },
            { "138", "Nice 9x15 Non-Woven Bags", "Bags",
                    "Large 9x15 inch non-woven bags for shoes and event gifting. Spacious with reinforced handles. Pack of 50, 60 packs per carton.",
                    50, 18.00,  3000, 16.00,  0, 0.0 },
            { "409", "Nigerian Bags F1", "Bags",
                    "F1-size Nigerian-style polythene bags with bold prints for retail and market use. Pack of 120.",
                    120, 5.00,  0, 0.0,  0, 0.0 },
            { "408", "Nigerian Bags F2", "Bags",
                    "F2-size colourful Nigerian-print bags for retail stalls and market vendors. Pack of 120.",
                    120, 5.50,  0, 0.0,  0, 0.0 },
            { "208", "Nigerian Bags F3", "Bags",
                    "F3-size Nigerian-style bags for clothing, fabrics, and gifts. Pack of 120.",
                    120, 6.00,  0, 0.0,  0, 0.0 },
            { "410", "Nigerian Bags F4", "Bags",
                    "F4-size Nigerian bags for medium to large retail items. Pack of 120.",
                    120, 6.50,  0, 0.0,  0, 0.0 },
            { "411", "Nigerian Bags F5", "Bags",
                    "Largest F5-size Nigerian-style bags for bulky items and fabric rolls. Pack of 120.",
                    120, 7.00,  0, 0.0,  0, 0.0 },
            { "555", "Nutrameal Sugar 5KG", "Food & Condiments",
                    "Refined white sugar in 5 KG bags for restaurants and bakeries. Consistent purity for high-volume use.",
                    1, 650.00,  0, 0.0,  0, 0.0 },
            { "554", "Nutella Ferrero Hazelnut Spread 350g", "Food & Condiments",
                    "Iconic Nutella spread in 350g jars for breakfast, baking, and dessert toppings in cafes and hotels.",
                    1, 650.00,  12, 620.00,  0, 0.0 },
            { "522", "Oak Natural Honey 1KG", "Food & Condiments",
                    "Pure Kenyan natural honey in 1 KG jars. Rich floral flavour for retail, bakeries, and health-conscious consumers.",
                    1, 950.00,  10, 900.00,  0, 0.0 },
            { "256", "Pan 12x20 Kraft Ziplock Bags", "Bags",
                    "Large 12x20 inch kraft ziplock bags for bulk dry goods and spices. Resealable and grease-resistant. Pack of 500.",
                    500, 8.00,  0, 0.0,  0, 0.0 },
            { "255", "Pan 10x15 Kraft Ziplock Bags", "Bags",
                    "10x15 inch kraft ziplock bags for snacks and specialty food retail. Resealable seal keeps contents fresh. Pack of 500.",
                    500, 6.50,  0, 0.0,  0, 0.0 },
            { "199", "Pan 1000ml Kraft Rectangle Container", "General",
                    "1000ml rectangular kraft containers for salads and takeaway meals. Pack of 50, 6 packs per carton.",
                    50, 28.00,  300, 26.00,  0, 0.0 },
            { "196", "Pan 1000ml Kraft Round Salad Bowl", "General",
                    "Round 1000ml kraft salad bowls for generous portions. Pack of 50, 6 packs per carton.",
                    50, 30.00,  300, 28.00,  0, 0.0 },
            { "422", "Pan 15x22 Kraft Ziplock Bags", "Bags",
                    "15x22 inch kraft ziplock bags for clothing and large food items. Pack of 500.",
                    500, 10.00,  0, 0.0,  0, 0.0 },
            { "419", "Pan 18x26 Kraft Ziplock Bags", "Bags",
                    "Large 18x26 inch kraft ziplock bags for bulky items and wholesale food. Pack of 500.",
                    500, 13.00,  0, 0.0,  0, 0.0 },
            { "420", "Pan 20x30 Kraft Ziplock Bags", "Bags",
                    "Extra-large 20x30 inch kraft ziplock bags for big retail and bulk food packaging. Pack of 500.",
                    500, 16.00,  0, 0.0,  0, 0.0 },
            { "253", "Pan 20cc PET Container", "Containers & Trays",
                    "Small 20cc clear PET containers for condiments and single-serve portions. Pack of 100, 20 packs per carton.",
                    100, 5.00,  2000, 4.50,  0, 0.0 },
            { "421", "Pan 23x33 Kraft Ziplock Bags", "Bags",
                    "Jumbo 23x33 inch kraft ziplock bags for large catering packs and bulk distribution. Pack of 500.",
                    500, 20.00,  0, 0.0,  0, 0.0 },
            { "200", "Pan 500ml Kraft Rectangle Container", "General",
                    "500ml rectangular kraft containers for takeaway sides and half portions. Pack of 50, 6 packs per carton.",
                    50, 22.00,  300, 20.00,  0, 0.0 },
            { "198", "Pan 500ml Kraft Round Salad Bowl", "General",
                    "Round 500ml kraft salad bowls for individual servings and fruit cups. Pack of 50, 6 packs per carton.",
                    50, 24.00,  300, 22.00,  0, 0.0 },
            { "201", "Pan 750ml Kraft Rectangle Container", "General",
                    "750ml rectangular kraft containers for three-quarter portion meals. Pack of 50, 6 packs per carton.",
                    50, 25.00,  300, 23.00,  0, 0.0 },
            { "197", "Pan 750ml Kraft Round Salad Bowl", "General",
                    "Round 750ml kraft salad bowls for medium portions. Pack of 50, 6 packs per carton.",
                    50, 27.00,  300, 25.00,  0, 0.0 },
            { "190", "Pan 890cc Aluminium Container", "Containers & Trays",
                    "890cc aluminium containers for oven, grill, and takeaway use. Heat-safe and leak-resistant. Pack of 1000.",
                    1000, 12.00,  0, 0.0,  0, 0.0 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        if (PRODUCTS.length == 0) { log.warn("ProductSeeder5: empty"); return; }
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
        log.info("ProductSeeder5: saved {} products with pricing tiers", batch.size());
    }
}