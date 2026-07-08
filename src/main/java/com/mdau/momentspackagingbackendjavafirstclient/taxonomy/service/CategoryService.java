package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.CategoryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.CategoryDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Category;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Segment;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Subcategory;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.CategoryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.SegmentRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SegmentRepository segmentRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductRepository productRepository;

    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getBySegment(UUID segmentId) {
        return categoryRepository.findBySegmentId(segmentId)
                .stream()
                .map(CategoryDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(UUID id) {
        return categoryRepository.findById(id)
                .map(CategoryDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryDto createCategory(CategoryCreateRequest request) {
        Segment segment = segmentRepository.findById(request.getSegmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + request.getSegmentId()));
        if (categoryRepository.existsByNameAndSegmentId(request.getName(), segment.getId())) {
            throw new ConflictException("Category already exists in this segment: " + request.getName());
        }
        String slug = SlugUtil.toSlug(request.getName());
        Category category = Category.builder()
                .segment(segment)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return new CategoryDto(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryDto updateCategory(UUID id, CategoryCreateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        if (request.getSegmentId() != null && !request.getSegmentId().equals(category.getSegment().getId())) {
            Segment segment = segmentRepository.findById(request.getSegmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + request.getSegmentId()));
            category.setSegment(segment);
        }
        if (request.getName() != null) {
            category.setName(request.getName());
            category.setSlug(SlugUtil.toSlug(request.getName()));
        }
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getSortOrder()   != null) category.setSortOrder(request.getSortOrder());
        return new CategoryDto(categoryRepository.save(category));
    }

    /**
     * @param reassignTo if the category still has subcategories and this is set, they're
     *                    moved onto this other category first instead of blocking the delete.
     * @param cascade     if the category still has subcategories and this is true (and
     *                    reassignTo isn't set), they're deleted along with it. Any products
     *                    still under those subcategories are unassigned (subcategory set to
     *                    null), never deleted — a taxonomy cleanup must never take real
     *                    inventory down with it.
     */
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void deleteCategory(UUID id, UUID reassignTo, boolean cascade) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        List<Subcategory> children = subcategoryRepository.findByCategoryId(id);
        if (!children.isEmpty()) {
            if (reassignTo != null) {
                if (reassignTo.equals(id)) {
                    throw new ConflictException("Cannot reassign a category's subcategories to itself.");
                }
                Category target = categoryRepository.findById(reassignTo)
                        .orElseThrow(() -> new ResourceNotFoundException("Reassignment target category not found: " + reassignTo));
                children.forEach(sc -> sc.setCategory(target));
                subcategoryRepository.saveAll(children);
            } else if (cascade) {
                List<UUID> childIds = children.stream().map(Subcategory::getId).collect(Collectors.toList());
                List<Product> orphaned = productRepository.findBySubcategoryIdInAndDeletedFalse(childIds);
                if (!orphaned.isEmpty()) {
                    orphaned.forEach(p -> p.setSubcategory(null));
                    productRepository.saveAll(orphaned);
                }
                subcategoryRepository.deleteAll(children);
            } else {
                throw new ConflictException(
                        "Cannot delete category: " + children.size() + " subcategories still belong to it. Reassign or delete them first.");
            }
        }
        categoryRepository.delete(category);
    }
}
