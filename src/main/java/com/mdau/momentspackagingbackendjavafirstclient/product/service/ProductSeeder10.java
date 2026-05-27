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
 * Product seeder part 10 of 10 — seeds products 370 to 406.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(19)
@RequiredArgsConstructor
public class ProductSeeder10 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "438", "THERMO 6*8 TRAY (60PKTS*25PCS)", "PKT", "Containers & Trays" },
            new String[]{ "442", "THERMO DISPOSABLE 11\" WHITE PLAIN PLATE(20PKTS*25PCS)", "PKT", "Tableware & Hygiene" },
            new String[]{ "114", "THERMO DISPOSABLE 8\" WHITE 3PART PLATE(40PKTS*25PCS)", "PKT", "Tableware & Hygiene" },
            new String[]{ "149", "THERMO DISPOSABLE 8\" WHITE PLAIN PLATE(40PKTS*25PCS)", "PKT", "Tableware & Hygiene" },
            new String[]{ "159", "THERMO DISPOSABLE FORKS WHITE(10PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "439", "THERMO FV2 WHITE TRAY (20PKTS*25PCS)", "PKT", "Containers & Trays" },
            new String[]{ "443", "THERMO TABLE SPOONS (10PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "441", "THERMO TEA SPOONS (20PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "243", "TOOTHPICK JARS(1*40PKTS)", "PKT", "Cutlery" },
            new String[]{ "530", "TOPS BLACK STRAWS (150PCS*24)", "PKT", "Straws & Stirrers" },
            new String[]{ "238", "TWINE 1PLY (10PCS*6PKTS) KAMBA", "PCS", "Miscellaneous" },
            new String[]{ "415", "VELVEX KITCHEN TOWEL(1*48PCS)", "PKT", "Tableware & Hygiene" },
            new String[]{ "500", "WACO CLR ZIPLOCK BAGS 1KG(20PCS)", "PKT", "Bags" },
            new String[]{ "499", "WACO CLR ZIPLOCK BAGS 2KG(15PCS)", "PKT", "Bags" },
            new String[]{ "501", "WACO CLR ZIPLOCK BAGS HALF KG(25PCS)", "PKT", "Bags" },
            new String[]{ "148", "XPRO 120/250ML CLEAR CONT(15PKTS*40PCS)", "PKT", "Containers & Trays" },
            new String[]{ "398", "XPRO 120/250ML CLR  CONT +LIDS(40PKTS*10PC)", "PKT", "Cups & Lids" },
            new String[]{ "397", "XPRO 120/500ML CLR  CONT +LIDS(40PKTS*10PCS)", "PKT", "Cups & Lids" },
            new String[]{ "113", "XPRO 120/500ML CLR  CONT(15PKTS*40PCS)", "PKT", "Containers & Trays" },
            new String[]{ "130", "XPRO 120MM FLAT LID(15PKTS*40PCS)", "PKT", "Cups & Lids" },
            new String[]{ "242", "XPRO 4OZ CLR CUPS(35PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "110", "XPRO 4OZ WHITE CUP(35PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "207", "XPRO 5*7 TRAY (50PKTS*25PCS)", "PKT", "Containers & Trays" },
            new String[]{ "396", "XPRO 5OZ CLR CUPS(35PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "150", "XPRO 6OZ CLEAR(40PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "139", "XPRO 72/200ML CLEAR CUP(50PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "121", "XPRO 72/50ML CLR CONT(50PKTS*50PCS)", "PKT", "Containers & Trays" },
            new String[]{ "122", "XPRO 72/75 CLR CONT(50PKTS*50PCS)", "PKT", "Containers & Trays" },
            new String[]{ "120", "XPRO 72MM FLAT LID(50PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "186", "XPRO 82/250ML CLEAR CUPS(50PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "147", "XPRO 95/500 CLEAR CUP(40PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "142", "ZARA SEVIETTES(1*18PCS)", "PCS", "Tableware & Hygiene" },
            new String[]{ "509", "ZESTA ICING SUGAR (500GMX24)", "GRMS", "Food & Condiments" },
            new String[]{ "490", "ZIPPER BAGS", "PKT", "Bags" },
            new String[]{ "233", "ZURI NO.1(40PKTS*50PCS)", "PKT", "General" },
            new String[]{ "232", "ZURI NO.1/2(50PKTS*50PCS)", "PKT", "General" },
            new String[]{ "234", "ZURI NO.2(50PKTS*50PCS)", "PKT", "General" }
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
        log.info("ProductSeeder10: saved {} products", batch.size());
    }
}