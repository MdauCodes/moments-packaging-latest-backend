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
     * @param reassignTo if products are still tagged with this industry and this is set,
     *                    they're re-tagged with the other industry first instead of
     *                    blocking the delete.
     * @param cascade     if products are still tagged with this industry and this is true
     *                    (and reassignTo isn't set), the tag is simply removed from those
     *                    products — the products themselves are never touched otherwise.
     */
    @CacheEvict(value = "industries", allEntries = true)
    @Transactional
    public void deleteIndustry(UUID id, UUID reassignTo, boolean cascade) {
        Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry not found: " + id));

        List<Product> tagged = productRepository.findByIndustries_IdAndDeletedFalse(id);
        if (!tagged.isEmpty()) {
            if (reassignTo != null) {
                if (reassignTo.equals(id)) {
                    throw new ConflictException("Cannot reassign an industry's products to itself.");
                }
                Industry target = industryRepository.findById(reassignTo)
                        .orElseThrow(() -> new ResourceNotFoundException("Reassignment target industry not found: " + reassignTo));
                tagged.forEach(p -> {
                    p.getIndustries().remove(industry);
                    p.getIndustries().add(target);
                });
                productRepository.saveAll(tagged);
            } else if (cascade) {
                tagged.forEach(p -> p.getIndustries().remove(industry));
                productRepository.saveAll(tagged);
            } else {
                throw new ConflictException(
                        "Cannot delete industry: " + tagged.size() + " products are still tagged with it. Reassign or untag them first.");
            }
        }
        industryRepository.delete(industry);
    }
}