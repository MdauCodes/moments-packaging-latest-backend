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
 * Product seeder part 2 of 10 — seeds products 42 to 82.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(11)
@RequiredArgsConstructor
public class ProductSeeder2 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "360", "DUNE NO.5 KHAKI(1*15KGS)", "KGS", "General" },
            new String[]{ "495", "EAGLE 57*40MM THEMAL ROLL (1*100PCS)", "PCS", "General" },
            new String[]{ "357", "ECO HAPA 12 OZ BLACK RIPPLE CUP(20PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "358", "ECO HAPA 12OZ LIDS BLACK(20PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "259", "ECO HAPA 30*1500 JUMBO CLING FILM(1PC)", "PCS", "Wrapping & Foil" },
            new String[]{ "417", "ECO HAPA 30*30 ALLUMINIUM FOIL(1*15PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "109", "ECO HAPA 30*30 CLING FILM(1*36PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "127", "ECO HAPA 30*300 CLING FILM(1*15PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "418", "ECO HAPA 45*30 ALLUMINIUM FOIL(1*12PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "157", "ECO HAPA 45*5 ALLUMINIUM FOIL(1*36PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "416", "ECO HAPA 45*60 ALLUMINIUM FOIL(1*12PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "111", "ECO HAPA 45*90 ALLUMINIUM FOIL(1*12PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "497", "ECO HAPA 57*40 MM THERMAL ROLL(1*100PCS)", "PCS", "Thermal Rolls" },
            new String[]{ "496", "ECO HAPA 8 OZ BLACK  RIPPLE CUP(20PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "498", "ECO HAPA 8OZ BLACK LIDS(20PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "430", "ECO MEDICAL BAGS 2.5*5(10PKTS*100PCS)", "PKT", "Bags" },
            new String[]{ "429", "ECOLAB  CHERRY SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "433", "ECOLAB  GIRRAFFE SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "432", "ECOLAB  HUMMING BIRD SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "431", "ECOLAB  MATATU SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "435", "ECOLAB  RED ROSE SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "434", "ECOLAB  SAFARI SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "383", "ECOLAB # 15RED(40PKTS*50PCS)", "PKT", "General" },
            new String[]{ "382", "ECOLAB #15BLUE(40PKTS*50PCS)", "PKT", "General" },
            new String[]{ "381", "ECOLAB #15GREEN(40PKTS*50PCS)", "PKT", "General" },
            new String[]{ "388", "ECOLAB #22 BLUE(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "387", "ECOLAB #22 GREEN(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "389", "ECOLAB #22 RED(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "385", "ECOLAB #25 BLUE(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "386", "ECOLAB #25 GREEN(10PKTS*50PCS)", "PKT", "General" },
            new String[]{ "384", "ECOLAB #25 RED(10PKTS*50PCS)", "PKT", "General" },
            new String[]{ "390", "ECOLAB #25 SUPREME RED(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "426", "ECOLAB GUCCI  SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "428", "ECOLAB KITENGE SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "425", "ECOLAB MREMBO SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "427", "ECOLAB PREMIER LEAGUE SMART BAGS(1*250PCS)", "PCS", "Bags" },
            new String[]{ "467", "ECSTACY 2OZ KRAFT SQUARE SAUCE CUP+LID(1*50PCS)", "PCS", "Cups & Lids" },
            new String[]{ "466", "ECSTACY 4OZ KRAFT SQUARE SAUCE CUP+LID(1*50PCS)", "PCS", "Cups & Lids" },
            new String[]{ "469", "ECSTACY BUBBLE TEA STRAWS COLOURED (1*100PCS)", "PKT", "Straws & Stirrers" },
            new String[]{ "468", "ECSTACY CORR WRAPS BOX 26CM*10CM(1*50PCS) SHAWARMA", "PCS", "Containers & Trays" },
            new String[]{ "465", "ECSTACY FLEXIBLE STRAWS COLOURED(24PKTS*100PCS)", "PKT", "Straws & Stirrers" }
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
        log.info("ProductSeeder2: saved {} products", batch.size());
    }
}