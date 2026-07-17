package com.mdau.momentspackagingbackendjavafirstclient.industry.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.industry.dto.IndustryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.industry.dto.IndustryDto;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.industry.repository.IndustryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
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
public class IndustryService {

    private final IndustryRepository industryRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    @Cacheable("industries")
    @Transactional(readOnly = true)
    public List<IndustryDto> getAllIndustries() {
        return industryRepository.findAll()
                .stream()
                .map(IndustryDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IndustryDto getById(UUID id) {
        return industryRepository.findById(id)
                .map(IndustryDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found: " + id));
    }

    @CacheEvict(value = "industries", allEntries = true)
    @Transactional
    public IndustryDto createIndustry(IndustryCreateRequest request) {
        if (industryRepository.existsByName(request.getName())) {
            throw new ConflictException("Industry already exists: " + request.getName());
        }
        String slug = SlugUtil.toSlug(request.getName());
        Industry industry = Industry.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .build();
        return new IndustryDto(industryRepository.save(industry));
    }

    @CacheEvict(value = "industries", allEntries = true)
    @Transactional
    public IndustryDto updateIndustry(UUID id, IndustryCreateRequest request) {
        Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found: " + id));
        if (request.getName() != null) {
            industry.setName(request.getName());
            industry.setSlug(SlugUtil.toSlug(request.getName()));
        }
        if (request.getDescription() != null) industry.setDescription(request.getDescription());
        if (request.getIconUrl()     != null) industry.setIconUrl(request.getIconUrl());
        return new IndustryDto(industryRepository.save(industry));
    }

    /**
     * Deleting an industry only ever deletes the industry itself. Every product, category and
     * subcategory tagged with it simply loses that one tag — nothing else about them changes,
     * and nothing is ever deleted as a side effect. Pass reassignTo to re-tag everything with a
     * replacement industry instead of just untagging.
     */
    @CacheEvict(value = "industries", allEntries = true)
    @Transactional
    public void deleteIndustry(UUID id, UUID reassignTo) {
        Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found: " + id));

        List<Product> taggedProducts = productRepository.findByIndustries_IdAndDeletedFalse(id);
        List<Category> taggedCategories = categoryRepository.findByIndustries_Id(id);
        List<Subcategory> taggedSubcategories = subcategoryRepository.findByIndustries_Id(id);

        if (reassignTo != null) {
            if (reassignTo.equals(id)) {
                throw new ConflictException("Cannot reassign an industry's products to itself.");
            }
            Industry target = industryRepository.findById(reassignTo)
                    .orElseThrow(() -> new ResourceNotFoundException("Reassignment target industry not found: " + reassignTo));
            taggedProducts.forEach(p -> {
                p.getIndustries().remove(industry);
                p.getIndustries().add(target);
            });
            taggedCategories.forEach(c -> {
                c.getIndustries().remove(industry);
                c.getIndustries().add(target);
            });
            taggedSubcategories.forEach(sc -> {
                sc.getIndustries().remove(industry);
                sc.getIndustries().add(target);
            });
        } else {
            taggedProducts.forEach(p -> p.getIndustries().remove(industry));
            taggedCategories.forEach(c -> c.getIndustries().remove(industry));
            taggedSubcategories.forEach(sc -> sc.getIndustries().remove(industry));
        }
        productRepository.saveAll(taggedProducts);
        categoryRepository.saveAll(taggedCategories);
        subcategoryRepository.saveAll(taggedSubcategories);
        industryRepository.delete(industry);
    }
}