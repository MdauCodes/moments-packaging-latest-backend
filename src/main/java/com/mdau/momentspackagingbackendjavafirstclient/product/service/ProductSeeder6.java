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
@Order(15)
@RequiredArgsConstructor
public class ProductSeeder6 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final Object[][] PRODUCTS = {
            { "191", "Pan 890cc Aluminium Lids", "Cups & Lids",
                    "Aluminium lids for 890cc containers. Secure fit for storage, transport, and freezer use. Pack of 1000.",
                    1000, 8.00,  0, 0.0,  0, 0.0 },
            { "487", "Pan Bamboo Paddle Pick 12cm", "Miscellaneous",
                    "12cm bamboo paddle picks for canapés, sandwiches, and event food displays. Natural look for upscale presentation. Pack of 100.",
                    100, 5.00,  1000, 4.50,  0, 0.0 },
            { "488", "Pan Bamboo Paddle Pick 18cm", "Miscellaneous",
                    "18cm bamboo paddle picks for larger skewered foods and cocktail garnishes. Natural finish for elegant food service. Pack of 100.",
                    100, 6.50,  1000, 6.00,  0, 0.0 },
            { "551", "Pan Black Microwave Container 24oz", "Cups & Lids",
                    "Black microwave-safe 24oz containers for meal prep and takeaway. Sturdy and heat-resistant. Pack of 150.",
                    150, 18.00,  0, 0.0,  0, 0.0 },
            { "266", "Pan Black Microwave Container 2-Part", "Containers & Trays",
                    "Black 2-compartment microwave-safe containers for combo meals. Keeps foods separate during delivery. Pack of 200.",
                    200, 22.00,  0, 0.0,  0, 0.0 },
            { "552", "Pan Black Microwave Container 32oz", "Cups & Lids",
                    "Black microwave-safe 32oz containers for large portions and meal prep. Pack of 150.",
                    150, 20.00,  0, 0.0,  0, 0.0 },
            { "553", "Pan Black Microwave Container 38oz", "Cups & Lids",
                    "Extra-large 38oz black microwave containers for generous portions and family meals. Pack of 150.",
                    150, 22.00,  0, 0.0,  0, 0.0 },
            { "494", "Pan Black Microwave Container 3-Part", "Containers & Trays",
                    "Black 3-compartment microwave-safe containers for full meals with sides. Pack of 200.",
                    200, 25.00,  0, 0.0,  0, 0.0 },
            { "549", "Pan Bamboo Heart Knot Picks 12cm", "Miscellaneous",
                    "Decorative bamboo heart-knot picks for desserts, appetisers, and event catering. Adds a charming touch. Pack of 100.",
                    100, 7.00,  1000, 6.50,  0, 0.0 },
            { "424", "Pan Coloured Plastic Ice Cream Spoons", "Cutlery",
                    "Bright coloured plastic ice cream spoons for parlours and dessert shops. Fun and hygienic for single use. Pack of 100, 100 packs per carton.",
                    100, 2.50,  10000, 2.20,  0, 0.0 },
            { "493", "Pan Ice Cream Cup 12oz Printed", "Cups & Lids",
                    "Printed 12oz ice cream cups with branded design for ice cream parlours and dessert shops. Pack of 50, 20 packs per carton.",
                    50, 18.00,  1000, 16.50,  0, 0.0 },
            { "489", "Pan Ice Cream Cup 4oz Printed", "Cups & Lids",
                    "Printed 4oz ice cream cups for small scoops and gelato servings. Pack of 50, 20 packs per carton.",
                    50, 10.00,  1000, 9.00,  0, 0.0 },
            { "492", "Pan Ice Cream Cup 8oz Printed", "Cups & Lids",
                    "Printed 8oz ice cream cups for standard scoops and frozen desserts. Pack of 50, 20 packs per carton.",
                    50, 14.00,  1000, 12.50,  0, 0.0 },
            { "550", "Pan Ice Cream Cup 6oz Printed", "Cups & Lids",
                    "Printed 6oz ice cream cups for medium servings. Fun design for dessert shops. Pack of 50, 20 packs per carton.",
                    50, 12.00,  1000, 11.00,  0, 0.0 },
            { "548", "Pan Ice Cream Wooden Spoon Sticks Wrapped", "Cutlery",
                    "Individually wrapped wooden spoon sticks for ice cream bars and dessert events. Pack of 50.",
                    50, 8.00,  500, 7.20,  0, 0.0 },
            { "446", "Pan Kraft Takeaway Deli Box 2-Part", "Containers & Trays",
                    "2-compartment kraft takeaway deli box for combo meals. Eco-friendly and grease-resistant. Pack of 50.",
                    50, 32.00,  500, 29.00,  0, 0.0 },
            { "188", "Pan Kraft Takeaway Deli Box Large", "Containers & Trays",
                    "Large brown kraft deli box for generous takeaway portions. Strong base and secure closure. Pack of 50.",
                    50, 35.00,  500, 32.00,  0, 0.0 },
            { "187", "Pan Kraft Takeaway Deli Box Medium", "Containers & Trays",
                    "Medium brown kraft deli box for standard meal portions and takeaway. Pack of 50.",
                    50, 28.00,  500, 25.50,  0, 0.0 },
            { "189", "Pan Kraft Takeaway Deli Box Small", "Containers & Trays",
                    "Small brown kraft deli box for snacks, sides, and light meals. Pack of 50.",
                    50, 22.00,  500, 20.00,  0, 0.0 },
            { "192", "Pan No.1 Brown Handled Bags", "Bags",
                    "No.1 size brown kraft handled bags for small retail items. Pack of 100.",
                    100, 22.00,  1000, 20.00,  5000, 18.00 },
            { "193", "Pan No.2 Brown Handled Bags", "Bags",
                    "No.2 brown kraft handled bags for medium retail packaging. Pack of 100.",
                    100, 28.00,  1000, 25.50,  5000, 23.00 },
            { "194", "Pan No.3 Brown Handled Bags", "Bags",
                    "No.3 brown kraft bags for larger retail and gift items. Pack of 100.",
                    100, 34.00,  1000, 31.00,  5000, 28.00 },
            { "195", "Pan No.4 Brown Handled Bags", "Bags",
                    "No.4 brown kraft handled bags for large clothing and gift retail. Pack of 100.",
                    100, 40.00,  1000, 37.00,  5000, 34.00 },
            { "485", "Pan PET U-Shaped Cup 12oz", "Cups & Lids",
                    "Clear U-shaped PET cups for cold drinks, smoothies, and bubble tea. Pack of 50, 20 packs per carton.",
                    50, 14.00,  1000, 12.50,  0, 0.0 },
            { "484", "Pan PET U-Shaped Cup 16oz", "Cups & Lids",
                    "Large 16oz clear PET U-shaped cups for big cold drinks and slushies. Pack of 50, 20 packs per carton.",
                    50, 16.00,  1000, 14.50,  0, 0.0 },
            { "486", "Pan PET Flat Lid 12oz/16oz Clear", "Cups & Lids",
                    "Clear flat lids for 12oz and 16oz PET cups. Secure fit for cold drinks. Pack of 50, 20 packs per carton.",
                    50, 8.00,  1000, 7.20,  0, 0.0 },
            { "423", "Pan Tea Spoon", "Cutlery",
                    "White disposable tea spoons for stirring and small desserts. Pack of 50, 40 packs per carton.",
                    50, 2.50,  2000, 2.20,  0, 0.0 },
            { "483", "Pan Wooden Stirrers Wrapped", "Straws & Stirrers",
                    "Individually wrapped wooden coffee stirrers. Eco-friendly and hygienic for cafes and hotels. Pack of 500.",
                    500, 10.00,  0, 0.0,  0, 0.0 },
            { "491", "Pan Wooden Ice Cream Sticks Unwrapped", "Cutlery",
                    "Natural wooden ice cream sticks for homemade ice pops and dessert bars. Pack of 50.",
                    50, 6.00,  500, 5.50,  0, 0.0 },
            { "536", "Pembe Maize Flour 2KG", "Food & Condiments",
                    "Kenya's favourite Pembe maize flour in 2kg packs for ugali, porridge, and baking. Bale of 12.",
                    12, 200.00,  0, 0.0,  0, 0.0 },
            { "510", "Peptang Maple Syrup 500ml", "Food & Condiments",
                    "Rich maple-flavoured syrup in 500ml bottles for pancakes, waffles, and desserts. Carton of 12.",
                    12, 380.00,  0, 0.0,  0, 0.0 },
            { "502", "Plasitec Pepper Sachets", "Food & Condiments",
                    "Black pepper in individual single-serve sachets for restaurants and catering. Carton of 1000.",
                    1000, 1.00,  5000, 0.90,  20000, 0.80 },
            { "296", "Plasitec Polypots", "Jars & Bottles",
                    "Clear polypot containers for jams, honey, spices, and condiments. Airtight seal keeps contents fresh. Pack of 1000.",
                    1000, 8.00,  0, 0.0,  0, 0.0 },
            { "295", "Plasitec Salt Sachets", "Food & Condiments",
                    "Fine salt in single-serve sachets for restaurants and takeaways. Carton of 1000.",
                    1000, 0.80,  5000, 0.70,  20000, 0.60 },
            { "294", "Plasitec Sugar Sachets White", "Food & Condiments",
                    "Refined white sugar in hygienic single-serve sachets for coffee stations. Carton of 1000.",
                    1000, 0.90,  5000, 0.80,  20000, 0.70 },
            { "394", "Pressmaster 15-Inch Pizza Box", "Containers & Trays",
                    "15-inch corrugated pizza boxes for large pizzas. Sturdy base and vented lid for crispy delivery. Pack of 100.",
                    100, 55.00,  0, 0.0,  0, 0.0 },
            { "476", "Primark Disposable Forks White", "Cutlery",
                    "White disposable plastic forks for takeaway and events. Pack of 100, 10 packs per carton.",
                    100, 3.00,  1000, 2.70,  5000, 2.40 },
            { "158", "Primark Disposable Spoons White", "Cutlery",
                    "White disposable spoons for food service and events. Pack of 100, 10 packs per carton.",
                    100, 3.00,  1000, 2.70,  5000, 2.40 },
            { "412", "Primark Disposable Spoons Small Pack", "Cutlery",
                    "White disposable spoons in smaller packs of 25. Convenient for low-volume operations. Pack of 25, 40 packs per carton.",
                    25, 1.00,  1000, 0.90,  0, 0.0 },
            { "395", "Primark Tea Spoons", "Cutlery",
                    "White disposable tea spoons for coffee bars and catering. Pack of 100, 10 packs per carton.",
                    100, 2.50,  1000, 2.20,  5000, 1.90 },
            { "521", "Pristine Whipping Cream 1L", "Food & Condiments",
                    "Professional whipping cream in 1-litre cartons for bakeries, cafes, and dessert preparation. Carton of 12.",
                    12, 420.00,  0, 0.0,  0, 0.0 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        if (PRODUCTS.length == 0) { log.warn("ProductSeeder6: empty"); return; }
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
        log.info("ProductSeeder6: saved {} products with pricing tiers", batch.size());
    }
}