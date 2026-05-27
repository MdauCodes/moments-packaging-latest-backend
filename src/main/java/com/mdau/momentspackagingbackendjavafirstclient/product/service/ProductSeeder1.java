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
 * Product seeder part 1 of 10 — seeds products 1 to 41.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class ProductSeeder1 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "529", "30*36 BAGS 50S (1*1)", "PCS", "Bags" },
            new String[]{ "527", "ALLEGRO  EXTRA VIRGIN OLIVE OIL(1LTR*12)", "LITRES", "Food & Condiments" },
            new String[]{ "151", "AVERY 57*40 MM THERMAL ROLL(1*100PCS)", "PCS", "Thermal Rolls" },
            new String[]{ "128", "AVERY 79*80MM THERMAL ROLL(1*50PCS)", "PCS", "Thermal Rolls" },
            new String[]{ "513", "BASMATI RICE(1*25KGS)", "BALE", "Food & Condiments" },
            new String[]{ "126", "BESA SALT SATCHETS(1*1000PCS)", "CTN", "Food & Condiments" },
            new String[]{ "116", "BESA SUGAR SATCHETS  WHITE(1*1000PCS)", "CTN", "Food & Condiments" },
            new String[]{ "297", "BIO BLEND STRAWS", "PKT", "Straws & Stirrers" },
            new String[]{ "220", "BLACK CHINA SMART BAGS", "PCS", "Bags" },
            new String[]{ "225", "BLUE SMARTBAG", "PCS", "Bags" },
            new String[]{ "227", "CHILLI SAUCE(1*300SATCHETS)", "CTN", "Food & Condiments" },
            new String[]{ "214", "CHINA SMART BAG MEDIUM(1*25PCS)", "PCS", "Bags" },
            new String[]{ "240", "CLICK NO.1/4(150PKTS*50PCS)", "PKT", "General" },
            new String[]{ "505", "CLOVERS COCOA POWDER (400GMX12)", "PKT", "Food & Condiments" },
            new String[]{ "458", "DFAC 12OZ SINGLE WALL WHITE PLAIN(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "461", "DFAC 45*300M CLING FILM(1*5PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "460", "DFAC 45*5 CLING FILM(1*5PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "459", "DFAC 45*90 ALL FOIL(1*6PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "456", "DFAC 8 OZ BLACK PRINTED RIPPLE CUP(20PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "248", "DFAC 850ML AND 1000ML LIDS(1*500PCS)", "PCS", "Cups & Lids" },
            new String[]{ "538", "DFAC 8OZ SINGLE WALL PLAIN WHITE(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "455", "DFAC BIO DEGREDABLE  2COMP BOX 850ML(1*500PCS)", "PCS", "Containers & Trays" },
            new String[]{ "457", "DFAC BIO DEGREDABLE  7 INCH PLATE (1*500PCS)", "PCS", "Tableware & Hygiene" },
            new String[]{ "247", "DFAC BIO DEGREDABLE BOX 1000ML(1*500PCS)", "PCS", "Containers & Trays" },
            new String[]{ "249", "DFAC CLAMSHELL BIO DEGREDABLE BOX 600ML(1*250PCS)", "PCS", "Containers & Trays" },
            new String[]{ "539", "DFAC HANAN WET WIPES(10PKTS*100PCS)", "PKT", "Tableware & Hygiene" },
            new String[]{ "540", "DFAC RIPPLE 12OZ PRINTED PPR CUP(20PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "251", "DFAC WOODEN FORKS(100PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "252", "DFAC WOODEN KNIFE(100PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "250", "DFAC WOODEN SPOONS (100PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "531", "DFAC210040 HD CORNSTARCH CUTLERY SET (1*500PCS)", "PCS", "Cutlery" },
            new String[]{ "347", "DISPOSABLE GARB BAG(1*50PCS) GARBAGE BAGS", "PKT", "Bags" },
            new String[]{ "362", "DUNE NO.1 BRWN HANDLED BAGS(1*100PCS)", "PCS", "Bags" },
            new String[]{ "359", "DUNE NO.10 KHAKI(1*15KGS)", "KGS", "General" },
            new String[]{ "351", "DUNE NO.14 KHAKI(1*15KGS)", "KGS", "General" },
            new String[]{ "352", "DUNE NO.16 KHAKI(1*15KGS)", "KGS", "General" },
            new String[]{ "363", "DUNE NO.2 BRWN HANDLED BAGS(1*100PCS)", "PCS", "Bags" },
            new String[]{ "350", "DUNE NO.2 KHAKI(1*15KGS)", "KGS", "General" },
            new String[]{ "364", "DUNE NO.3 BRWN HANDLED BAGS(1*100PCS)", "PCS", "Bags" },
            new String[]{ "361", "DUNE NO.4 BRWN HANDLED BAGS(1*100PCS)", "PCS", "Bags" },
            new String[]{ "365", "DUNE NO.5 BRWN HANDLED BAGS(1*100PCS)", "PCS", "Bags" }
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
        log.info("ProductSeeder1: saved {} products", batch.size());
    }
}