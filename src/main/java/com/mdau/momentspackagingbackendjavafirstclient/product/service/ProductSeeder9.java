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
 * Product seeder part 9 of 10 — seeds products 329 to 369.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(18)
@RequiredArgsConstructor
public class ProductSeeder9 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "482", "TECH 94MM YOGHURT CUPS LIDS(50PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "379", "TECH 98MM HONEY JARS CAPS GOLD(1**350PCS)", "PCS", "Jars & Bottles" },
            new String[]{ "373", "TECH ART 32 CLR PET CONT(1*500PCS)", "PCS", "Containers & Trays" },
            new String[]{ "374", "TECH ART 58 CLR PET CONT(1*500PCS)", "PCS", "Containers & Trays" },
            new String[]{ "281", "TECH CAKE 10*10*4 BOX(1*100PCS)", "PCS", "Containers & Trays" },
            new String[]{ "282", "TECH CAKE 12*12*4 BOX(1*100PCS)", "PCS", "Containers & Trays" },
            new String[]{ "278", "TECH CAKE 5*5*4 BOX(1*100PCS)", "PCS", "Containers & Trays" },
            new String[]{ "279", "TECH CAKE 6*6*4 BOX(1*100PCS)", "PCS", "Containers & Trays" },
            new String[]{ "280", "TECH CAKE 8*8*4 BOX(1*100PCS)", "PCS", "Containers & Trays" },
            new String[]{ "285", "TECH CHIPS  BOAT TRAY SMALL(1*250PCS)", "PCS", "Containers & Trays" },
            new String[]{ "284", "TECH CHIPS POUCH SMALL(1*1000PCS)", "PCS", "Containers & Trays" },
            new String[]{ "229", "TECH DISPOSABLE 11\" WHITE 3PART PLATE(20PKTS*25PCS)", "PKT", "Tableware & Hygiene" },
            new String[]{ "228", "TECH DISPOSABLE 11\" WHITE PLAIN PLATE(20PKTS*25PCS)", "PKT", "Tableware & Hygiene" },
            new String[]{ "289", "TECH FOOD AND DESSERT BS4 TRAY(1*1000PCS)", "PCS", "Containers & Trays" },
            new String[]{ "272", "TECH GENERIC PIZZA BOX LARGE(1*100PCS)", "PCS", "Containers & Trays" },
            new String[]{ "273", "TECH GENERIC PIZZA BOX MEDIUM(1*100PCS)", "PCS", "Containers & Trays" },
            new String[]{ "274", "TECH GENERIC PIZZA BOX SMALL(1*100PCS)", "PCS", "Containers & Trays" },
            new String[]{ "275", "TECH PAPER POPCORN TURB RED LARGE(1*250PCS)", "PCS", "Containers & Trays" },
            new String[]{ "276", "TECH PAPER POPCORN TURB RED MEDIUM(1*500PCS)", "PCS", "Containers & Trays" },
            new String[]{ "277", "TECH PAPER POPCORN TURB RED SMALL(1*500PCS)", "PCS", "Containers & Trays" },
            new String[]{ "241", "TECH SHOT GLASS(25PKTS*40PCS)", "PKT", "General" },
            new String[]{ "286", "TECH SINGLE WALL 5OZ WHITE(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "287", "TECH SINGLE WALL 7OZ PPR CUP(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "153", "TECH STRAWBERRY 250ML PIERCED(1*500PCS)", "PCS", "Straws & Stirrers" },
            new String[]{ "107", "TECH STRAWBERRY 250ML UNPIERCED(1*500PCS)", "PCS", "Straws & Stirrers" },
            new String[]{ "154", "TEEPEE DS6 BLACK UNWRAPPED MILKSHAKE STRAW(1*30PKTS)", "PKT", "Straws & Stirrers" },
            new String[]{ "146", "TEEPEE DS7 BLACK WRAPPED MILKSHAKE STRAW(1*30PKTS)", "PKT", "Straws & Stirrers" },
            new String[]{ "185", "TEPEE DS3 CLR STRAWS (1*24PKTS)", "PKT", "Straws & Stirrers" },
            new String[]{ "140", "TEXPLAST #15  KIKUYU BAGS(40PKTS*50PCS)", "PKT", "Bags" },
            new String[]{ "132", "TEXPLAST V24 BOX BOTTOM SACKS(1*1000PCS)", "PCS", "Containers & Trays" },
            new String[]{ "131", "TEXPLAST V30 BOX BOTTOM SACKS(1*500PCS)", "PCS", "Containers & Trays" },
            new String[]{ "133", "TEXPLAST#22 KIKUYU(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "537", "TEXTPLAST 50KGS GREEN(1*1000PCS)", "PCS", "General" },
            new String[]{ "230", "TEXTPLAST 50KGS RED PANNEL SACK(1*1000PCS)", "PCS", "Bags" },
            new String[]{ "470", "TEXTPLAST 90KG BLUE PANEL SACKS (1X500)PCS", "PCS", "Bags" },
            new String[]{ "239", "TEXTPLAST 90KG RED PANEL SACKS (1X500)PCS", "PCS", "Bags" },
            new String[]{ "125", "TEXTPLAST V22 BOX BOTTOM SACKS(1*1000PCS)", "PCS", "Containers & Trays" },
            new String[]{ "213", "THANK YOU LARGE SMART BAGS(1*25PCS)", "PCS", "Bags" },
            new String[]{ "223", "THANK YOU SMALL SMART BAG", "PCS", "Bags" },
            new String[]{ "440", "THERMO 5*7 1000ML CLR PUNNET (35PKTS*10PCS)", "PKT", "Containers & Trays" },
            new String[]{ "437", "THERMO 5*7 TRAY (60PKTS*25PCS)", "PKT", "Containers & Trays" }
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
        log.info("ProductSeeder9: saved {} products", batch.size());
    }
}