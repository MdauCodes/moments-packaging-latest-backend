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
 * Product seeder part 8 of 10 — seeds products 288 to 328.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(17)
@RequiredArgsConstructor
public class ProductSeeder8 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "378", "TECH 1000ML CLEAR HONEY JAR(1*50PCS)", "PCS", "Jars & Bottles" },
            new String[]{ "544", "TECH 120/1000ML CONTAINER(15PKTS*40PCS)", "PKT", "Containers & Trays" },
            new String[]{ "369", "TECH 120/250ML CLEAR CONT(15PKTS*40PCS)", "PKT", "Containers & Trays" },
            new String[]{ "370", "TECH 120/500ML CLEAR CONT(15PKTS*40PCS)", "PKT", "Containers & Trays" },
            new String[]{ "206", "TECH 12OZ BLACK LIDS(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "372", "TECH 12OZ SINGLE WALL PRINTED(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "451", "TECH 12OZ WHITE  LIDS(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "376", "TECH 250ML CLEAR HONEY JAR(1*100PCS)", "PCS", "Jars & Bottles" },
            new String[]{ "546", "TECH 3OZCUPS WHITE(50PCS*50PKTS)", "PKT", "Cups & Lids" },
            new String[]{ "377", "TECH 400ML CLEAR HONEY JAR(1*100PCS)", "PCS", "Jars & Bottles" },
            new String[]{ "283", "TECH 5*10 FISH PUNNET BLACK(1*200PCS)", "PCS", "Containers & Trays" },
            new String[]{ "290", "TECH 5*5*3  SINGLE WALL BURGER BOX(1*200PCS)", "PCS", "Cups & Lids" },
            new String[]{ "366", "TECH 5*7  500ML BLACK PUNNETS NO LIDS(1*250PCS)", "PCS", "Cups & Lids" },
            new String[]{ "144", "TECH 5*7  500ML PUNNETS(35PKTS*10PCS)", "PKT", "Containers & Trays" },
            new String[]{ "349", "TECH 5*7  PUNNET CLR LIDS(1*250PCS)", "PCS", "Cups & Lids" },
            new String[]{ "368", "TECH 5*7 1000ML CLR PUNNETS NO LIDS(1*250PCS)", "PCS", "Cups & Lids" },
            new String[]{ "129", "TECH 5*7 1000ML PUNNETS(35PKTS*10PCS)", "PKT", "Containers & Trays" },
            new String[]{ "367", "TECH 5*7 500ML CLR PUNNETS NO LIDS(1*250PCS)", "PCS", "Cups & Lids" },
            new String[]{ "348", "TECH 5*71000ML BLACK PUNNETS NO LIDS(1*250PCS)", "PCS", "Cups & Lids" },
            new String[]{ "135", "TECH 500CC CLR PUNNETS(12PKTS*20PCS)", "PKT", "Containers & Trays" },
            new String[]{ "257", "TECH 52/30MM CLR CONT(20PKTS*250PCS)", "PKT", "Containers & Trays" },
            new String[]{ "258", "TECH 52MM LID(20PKTS*250PCS)", "PKT", "Cups & Lids" },
            new String[]{ "375", "TECH 5OZ CLR CUPS(35PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "380", "TECH 63MM  HONEY JAR CAPS YELLOW(1*1000PCS)", "PCS", "Jars & Bottles" },
            new String[]{ "145", "TECH 72/100 CLR CONT(50PKTS*50PCS)", "PKT", "Containers & Trays" },
            new String[]{ "124", "TECH 72/200ML CLR CUP (50PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "156", "TECH 750CC CLR PUNNETS(12PKTS*20PCS)", "PKT", "Containers & Trays" },
            new String[]{ "542", "TECH 82/120MM CONTAINER(50PKT*50PCS)", "PKT", "Containers & Trays" },
            new String[]{ "543", "TECH 82/150MM CONTAINER(50PKT*50PCS)", "PKT", "Containers & Trays" },
            new String[]{ "152", "TECH 82/250 CLEAR CUP(50PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "105", "TECH 82/300 CLEAR CUP(50PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "103", "TECH 82DOME LIDS(50PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "545", "TECH 82MM PET FLAT  LID(50PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "205", "TECH 8OZ LIDS(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "204", "TECH 8OZ SINGLE WALL PRINTED(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "547", "TECH 9 INCH 3 PART PLATE WHITE(20PKTS*25PCS)", "PKT", "Tableware & Hygiene" },
            new String[]{ "291", "TECH 9*5*3  DOUBLE WALL BURGER BOX(1*200PCS)", "PCS", "Cups & Lids" },
            new String[]{ "108", "TECH 94/200 CONTAINER(25PKTS*50PCS)", "PKT", "Containers & Trays" },
            new String[]{ "449", "TECH 94/400 CLR CUP(25PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "452", "TECH 94MM DOME LID(50PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "203", "TECH 94MM FLAT UNPIERCED LIDS (25PKTS*100PCS)", "PKT", "Cups & Lids" }
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
        log.info("ProductSeeder8: saved {} products", batch.size());
    }
}