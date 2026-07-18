package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.industry.repository.IndustryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.BulkClassifyRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.BulkClassifyResponse;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductPricingTierDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.tag.entity.Tag;
import com.mdau.momentspackagingbackendjavafirstclient.tag.repository.TagRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Subcategory;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.SubcategoryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.PriceUnit;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductClick;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductPricingTier;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductUom;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartItemRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository            productRepository;
    private final ProductClickRepository       productClickRepository;
    private final ProductPricingTierRepository pricingTierRepository;
    private final CartItemRepository           cartItemRepository;
    private final IndustryRepository           industryRepository;
    private final ProductUomRepository         uomRepository;
    private final SubcategoryRepository        subcategoryRepository;
    private final TagRepository                tagRepository;

    // Ã¢â€â‚¬Ã¢â€â‚¬ Standard paginated listing Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(UUID industryId, Boolean isDiscount,
                                        Boolean isNewArrival, Boolean isFastMoving,
                                        String category, Pageable pageable) {
        return getProducts(industryId, isDiscount, isNewArrival, isFastMoving, category, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(UUID industryId, Boolean isDiscount,
                                        Boolean isNewArrival, Boolean isFastMoving,
                                        String category, UUID subcategoryId, Pageable pageable) {
        return getProducts(industryId, isDiscount, isNewArrival, isFastMoving, category, subcategoryId, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(UUID industryId, Boolean isDiscount,
                                        Boolean isNewArrival, Boolean isFastMoving,
                                        String category, UUID subcategoryId, UUID tagId, Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), 100);
        Pageable capped = PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
        return productRepository
                .findAllWithFilters(industryId, tagId, isDiscount, isNewArrival, isFastMoving, category, subcategoryId, capped)
                .map(p -> toDtoPublic(p));
    }

    /**
     * Diversified product listing for the public storefront.
     *
     * Algorithm:
     *  1. Fetch all active products (uses existing findAllActive Ã¢â‚¬â€ already loaded).
     *  2. Group by category.
     *  3. Round-robin across categories so each page shows a mix.
     *  4. Within each category, sort by: new arrivals first, then fast-moving,
     *     then monthly clicks desc, then random seed so order changes across requests.
     *
     * This maximises the chance a visitor sees something relevant regardless of
     * which category they came for, driving conversions.
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getDiversifiedProducts(int page, int size) {
        int cappedSize = Math.min(size, 100);

        List<Product> all = productRepository.findAllActive();
        if (all.isEmpty()) return Page.empty();

        // Group by category (null category Ã¢â€ â€™ "OTHER")
        Map<String, List<Product>> byCategory = all.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory().toUpperCase() : "OTHER"));

        // Sort within each category: new arrivals Ã¢â€ â€™ fast moving Ã¢â€ â€™ monthly clicks
        byCategory.forEach((cat, products) ->
                products.sort(Comparator
                        .comparing(Product::getIsNewArrival, Comparator.reverseOrder())
                        .thenComparing(Product::getIsFastMoving, Comparator.reverseOrder())
                        .thenComparing(Product::getMonthlyClicks, Comparator.reverseOrder())));

        // Round-robin interleave across categories
        List<String> categories = new ArrayList<>(byCategory.keySet());
        Collections.sort(categories); // deterministic order
        List<Product> interleaved = new ArrayList<>(all.size());
        Map<String, Integer> cursors = new HashMap<>();
        categories.forEach(c -> cursors.put(c, 0));

        boolean added = true;
        while (added) {
            added = false;
            for (String cat : categories) {
                int idx = cursors.get(cat);
                List<Product> catList = byCategory.get(cat);
                if (idx < catList.size()) {
                    interleaved.add(catList.get(idx));
                    cursors.put(cat, idx + 1);
                    added = true;
                }
            }
        }

        // Manual pagination
        int fromIndex = page * cappedSize;
        if (fromIndex >= interleaved.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, cappedSize), interleaved.size());
        }
        int toIndex = Math.min(fromIndex + cappedSize, interleaved.size());
        List<ProductDto> pageContent = interleaved.subList(fromIndex, toIndex)
                .stream()
                .map(this::toDtoPublic)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, PageRequest.of(page, cappedSize), interleaved.size());
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

    // Ã¢â€â‚¬Ã¢â€â‚¬ Create / Update / Delete Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

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
                .originalBasePrice(request.getOriginalBasePrice())
                .priceUnit(request.getPriceUnit() != null ? request.getPriceUnit() : PriceUnit.PER_UNIT)
                .leadTimeDays(request.getLeadTimeDays() != null ? request.getLeadTimeDays() : 14)
                .customizable(request.getCustomizable() != null ? request.getCustomizable() : false)
                .stockCount(request.getStockCount() != null ? request.getStockCount() : 0)
                .lowStockThreshold(request.getLowStockThreshold() != null
                        ? request.getLowStockThreshold() : 10)
                .stockStatus(request.getStockStatus() != null
                        ? request.getStockStatus()
                        : deriveStockStatus(
                                request.getStockCount() != null ? request.getStockCount() : 0,
                                request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10))
                .vatRate(request.getVatRate() != null ? request.getVatRate() : new BigDecimal("0.1600"))
                .vatExempt(request.getVatExempt() != null ? request.getVatExempt() : false)
                .deleted(false)
                .industries(resolveIndustries(request.getIndustryIds()))
                .curatedTags(resolveTags(request.getTagIds()))
                .subcategory(request.getSubcategoryId() != null
                        ? subcategoryRepository.findById(request.getSubcategoryId())
                                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found: " + request.getSubcategoryId()))
                        : null)
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
        if (request.getLeadTimeDays()          != null) product.setLeadTimeDays(request.getLeadTimeDays());
        if (request.getCustomizable()          != null) product.setCustomizable(request.getCustomizable());
        if (request.getStockCount()            != null) product.setStockCount(request.getStockCount());
        if (request.getLowStockThreshold()     != null) product.setLowStockThreshold(request.getLowStockThreshold());
        // Keep stockStatus consistent with stockCount unless the caller explicitly overrides it
        // (e.g. MADE_TO_ORDER, which is legitimately independent of count).
        if (request.getStockStatus() != null) {
            product.setStockStatus(request.getStockStatus());
        } else if (request.getStockCount() != null) {
            product.setStockStatus(deriveStockStatus(product.getStockCount(), product.getLowStockThreshold()));
        }
        if (request.getVatRate()               != null) product.setVatRate(request.getVatRate());
        if (request.getVatExempt()             != null) product.setVatExempt(request.getVatExempt());
        if (request.getIndustryIds()           != null) product.setIndustries(resolveIndustries(request.getIndustryIds()));
        if (request.getTagIds()                != null) product.setCuratedTags(resolveTags(request.getTagIds()));
        if (Boolean.TRUE.equals(request.getClearSubcategory())) {
            product.setSubcategory(null);
        } else if (request.getSubcategoryId()  != null) {
            product.setSubcategory(subcategoryRepository.findById(request.getSubcategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found: " + request.getSubcategoryId())));
        }
        if (request.getPriceUnit()             != null) product.setPriceUnit(request.getPriceUnit());

        // Compare-at price: explicit clear takes priority, then value update
        if (Boolean.TRUE.equals(request.getClearOriginalBasePrice())) {
            product.setOriginalBasePrice(null);
        } else if (request.getOriginalBasePrice() != null) {
            product.setOriginalBasePrice(request.getOriginalBasePrice());
        }

        Product saved = productRepository.save(product);

        if (request.getPricingTiers() != null) {
            cartItemRepository.nullifyTiersByProductId(saved.getId());
            pricingTierRepository.deleteByProductId(saved.getId());
            savePricingTiers(saved, request.getPricingTiers());
        }

        return toDtoAdmin(saved);
    }

    @CacheEvict(value = "recommended-products", allEntries = true)
    @Transactional
    public BulkClassifyResponse bulkClassify(BulkClassifyRequest request) {
        if (request.getSubcategoryId() == null && request.getIndustryIds() == null && request.getTagIds() == null
                && !Boolean.TRUE.equals(request.getClearSubcategory())) {
            throw new IllegalArgumentException("At least one of subcategoryId, industryIds, tagIds or clearSubcategory must be provided");
        }

        List<Product> products = productRepository.findAllById(request.getProductIds());
        if (products.size() != request.getProductIds().size()) {
            throw new ResourceNotFoundException("One or more products in productIds were not found");
        }

        Subcategory subcategory = null;
        if (request.getSubcategoryId() != null) {
            subcategory = subcategoryRepository.findById(request.getSubcategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found: " + request.getSubcategoryId()));
        }
        Set<Industry> industries = request.getIndustryIds() != null
                ? resolveIndustries(request.getIndustryIds())
                : null;
        Set<Tag> tags = request.getTagIds() != null
                ? new HashSet<>(tagRepository.findAllById(request.getTagIds()))
                : null;

        // Additive, not a replace — the admin UI's own copy promises "leave unchecked to keep
        // unchanged," which only holds if checking one industry/tag ADDS it rather than wiping
        // out every other industry/tag a product already had.
        for (Product product : products) {
            if (Boolean.TRUE.equals(request.getClearSubcategory())) product.setSubcategory(null);
            else if (subcategory != null) product.setSubcategory(subcategory);
            if (industries != null) product.getIndustries().addAll(industries);
            if (tags != null) product.getCuratedTags().addAll(tags);
        }
        productRepository.saveAll(products);

        return new BulkClassifyResponse(products.size(), request.getProductIds());
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

    // Ã¢â€â‚¬Ã¢â€â‚¬ DTO builders Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

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

    // Ã¢â€â‚¬Ã¢â€â‚¬ Pricing tier persistence Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private void savePricingTiers(Product product, List<ProductPricingTierDto> tierDtos) {
        if (tierDtos == null || tierDtos.isEmpty()) return;

        AtomicInteger autoOrder = new AtomicInteger(0);

        List<ProductPricingTier> tiers = tierDtos.stream()
                .map(dto -> {
                    if (dto.getCollectionName() == null || dto.getCollectionName().isBlank())
                        throw new IllegalArgumentException("Each UOM tier must have a collectionName");
                    if (dto.getQuantity() == null || dto.getQuantity() < 1)
                        throw new IllegalArgumentException("Each UOM tier must have a quantity >= 1");
                    if (dto.getPricePerUnit() == null)
                        throw new IllegalArgumentException("Each UOM tier must have a pricePerUnit");

                    BigDecimal collectionPrice = dto.getPricePerUnit()
                            .multiply(BigDecimal.valueOf(dto.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);

                    // Compute compare-at collection price if original price provided
                    BigDecimal originalCollectionPrice = null;
                    if (dto.getOriginalPricePerUnit() != null
                            && dto.getOriginalPricePerUnit().compareTo(dto.getPricePerUnit()) > 0) {
                        originalCollectionPrice = dto.getOriginalPricePerUnit()
                                .multiply(BigDecimal.valueOf(dto.getQuantity()))
                                .setScale(2, RoundingMode.HALF_UP);
                    }

                    int order = dto.getSortOrder() != null
                            ? dto.getSortOrder() : autoOrder.getAndIncrement();

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
                            .originalPricePerUnit(
                                    dto.getOriginalPricePerUnit() != null
                                    && dto.getOriginalPricePerUnit().compareTo(dto.getPricePerUnit()) > 0
                                            ? dto.getOriginalPricePerUnit() : null)
                            .collectionPrice(collectionPrice)
                            .originalCollectionPrice(originalCollectionPrice)
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

    private Set<Tag> resolveTags(List<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(tagRepository.findAllById(tagIds));
    }

    /** Mirrors the CASE WHEN logic in ProductRepository's deductStock/restoreStock/setStockCount. */
    private StockStatus deriveStockStatus(int stockCount, int lowStockThreshold) {
        if (stockCount <= 0) return StockStatus.OUT_OF_STOCK;
        if (stockCount <= lowStockThreshold) return StockStatus.LOW_STOCK;
        return StockStatus.IN_STOCK;
    }
}