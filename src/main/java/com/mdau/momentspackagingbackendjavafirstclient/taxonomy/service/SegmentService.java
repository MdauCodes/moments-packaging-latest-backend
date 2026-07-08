package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SegmentCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SegmentDto;
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
public class SegmentService {

    private final SegmentRepository segmentRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ProductRepository productRepository;

    @Cacheable("segments")
    @Transactional(readOnly = true)
    public List<SegmentDto> getAllSegments() {
        return segmentRepository.findAll()
                .stream()
                .map(SegmentDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SegmentDto getById(UUID id) {
        return segmentRepository.findById(id)
                .map(SegmentDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + id));
    }

    @CacheEvict(value = "segments", allEntries = true)
    @Transactional
    public SegmentDto createSegment(SegmentCreateRequest request) {
        if (segmentRepository.existsByName(request.getName())) {
            throw new ConflictException("Segment already exists: " + request.getName());
        }
        String slug = SlugUtil.toSlug(request.getName());
        Segment segment = Segment.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return new SegmentDto(segmentRepository.save(segment));
    }

    @CacheEvict(value = "segments", allEntries = true)
    @Transactional
    public SegmentDto updateSegment(UUID id, SegmentCreateRequest request) {
        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + id));
        if (request.getName() != null) {
            segment.setName(request.getName());
            segment.setSlug(SlugUtil.toSlug(request.getName()));
        }
        if (request.getDescription() != null) segment.setDescription(request.getDescription());
        if (request.getSortOrder()   != null) segment.setSortOrder(request.getSortOrder());
        return new SegmentDto(segmentRepository.save(segment));
    }

    /**
     * @param reassignTo if the segment still has categories and this is set, they're moved
     *                    onto this other segment first instead of blocking the delete.
     * @param cascade     if the segment still has categories and this is true (and
     *                    reassignTo isn't set), the categories — and their subcategories —
     *                    are deleted along with it. Any products still under those
     *                    subcategories are unassigned (subcategory set to null), never
     *                    deleted — a taxonomy cleanup must never take real inventory down
     *                    with it.
     */
    @CacheEvict(value = "segments", allEntries = true)
    @Transactional
    public void deleteSegment(UUID id, UUID reassignTo, boolean cascade) {
        Segment segment = segmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + id));
        List<Category> children = categoryRepository.findBySegmentId(id);
        if (!children.isEmpty()) {
            if (reassignTo != null) {
                if (reassignTo.equals(id)) {
                    throw new ConflictException("Cannot reassign a segment's categories to itself.");
                }
                Segment target = segmentRepository.findById(reassignTo)
                        .orElseThrow(() -> new ResourceNotFoundException("Reassignment target segment not found: " + reassignTo));
                children.forEach(c -> c.setSegment(target));
                categoryRepository.saveAll(children);
            } else if (cascade) {
                List<UUID> categoryIds = children.stream().map(Category::getId).collect(Collectors.toList());
                List<Subcategory> grandchildren = subcategoryRepository.findByCategoryIdIn(categoryIds);
                if (!grandchildren.isEmpty()) {
                    List<UUID> subcategoryIds = grandchildren.stream().map(Subcategory::getId).collect(Collectors.toList());
                    List<Product> orphaned = productRepository.findBySubcategoryIdInAndDeletedFalse(subcategoryIds);
                    if (!orphaned.isEmpty()) {
                        orphaned.forEach(p -> p.setSubcategory(null));
                        productRepository.saveAll(orphaned);
                    }
                    subcategoryRepository.deleteAll(grandchildren);
                }
                categoryRepository.deleteAll(children);
            } else {
                throw new ConflictException(
                        "Cannot delete segment: " + children.size() + " categories still belong to it. Reassign or delete them first.");
            }
        }
        segmentRepository.delete(segment);
    }
}
