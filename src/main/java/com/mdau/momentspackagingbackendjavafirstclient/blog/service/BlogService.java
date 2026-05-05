package com.mdau.momentspackagingbackendjavafirstclient.blog.service;

import com.mdau.momentspackagingbackendjavafirstclient.blog.dto.BlogCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.blog.dto.BlogDto;
import com.mdau.momentspackagingbackendjavafirstclient.blog.dto.BlogUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.blog.entity.Blog;
import com.mdau.momentspackagingbackendjavafirstclient.blog.entity.BlogStatus;
import com.mdau.momentspackagingbackendjavafirstclient.blog.repository.BlogRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.ReadingTimeCalculator;
import com.mdau.momentspackagingbackendjavafirstclient.common.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;

    // ── Public ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BlogDto> getPublishedBlogs(String template, Integer limit) {
        int cap = (limit != null) ? Math.min(limit, 50) : 50;
        return blogRepository
                .findAllByStatusAndTemplate(BlogStatus.PUBLISHED, template,
                        PageRequest.of(0, cap))
                .stream()
                .map(BlogDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BlogDto> getLatest(int limit) {
        int cap = Math.min(limit, 10);
        return blogRepository.findLatestPublished(PageRequest.of(0, cap))
                .stream()
                .map(BlogDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BlogDto getPublishedBySlug(String slug) {
        return blogRepository.findBySlugAndStatus(slug, BlogStatus.PUBLISHED)
                .map(BlogDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found: " + slug));
    }

    @Transactional(readOnly = true)
    public List<BlogDto> getRelated(String slug) {
        return blogRepository.findRelated(slug, PageRequest.of(0, 2))
                .stream()
                .map(BlogDto::new)
                .collect(Collectors.toList());
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BlogDto> getAllBlogs(String template, Integer limit) {
        int cap = (limit != null) ? Math.min(limit, 50) : 50;
        return blogRepository
                .findAllByStatusAndTemplate(null, template, PageRequest.of(0, cap))
                .stream()
                .map(BlogDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BlogDto getById(UUID id) {
        return blogRepository.findById(id)
                .map(BlogDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found: " + id));
    }

    @Transactional
    public BlogDto createBlog(BlogCreateRequest request) {
        String slug = generateUniqueSlug(request.getTitle());
        int readingTime = ReadingTimeCalculator.calculate(request.getBody());

        Blog blog = Blog.builder()
                .title(request.getTitle())
                .slug(slug)
                .excerpt(request.getExcerpt())
                .template(request.getTemplate())
                .coverImageUrl(request.getCoverImageUrl())
                .coverImageAlt(request.getCoverImageAlt())
                .coverImageCaption(request.getCoverImageCaption())
                .secondaryImageUrl(request.getSecondaryImageUrl())
                .body(request.getBody())
                .author(request.getAuthor())
                .tags(request.getTags())
                .readingTimeMin(readingTime)
                .status(BlogStatus.DRAFT)
                .build();

        return new BlogDto(blogRepository.save(blog));
    }

    @Transactional
    public BlogDto updateBlog(UUID id, BlogUpdateRequest request) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found: " + id));

        if (request.getTitle() != null) {
            blog.setTitle(request.getTitle());
            blog.setSlug(generateUniqueSlugExcluding(request.getTitle(), blog.getSlug()));
        }
        if (request.getExcerpt()          != null) blog.setExcerpt(request.getExcerpt());
        if (request.getTemplate()         != null) blog.setTemplate(request.getTemplate());
        if (request.getCoverImageUrl()    != null) blog.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getCoverImageAlt()    != null) blog.setCoverImageAlt(request.getCoverImageAlt());
        if (request.getCoverImageCaption()!= null) blog.setCoverImageCaption(request.getCoverImageCaption());
        if (request.getSecondaryImageUrl()!= null) blog.setSecondaryImageUrl(request.getSecondaryImageUrl());
        if (request.getAuthor()           != null) blog.setAuthor(request.getAuthor());
        if (request.getTags()             != null) blog.setTags(request.getTags());
        if (request.getBody()             != null) {
            blog.setBody(request.getBody());
            blog.setReadingTimeMin(ReadingTimeCalculator.calculate(request.getBody()));
        }

        return new BlogDto(blogRepository.save(blog));
    }

    @Transactional
    public BlogDto publish(UUID id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found: " + id));
        blog.setStatus(BlogStatus.PUBLISHED);
        if (blog.getPublishedAt() == null) {
            blog.setPublishedAt(Instant.now());
        }
        return new BlogDto(blogRepository.save(blog));
    }

    @Transactional
    public BlogDto unpublish(UUID id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found: " + id));
        blog.setStatus(BlogStatus.DRAFT);
        return new BlogDto(blogRepository.save(blog));
    }

    @Transactional
    public void deleteBlog(UUID id) {
        if (!blogRepository.existsById(id)) {
            throw new ResourceNotFoundException("Blog not found: " + id);
        }
        blogRepository.deleteById(id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String generateUniqueSlug(String title) {
        String base = SlugUtil.toSlug(title);
        String slug = base;
        int counter = 1;
        while (blogRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private String generateUniqueSlugExcluding(String title, String currentSlug) {
        String base = SlugUtil.toSlug(title);
        if (base.equals(currentSlug)) return currentSlug;
        String slug = base;
        int counter = 1;
        while (blogRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}