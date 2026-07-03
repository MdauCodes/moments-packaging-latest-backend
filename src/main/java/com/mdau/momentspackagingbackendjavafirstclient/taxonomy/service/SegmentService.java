package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SegmentCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SegmentDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Segment;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.CategoryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository.SegmentRepository;
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

    @CacheEvict(value = "segments", allEntries = true)
    @Transactional
    public void deleteSegment(UUID id) {
        if (!segmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Segment not found: " + id);
        }
        long childCategories = categoryRepository.countBySegmentId(id);
        if (childCategories > 0) {
            throw new ConflictException(
                    "Cannot delete segment: " + childCategories + " categories still belong to it. Reassign or delete them first.");
        }
        segmentRepository.deleteById(id);
    }
}
