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
 * Product seeder part 4 of 10 — seeds products 124 to 164.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(13)
@RequiredArgsConstructor
public class ProductSeeder4 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "168", "MAFUCO NO.1  KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "175", "MAFUCO NO.10  KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "176", "MAFUCO NO.12  KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "177", "MAFUCO NO.14 KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "178", "MAFUCO NO.16 KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "169", "MAFUCO NO.2  KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "179", "MAFUCO NO.20  KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "180", "MAFUCO NO.25 KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "170", "MAFUCO NO.3 KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "171", "MAFUCO NO.4  KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "172", "MAFUCO NO.5 KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "173", "MAFUCO NO.6 KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "174", "MAFUCO NO.8  KHAKI(1*10KGS)", "KGS", "General" },
            new String[]{ "503", "MAFUCO WHITE HANDLED TWISTED BAGS SMALL(1*50PCS)", "PCS", "Bags" },
            new String[]{ "269", "MANILA LARGE", "PKT", "General" },
            new String[]{ "270", "MANILA MEDIUM SIZE", "PKT", "General" },
            new String[]{ "271", "MANILA SMALL", "PKT", "General" },
            new String[]{ "268", "MANILA XL", "PKT", "General" },
            new String[]{ "506", "MEDJOOL DATES 5KGX1", "KGS", "Food & Condiments" },
            new String[]{ "162", "MEGA #15 BLUE (40PKTS*50PCS)", "PKT", "General" },
            new String[]{ "246", "MEGA #15GREEN(40PKTS*50PCS)", "PKT", "General" },
            new String[]{ "267", "MEGA #22 ARSORTED(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "164", "MEGA #22BLUE(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "245", "MEGA #22GREEN(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "244", "MEGA #22RED(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "165", "MEGA #25 BLUE(10PKTS*50PCS)", "PKT", "General" },
            new String[]{ "355", "MEGA #25 CLASSIC BLUE(10PKTS*50PCS)", "PKT", "General" },
            new String[]{ "356", "MEGA #25 CLASSIC GREEN(10PKTS*50PCS)", "PKT", "General" },
            new String[]{ "354", "MEGA #25 CLASSIC RED(10PKTS*50PCS)", "PKT", "General" },
            new String[]{ "166", "MEGA #25 RED(10PKTS*50PCS)", "PKT", "General" },
            new String[]{ "163", "MEGA#15 RED(40PKTS*50PCS)", "PKT", "General" },
            new String[]{ "143", "MIDATEX BLUE HAIRNET(20PKTS*100PCS)", "PKT", "Hygiene & PPE" },
            new String[]{ "123", "MIDATEX DISPOSABLE CLEAR GLOVES(100PKTS*100PCS)", "PKT", "Hygiene & PPE" },
            new String[]{ "115", "MIDATEX HAIRNETS WHITE(20PKTS*100PCS)", "PKT", "Hygiene & PPE" },
            new String[]{ "261", "MOH 48*100 YELLOWISH TAPE(1*36PCS)", "PCS", "Tapes" },
            new String[]{ "260", "MOH 48×100 TAN YELLOW TAPE(1*36PCS)", "PCS", "Tapes" },
            new String[]{ "265", "MOH 60*100 TAN YELLOW TAPE(1*36PCS)", "PCS", "Tapes" },
            new String[]{ "263", "MOH 60*200 TAN YELLOW TAPE(1*30PCS)", "PCS", "Tapes" },
            new String[]{ "264", "MOH 60*200 YELLOWISH TAPE(1*30PCS)", "PCS", "Tapes" },
            new String[]{ "262", "MOH 72*250 YELLOWISH TAPE(1*24PCS)", "PCS", "Tapes" },
            new String[]{ "212", "MOONRAY 12OZ SINGLE WALL PRINTED(20PKTS*50PCS)", "PKT", "Cups & Lids" }
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
        log.info("ProductSeeder4: saved {} products", batch.size());
    }
}