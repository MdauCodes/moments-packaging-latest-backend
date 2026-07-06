package com.mdau.momentspackagingbackendjavafirstclient.tag.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.ProductSearchService;
import com.mdau.momentspackagingbackendjavafirstclient.tag.entity.Tag;
import com.mdau.momentspackagingbackendjavafirstclient.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bootstraps real Tag data so the storefront's "What do you need?" chips have
 * something backed by the database instead of staying empty until an admin
 * manually creates every tag by hand. Mirrors the frontend's GLOBAL_QUICK_FINDS
 * list (products.index.tsx) — same labels, same verified-against-real-inventory
 * search keywords — and auto-assigns each tag to whatever products the existing
 * search-scoring logic (ProductSearchService) actually matches for that keyword.
 * Runs once: skips entirely once any Tag exists, so it never fights with
 * admin-created/edited/deleted tags afterward.
 */
@Slf4j
@Component
@Order(42)
@RequiredArgsConstructor
public class TagSeeder implements ApplicationRunner {

    private final TagRepository tagRepository;
    private final ProductRepository productRepository;
    private final ProductSearchService productSearchService;

    private static final int MAX_PRODUCTS_PER_TAG = 200;

    private static final String[][] SEED_TAGS = {
        {"Hot drinks & coffee",        "hot cup"},
        {"Cold drinks & juice",        "cold cup"},
        {"Takeaway & delivery",        "takeaway box"},
        {"POS & receipt rolls",        "thermal roll"},
        {"Cutlery",                    "cutlery"},
        {"Straws & stirrers",          "straw"},
        {"Cling film & wrapping",      "cling film"},
        {"Shopping & boutique bags",   "twisted handle"},
        {"Fresh produce packs",        "punnet"},
        {"Gloves",                     "gloves"},
        {"Face masks",                 "face mask"},
        {"Jars & containers",          "jar"},
        {"Reusable tote bags",         "non-woven"},
        {"Hairnets",                   "hairnet"},
        {"Grain & bulk sacks",         "panel sack"},
        {"Produce net bags",           "net bag"},
        {"Honey jars",                 "honey jar"},
        {"Tapes & sealing",            "tape"},
        {"Stretch & pallet wrap",      "stretch wrap"},
        {"Foil & foil trays",          "aluminium foil"},
        {"Plates",                     "plates"},
        {"Bin liners",                 "garbage bag"},
        {"Gift & premium bags",        "millinary"},
        {"Farm & market sacks",        "khaki bags"},
        {"Printed smart bags",         "smart bags"},
        {"Brown handled bags",         "brown handled"},
        {"Ice cream cups",             "ice cream cups"},
        {"Wet wipes",                  "wet wipes"},
        {"Manila envelopes",           "manila envelope"},
        {"Foil tins",                  "foil tin"},
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (tagRepository.count() > 0) {
            log.info("Tags already seeded, skipping.");
            return;
        }

        int totalAssignments = 0;
        for (String[] entry : SEED_TAGS) {
            String label = entry[0];
            String keyword = entry[1];

            Tag tag = tagRepository.save(Tag.builder()
                    .name(label)
                    .slug(SlugUtil.toSlug(label))
                    .build());

            List<Product> matches = productSearchService.searchProductEntities(keyword, MAX_PRODUCTS_PER_TAG);
            for (Product product : matches) {
                product.getCuratedTags().add(tag);
            }
            if (!matches.isEmpty()) {
                productRepository.saveAll(matches);
            }
            totalAssignments += matches.size();
        }

        log.info("Seeded {} tags with {} total product assignments.", SEED_TAGS.length, totalAssignments);
    }
}
