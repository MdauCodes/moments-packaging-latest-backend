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
 * Product seeder part 2 of 10 – products 42 to 82.
 */
@Slf4j
@Component
@Order(11)
@RequiredArgsConstructor
public class ProductSeeder2 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final Object[][] PRODUCTS = {
            { "360", "Dune No.5 Khaki Bags 15KG", "General",
                    "Large-format No.5 khaki polythene bags sold by the 15 KG bale. A reliable bulk packaging staple for traders and wholesalers.",
                    1, 2800.00,  0, 0.0,  0, 0.0 },

            { "495", "Eagle 57*40mm Thermal Roll", "Thermal Rolls",
                    "Eagle-brand 57x40 mm thermal rolls for POS and receipt printing. BPA-free coating with long shelf life. Pack of 100.",
                    100, 17.00,  1000, 15.50,  5000, 14.00 },

            { "357", "Eco Hapa 12oz Black Ripple Cup", "Cups & Lids",
                    "All-black ripple-wall 12oz cups. Double-layer insulation protects hands while the matte finish adds premium café aesthetics. Pack of 25.",
                    25, 20.00,  500, 18.50,  2500, 17.00 },

            { "358", "Eco Hapa 12oz Black Lids", "Cups & Lids",
                    "Snap-fit black flat lids for 12oz Eco Hapa and standard hot cups. Secure seal prevents spills during commutes. Pack of 25.",
                    25, 8.00,  500, 7.20,  2500, 6.50 },

            { "259", "Eco Hapa 30*1500 Jumbo Cling Film", "Wrapping & Foil",
                    "Industrial jumbo 30cm x 1500m cling film for high-volume food wrapping. Superior cling strength for deli counters and commercial kitchens. Single roll.",
                    1, 3500.00,  5, 3300.00,  0, 0.0 },

            { "417", "Eco Hapa 30*30 Aluminium Foil", "Wrapping & Foil",
                    "30cm x 30m aluminium foil for everyday kitchen and catering use. Excellent heat retention for baking and grilling. Pack of 15.",
                    15, 280.00,  75, 260.00,  0, 0.0 },

            { "109", "Eco Hapa 30*30 Cling Film", "Wrapping & Foil",
                    "Standard 30cm x 30m cling film for everyday food wrapping. Crystal-clear, microwave-safe, easy to dispense. Pack of 36.",
                    36, 120.00,  180, 110.00,  900, 100.00 },

            { "127", "Eco Hapa 30*300 Cling Film", "Wrapping & Foil",
                    "Long 30cm x 300m commercial cling film for high-frequency catering and food prep. Fewer roll changes, consistent wrap quality. Pack of 15.",
                    15, 550.00,  75, 520.00,  0, 0.0 },

            { "418", "Eco Hapa 45*30 Aluminium Foil", "Wrapping & Foil",
                    "Wide 45cm x 30m foil for covering large trays and wrapping whole birds. Suitable for ovens, BBQs, and commercial kitchen storage. Pack of 12.",
                    12, 350.00,  60, 330.00,  0, 0.0 },

            { "157", "Eco Hapa 45*5 Aluminium Foil", "Wrapping & Foil",
                    "Compact 45cm x 5m foil rolls — convenient for home and small catering operations. Lightweight, easy to handle, and economical. Pack of 36.",
                    36, 100.00,  180, 90.00,  900, 82.00 },

            { "416", "Eco Hapa 45*60 Aluminium Foil", "Wrapping & Foil",
                    "Mid-length 45cm x 60m foil roll for busy kitchens. Balance of length and ease of use for daily baking and covering tasks. Pack of 12.",
                    12, 480.00,  60, 455.00,  0, 0.0 },

            { "111", "Eco Hapa 45*90 Aluminium Foil", "Wrapping & Foil",
                    "Professional 45cm x 90m foil for commercial food service. Consistent quality for high-volume wrapping, baking, and preservation. Pack of 12.",
                    12, 620.00,  60, 590.00,  0, 0.0 },

            { "497", "Eco Hapa 57*40mm Thermal Roll", "Thermal Rolls",
                    "Eco Hapa 57x40 mm thermal rolls for POS terminals and receipt printers. Sharp print quality and compatible with leading printer brands. Pack of 100.",
                    100, 18.00,  1000, 16.50,  5000, 15.00 },

