package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.PriceUnit;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Product seeder part 3 of 10 — seeds products 83 to 123.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(12)
@RequiredArgsConstructor
public class ProductSeeder3 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "464", "ECSTACY UMBRELLA STRAWS(24PKTS*50PCS)", "PKT", "Straws & Stirrers" },
            new String[]{ "535", "EXE WHEAT FLOUR(2KG*12PCS)", "BALE", "Food & Condiments" },
            new String[]{ "541", "FAY HANDTOWEL(1*12PCS)", "PKT", "Tableware & Hygiene" },
            new String[]{ "221", "FLOWERED SMARTBAGS", "PCS", "Bags" },
            new String[]{ "224", "FNA SMARTBAGS", "PCS", "Bags" },
            new String[]{ "520", "FRESH FRI COOKING OIL (20LTR*1)", "LITRES", "Food & Condiments" },
            new String[]{ "528", "GARBAGE BAGS 100S (1*1)", "PCS", "Bags" },
            new String[]{ "517", "GRANORO  SPAGHETTI RISTORANTI (500GM*26)", "PKT", "Food & Condiments" },
            new String[]{ "518", "GRANORO FETTUCCUNE(500GM*24)", "GRMS", "Food & Condiments" },
            new String[]{ "514", "GRANORO LASAGNA( 500GMX27)", "GRMS", "Food & Condiments" },
            new String[]{ "516", "GRANORO PENNE(500GM*24)", "PKT", "Food & Condiments" },
            new String[]{ "475", "GREASEPROOF SHEETS(1*320PCS)", "BALE", "Wrapping & Foil" },
            new String[]{ "445", "HANDSTRETCH (1*6PCS) STRETCH WRAP", "PCS", "Wrapping & Foil" },
            new String[]{ "511", "HEINZ TOMATO KETCHUP( 300MLX12)", "PCS", "Food & Condiments" },
            new String[]{ "508", "KAPUTEI BBQ SAUCE( 400GX12)", "GRMS", "Food & Condiments" },
            new String[]{ "414", "KENYAN NEWSPAPER(1*40KGS)", "BALE", "Miscellaneous" },
            new String[]{ "507", "KENYLON BAKED BEANS( 420GMX24)", "GRMS", "Food & Condiments" },
            new String[]{ "353", "KIFARU SCOURING PADS(6PCS)*60PKTS", "PKT", "Hygiene & PPE" },
            new String[]{ "183", "KINGS NO.1 KHAKI(40PKTS*50PCS)", "PKT", "General" },
            new String[]{ "182", "KINGS NO.1/2 KHAKI(50PKTS*50PCS)", "PKT", "General" },
            new String[]{ "181", "KINGS NO.1/4 KHAKI(50PKTS*50PCS)", "PKT", "General" },
            new String[]{ "184", "KINGS NO.2 KHAKI(30PKTS*50PCS)", "PKT", "General" },
            new String[]{ "219", "LAMINATED 3D BAGS", "PCS", "Bags" },
            new String[]{ "218", "LARGE 3D SMART BAG", "PCS", "Bags" },
            new String[]{ "534", "LIZ PACK SALT SATCHETS(1*1000PCS)", "CTN", "Food & Condiments" },
            new String[]{ "532", "LLC  BAGS 9X15CM (1*100)", "PCS", "Bags" },
            new String[]{ "533", "LLC BAGS 10X15CM (1X100)", "PCS", "Bags" },
            new String[]{ "512", "LUXEAPERS PITTED BLACK SLICED  OLIVES (345GMX12)", "PCS", "Food & Condiments" },
            new String[]{ "556", "MACADAMIA NUTS", "KGS", "Food & Condiments" },
            new String[]{ "471", "MADE IN CHINA 60*100 CLR YELLOW TAPE(1*)60PCS", "PCS", "Tapes" },
            new String[]{ "462", "MADE IN CHINA 60*200 CLR YELLOW TAPE(1*60PCS)", "PCS", "Tapes" },
            new String[]{ "401", "MAFUCO BRWN HANDLED TWISTED BAGS LARGE(1*50PCS)", "PCS", "Bags" },
            new String[]{ "400", "MAFUCO BRWN HANDLED TWISTED BAGS MEDIUM(1*50PCS)", "PCS", "Bags" },
            new String[]{ "399", "MAFUCO BRWN HANDLED TWISTED BAGS SMALL(1*50PCS)", "PCS", "Bags" },
            new String[]{ "402", "MAFUCO MILLINARY 3*5 BROWN BAG(20PKTS*100PCS)", "PKT", "Bags" },
            new String[]{ "403", "MAFUCO MILLINARY 4*6 BROWN BAG(20PKTS*100PCS)", "PKT", "Bags" },
            new String[]{ "404", "MAFUCO MILLINARY 5*6 BROWN BAG(20PKTS*100PCS)", "PKT", "Bags" },
            new String[]{ "444", "MAFUCO MILLINARY 6*10 BROWN BAG(20PKTS*100PCS)", "PKT", "Bags" },
            new String[]{ "405", "MAFUCO MILLINARY 6*7.5 BROWN BAG(20PKTS*100PCS)", "PKT", "Bags" },
            new String[]{ "406", "MAFUCO MILLINARY 7*9 BROWN BAG(20PKTS*100PCS)", "PKT", "Bags" },
            new String[]{ "407", "MAFUCO MILLINARY 8.5*10 BROWN BAG(20PKTS*100PCS)", "PKT", "Bags" }
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seederHelper.shouldSeed()) return;
        List<Product> batch = new ArrayList<>();
        for (String[] row : PRODUCTS) {
            batch.add(seederHelper.buildProduct(row[0], row[1], row[2], row[3], batch));
        }
        productRepository.saveAll(batch);
        log.info("ProductSeeder3: saved {} products", batch.size());
    }
}