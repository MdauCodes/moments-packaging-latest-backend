package com.mdau.momentspackagingbackendjavafirstclient.industry.service;

import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.industry.repository.IndustryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class IndustrySeeder implements ApplicationRunner {

    private final IndustryRepository industryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (industryRepository.count() > 0) {
            log.info("Industries already seeded, skipping.");
            return;
        }

        List<Industry> industries = List.of(
            Industry.builder()
                .name("Food & Beverage")
                .slug("food-and-beverage")
                .description("Packaging solutions for food and beverage products including bottles, jars, pouches and containers.")
                .build(),
            Industry.builder()
                .name("Retail & E-Commerce")
                .slug("retail-and-ecommerce")
                .description("Custom packaging for retail stores and online businesses including boxes, mailers and bags.")
                .build(),
            Industry.builder()
                .name("Health & Beauty")
                .slug("health-and-beauty")
                .description("Premium packaging for cosmetics, skincare, pharmaceuticals and personal care products.")
                .build(),
            Industry.builder()
                .name("Agriculture")
                .slug("agriculture")
                .description("Durable packaging for agricultural products including grains, produce and farm supplies.")
                .build(),
            Industry.builder()
                .name("Manufacturing")
                .slug("manufacturing")
                .description("Industrial packaging solutions for manufactured goods, components and equipment.")
                .build(),
            Industry.builder()
                .name("Hospitality")
                .slug("hospitality")
                .description("Branded packaging for hotels, restaurants, cafes and event catering businesses.")
                .build(),
            Industry.builder()
                .name("Fashion & Apparel")
                .slug("fashion-and-apparel")
                .description("Stylish packaging for clothing, accessories, shoes and fashion retail brands.")
                .build(),
            Industry.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Protective and branded packaging for electronic devices, gadgets and accessories.")
                .build()
        );

        industryRepository.saveAll(industries);
        log.info("Seeded {} industries successfully.", industries.size());
    }
}