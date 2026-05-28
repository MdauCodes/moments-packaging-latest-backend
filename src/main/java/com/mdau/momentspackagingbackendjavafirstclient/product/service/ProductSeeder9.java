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
@Order(18)
@RequiredArgsConstructor
public class ProductSeeder9 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final Object[][] PRODUCTS = {
            { "482", "Tech 94mm Yoghurt Cup Lids", "Cups & Lids",
                    "94mm flat lids for yoghurt cups and cold dessert containers. Secure snap-fit. Pack of 50, 50 packs per carton.",
                    50, 6.00,  2500, 5.40,  0, 0.0 },
            { "379", "Tech 98mm Honey Jar Caps Gold", "Jars & Bottles",
                    "Gold 98mm caps for large honey jars. Premium finish for retail and gifting. Pack of 350.",
                    350, 5.00,  0, 0.0,  0, 0.0 },
            { "373", "Tech Art 32 Clear PET Container", "Containers & Trays",
                    "Clear Art-32 PET containers for premium deli and food display. Pack of 500.",
                    500, 8.00,  0, 0.0,  0, 0.0 },
            { "374", "Tech Art 58 Clear PET Container", "Containers & Trays",
                    "Clear Art-58 PET containers for larger premium food display. Pack of 500.",
                    500, 10.00,  0, 0.0,  0, 0.0 },
            { "281", "Tech Cake Box 10x10x4", "Containers & Trays",
                    "White cake boxes 10x10x4 inches for medium cakes and baked goods. Pack of 100.",
                    100, 45.00,  0, 0.0,  0, 0.0 },
            { "282", "Tech Cake Box 12x12x4", "Containers & Trays",
                    "Large white cake boxes 12x12x4 inches for full-size celebration cakes. Pack of 100.",
                    100, 55.00,  0, 0.0,  0, 0.0 },
            { "278", "Tech Cake Box 5x5x4", "Containers & Trays",
                    "Small cake boxes 5x5x4 inches for individual cakes and dessert gifting. Pack of 100.",
                    100, 25.00,  0, 0.0,  0, 0.0 },
            { "279", "Tech Cake Box 6x6x4", "Containers & Trays",
                    "Cake boxes 6x6x4 inches for small cakes and brownies. Pack of 100.",
                    100, 30.00,  0, 0.0,  0, 0.0 },
            { "280", "Tech Cake Box 8x8x4", "Containers & Trays",
                    "Medium cake boxes 8x8x4 inches for standard round and square cakes. Pack of 100.",
                    100, 38.00,  0, 0.0,  0, 0.0 },
            { "285", "Tech Chips Boat Tray Small", "Containers & Trays",
                    "Small boat-shaped chip trays for fries, snacks, and street food. Pack of 250.",
                    250, 7.00,  0, 0.0,  0, 0.0 },
            { "284", "Tech Chips Pouch Small", "Containers & Trays",
                    "Small chip pouches for fries, popcorn, and snacks at events and outlets. Pack of 1000.",
                    1000, 4.00,  0, 0.0,  0, 0.0 },
            { "229", "Tech Disposable 11-Inch White 3-Part Plate", "Tableware & Hygiene",
                    "11-inch white 3-compartment plates for buffets and event catering. Pack of 25, 20 packs per carton.",
                    25, 16.00,  500, 14.50,  0, 0.0 },
            { "228", "Tech Disposable 11-Inch White Plain Plate", "Tableware & Hygiene",
                    "11-inch white plain disposable plates for events and food service. Pack of 25, 20 packs per carton.",
                    25, 14.00,  500, 12.50,  0, 0.0 },
            { "289", "Tech Food and Dessert BS4 Tray", "Containers & Trays",
                    "BS4 black presentation trays for desserts and sushi. Pack of 1000.",
                    1000, 7.00,  0, 0.0,  0, 0.0 },
            { "272", "Tech Generic Pizza Box Large", "Containers & Trays",
                    "Large generic pizza boxes for 12-14 inch pizzas. Vented lid for crispy delivery. Pack of 100.",
                    100, 60.00,  0, 0.0,  0, 0.0 },
            { "273", "Tech Generic Pizza Box Medium", "Containers & Trays",
                    "Medium pizza boxes for 10 inch pizzas. Sturdy corrugated construction. Pack of 100.",
                    100, 50.00,  0, 0.0,  0, 0.0 },
            { "274", "Tech Generic Pizza Box Small", "Containers & Trays",
                    "Small pizza boxes for 8 inch pizzas and calzones. Pack of 100.",
                    100, 40.00,  0, 0.0,  0, 0.0 },
            { "275", "Tech Paper Popcorn Tub Red Large", "Containers & Trays",
                    "Large red paper popcorn tubs for cinemas, events, and snack bars. Pack of 250.",
                    250, 18.00,  0, 0.0,  0, 0.0 },
            { "276", "Tech Paper Popcorn Tub Red Medium", "Containers & Trays",
                    "Medium red popcorn tubs for events and concession stands. Pack of 500.",
                    500, 14.00,  0, 0.0,  0, 0.0 },
            { "277", "Tech Paper Popcorn Tub Red Small", "Containers & Trays",
                    "Small red popcorn tubs for individual snack portions. Pack of 500.",
                    500, 10.00,  0, 0.0,  0, 0.0 },
            { "241", "Tech Shot Glass", "General",
                    "Clear disposable shot glasses for events, tastings, and promotions. Pack of 40, 25 packs per carton.",
                    40, 6.00,  1000, 5.40,  0, 0.0 },
            { "286", "Tech Single Wall 5oz White Cup", "Cups & Lids",
                    "White 5oz single-wall cups for espresso and small hot drinks. Pack of 50, 20 packs per carton.",
                    50, 7.00,  1000, 6.30,  0, 0.0 },
            { "287", "Tech Single Wall 7oz Paper Cup", "Cups & Lids",
                    "White 7oz single-wall cups for medium hot drinks. Pack of 50, 20 packs per carton.",
                    50, 8.50,  1000, 7.60,  0, 0.0 },
            { "153", "Tech Strawberry 250ml Pierced Lid", "Straws & Stirrers",
                    "Clear 250ml lids with straw hole for cold drinks. Pack of 500.",
                    500, 5.00,  0, 0.0,  0, 0.0 },
            { "107", "Tech Strawberry 250ml Unpierced Lid", "Straws & Stirrers",
                    "Clear 250ml flat lids without hole for cold drinks and desserts. Pack of 500.",
                    500, 4.50,  0, 0.0,  0, 0.0 },
            { "154", "Teepee DS6 Black Unwrapped Milkshake Straw", "Straws & Stirrers",
                    "Wide black milkshake straws in bulk packs. Perfect for thick shakes and smoothies. Pack of 30 bundles.",
                    30, 85.00,  0, 0.0,  0, 0.0 },
            { "146", "Teepee DS7 Black Wrapped Milkshake Straw", "Straws & Stirrers",
                    "Individually wrapped wide black milkshake straws for hygiene-conscious service. Pack of 30 bundles.",
                    30, 100.00,  0, 0.0,  0, 0.0 },
            { "185", "Teepee DS3 Clear Straws", "Straws & Stirrers",
                    "Clear flexible straws in bulk packs for restaurants and food service. Pack of 24 bundles.",
                    24, 75.00,  0, 0.0,  0, 0.0 },
            { "140", "Texplast No.15 Kikuyu Bags", "Bags",
                    "Kikuyu-pattern No.15 polythene bags popular in Kenyan markets. Pack of 50, 40 packs per carton.",
                    50, 4.50,  2000, 4.00,  0, 0.0 },
            { "132", "Texplast V24 Box Bottom Sacks", "Containers & Trays",
                    "V24 box-bottom sacks for grains, flour, and bulk packaging. Pack of 1000.",
                    1000, 8.00,  0, 0.0,  0, 0.0 },
            { "131", "Texplast V30 Box Bottom Sacks", "Containers & Trays",
                    "V30 box-bottom sacks for heavier grains and bulk goods. Pack of 500.",
                    500, 10.00,  0, 0.0,  0, 0.0 },
            { "133", "Texplast No.22 Kikuyu Bags", "General",
                    "Kikuyu-pattern No.22 bags for medium retail packaging. Pack of 50, 20 packs per carton.",
                    50, 6.50,  1000, 5.80,  0, 0.0 },
            { "537", "Textplast 50KG Green Panel Sack", "General",
                    "Green 50 KG panel sacks for agricultural produce and bulk goods. Pack of 1000.",
                    1000, 6.00,  0, 0.0,  0, 0.0 },
            { "230", "Textplast 50KG Red Panel Sack", "Bags",
                    "Red 50 KG panel sacks for grain and produce storage and transport. Pack of 1000.",
                    1000, 6.00,  0, 0.0,  0, 0.0 },
            { "470", "Textplast 90KG Blue Panel Sack", "Bags",
                    "Heavy-duty 90 KG blue panel sacks for large-volume agricultural and industrial use. Pack of 500.",
                    500, 12.00,  0, 0.0,  0, 0.0 },
            { "239", "Textplast 90KG Red Panel Sack", "Bags",
                    "Heavy-duty 90 KG red panel sacks for bulk grain and produce transport. Pack of 500.",
                    500, 12.00,  0, 0.0,  0, 0.0 },
            { "125", "Textplast V22 Box Bottom Sacks", "Containers & Trays",
                    "V22 box-bottom sacks for standard bulk packaging. Pack of 1000.",
                    1000, 7.00,  0, 0.0,  0, 0.0 },
            { "213", "Thank You Large Smart Bags", "Bags",
                    "Large Thank You printed smart bags for retail checkout — the classic customer appreciation bag. Pack of 25.",
                    25, 10.00,  250, 9.00,  0, 0.0 },
            { "223", "Thank You Small Smart Bag", "Bags",
                    "Small Thank You printed smart bags for boutiques and pharmacies. Pack of 250.",
                    250, 3.00,  2500, 2.70,  0, 0.0 },
            { "440", "Thermo 5x7 1000ml Clear Punnet", "Containers & Trays",
                    "Clear 1000ml 5x7 punnets for produce and deli. Pack of 10, 35 packs per carton.",
                    10, 10.00,  350, 9.00,  0, 0.0 },
            { "437", "Thermo 5x7 Tray", "Containers & Trays",
                    "Foam 5x7 trays for fresh produce, meat, and fish display. Pack of 25, 60 packs per carton.",
                    25, 5.00,  1500, 4.50,  0, 0.0 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        if (PRODUCTS.length == 0) { log.warn("ProductSeeder9: empty"); return; }
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
        log.info("ProductSeeder9: saved {} products with pricing tiers", batch.size());
    }
}