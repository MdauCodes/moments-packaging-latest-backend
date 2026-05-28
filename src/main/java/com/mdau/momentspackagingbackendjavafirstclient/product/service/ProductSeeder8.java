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
@Order(17)
@RequiredArgsConstructor
public class ProductSeeder8 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final Object[][] PRODUCTS = {
            { "378", "Tech 1000ml Clear Honey Jar", "Jars & Bottles",
                    "Clear 1000ml PET honey jars with wide mouth for easy filling. Ideal for honey producers and food retailers. Pack of 50.",
                    50, 35.00,  500, 32.00,  0, 0.0 },
            { "544", "Tech 120/1000ml Container", "Containers & Trays",
                    "Clear 1000ml containers in the 120mm series for deli, salads, and meal prep. Pack of 40, 15 packs per carton.",
                    40, 14.00,  600, 12.50,  0, 0.0 },
            { "369", "Tech 120/250ml Clear Container", "Containers & Trays",
                    "Clear 250ml containers for condiments, sauces, and small portions. Pack of 40, 15 packs per carton.",
                    40, 7.00,  600, 6.30,  0, 0.0 },
            { "370", "Tech 120/500ml Clear Container", "Containers & Trays",
                    "Clear 500ml containers for deli items, salads, and desserts. Pack of 40, 15 packs per carton.",
                    40, 10.00,  600, 9.00,  0, 0.0 },
            { "206", "Tech 12oz Black Lids", "Cups & Lids",
                    "Black flat lids for 12oz paper cups. Secure fit and clean look. Pack of 50, 20 packs per carton.",
                    50, 7.00,  1000, 6.30,  0, 0.0 },
            { "372", "Tech 12oz Single Wall Printed Cup", "Cups & Lids",
                    "Pre-printed 12oz single-wall cups for branded coffee service. Pack of 50, 20 packs per carton.",
                    50, 12.00,  1000, 11.00,  5000, 10.00 },
            { "451", "Tech 12oz White Lids", "Cups & Lids",
                    "White flat lids for 12oz cups. Clean and professional. Pack of 50, 20 packs per carton.",
                    50, 6.50,  1000, 5.80,  0, 0.0 },
            { "376", "Tech 250ml Clear Honey Jar", "Jars & Bottles",
                    "Clear 250ml PET honey jar for small-batch honey and condiments. Pack of 100.",
                    100, 18.00,  500, 16.50,  0, 0.0 },
            { "546", "Tech 3oz Cups White", "Cups & Lids",
                    "Small white 3oz portion cups for sauces, condiments, and samples. Pack of 50, 50 packs per carton.",
                    50, 4.50,  2500, 4.00,  0, 0.0 },
            { "377", "Tech 400ml Clear Honey Jar", "Jars & Bottles",
                    "Clear 400ml PET honey jars for medium-batch honey retail and gifting. Pack of 100.",
                    100, 22.00,  500, 20.00,  0, 0.0 },
            { "283", "Tech 5x10 Fish Punnet Black", "Containers & Trays",
                    "Black 5x10 inch fish punnets for fresh fish display and market retail. Pack of 200.",
                    200, 10.00,  0, 0.0,  0, 0.0 },
            { "290", "Tech Single Wall Burger Box Small", "Cups & Lids",
                    "5x5x3 inch single-wall burger box for sliders and small burgers. Pack of 200.",
                    200, 20.00,  0, 0.0,  0, 0.0 },
            { "366", "Tech 5x7 500ml Black Punnet No Lid", "Cups & Lids",
                    "Black 500ml 5x7 punnets without lids for fresh produce display. Pack of 250.",
                    250, 8.00,  0, 0.0,  0, 0.0 },
            { "144", "Tech 5x7 500ml Punnets", "Containers & Trays",
                    "Clear 500ml 5x7 punnets with lids for fresh produce and deli items. Pack of 10, 35 packs per carton.",
                    10, 8.00,  350, 7.20,  0, 0.0 },
            { "349", "Tech 5x7 Punnet Clear Lids", "Cups & Lids",
                    "Clear lids for 5x7 punnets. Secure snap-fit for produce and deli display. Pack of 250.",
                    250, 5.00,  0, 0.0,  0, 0.0 },
            { "368", "Tech 5x7 1000ml Clear Punnets No Lid", "Cups & Lids",
                    "Clear 1000ml 5x7 punnets without lids for bulk produce display. Pack of 250.",
                    250, 10.00,  0, 0.0,  0, 0.0 },
            { "129", "Tech 5x7 1000ml Punnets", "Containers & Trays",
                    "Clear 1000ml 5x7 punnets with lids. Ideal for salads and deli. Pack of 10, 35 packs per carton.",
                    10, 10.00,  350, 9.00,  0, 0.0 },
            { "367", "Tech 5x7 500ml Clear Punnets No Lid", "Cups & Lids",
                    "Clear 500ml 5x7 punnets without lids for self-service produce display. Pack of 250.",
                    250, 7.00,  0, 0.0,  0, 0.0 },
            { "348", "Tech 5x7 1000ml Black Punnets No Lid", "Cups & Lids",
                    "Black 1000ml 5x7 punnets without lids for premium produce display. Pack of 250.",
                    250, 11.00,  0, 0.0,  0, 0.0 },
            { "135", "Tech 500cc Clear Punnets", "Containers & Trays",
                    "Clear 500cc punnets for fresh produce and bakery display. Pack of 20, 12 packs per carton.",
                    20, 8.00,  240, 7.20,  0, 0.0 },
            { "257", "Tech 52/30mm Clear Container", "Containers & Trays",
                    "Small 52mm diameter clear containers for sauces and condiment portions. Pack of 250, 20 packs per carton.",
                    250, 4.00,  5000, 3.60,  0, 0.0 },
            { "258", "Tech 52mm Lid", "Cups & Lids",
                    "52mm flat lids for small portion containers. Secure seal for condiments. Pack of 250, 20 packs per carton.",
                    250, 3.00,  5000, 2.70,  0, 0.0 },
            { "375", "Tech 5oz Clear Cups", "Cups & Lids",
                    "Clear 5oz cups for cold drinks, juices, and samples. Pack of 50, 35 packs per carton.",
                    50, 5.00,  1750, 4.50,  0, 0.0 },
            { "380", "Tech 63mm Honey Jar Caps Yellow", "Jars & Bottles",
                    "Yellow 63mm caps for honey jars. Airtight seal for freshness and retail presentation. Pack of 1000.",
                    1000, 4.00,  0, 0.0,  0, 0.0 },
            { "145", "Tech 72/100 Clear Container", "Containers & Trays",
                    "Clear 100ml containers in the 72mm series for sauces and condiments. Pack of 50, 50 packs per carton.",
                    50, 5.00,  2500, 4.50,  0, 0.0 },
            { "124", "Tech 72/200ml Clear Cup", "Cups & Lids",
                    "Clear 200ml cups in the 72mm series for cold drinks and yoghurt. Pack of 25, 50 packs per carton.",
                    25, 6.00,  1250, 5.40,  0, 0.0 },
            { "156", "Tech 750cc Clear Punnets", "Containers & Trays",
                    "Clear 750cc punnets for medium produce and bakery display. Pack of 20, 12 packs per carton.",
                    20, 10.00,  240, 9.00,  0, 0.0 },
            { "542", "Tech 82/120mm Container", "Containers & Trays",
                    "Clear 82mm diameter 120ml containers for small portions and samples. Pack of 50, 50 packs per carton.",
                    50, 6.00,  2500, 5.40,  0, 0.0 },
            { "543", "Tech 82/150mm Container", "Containers & Trays",
                    "Clear 82mm diameter 150ml containers for condiments and dips. Pack of 50, 50 packs per carton.",
                    50, 7.00,  2500, 6.30,  0, 0.0 },
            { "152", "Tech 82/250 Clear Cup", "Cups & Lids",
                    "Clear 250ml cups in the 82mm series for cold drinks. Pack of 25, 50 packs per carton.",
                    25, 7.00,  1250, 6.30,  0, 0.0 },
            { "105", "Tech 82/300 Clear Cup", "Cups & Lids",
                    "Clear 300ml cups in the 82mm series for juices and smoothies. Pack of 25, 50 packs per carton.",
                    25, 8.00,  1250, 7.20,  0, 0.0 },
            { "103", "Tech 82 Dome Lids", "Cups & Lids",
                    "Dome lids for 82mm cups. Accommodates toppings and whipped cream. Pack of 50, 50 packs per carton.",
                    50, 6.00,  2500, 5.40,  0, 0.0 },
            { "545", "Tech 82mm PET Flat Lid", "Cups & Lids",
                    "Flat PET lids for 82mm cups. Clean profile and spill-resistant. Pack of 50, 50 packs per carton.",
                    50, 5.00,  2500, 4.50,  0, 0.0 },
            { "205", "Tech 8oz Lids", "Cups & Lids",
                    "Flat lids for 8oz paper cups. Secure snap-fit. Pack of 50, 20 packs per carton.",
                    50, 6.00,  1000, 5.40,  0, 0.0 },
            { "204", "Tech 8oz Single Wall Printed Cup", "Cups & Lids",
                    "Pre-printed 8oz single-wall cups for branded hot drinks. Pack of 50, 20 packs per carton.",
                    50, 10.00,  1000, 9.00,  5000, 8.00 },
            { "547", "Tech 9-Inch 3-Part Plate White", "Tableware & Hygiene",
                    "9-inch white 3-compartment disposable plates for buffet and event catering. Pack of 25, 20 packs per carton.",
                    25, 14.00,  500, 12.50,  0, 0.0 },
            { "291", "Tech Double Wall Burger Box", "Cups & Lids",
                    "9x5x3 inch double-wall burger box for premium burgers. Extra insulation keeps food hotter longer. Pack of 200.",
                    200, 28.00,  0, 0.0,  0, 0.0 },
            { "108", "Tech 94/200 Container", "Containers & Trays",
                    "Clear 200ml containers in the 94mm series for salads and deli. Pack of 50, 25 packs per carton.",
                    50, 8.00,  1250, 7.20,  0, 0.0 },
            { "449", "Tech 94/400 Clear Cup", "Cups & Lids",
                    "Clear 400ml cups in the 94mm series for cold drinks and desserts. Pack of 50, 25 packs per carton.",
                    50, 10.00,  1250, 9.00,  0, 0.0 },
            { "452", "Tech 94mm Dome Lid", "Cups & Lids",
                    "Dome lids for 94mm cups for whipped cream and toppings. Pack of 50, 50 packs per carton.",
                    50, 7.00,  2500, 6.30,  0, 0.0 },
            { "203", "Tech 94mm Flat Unpierced Lids", "Cups & Lids",
                    "Flat unpierced lids for 94mm cups. Secure seal for cold drinks. Pack of 100, 25 packs per carton.",
                    100, 8.00,  2500, 7.20,  0, 0.0 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        if (PRODUCTS.length == 0) { log.warn("ProductSeeder8: empty"); return; }
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
        log.info("ProductSeeder8: saved {} products with pricing tiers", batch.size());
    }
}