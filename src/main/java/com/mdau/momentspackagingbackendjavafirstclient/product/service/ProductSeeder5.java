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
 * Product seeder part 5 of 10 — seeds products 165 to 205.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(14)
@RequiredArgsConstructor
public class ProductSeeder5 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "211", "MOONRAY 12OZ SINGLE WALL WHITE(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "209", "MOONRAY 12OZ SLEEVES(40PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "210", "MOONRAY 8OZ SLEEVES(40PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "119", "MOONRAY NITRILE BLACK GLOVES(10PKTS*100PCS)", "PKT", "Hygiene & PPE" },
            new String[]{ "134", "MOONRAY NITRILE BLUE GLOVES(10PKTS*100PCS)", "PKT", "Hygiene & PPE" },
            new String[]{ "202", "MR.TOOTHPICK (1*10PKTS)", "PKT", "Cutlery" },
            new String[]{ "167", "MREMBO #20 RED NETS(100PKTS*50PCS)", "PKT", "General" },
            new String[]{ "453", "NET FACE MASK BLACK  3PLY(80PKTS*50PCS)", "PKT", "Hygiene & PPE" },
            new String[]{ "454", "NET FACE MASK BLUE 3PLY(80PKTS*50PCS)", "PKT", "Hygiene & PPE" },
            new String[]{ "448", "NET WIRE (1*5PCS)", "PKT", "Miscellaneous" },
            new String[]{ "473", "NEWDAWN DISPOSABLE FORKS (10PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "472", "NEWDAWN TABLE SPOONS (10PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "392", "NEZO   30*30 CLING FILM(1*24PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "391", "NEZO 30*300 CLIMG FILM(1*12PCS)", "PCS", "General" },
            new String[]{ "393", "NEZO 45*5 ALLUMINIUMNFOIL(1*36PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "137", "NICE 5*8 NON WOOVEN BAGS(100PKTS*50PCS)", "PKT", "Bags" },
            new String[]{ "136", "NICE 6*9 NON WOOVEN BAGS(100PKTS*50PCS)", "PKT", "Bags" },
            new String[]{ "102", "NICE 7*12NON WOOVEN BAGS(100PKTS*50PCS)", "PKT", "Bags" },
            new String[]{ "138", "NICE 9*15 NON WOOVEN BAGS(60PKTS*50PCS)", "PKT", "Bags" },
            new String[]{ "409", "NIGERIAN BAGS F1(1*120PCS)", "PCS", "Bags" },
            new String[]{ "408", "NIGERIAN BAGS F2(1*120PCS)", "PCS", "Bags" },
            new String[]{ "208", "NIGERIAN BAGS F3(1*120PCS)", "PCS", "Bags" },
            new String[]{ "410", "NIGERIAN BAGS F4(1*120PCS)", "PCS", "Bags" },
            new String[]{ "411", "NIGERIAN BAGS F5(1*120PCS)", "PCS", "Bags" },
            new String[]{ "555", "NUTRAMEAL SUGAR 1*5KGS", "BAG", "Food & Condiments" },
            new String[]{ "554", "NUTTELLA FERRERO HAZELNUT SPREAD 350G", "PCS", "Food & Condiments" },
            new String[]{ "522", "OAK NATURAL HONEY ( 1KG)", "KGS", "Food & Condiments" },
            new String[]{ "256", "PAN  12*20 KRAFT ZIPLOCK(1*500PCS)", "PCS", "Bags" },
            new String[]{ "255", "PAN 10*15 KRAFT ZIPLOCK(1*500PCS)", "PCS", "Bags" },
            new String[]{ "199", "PAN 1000ML  KRAFT RECTANGLE(6PKTS*50PCS)", "PCS", "General" },
            new String[]{ "196", "PAN 1000ML KRAFT ROUND SALAD BOWL(6PKTS*50PCS)", "PCS", "General" },
            new String[]{ "422", "PAN 15*22 KRAFT ZIPLOCK(1*500PCS)", "PCS", "Bags" },
            new String[]{ "419", "PAN 18*26 KRAFT ZIPLOCK(1*500PCS)", "PCS", "Bags" },
            new String[]{ "420", "PAN 20*30 KRAFT ZIPLOCK(1*500PCS)", "PCS", "Bags" },
            new String[]{ "253", "PAN 20CC PET CONT(20PKTS*100PCS)", "PKT", "Containers & Trays" },
            new String[]{ "421", "PAN 23*33 KRAFT ZIPLOCK(1*500PCS)", "PCS", "Bags" },
            new String[]{ "200", "PAN 500ML KRAFT RECTANGLE(6PKTS*50PCS)", "PCS", "General" },
            new String[]{ "198", "PAN 500ML KRAFT ROUND SALAD BOWL(6PKTS*50PCS)", "PCS", "General" },
            new String[]{ "201", "PAN 750ML  KRAFT RECTANGLE(6PKTS*50PCS)", "PCS", "General" },
            new String[]{ "197", "PAN 750ML KRAFTROUND SALAD BOWL(6PKTS*50PCS)", "PCS", "General" },
            new String[]{ "190", "PAN 890CC ALLUMINIUM CONT(1*1000PCS)", "PCS", "Containers & Trays" }
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
        log.info("ProductSeeder5: saved {} products", batch.size());
    }
}