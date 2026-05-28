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
@Order(13)
@RequiredArgsConstructor
public class ProductSeeder4 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final Object[][] PRODUCTS = {
            { "168", "Mafuco No.1 Khaki Bags 10KG", "General",
                    "No.1 khaki polythene bags sold by the 10 KG bale. Entry-level size for small retail and pharmacy use.",
                    1, 1500.00,  0, 0.0,  0, 0.0 },
            { "175", "Mafuco No.10 Khaki Bags 10KG", "General",
                    "No.10 khaki bags in 10 KG bales. Mid-range size for general retail packaging and market stalls.",
                    1, 2000.00,  0, 0.0,  0, 0.0 },
            { "176", "Mafuco No.12 Khaki Bags 10KG", "General",
                    "No.12 khaki bags in 10 KG bales for medium-load retail and grocery use.",
                    1, 2200.00,  0, 0.0,  0, 0.0 },
            { "177", "Mafuco No.14 Khaki Bags 10KG", "General",
                    "No.14 khaki bags in 10 KG bales for supermarkets, wholesalers, and bulk retail.",
                    1, 2400.00,  0, 0.0,  0, 0.0 },
            { "178", "Mafuco No.16 Khaki Bags 10KG", "General",
                    "Larger No.16 khaki bags in 10 KG bales for heavier items and bulk grocery distribution.",
                    1, 2600.00,  0, 0.0,  0, 0.0 },
            { "169", "Mafuco No.2 Khaki Bags 10KG", "General",
                    "No.2 khaki bags in 10 KG bales — popular for everyday retail and market packaging.",
                    1, 1600.00,  0, 0.0,  0, 0.0 },
            { "179", "Mafuco No.20 Khaki Bags 10KG", "General",
                    "No.20 khaki bags in 10 KG bales for large grocery items and wholesale distribution.",
                    1, 2800.00,  0, 0.0,  0, 0.0 },
            { "180", "Mafuco No.25 Khaki Bags 10KG", "General",
                    "Largest Mafuco khaki bags in 10 KG bales. Heavy-duty for bulk retail and agricultural packaging.",
                    1, 3000.00,  0, 0.0,  0, 0.0 },
            { "170", "Mafuco No.3 Khaki Bags 10KG", "General",
                    "No.3 khaki bags in 10 KG bales. Widely used in hardware stores and markets.",
                    1, 1700.00,  0, 0.0,  0, 0.0 },
            { "171", "Mafuco No.4 Khaki Bags 10KG", "General",
                    "No.4 khaki bags in 10 KG bales. Versatile for fresh produce and supermarket use.",
                    1, 1800.00,  0, 0.0,  0, 0.0 },
            { "172", "Mafuco No.5 Khaki Bags 10KG", "General",
                    "No.5 khaki bags in 10 KG bales — ideal for grocery stores, butcheries, and market vendors.",
                    1, 1900.00,  0, 0.0,  0, 0.0 },
            { "173", "Mafuco No.6 Khaki Bags 10KG", "General",
                    "No.6 khaki bags in 10 KG bales for medium grocery and retail packaging.",
                    1, 1950.00,  0, 0.0,  0, 0.0 },
            { "174", "Mafuco No.8 Khaki Bags 10KG", "General",
                    "No.8 khaki bags in 10 KG bales — popular mid-size for supermarkets and retail outlets.",
                    1, 1980.00,  0, 0.0,  0, 0.0 },
            { "503", "Mafuco White Handled Twisted Bags Small", "Bags",
                    "Small white kraft paper bags with twisted rope handles. Clean look for pharmacies and boutiques. Pack of 50.",
                    50, 22.00,  500, 20.00,  2500, 18.00 },
            { "269", "Manila Envelopes Large", "General",
                    "Large manila envelopes for documents and office correspondence. Sturdy kraft with gummed seal. Pack of 50.",
                    50, 18.00,  500, 16.00,  0, 0.0 },
            { "270", "Manila Envelopes Medium", "General",
                    "Standard A4 manila envelopes for business correspondence. Pack of 50.",
                    50, 14.00,  500, 12.50,  0, 0.0 },
            { "271", "Manila Envelopes Small", "General",
                    "Small manila envelopes for letters, invoices, and receipts. Pack of 50.",
                    50, 10.00,  500, 9.00,  0, 0.0 },
            { "268", "Manila Envelopes XL", "General",
                    "Extra-large manila envelopes for A3 documents and architectural drawings. Pack of 50.",
                    50, 25.00,  500, 22.00,  0, 0.0 },
            { "506", "Medjool Dates 5KG", "Food & Condiments",
                    "Premium large Medjool dates in 5 KG packs. Naturally sweet for hotels, bakeries, and health food shops.",
                    1, 3500.00,  0, 0.0,  0, 0.0 },
            { "162", "Mega No.15 Blue Bags", "General",
                    "Blue No.15 polythene bags for retail and market use. Pack of 50, 40 packs per carton.",
                    50, 4.50,  2000, 4.00,  10000, 3.60 },
            { "246", "Mega No.15 Green Bags", "General",
                    "Green No.15 bags for eco-themed retail and fresh produce. Pack of 50, 40 packs per carton.",
                    50, 4.50,  2000, 4.00,  10000, 3.60 },
            { "267", "Mega No.22 Assorted Bags", "General",
                    "Assorted colour No.22 bags. Pack of 50, 20 packs per carton.",
                    50, 6.50,  1000, 5.80,  0, 0.0 },
            { "164", "Mega No.22 Blue Bags", "General",
                    "Blue No.22 bags for medium retail packaging. Pack of 50, 20 packs per carton.",
                    50, 6.50,  1000, 5.80,  5000, 5.20 },
            { "245", "Mega No.22 Green Bags", "General",
                    "Green No.22 bags for organic retail and fresh produce. Pack of 50, 20 packs per carton.",
                    50, 6.50,  1000, 5.80,  5000, 5.20 },
            { "244", "Mega No.22 Red Bags", "General",
                    "Bold red No.22 bags for high-visibility retail packaging. Pack of 50, 20 packs per carton.",
                    50, 6.50,  1000, 5.80,  5000, 5.20 },
            { "165", "Mega No.25 Blue Bags", "General",
                    "Spacious blue No.25 bags for supermarket checkout and bulk retail. Pack of 50, 10 packs per carton.",
                    50, 8.00,  500, 7.20,  2500, 6.50 },
            { "355", "Mega No.25 Classic Blue Bags", "General",
                    "Premium classic blue No.25 bags with improved gauge for heavier loads. Pack of 50, 10 packs per carton.",
                    50, 9.00,  500, 8.20,  2500, 7.50 },
            { "356", "Mega No.25 Classic Green Bags", "General",
                    "Premium classic green No.25 bags — durable eco-themed retail bag. Pack of 50, 10 packs per carton.",
                    50, 9.00,  500, 8.20,  2500, 7.50 },
            { "354", "Mega No.25 Classic Red Bags", "General",
                    "Premium classic red No.25 bags with extra strength for heavier items. Pack of 50, 10 packs per carton.",
                    50, 9.00,  500, 8.20,  2500, 7.50 },
            { "166", "Mega No.25 Red Bags", "General",
                    "Large red No.25 bags for high-visibility retail and market stalls. Pack of 50, 10 packs per carton.",
                    50, 8.00,  500, 7.20,  2500, 6.50 },
            { "163", "Mega No.15 Red Bags", "General",
                    "Red No.15 bags for bright retail packaging. Pack of 50, 40 packs per carton.",
                    50, 4.50,  2000, 4.00,  10000, 3.60 },
            { "143", "Midatex Blue Hairnets", "Hygiene & PPE",
                    "Blue disposable hairnets for food handlers and kitchen staff. Lightweight and breathable. Pack of 100, 20 packs per carton.",
                    100, 3.00,  2000, 2.70,  0, 0.0 },
            { "123", "Midatex Disposable Clear Gloves", "Hygiene & PPE",
                    "Clear polyethylene gloves for food handling and hygiene. Pack of 100, 100 packs per carton.",
                    100, 4.50,  10000, 4.00,  0, 0.0 },
            { "115", "Midatex Hairnets White", "Hygiene & PPE",
                    "White disposable hairnets for food production and catering environments. Pack of 100, 20 packs per carton.",
                    100, 3.00,  2000, 2.70,  0, 0.0 },
            { "261", "MOH 48x100 Yellowish Tape", "Tapes",
                    "48mm x 100m yellowish packing tape for carton sealing. Strong adhesive. Pack of 36 rolls.",
                    36, 60.00,  0, 0.0,  0, 0.0 },
            { "260", "MOH 48x100 Tan Yellow Tape", "Tapes",
                    "48mm x 100m tan-yellow packing tape for sealing boxes in transit. Pack of 36 rolls.",
                    36, 60.00,  0, 0.0,  0, 0.0 },
            { "265", "MOH 60x100 Tan Yellow Tape", "Tapes",
                    "Wide 60mm x 100m tan-yellow tape for heavy-duty carton sealing. Pack of 36 rolls.",
                    36, 75.00,  0, 0.0,  0, 0.0 },
            { "263", "MOH 60x200 Tan Yellow Tape", "Tapes",
                    "Long-run 60mm x 200m tan-yellow tape for high-volume packing. Pack of 30 rolls.",
                    30, 120.00,  0, 0.0,  0, 0.0 },
            { "264", "MOH 60x200 Yellowish Tape", "Tapes",
                    "60mm x 200m yellowish tape for sealing and bundling. Extended length reduces downtime. Pack of 30 rolls.",
                    30, 120.00,  0, 0.0,  0, 0.0 },
            { "262", "MOH 72x250 Yellowish Tape", "Tapes",
                    "Extra-wide 72mm x 250m heavy-duty yellowish tape for large carton sealing. Pack of 24 rolls.",
                    24, 180.00,  0, 0.0,  0, 0.0 },
            { "212", "Moonray 12oz Single Wall Printed Cup", "Cups & Lids",
                    "Pre-printed 12oz single-wall paper cups with Moonray design for branded coffee service. Pack of 50, 20 packs per carton.",
                    50, 12.00,  1000, 11.00,  5000, 10.00 },
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        if (PRODUCTS.length == 0) { log.warn("ProductSeeder4: empty"); return; }
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
        log.info("ProductSeeder4: saved {} products with pricing tiers", batch.size());
    }
}