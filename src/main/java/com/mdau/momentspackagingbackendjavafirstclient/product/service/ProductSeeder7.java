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
 * Product seeder part 7 of 10 — seeds products 247 to 287.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(16)
@RequiredArgsConstructor
public class ProductSeeder7 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "436", "PYRAMID #15 ARSORTED(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "413", "PYRAMID #22 ARSORTED(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "474", "PYRAMID #25 ARSORTED(20PKTS*50PCS)", "PKT", "General" },
            new String[]{ "254", "RASMY 45*90 ALL FOIL(1*12PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "160", "RASMY ADHESIVE TAPE 60MM*100(12ROLLS*5PCS)", "PCS", "Tapes" },
            new String[]{ "161", "RASMY MASKING TAPE 8UPS(24PKTS*8PCS)", "PKT", "Tapes" },
            new String[]{ "226", "RED SMARTBAG", "PCS", "Bags" },
            new String[]{ "515", "RED TABASCO (60ML*48)", "ML", "Food & Condiments" },
            new String[]{ "519", "ROSEMARY LEAVES (1*KGS)", "KGS", "Food & Condiments" },
            new String[]{ "523", "SAF INSTANT YEAST( 500GM*20)", "GRMS", "Food & Condiments" },
            new String[]{ "477", "SAWA  NO.1/4 (50PCS*120PKTS)", "PKT", "General" },
            new String[]{ "237", "SAWA NO. 5(40PKTS*50PCS)", "PKT", "General" },
            new String[]{ "235", "SAWA NO.3(56PKTS*50PCS)", "PKT", "General" },
            new String[]{ "236", "SAWA NO.4(48PKTS*50PCS)", "PKT", "General" },
            new String[]{ "217", "SHINNY BAG LARGE(1*25PCS)", "PCS", "Bags" },
            new String[]{ "216", "SHINNY BAG MEDIUM(1*25PCS)", "PCS", "Bags" },
            new String[]{ "215", "SHINNY BAG SMALL(1*25PCS)", "PCS", "Bags" },
            new String[]{ "447", "SKEWERS (1*100PCS)", "PKT", "Miscellaneous" },
            new String[]{ "222", "SMALL CHINA SMART BAG", "PCS", "Bags" },
            new String[]{ "524", "SPICE BLACK PEPPER CORN (10KG)", "KGS", "Food & Condiments" },
            new String[]{ "525", "SPICE BLACK PEPPER POWDER (10KG)", "KGS", "Food & Condiments" },
            new String[]{ "526", "SPICE PINE NUTS (KG)", "KGS", "Food & Condiments" },
            new String[]{ "504", "SPICE T/HEAT OREGANO LEAVES( 20GMX6)", "PKT", "Food & Condiments" },
            new String[]{ "292", "STAT 1200CC ALLUMINIUM CONT(1*400PCS)", "PCS", "Containers & Trays" },
            new String[]{ "293", "STAT 1900CC ALLUMINIUM CONT(1*300PCS)", "PCS", "Containers & Trays" },
            new String[]{ "231", "STAT 30*15 CLING FILM(1*24PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "101", "STAT 30*30 CLING FILM(1*24PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "117", "STAT 30*300 CLING FILM BLADE(1*12PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "141", "STAT 30*300 CLING FILM NO BLADE(1*12PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "104", "STAT 45*5 ALLUMINIUM  FOIL(1*24PCS)", "PCS", "Wrapping & Foil" },
            new String[]{ "481", "SUMO  CLR TAPE 12UPS(24PKTS*12PCS)", "PKT", "Tapes" },
            new String[]{ "480", "SUMO  CLR TAPE 8UPS(24PKTS*8PCS)", "PKT", "Tapes" },
            new String[]{ "479", "SUMO  MASKING TAPE 3UPS(24PKTS*3PCS)", "PKT", "Tapes" },
            new String[]{ "478", "SUMO  MASKING TAPE 8UPS(24PKTS*8PCS)", "PKT", "Tapes" },
            new String[]{ "118", "SUNFRESH TOMATO SAUCE SATCHETS(1*300PCS)", "CTN", "Food & Condiments" },
            new String[]{ "371", "TECH  120MM FLAT LID(15PKTS*40PCS)", "PKT", "Cups & Lids" },
            new String[]{ "450", "TECH  12OZ SINGLE WALL WHITE(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "288", "TECH  FOOD AND DESSERT BS3 TRAY(1*1000PCS)", "PCS", "Containers & Trays" },
            new String[]{ "112", "TECH 100 DOME LIDS(50PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "155", "TECH 100/500 CLEAR CUP(50PKTS*25PCS)", "PKT", "Cups & Lids" },
            new String[]{ "106", "TECH 1000CC CLR PUNNETS (10PKTS*20PCS)", "PKT", "Containers & Trays" }
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
        log.info("ProductSeeder7: saved {} products", batch.size());
    }
}