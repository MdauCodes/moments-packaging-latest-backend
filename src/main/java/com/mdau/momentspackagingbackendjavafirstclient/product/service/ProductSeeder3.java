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
@Order(12)
@RequiredArgsConstructor
public class ProductSeeder3 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final Object[][] PRODUCTS = {
            { "464", "Ecstacy Umbrella Straws", "Straws & Stirrers",
                    "Wide umbrella-top straws for cocktails and mocktails. Eye-catching design elevates bar and event presentations. Pack of 50, 24 packs per carton.",
                    50, 4.00,  1200, 3.50,  0, 0.0 },
            { "535", "Exe Wheat Flour 2KG", "Food & Condiments",
                    "Refined wheat flour in 2kg packs for bakeries, restaurants, and home use. Consistent texture for breads, pastries, and chapati. Bale of 12.",
                    12, 220.00,  0, 0.0,  0, 0.0 },
            { "541", "Fay Hand Towel", "Tableware & Hygiene",
                    "Soft disposable hand towels for restaurants, hotels, and washrooms. Highly absorbent and gentle on skin. Pack of 12 rolls.",
                    12, 180.00,  0, 0.0,  0, 0.0 },
            { "221", "Flowered Smart Bags", "Bags",
                    "Floral-print polythene smart bags for florists, gift shops, and boutiques. Vibrant flower design adds a festive touch. Pack of 250.",
                    250, 3.20,  2500, 2.90,  12500, 2.60 },
            { "224", "FNA Smart Bags", "Bags",
                    "FNA-branded polythene smart bags for general retail packaging. Durable and lightweight for high-volume outlets. Pack of 250.",
                    250, 3.00,  2500, 2.70,  12500, 2.40 },
            { "520", "Fresh Fri Cooking Oil 20LTR", "Food & Condiments",
                    "Premium 20-litre Fresh Fri cooking oil for commercial kitchens and frying operations. Long-lasting fry life and neutral flavour.",
                    1, 5800.00,  0, 0.0,  0, 0.0 },
            { "528", "Garbage Bags 100S", "Bags",
                    "Heavy-duty black garbage bags in packs of 100. Thick gauge for offices, restaurants, and households. Tear-resistant for bulky waste.",
                    100, 10.00,  500, 9.00,  2500, 8.00 },
            { "517", "Granoro Spaghetti Ristoranti 500g", "Food & Condiments",
                    "Premium Italian spaghetti from Granoro. Al dente texture for restaurants and pasta bars. Carton of 26 packs.",
                    26, 180.00,  0, 0.0,  0, 0.0 },
            { "518", "Granoro Fettuccine 500g", "Food & Condiments",
                    "Classic flat-ribbon fettuccine from Granoro. Perfect for cream and meat sauces in Italian cuisine. Carton of 24.",
                    24, 185.00,  0, 0.0,  0, 0.0 },
            { "514", "Granoro Lasagna 500g", "Food & Condiments",
                    "Authentic Italian lasagna sheets from Granoro. Ready to layer with bolognese and béchamel for restaurants. Carton of 27.",
                    27, 190.00,  0, 0.0,  0, 0.0 },
            { "516", "Granoro Penne 500g", "Food & Condiments",
                    "Ridged penne pasta from Granoro — ideal for chunky sauces, bakes, and salads. Carton of 24.",
                    24, 180.00,  0, 0.0,  0, 0.0 },
            { "475", "Greaseproof Sheets", "Wrapping & Foil",
                    "Non-stick greaseproof paper sheets for wrapping burgers, pastries, and fried foods. Prevents grease seepage. Pack of 320.",
                    320, 1.50,  1600, 1.30,  0, 0.0 },
            { "445", "Hand Stretch Wrap Film", "Wrapping & Foil",
                    "Manual stretch wrap film for securing pallets and bundles. Strong cling, high elongation for warehouses. Pack of 6 rolls.",
                    6, 850.00,  30, 800.00,  0, 0.0 },
            { "511", "Heinz Tomato Ketchup 300ml", "Food & Condiments",
                    "Iconic Heinz tomato ketchup in 300ml bottles for restaurants and fast-food outlets. Carton of 12.",
                    12, 420.00,  0, 0.0,  0, 0.0 },
            { "508", "Kaputei BBQ Sauce 400g", "Food & Condiments",
                    "Smoky and tangy BBQ sauce in 400g jars for grills, restaurants, and catering. Carton of 12.",
                    12, 380.00,  0, 0.0,  0, 0.0 },
            { "414", "Kenyan Newspaper 40KG Bale", "Miscellaneous",
                    "Bulk Kenyan newspaper bales used for wrapping fragile goods and cushioning in transit. 40 KG bale.",
                    1, 1800.00,  0, 0.0,  0, 0.0 },
            { "507", "Kenylon Baked Beans 420g", "Food & Condiments",
                    "Ready-to-eat baked beans in tomato sauce, 420g tins. A food service staple for hotels and canteens. Carton of 24.",
                    24, 120.00,  0, 0.0,  0, 0.0 },
            { "353", "Kifaru Scouring Pads", "Hygiene & PPE",
                    "Heavy-duty scouring pads for commercial kitchen cleaning. Removes stubborn grease from pots and pans. Pack of 6, 60 packs per carton.",
                    6, 35.00,  360, 32.00,  0, 0.0 },
            { "183", "Kings No.1 Khaki Bags", "General",
                    "Kings-brand No.1 khaki polythene bags for retail and market use. Pack of 50, 40 packs per carton.",
                    50, 5.00,  2000, 4.50,  0, 0.0 },
            { "182", "Kings No.1/2 Khaki Bags", "General",
                    "Kings No.1/2 khaki bags for small retail and pharmacy dispensing. Pack of 50, 50 packs per carton.",
                    50, 4.50,  2500, 4.00,  0, 0.0 },
            { "181", "Kings No.1/4 Khaki Bags", "General",
                    "Smallest Kings khaki bags for single-item retail and spice portions. Pack of 50, 50 packs per carton.",
                    50, 4.00,  2500, 3.50,  0, 0.0 },
            { "184", "Kings No.2 Khaki Bags", "General",
                    "Kings No.2 khaki bags for medium retail packaging. Pack of 50, 30 packs per carton.",
                    50, 6.00,  1500, 5.50,  0, 0.0 },
            { "219", "Laminated 3D Bags", "Bags",
                    "Glossy laminated 3D-effect bags for premium gifting and luxury retail. Pack of 250.",
                    250, 6.00,  2500, 5.50,  0, 0.0 },
            { "218", "Large 3D Smart Bag", "Bags",
                    "Large 3D effect smart bags for clothing stores, shoe shops, and large gift items. Pack of 250.",
                    250, 7.00,  2500, 6.50,  0, 0.0 },
            { "534", "Liz Pack Salt Sachets", "Food & Condiments",
                    "Fine table salt in hygienic individual sachets for restaurants and catering. Carton of 1000.",
                    1000, 0.80,  5000, 0.70,  20000, 0.60 },
            { "532", "LLC Bags 9x15cm", "Bags",
                    "Clear polythene bags 9x15cm for packaging small items and accessories. Pack of 100.",
                    100, 4.00,  1000, 3.60,  5000, 3.20 },
            { "533", "LLC Bags 10x15cm", "Bags",
                    "Clear polythene bags 10x15cm for food, cosmetics, and small retail items. Pack of 100.",
                    100, 4.50,  1000, 4.00,  5000, 3.60 },
            { "512", "Luxeapers Pitted Black Sliced Olives 345g", "Food & Condiments",
                    "Premium pitted and sliced black olives in brine for pizzas, salads, and Mediterranean dishes. Carton of 12.",
                    12, 480.00,  0, 0.0,  0, 0.0 },
            { "556", "Macadamia Nuts", "Food & Condiments",
                    "Premium Kenyan macadamia nuts sold by the kilogram. Rich buttery flavour for snacking, baking, and confectionery.",
                    1, 1800.00,  10, 1700.00,  0, 0.0 },
            { "471", "Made In China 60x100 Clear Yellow Tape", "Tapes",
                    "60mm x 100m clear yellow packing tape for carton sealing. Strong adhesive, smooth unwind. Pack of 60 rolls.",
                    60, 55.00,  0, 0.0,  0, 0.0 },
            { "462", "Made In China 60x200 Clear Yellow Tape", "Tapes",
                    "60mm x 200m long-run yellow tape for high-volume packing. Cost-effective for warehouses. Pack of 60 rolls.",
                    60, 90.00,  0, 0.0,  0, 0.0 },
            { "401", "Mafuco Brown Handled Twisted Bags Large", "Bags",
                    "Large brown kraft paper bags with twisted rope handles for heavy retail items. Pack of 50.",
                    50, 40.00,  500, 37.00,  2500, 34.00 },
            { "400", "Mafuco Brown Handled Twisted Bags Medium", "Bags",
                    "Medium brown kraft bags with twisted handles for boutiques and gift shops. Pack of 50.",
                    50, 32.00,  500, 29.00,  2500, 26.00 },
            { "399", "Mafuco Brown Handled Twisted Bags Small", "Bags",
                    "Small brown kraft bags with twisted rope handles for jewellery and small gifts. Pack of 50.",
                    50, 25.00,  500, 22.00,  2500, 20.00 },
            { "402", "Mafuco Millinary 3x5 Brown Bag", "Bags",
                    "Small 3x5 inch brown paper bags for pharmacy and spice packaging. Pack of 100, 20 packs per carton.",
                    100, 8.00,  2000, 7.20,  0, 0.0 },
            { "403", "Mafuco Millinary 4x6 Brown Bag", "Bags",
                    "4x6 inch brown kraft bags for snacks, spices, and dry goods. Pack of 100, 20 packs per carton.",
                    100, 10.00,  2000, 9.00,  0, 0.0 },
            { "404", "Mafuco Millinary 5x6 Brown Bag", "Bags",
                    "5x6 inch brown paper bags for small food portions and nuts. Pack of 100, 20 packs per carton.",
                    100, 12.00,  2000, 11.00,  0, 0.0 },
            { "444", "Mafuco Millinary 6x10 Brown Bag", "Bags",
                    "6x10 inch brown bags for sandwiches and pastries. Pack of 100, 20 packs per carton.",
                    100, 16.00,  2000, 14.50,  0, 0.0 },
            { "405", "Mafuco Millinary 6x7.5 Brown Bag", "Bags",
                    "6x7.5 inch brown bags for burgers, snacks, and food service. Pack of 100, 20 packs per carton.",
                    100, 14.00,  2000, 12.50,  0, 0.0 },
            { "406", "Mafuco Millinary 7x9 Brown Bag", "Bags",
                    "7x9 inch brown bags for medium food portions and retail goods. Pack of 100, 20 packs per carton.",
                    100, 18.00,  2000, 16.50,  0, 0.0 },
            { "407", "Mafuco Millinary 8.5x10 Brown Bag", "Bags",
                    "Large 8.5x10 inch brown bags for big portions and baked goods. Pack of 100, 20 packs per carton.",
                    100, 22.00,  2000, 20.00,  0, 0.0 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        if (PRODUCTS.length == 0) { log.warn("ProductSeeder3: empty"); return; }
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
        log.info("ProductSeeder3: saved {} products with pricing tiers", batch.size());
    }
}