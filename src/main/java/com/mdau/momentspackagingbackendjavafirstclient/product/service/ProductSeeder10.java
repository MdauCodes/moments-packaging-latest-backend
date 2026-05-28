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
@Order(19)
@RequiredArgsConstructor
public class ProductSeeder10 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final Object[][] PRODUCTS = {
            { "438", "Thermo 6x8 Tray", "Containers & Trays",
                    "Foam 6x8 trays for meat, fish, and produce display in supermarkets and butcheries. Pack of 25, 60 packs per carton.",
                    25, 6.00,  1500, 5.40,  0, 0.0 },
            { "442", "Thermo Disposable 11-Inch White Plain Plate", "Tableware & Hygiene",
                    "11-inch white plain disposable plates for events and buffets. Pack of 25, 20 packs per carton.",
                    25, 14.00,  500, 12.50,  0, 0.0 },
            { "114", "Thermo Disposable 8-Inch White 3-Part Plate", "Tableware & Hygiene",
                    "8-inch white 3-compartment plates for food service and catering. Pack of 25, 40 packs per carton.",
                    25, 10.00,  1000, 9.00,  0, 0.0 },
            { "149", "Thermo Disposable 8-Inch White Plain Plate", "Tableware & Hygiene",
                    "8-inch white plain disposable plates for everyday food service. Pack of 25, 40 packs per carton.",
                    25, 8.00,  1000, 7.20,  0, 0.0 },
            { "159", "Thermo Disposable Forks White", "Cutlery",
                    "White disposable plastic forks for takeaway and catering. Pack of 100, 10 packs per carton.",
                    100, 3.00,  1000, 2.70,  5000, 2.40 },
            { "439", "Thermo FV2 White Tray", "Containers & Trays",
                    "White FV2 foam trays for produce and meat display. Pack of 25, 20 packs per carton.",
                    25, 5.00,  500, 4.50,  0, 0.0 },
            { "443", "Thermo Table Spoons", "Cutlery",
                    "White disposable table spoons for events and food service. Pack of 100, 10 packs per carton.",
                    100, 3.00,  1000, 2.70,  5000, 2.40 },
            { "441", "Thermo Tea Spoons", "Cutlery",
                    "White disposable tea spoons for coffee bars and catering. Pack of 100, 20 packs per carton.",
                    100, 2.50,  2000, 2.20,  0, 0.0 },
            { "243", "Toothpick Jars", "Cutlery",
                    "Glass-style toothpick jars filled with toothpicks for restaurant tables. Pack of 40 jars.",
                    40, 80.00,  0, 0.0,  0, 0.0 },
            { "530", "Tops Black Straws", "Straws & Stirrers",
                    "Bold black straws for cold drinks, cocktails, and juice bars. Pack of 150, 24 packs per carton.",
                    150, 4.00,  3600, 3.50,  0, 0.0 },
            { "238", "Twine 1PLY Kamba", "Miscellaneous",
                    "Single-ply natural twine for tying, bundling, and craft use. Pack of 10 rolls, 6 packs per carton.",
                    10, 120.00,  60, 110.00,  0, 0.0 },
            { "415", "Velvex Kitchen Towel", "Tableware & Hygiene",
                    "Absorbent Velvex kitchen paper towels for food prep, cleaning, and hygiene. Pack of 48 rolls.",
                    48, 12.00,  0, 0.0,  0, 0.0 },
            { "500", "Waco Clear Ziplock Bags 1KG", "Bags",
                    "Clear ziplock bags sized for 1 KG contents — ideal for dry goods, nuts, and food storage. Pack of 20.",
                    20, 80.00,  0, 0.0,  0, 0.0 },
            { "499", "Waco Clear Ziplock Bags 2KG", "Bags",
                    "Clear ziplock bags for 2 KG contents — bulk nuts, grains, and food packaging. Pack of 15.",
                    15, 90.00,  0, 0.0,  0, 0.0 },
            { "501", "Waco Clear Ziplock Bags Half KG", "Bags",
                    "Small ziplock bags for half-kilogram portions of spices and dry goods. Pack of 25.",
                    25, 60.00,  0, 0.0,  0, 0.0 },
            { "148", "XPRO 120/250ml Clear Container", "Containers & Trays",
                    "Clear 250ml containers in the 120mm series for deli and meal prep. Pack of 40, 15 packs per carton.",
                    40, 7.00,  600, 6.30,  0, 0.0 },
            { "398", "XPRO 120/250ml Clear Container + Lid", "Cups & Lids",
                    "Clear 250ml containers with matching lids. Pack of 10, 40 packs per carton.",
                    10, 8.00,  400, 7.20,  0, 0.0 },
            { "397", "XPRO 120/500ml Clear Container + Lid", "Cups & Lids",
                    "Clear 500ml containers with matching lids for deli and takeaway. Pack of 10, 40 packs per carton.",
                    10, 10.00,  400, 9.00,  0, 0.0 },
            { "113", "XPRO 120/500ml Clear Container", "Containers & Trays",
                    "Clear 500ml containers in the 120mm series. Pack of 40, 15 packs per carton.",
                    40, 10.00,  600, 9.00,  0, 0.0 },
            { "130", "XPRO 120mm Flat Lid", "Cups & Lids",
                    "120mm flat lids for XPRO containers. Secure snap-fit. Pack of 40, 15 packs per carton.",
                    40, 5.00,  600, 4.50,  0, 0.0 },
            { "242", "XPRO 4oz Clear Cups", "Cups & Lids",
                    "Clear 4oz cups for small cold drinks and samples. Pack of 50, 35 packs per carton.",
                    50, 4.50,  1750, 4.00,  0, 0.0 },
            { "110", "XPRO 4oz White Cup", "Cups & Lids",
                    "White 4oz cups for hot espresso shots and small portions. Pack of 50, 35 packs per carton.",
                    50, 5.00,  1750, 4.50,  0, 0.0 },
            { "207", "XPRO 5x7 Tray", "Containers & Trays",
                    "Clear 5x7 trays for fresh produce and deli display. Pack of 25, 50 packs per carton.",
                    25, 5.00,  1250, 4.50,  0, 0.0 },
            { "396", "XPRO 5oz Clear Cups", "Cups & Lids",
                    "Clear 5oz cups for cold drinks and juices. Pack of 50, 35 packs per carton.",
                    50, 5.00,  1750, 4.50,  0, 0.0 },
            { "150", "XPRO 6oz Clear Cup", "Cups & Lids",
                    "Clear 6oz cups for cold drinks and desserts. Pack of 50, 40 packs per carton.",
                    50, 6.00,  2000, 5.40,  0, 0.0 },
            { "139", "XPRO 72/200ml Clear Cup", "Cups & Lids",
                    "Clear 200ml cups in the 72mm series for cold drinks and yoghurt. Pack of 25, 50 packs per carton.",
                    25, 6.00,  1250, 5.40,  0, 0.0 },
            { "121", "XPRO 72/50ml Clear Container", "Containers & Trays",
                    "Small 50ml clear containers for sauces and condiments. Pack of 50, 50 packs per carton.",
                    50, 4.00,  2500, 3.60,  0, 0.0 },
            { "122", "XPRO 72/75ml Clear Container", "Containers & Trays",
                    "75ml clear containers for small condiment portions. Pack of 50, 50 packs per carton.",
                    50, 4.50,  2500, 4.00,  0, 0.0 },
            { "120", "XPRO 72mm Flat Lid", "Cups & Lids",
                    "72mm flat lids for XPRO cups and containers. Secure seal. Pack of 50, 50 packs per carton.",
                    50, 4.00,  2500, 3.60,  0, 0.0 },
            { "186", "XPRO 82/250ml Clear Cup", "Cups & Lids",
                    "Clear 250ml cups in the 82mm series. Pack of 25, 50 packs per carton.",
                    25, 7.00,  1250, 6.30,  0, 0.0 },
            { "147", "XPRO 95/500 Clear Cup", "Cups & Lids",
                    "Clear 500ml cups in the 95mm series for large cold drinks. Pack of 25, 40 packs per carton.",
                    25, 9.00,  1000, 8.10,  0, 0.0 },
            { "142", "Zara Serviettes", "Tableware & Hygiene",
                    "Soft paper serviettes for restaurants, hotels, and events. Clean presentation and high absorbency. Pack of 18.",
                    18, 120.00,  0, 0.0,  0, 0.0 },
            { "509", "Zesta Icing Sugar 500g", "Food & Condiments",
                    "Fine icing sugar for cake decorating, glazing, and pastry making. 500g packs. Carton of 24.",
                    24, 85.00,  0, 0.0,  0, 0.0 },
            { "490", "Zipper Bags", "Bags",
                    "Versatile clear zipper bags for food storage, retail, and packing. Resealable and durable. Pack of 100.",
                    100, 8.00,  1000, 7.20,  5000, 6.50 },
            { "233", "Zuri No.1 Bags", "General",
                    "Zuri No.1 polythene bags for retail and market use. Pack of 50, 40 packs per carton.",
                    50, 5.00,  2000, 4.50,  0, 0.0 },
            { "232", "Zuri No.1/2 Bags", "General",
                    "Zuri No.1/2 bags for small retail and pharmacy packaging. Pack of 50, 50 packs per carton.",
                    50, 4.50,  2500, 4.00,  0, 0.0 },
            { "234", "Zuri No.2 Bags", "General",
                    "Zuri No.2 bags for medium retail and grocery packaging. Pack of 50, 50 packs per carton.",
                    50, 6.00,  2500, 5.40,  0, 0.0 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        if (PRODUCTS.length == 0) { log.warn("ProductSeeder10: empty"); return; }
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
        log.info("ProductSeeder10: saved {} products with pricing tiers", batch.size());
    }
}