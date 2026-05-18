package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductUom;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductUomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductUomSeeder implements ApplicationRunner {

    private final ProductUomRepository uomRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing("PIECE",  "Piece",  "A single individual unit of the product.", 0);
        seedIfMissing("PACKET", "Packet", "A packet containing multiple pieces.", 1);
        seedIfMissing("CARTON", "Carton", "A carton containing multiple packets.", 2);
        seedIfMissing("BALE",   "Bale",   "A bale — the largest default unit of measurement.", 3);
        log.info("UOM seeding complete.");
    }

    private void seedIfMissing(String code, String name, String description, int sortOrder) {
        if (!uomRepository.existsByCodeAndDeletedFalse(code)) {
            uomRepository.save(ProductUom.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .isDefault(true)
                    .sortOrder(sortOrder)
                    .deleted(false)
                    .build());
            log.info("Seeded UOM: {}", code);
        }
    }
}