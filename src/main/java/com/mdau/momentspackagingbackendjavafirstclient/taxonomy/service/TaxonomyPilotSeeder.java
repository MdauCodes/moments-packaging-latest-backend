package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Category;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Segment;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Subcategory;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.CategoryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.SegmentRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Phase 0/2 pilot: seeds one real Segment -> Category -> Subcategory slice
 * (Food Packaging / Bags / Boxes &amp; Trays) and migrates a small subset of
 * existing products from their legacy {@link Product#getCategory()} string
 * into the new hierarchy, so admin CRUD + bulk-classify can be proven out
 * on real data before the full ~500-product rollout. Every remaining
 * product keeps its legacy category string untouched and simply has a
 * null subcategory until reassigned.
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class TaxonomyPilotSeeder implements ApplicationRunner {

    private static final String PILOT_SEGMENT_SLUG = "food-packaging";

    /**
     * Max products migrated per subcategory. Kept deliberately tiny — just enough (1 per
     * subcategory) so the hierarchy has a real example to look at, while leaving the vast
     * majority of matching products unclassified. That's the point of the pilot: staff need
     * an actual backlog to practice bulk-classify on, not a catalog we've already done for them.
     */
    private static final int MAX_PRODUCTS_PER_SUBCATEGORY = 1;

    private final SegmentRepository segmentRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (segmentRepository.findBySlug(PILOT_SEGMENT_SLUG).isPresent()) {
            log.info("Taxonomy pilot data already seeded, skipping.");
            return;
        }

        Segment segment = segmentRepository.save(Segment.builder()
                .name("Food Packaging")
                .slug(PILOT_SEGMENT_SLUG)
                .description("Packaging used to hold, wrap, or serve food and drink.")
                .sortOrder(1)
                .build());

        Category bags = categoryRepository.save(Category.builder()
                .segment(segment)
                .name("Bags")
                .slug("bags")
                .description("Carrier and packing bags.")
                .sortOrder(1)
                .build());

        Category boxesAndTrays = categoryRepository.save(Category.builder()
                .segment(segment)
                .name("Boxes & Trays")
                .slug("boxes-and-trays")
                .description("Rigid boxes and trays for food.")
                .sortOrder(2)
                .build());

        List<Subcategory> bagSubcategories = subcategoryRepository.saveAll(List.of(
                Subcategory.builder().category(bags).name("T-shirt Bags").slug("t-shirt-bags").sortOrder(1).build(),
                Subcategory.builder().category(bags).name("Draw-string Bags").slug("draw-string-bags").sortOrder(2).build(),
                Subcategory.builder().category(bags).name("Zip-lock Bags").slug("zip-lock-bags").sortOrder(3).build()
        ));

        List<Subcategory> boxSubcategories = subcategoryRepository.saveAll(List.of(
                Subcategory.builder().category(boxesAndTrays).name("Pizza Boxes").slug("pizza-boxes").sortOrder(1).build(),
                Subcategory.builder().category(boxesAndTrays).name("Cake Boxes").slug("cake-boxes").sortOrder(2).build(),
                Subcategory.builder().category(boxesAndTrays).name("Food Trays").slug("food-trays").sortOrder(3).build()
        ));

        log.info("Seeded pilot taxonomy: 1 segment, 2 categories, {} subcategories.",
                bagSubcategories.size() + boxSubcategories.size());

        int migrated = migrateSubset("Bags", bagSubcategories)
                + migrateSubset("Containers & Trays", boxSubcategories);

        log.info("Taxonomy pilot migration: reassigned {} existing products into the new hierarchy.", migrated);
    }

    /**
     * Pulls a small, round-robin-distributed subset of products still on the given legacy
     * category string and assigns each to one of the target pilot subcategories.
     */
    private int migrateSubset(String legacyCategory, List<Subcategory> targets) {
        List<Product> candidates = productRepository.findByCategoryAndDeletedFalseAndSubcategoryIsNull(legacyCategory);
        int limit = Math.min(candidates.size(), targets.size() * MAX_PRODUCTS_PER_SUBCATEGORY);

        for (int i = 0; i < limit; i++) {
            Product product = candidates.get(i);
            Subcategory target = targets.get(i % targets.size());
            product.setSubcategory(target);
        }
        if (limit > 0) {
            productRepository.saveAll(candidates.subList(0, limit));
        }
        return limit;
    }
}
