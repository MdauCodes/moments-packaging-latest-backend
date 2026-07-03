package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SubcategoryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SubcategoryDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Category;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Subcategory;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.CategoryRepository;
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
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Cacheable("subcategories")
    @Transactional(readOnly = true)
    public List<SubcategoryDto> getAllSubcategories() {
        return subcategoryRepository.findAll()
                .stream()
                .map(SubcategoryDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubcategoryDto> getByCategory(UUID categoryId) {
        return subcategoryRepository.findByCategoryId(categoryId)
                .stream()
                .map(SubcategoryDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubcategoryDto getById(UUID id) {
        return subcategoryRepository.findById(id)
                .map(SubcategoryDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found: " + id));
    }

    @CacheEvict(value = "subcategories", allEntries = true)
    @Transactional
    public SubcategoryDto createSubcategory(SubcategoryCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
        if (subcategoryRepository.existsByNameAndCategoryId(request.getName(), category.getId())) {
            throw new ConflictException("Subcategory already exists in this category: " + request.getName());
        }
        String slug = SlugUtil.toSlug(request.getName());
        Subcategory subcategory = Subcategory.builder()
                .category(category)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return new SubcategoryDto(subcategoryRepository.save(subcategory));
    }

    @CacheEvict(value = "subcategories", allEntries = true)
    @Transactional
    public SubcategoryDto updateSubcategory(UUID id, SubcategoryCreateRequest request) {
        Subcategory subcategory = subcategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found: " + id));
        if (request.getCategoryId() != null && !request.getCategoryId().equals(subcategory.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
            subcategory.setCategory(category);
        }
        if (request.getName() != null) {
            subcategory.setName(request.getName());
            subcategory.setSlug(SlugUtil.toSlug(request.getName()));
        }
        if (request.getDescription() != null) subcategory.setDescription(request.getDescription());
        if (request.getSortOrder()   != null) subcategory.setSortOrder(request.getSortOrder());
        return new SubcategoryDto(subcategoryRepository.save(subcategory));
    }

    @CacheEvict(value = "subcategories", allEntries = true)
    @Transactional
    public void deleteSubcategory(UUID id) {
        if (!subcategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subcategory not found: " + id);
        }
        long attachedProducts = productRepository.countBySubcategoryIdAndDeletedFalse(id);
        if (attachedProducts > 0) {
            throw new ConflictException(
                    "Cannot delete subcategory: " + attachedProducts + " products are still assigned to it. Reassign them first.");
        }
        subcategoryRepository.deleteById(id);
    }
}
