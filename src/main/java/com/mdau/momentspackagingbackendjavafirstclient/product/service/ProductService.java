package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.industry.repository.IndustryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductPricingTierDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.PriceUnit;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductClick;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductPricingTier;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductUom;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductClickRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductPricingTierRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductUomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository            productRepository;
    private final ProductClickRepository       productClickRepository;
    private final ProductPricingTierRepository pricingTierRepository;
    private final IndustryRepository           industryRepository;
    private final ProductUomRepository         uomRepository;

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(UUID industryId, Boolean isDiscount,
                                        Boolean isNewArrival, Boolean isFastMoving,
                                        String category, Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), 100);
        Pageable capped = PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
        return productRepository
                .findAllWithFilters(industryId, isDiscount, isNewArrival, isFastMoving, category, capped)
                .map(p -> toDtoPublic(p));
    }

    @Cacheable("recommended-products")
    @Transactional(readOnly = true)
    public List<ProductDto> getRecommended() {
        return productRepository
                .findTopByMonthlyClicks(PageRequest.of(0, 4))
                .stream()
                .map(p -> toDtoPublic(p))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getBySlug(String slug) {
        Product product = productRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
        return toDtoPublic(product);
    }

    @Transactional(readOnly = true)
    public ProductDto getById(UUID id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        // Admin endpoint — return all tiers including disabled
        return toDtoAdmin(product);
    }

    @Async
    @Transactional
    public void recordClick(UUID productId) {
        productRepository.findByIdAndDeletedFalse(productId).ifPresent(product -> {
            productRepository.incrementClicks(productId);
            ProductClick click = ProductClick.builder().product(product).build();
            productClickRepository.save(click);
        });
    }

    @CacheEvict(value = "recommended-products", allEntries = true)
    @Transactional
    public ProductDto createProduct(ProductCreateRequest request) {
        String slug = generateUniqueSlug(request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .category(request.getCategory())
                .description(request.getDescription())
                .moq(request.getMoq() != null ? request.getMoq() : 1)
                .individualSalesEnabled(request.getIndividualSalesEnabled() != null
                        ? request.getIndividualSalesEnabled() : true)
                .sizes(request.getSizes())
                .tags(request.getTags())
                .keywords(request.getKeywords())
                .primaryImageUrl(request.getPrimaryImageUrl())
                .imageUrls(request.getImageUrls())
                .isDiscount(request.getIsDiscount() != null ? request.getIsDiscount() : false)
                .discountPercent(request.getDiscountPercent())
                .isNewArrival(request.getIsNewArrival() != null ? request.getIsNewArrival() : false)
                .isFastMoving(request.getIsFastMoving() != null ? request.getIsFastMoving() : false)
                .material(request.getMaterial())
                .finish(request.getFinish())
                .basePrice(request.getBasePrice())
                .priceUnit(request.getPriceUnit() != null ? request.getPriceUnit() : PriceUnit.PER_UNIT)
                .stockStatus(request.getStockStatus() != null
                        ? request.getStockStatus() : StockStatus.IN_STOCK)
                .leadTimeDays(request.getLeadTimeDays() != null ? request.getLeadTimeDays() : 14)
                .customizable(request.getCustomizable() != null ? request.getCustomizable() : false)
                .stockCount(request.getStockCount() != null ? request.getStockCount() : 0)
                .lowStockThreshold(request.getLowStockThreshold() != null
                        ? request.getLowStockThreshold() : 10)
                .deleted(false)
                .industries(resolveIndustries(request.getIndustryIds()))
                .build();

        Product saved = productRepository.save(product);
        savePricingTiers(saved, request.getPricingTiers());
        return toDtoAdmin(saved);
    }

    @CacheEvict(value = "recommended-products", allEntries = true)
    @Transactional
    public ProductDto updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (request.getName() != null) {
            product.setName(request.getName());
            if (!SlugUtil.toSlug(request.getName()).equals(product.getSlug())) {
                product.setSlug(generateUniqueSlug(request.getName()));
            }
        }
        if (request.getCategory()              != null) product.setCategory(request.getCategory());
        if (request.getDescription()           != null) product.setDescription(request.getDescription());
        if (request.getMoq()                   != null) product.setMoq(request.getMoq());
        if (request.getIndividualSalesEnabled() != null) product.setIndividualSalesEnabled(request.getIndividualSalesEnabled());
        if (request.getSizes()                 != null) product.setSizes(request.getSizes());
        if (request.getTags()                  != null) product.setTags(request.getTags());
        if (request.getKeywords()              != null) product.setKeywords(request.getKeywords());
        if (request.getPrimaryImageUrl()       != null) product.setPrimaryImageUrl(request.getPrimaryImageUrl());
        if (request.getImageUrls()             != null) product.setImageUrls(request.getImageUrls());
        if (request.getIsDiscount()            != null) product.setIsDiscount(request.getIsDiscount());
        if (request.getDiscountPercent()       != null) product.setDiscountPercent(request.getDiscountPercent());
        if (request.getIsNewArrival()          != null) product.setIsNewArrival(request.getIsNewArrival());
        if (request.getIsFastMoving()          != null) product.setIsFastMoving(request.getIsFastMoving());
        if (request.getMaterial()              != null) product.setMaterial(request.getMaterial());
        if (request.getFinish()                != null) product.setFinish(request.getFinish());
        if (request.getBasePrice()             != null) product.setBasePrice(request.getBasePrice());
        if (request.getPriceUnit()             != null) product.setPriceUnit(request.getPriceUnit());
        if (request.getStockStatus()           != null) product.setStockStatus(request.getStockStatus());
        if (request.getLeadTimeDays()          != null) product.setLeadTimeDays(request.getLeadTimeDays());
        if (request.getCustomizable()          != null) product.setCustomizable(request.getCustomizable());
        if (request.getStockCount()            != null) product.setStockCount(request.getStockCount());
        if (request.getLowStockThreshold()     != null) product.setLowStockThreshold(request.getLowStockThreshold());
        if (request.getIndustryIds()           != null) product.setIndustries(resolveIndustries(request.getIndustryIds()));

        Product saved = productRepository.save(product);

        if (request.getPricingTiers() != null) {
            pricingTierRepository.deleteByProductId(saved.getId());
            savePricingTiers(saved, request.getPricingTiers());
        }

        return toDtoAdmin(saved);
    }

    @CacheEvict(value = "recommended-products", allEntries = true)
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setDeleted(true);
        productRepository.save(product);
        log.info("Product {} soft-deleted", id);
    }

    // ── DTO builders ────────────────────────────────────────────────────────

    /**
     * Public DTO — only enabled tiers are included.
     * Disabled UOMs are completely absent from the response.
     */
    private ProductDto toDtoPublic(Product product) {
        initCollections(product);
        List<ProductPricingTierDto> tiers = pricingTierRepository
                .findByProductId(product.getId())
                .stream()
                .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                .map(ProductPricingTierDto::new)
                .collect(Collectors.toList());
        return new ProductDto(product, tiers);
    }

    /**
     * Admin DTO — all tiers included (enabled and disabled)
     * so staff can see and toggle them in the product editor.
     */
    private ProductDto toDtoAdmin(Product product) {
        initCollections(product);
        List<ProductPricingTierDto> tiers = pricingTierRepository
                .findByProductId(product.getId())
                .stream()
                .map(ProductPricingTierDto::new)
                .collect(Collectors.toList());
        return new ProductDto(product, tiers);
    }

    private void initCollections(Product product) {
        Hibernate.initialize(product.getSizes());
        Hibernate.initialize(product.getTags());
        Hibernate.initialize(product.getKeywords());
        Hibernate.initialize(product.getImageUrls());
        Hibernate.initialize(product.getIndustries());
    }

    // ── Pricing tier persistence ─────────────────────────────────────────────

    private void savePricingTiers(Product product, List<ProductPricingTierDto> tierDtos) {
        if (tierDtos == null || tierDtos.isEmpty()) return;

        AtomicInteger autoOrder = new AtomicInteger(0);

        List<ProductPricingTier> tiers = tierDtos.stream()
                .map(dto -> {
                    if (dto.getCollectionName() == null || dto.getCollectionName().isBlank())
                        throw new IllegalArgumentException(
                                "Each UOM tier must have a collectionName");
                    if (dto.getQuantity() == null || dto.getQuantity() < 1)
                        throw new IllegalArgumentException(
                                "Each UOM tier must have a quantity (pieces) >= 1");
                    if (dto.getPricePerUnit() == null)
                        throw new IllegalArgumentException(
                                "Each UOM tier must have a pricePerUnit (price per piece)");

                    BigDecimal collectionPrice = dto.getPricePerUnit()
                            .multiply(BigDecimal.valueOf(dto.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);

                    int order = dto.getSortOrder() != null
                            ? dto.getSortOrder() : autoOrder.getAndIncrement();

                    // Resolve UOM if uomId provided
                    ProductUom uom = null;
                    if (dto.getUomId() != null) {
                        uom = uomRepository.findById(dto.getUomId()).orElse(null);
                    }

                    return ProductPricingTier.builder()
                            .product(product)
                            .uom(uom)
                            .collectionName(dto.getCollectionName())
                            .uomDescription(dto.getUomDescription())
                            .quantity(dto.getQuantity())
                            .pricePerUnit(dto.getPricePerUnit())
                            .collectionPrice(collectionPrice)
                            .sortOrder(order)
                            .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                            .minQuantity(dto.getMinQuantity())
                            .maxQuantity(dto.getMaxQuantity())
                            .build();
                })
                .collect(Collectors.toList());

        pricingTierRepository.saveAll(tiers);
        log.debug("Saved {} UOM tiers for product {}", tiers.size(), product.getId());
    }

    private String generateUniqueSlug(String name) {
        String base = SlugUtil.toSlug(name);
        String slug = base;
        int counter = 1;
        while (productRepository.existsBySlugAndDeletedFalse(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private Set<Industry> resolveIndustries(List<UUID> industryIds) {
        if (industryIds == null || industryIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(industryRepository.findAllById(industryIds));
    }
}