            { "496", "Eco Hapa 8oz Black Ripple Cup", "Cups & Lids",
                    "Sleek black ripple-wall 8oz cups for espresso, tea, and hot beverages. Double-layer insulation and bold aesthetic make these a café favourite. Pack of 25.",
                    25, 16.00,  500, 14.50,  2500, 13.00 },

            { "498", "Eco Hapa 8oz Black Lids", "Cups & Lids",
                    "Flat black lids with secure fit for 8oz hot cups. Spill-resistant design compatible with standard 8oz cup sizes. Pack of 25.",
                    25, 6.50,  500, 5.80,  2500, 5.20 },

            { "430", "Eco Medical Bags 2.5*5", "Bags",
                    "Small medical-grade polythene bags for pharmacies, clinics, and laboratories. Clear, hygienic, and sealed for dispensing tablets and medical supplies. Pack of 100.",
                    100, 3.50,  1000, 3.10,  5000, 2.80 },

            { "429", "Ecolab Cherry Smart Bags", "Bags",
                    "Cherry-printed smart bags that turn every purchase into a gift. Popular with florists, juice bars, and gift shops. Pack of 250.",
                    250, 3.20,  2500, 2.90,  12500, 2.60 },

            { "433", "Ecolab Giraffe Smart Bags", "Bags",
                    "Playful giraffe-printed bags loved by toy stores, children's clothing shops, and kids' event vendors. Pack of 250.",
                    250, 3.20,  2500, 2.90,  12500, 2.60 },

            { "432", "Ecolab Humming Bird Smart Bags", "Bags",
                    "Delicate hummingbird-print bags — a favourite for florists, artisan bakeries, and wellness brands. Pack of 250.",
                    250, 3.20,  2500, 2.90,  12500, 2.60 },

            { "431", "Ecolab Matatu Smart Bags", "Bags",
                    "Bold matatu-art bags celebrating Kenyan street culture. Unique for local brands, street food vendors, and creative businesses. Pack of 250.",
                    250, 3.20,  2500, 2.90,  12500, 2.60 },

            { "435", "Ecolab Red Rose Smart Bags", "Bags",
                    "Romantic red rose printed bags ideal for florists, jewellers, and Valentine's gifting. Communicates love and elegance. Pack of 250.",
                    250, 3.20,  2500, 2.90,  12500, 2.60 },

            { "434", "Ecolab Safari Smart Bags", "Bags",
                    "African safari-themed bags with wildlife prints — perfect for souvenir shops, lodges, and tourism businesses. Pack of 250.",
                    250, 3.20,  2500, 2.90,  12500, 2.60 },

            { "383", "Ecolab No.15 Red Bags", "General",
                    "Red No.15 polythene bags for retail and distribution. Bright colour stands out at market stalls. Pack of 50, 40 packs per carton.",
                    50, 4.50,  2000, 4.00,  10000, 3.60 },

            { "382", "Ecolab No.15 Blue Bags", "General",
                    "Blue No.15 polythene bags — practical, lightweight, and widely used across retail and market environments. Pack of 50.",
                    50, 4.50,  2000, 4.00,  10000, 3.60 },

            { "381", "Ecolab No.15 Green Bags", "General",
                    "Green No.15 bags for eco-themed branding and everyday retail. Natural colour for fresh produce vendors and organic markets. Pack of 50.",
                    50, 4.50,  2000, 4.00,  10000, 3.60 },

            { "388", "Ecolab No.22 Blue Bags", "General",
                    "Blue No.22 polythene bags for medium-sized goods. Durable and suitable for supermarkets, pharmacies, and general retail. Pack of 50.",
                    50, 6.50,  1000, 5.80,  5000, 5.20 },

            { "387", "Ecolab No.22 Green Bags", "General",
                    "Green No.22 bags for fresh produce, farm products, and organic retail. Comfortably sized for medium grocery purchases. Pack of 50.",
                    50, 6.50,  1000, 5.80,  5000, 5.20 },

