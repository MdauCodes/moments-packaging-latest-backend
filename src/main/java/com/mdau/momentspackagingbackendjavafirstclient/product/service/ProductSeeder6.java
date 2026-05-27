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
 * Product seeder part 6 of 10 — seeds products 206 to 246.
 * Skips if products already exist (handled by ProductSeeder1 guard).
 */
@Slf4j
@Component
@Order(15)
@RequiredArgsConstructor
public class ProductSeeder6 implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSeederHelper seederHelper;

    private static final String[][] PRODUCTS = {
            new String[]{ "191", "PAN 890CC LIDS (1*1000PCS)", "PCS", "Cups & Lids" },
            new String[]{ "487", "PAN BAMBOO PADDLE PICK 12CM(1*100PCS)", "PKT", "Miscellaneous" },
            new String[]{ "488", "PAN BAMBOO PADDLE PICK 18CM(1*100PCS)", "PKT", "Miscellaneous" },
            new String[]{ "551", "PAN BLACK MICROWAVE CONT 24OZ(1*150PCS)", "PCS", "Cups & Lids" },
            new String[]{ "266", "PAN BLACK MICROWAVE CONT 2PART(1*200PCS)", "PCS", "Containers & Trays" },
            new String[]{ "552", "PAN BLACK MICROWAVE CONT 32OZ(1*150PCS)", "PCS", "Cups & Lids" },
            new String[]{ "553", "PAN BLACK MICROWAVE CONT 38OZ(1*150PCS)", "PCS", "Cups & Lids" },
            new String[]{ "494", "PAN BLACK MICROWAVE CONT 3PART(1*200PCS)", "PCS", "Containers & Trays" },
            new String[]{ "549", "PAN BMBOO HEART KNOT PICKS 12CM(1*100PC)", "PKT", "Miscellaneous" },
            new String[]{ "424", "PAN COLOURED PLASITIC  ICE CREAM SPOONS(100PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "493", "PAN ICE CREAM 12OZ PRINTED(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "489", "PAN ICE CREAM 4OZ PRINTED(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "492", "PAN ICE CREAM 8OZ PRINTED(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "550", "PAN ICE CREAM CUPS PRINTED 6OZ(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "548", "PAN ICE CREAM WOODEN SPOON STICKS WRAPPED(1*50PCS)", "PKT", "Cutlery" },
            new String[]{ "446", "PAN KRAFT T.AWAY DELI BOX 2PART(1*50PCS)", "PCS", "Containers & Trays" },
            new String[]{ "188", "PAN KTAFT T.AWAY DELI BOX LARGE BROWN(1*50PCS)", "PCS", "Containers & Trays" },
            new String[]{ "187", "PAN KTAFT T.AWAY DELI BOX MEDIUM BROWN(1*50PCS)", "PCS", "Containers & Trays" },
            new String[]{ "189", "PAN KTAFT T.AWAY DELI BOX SMALL BROWN(1*50PCS)", "PCS", "Containers & Trays" },
            new String[]{ "192", "PAN NO.1 BRWN HANDLED BAGS(1*100PCS)", "PCS", "Bags" },
            new String[]{ "193", "PAN NO.2 BRWN HANDLED BAGS(1*100PCS)", "PCS", "Bags" },
            new String[]{ "194", "PAN NO.3 BRWN HANDLED BAGS(1*100PCS)", "PCS", "Bags" },
            new String[]{ "195", "PAN NO.4 BRWN HANDLED BAGS(1*100PCS)", "PCS", "Bags" },
            new String[]{ "485", "PAN PET CUP U SHAPED 12OZ(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "484", "PAN PET CUP U SHAPED 16OZ(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "486", "PAN PET FLAT 12OZ/16OZ  CLR LIDS(20PKTS*50PCS)", "PKT", "Cups & Lids" },
            new String[]{ "423", "PAN TEA SPOON (40PKTS*50PCS)", "PKT", "Cutlery" },
            new String[]{ "483", "PAN WOODEEN STIRRERS WRAPPED(1*500PCS)", "PKT", "Straws & Stirrers" },
            new String[]{ "491", "PAN WOODEN ICE CREAM STICKS SPOON UNWRAPPED(1*50PCS)", "PKT", "Cutlery" },
            new String[]{ "536", "PEMBE MAIZE FLOUR(2KG*12PCS)", "BALE", "Food & Condiments" },
            new String[]{ "510", "PEPTANG MARPLE SYRUP (500MLX12)", "ML", "Food & Condiments" },
            new String[]{ "502", "PLASITEC PEPPER SATCHETS(1*1000PCS)", "CTN", "Food & Condiments" },
            new String[]{ "296", "PLASITEC POLYPOTS(1*1000PCS)", "PCS", "Jars & Bottles" },
            new String[]{ "295", "PLASITEC SALT SATCHETS(1*1000PCS)", "CTN", "Food & Condiments" },
            new String[]{ "294", "PLASITEC SUGAR SATCHETS  WHITE(1*1000PCS)", "CTN", "Food & Condiments" },
            new String[]{ "394", "PRESSMASTER 15 INCH PIZZA BOXES(1*100PCS)", "PCS", "Containers & Trays" },
            new String[]{ "476", "PRIMARK DISPOSABLE FORKS WHITE(10PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "158", "PRIMARK DISPOSABLE SPOONS WHITE(10PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "412", "PRIMARK DISPOSABLE SPOONS WHITE(40PKTS*25PKTS)", "PKT", "Cutlery" },
            new String[]{ "395", "PRIMARK TEA SPOONS (10PKTS*100PCS)", "PKT", "Cutlery" },
            new String[]{ "521", "PRISTINE WHIPPING CREAM (1L*12)", "LITRES", "Food & Condiments" }
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
        log.info("ProductSeeder6: saved {} products", batch.size());
    }
}