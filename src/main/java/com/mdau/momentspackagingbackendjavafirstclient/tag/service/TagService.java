package com.mdau.momentspackagingbackendjavafirstclient.tag.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import com.mdau.momentspackagingbackendjavafirstclient.tag.dto.TagCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.tag.dto.TagDto;
import com.mdau.momentspackagingbackendjavafirstclient.tag.entity.Tag;
import com.mdau.momentspackagingbackendjavafirstclient.tag.repository.TagRepository;
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
public class TagService {

    private final TagRepository tagRepository;

    @Cacheable("tags")
    @Transactional(readOnly = true)
    public List<TagDto> getAllTags() {
        return tagRepository.findAll()
                .stream()
                .map(TagDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TagDto getById(UUID id) {
        return tagRepository.findById(id)
                .map(TagDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
    }

    @CacheEvict(value = "tags", allEntries = true)
    @Transactional
    public TagDto createTag(TagCreateRequest request) {
        if (tagRepository.existsByName(request.getName())) {
            throw new ConflictException("Tag already exists: " + request.getName());
        }
        String slug = SlugUtil.toSlug(request.getName());
        Tag tag = Tag.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .build();
        return new TagDto(tagRepository.save(tag));
    }

    @CacheEvict(value = "tags", allEntries = true)
    @Transactional
    public TagDto updateTag(UUID id, TagCreateRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
        if (request.getName() != null) {
            tag.setName(request.getName());
            tag.setSlug(SlugUtil.toSlug(request.getName()));
        }
        if (request.getDescription() != null) tag.setDescription(request.getDescription());
        return new TagDto(tagRepository.save(tag));
    }

    @CacheEvict(value = "tags", allEntries = true)
    @Transactional
    public void deleteTag(UUID id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag not found: " + id);
        }
        tagRepository.deleteById(id);
    }
}