            { "389", "Ecolab No.22 Red Bags", "General",
                    "Bold red No.22 bags for promotional branding and high-visibility retail packaging. Pack of 50.",
                    50, 6.50,  1000, 5.80,  5000, 5.20 },

            { "385", "Ecolab No.25 Blue Bags", "General",
                    "Spacious blue No.25 bags for supermarket checkout, hardware, and bulk retail. Holds generous loads without tearing. Pack of 50.",
                    50, 8.00,  1000, 7.20,  5000, 6.50 },

            { "386", "Ecolab No.25 Green Bags", "General",
                    "Green No.25 bags for large grocery, nursery, and farm produce packaging. Practical eco-themed option. Pack of 50.",
                    50, 8.00,  500, 7.20,  2500, 6.50 },

            { "384", "Ecolab No.25 Red Bags", "General",
                    "Large red No.25 bags for retail and high-volume outlets. Vivid colour gives a branded feel to stalls and shops. Pack of 50.",
                    50, 8.00,  500, 7.20,  2500, 6.50 },

            { "390", "Ecolab No.25 Supreme Red Bags", "General",
                    "Premium-gauge Supreme red No.25 bags for heavier or sharp-edged items without risk of tearing. Pack of 50.",
                    50, 10.00,  1000, 9.00,  5000, 8.20 },

            { "426", "Ecolab Gucci Smart Bags", "Bags",
                    "Luxury-inspired designer-style smart bags for upscale boutiques, fashion retailers, and high-end gifting. Pack of 250.",
                    250, 4.00,  2500, 3.60,  12500, 3.20 },

            { "428", "Ecolab Kitenge Smart Bags", "Bags",
                    "Vibrant African kitenge-print bags celebrating bold patterns and Kenyan heritage. For cultural markets, fashion pop-ups, and artisan vendors. Pack of 250.",
                    250, 4.00,  2500, 3.60,  12500, 3.20 },

            { "425", "Ecolab Mrembo Smart Bags", "Bags",
                    "Stylish Mrembo (beautiful) bags for beauty salons, cosmetic shops, and personal care brands. Feminine and elegant. Pack of 250.",
                    250, 3.80,  2500, 3.40,  12500, 3.00 },

            { "427", "Ecolab Premier League Smart Bags", "Bags",
                    "Football-themed Premier League smart bags popular with sports shops and fan merchandise stalls. A hit with football fans of all ages. Pack of 250.",
                    250, 4.00,  2500, 3.60,  12500, 3.20 },

            { "467", "Ecstacy 2oz Kraft Square Sauce Cup + Lid", "Cups & Lids",
                    "Compact 2oz kraft paper sauce cups with matching lids for portion-controlled sauces, dips, and condiments. Natural kraft finish suits casual dining. Pack of 50.",
                    50, 12.00,  500, 11.00,  2500, 10.00 },

            { "466", "Ecstacy 4oz Kraft Square Sauce Cup + Lid", "Cups & Lids",
                    "Generous 4oz kraft cups with secure lids for hummus, guacamole, and dipping sauces at food service counters. Pack of 50.",
                    50, 16.00,  500, 14.50,  2500, 13.00 },

            { "469", "Ecstacy Bubble Tea Straws Coloured", "Straws & Stirrers",
                    "Wide-bore coloured straws for bubble tea and boba drinks. Large diameter allows tapioca pearls to pass through smoothly. Pack of 100.",
                    100, 5.00,  1000, 4.50,  5000, 4.00 },

            { "468", "Ecstacy Corrugated Wrap Box 26cm*10cm Shawarma", "Containers & Trays",
                    "Corrugated kraft wrapping boxes designed for shawarma, wraps, and rolls. Keeps contents warm, absorbs grease, clean eating on the go. Pack of 50.",
                    50, 22.00,  500, 20.00,  2500, 18.00 },

            { "465", "Ecstacy Flexible Straws Coloured", "Straws & Stirrers",
                    "Flexible bendable coloured straws with accordion neck for milkshakes, juices, and children's drinks. Fun for kids' events and hospitals. Pack of 100.",
                    100, 3.50,  2400, 3.00,  12000, 2.70 },
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

        log.info("ProductSeeder2: saved {} products with pricing tiers", batch.size());
    }
}