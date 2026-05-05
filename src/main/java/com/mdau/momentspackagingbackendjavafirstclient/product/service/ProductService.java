package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.industry.repository.IndustryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductClick;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductClickRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository        productRepository;
    private final ProductClickRepository   productClickRepository;
    private final IndustryRepository       industryRepository;

    // ── Public ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(UUID industryId, Boolean isDiscount,
                                        Boolean isNewArrival, Boolean isFastMoving,
                                        String category, Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), 100);
        Pageable capped = PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
        return productRepository
                .findAllWithFilters(industryId, isDiscount, isNewArrival, isFastMoving, category, capped)
                .map(ProductDto::new);
    }

    @Cacheable("recommended-products")
    @Transactional(readOnly = true)
    public List<ProductDto> getRecommended() {
        return productRepository
                .findTopByMonthlyClicks(PageRequest.of(0, 4))
                .stream()
                .map(ProductDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(ProductDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
    }

    @Async
    @Transactional
    public void recordClick(UUID productId) {
        productRepository.findById(productId).ifPresent(product -> {
            productRepository.incrementClicks(productId);
            ProductClick click = ProductClick.builder()
                    .product(product)
                    .build();
            productClickRepository.save(click);
        });
    }

    // ── Admin CRUD ───────────────────────────────────────────────────────────

    @Transactional
    public ProductDto createProduct(ProductCreateRequest request) {
        String slug = generateUniqueSlug(request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .category(request.getCategory())
                .description(request.getDescription())
                .moq(request.getMoq())
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
                .industries(resolveIndustries(request.getIndustryIds()))
                .build();

        return new ProductDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (request.getName() != null) {
            product.setName(request.getName());
            if (!SlugUtil.toSlug(request.getName()).equals(product.getSlug())) {
                product.setSlug(generateUniqueSlug(request.getName()));
            }
        }
        if (request.getCategory()        != null) product.setCategory(request.getCategory());
        if (request.getDescription()     != null) product.setDescription(request.getDescription());
        if (request.getMoq()             != null) product.setMoq(request.getMoq());
        if (request.getSizes()           != null) product.setSizes(request.getSizes());
        if (request.getTags()            != null) product.setTags(request.getTags());
        if (request.getKeywords()        != null) product.setKeywords(request.getKeywords());
        if (request.getPrimaryImageUrl() != null) product.setPrimaryImageUrl(request.getPrimaryImageUrl());
        if (request.getImageUrls()       != null) product.setImageUrls(request.getImageUrls());
        if (request.getIsDiscount()      != null) product.setIsDiscount(request.getIsDiscount());
        if (request.getDiscountPercent() != null) product.setDiscountPercent(request.getDiscountPercent());
        if (request.getIsNewArrival()    != null) product.setIsNewArrival(request.getIsNewArrival());
        if (request.getIsFastMoving()    != null) product.setIsFastMoving(request.getIsFastMoving());
        if (request.getMaterial()        != null) product.setMaterial(request.getMaterial());
        if (request.getFinish()          != null) product.setFinish(request.getFinish());
        if (request.getIndustryIds()     != null) product.setIndustries(resolveIndustries(request.getIndustryIds()));

        return new ProductDto(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ProductDto getById(UUID id) {
        return productRepository.findById(id)
                .map(ProductDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String generateUniqueSlug(String name) {
        String base = SlugUtil.toSlug(name);
        String slug = base;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private Set<Industry> resolveIndustries(List<UUID> industryIds) {
        if (industryIds == null || industryIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(industryRepository.findAllById(industryIds));
    }
}