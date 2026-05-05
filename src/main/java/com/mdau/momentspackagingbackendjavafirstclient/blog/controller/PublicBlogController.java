package com.mdau.momentspackagingbackendjavafirstclient.blog.controller;

import com.mdau.momentspackagingbackendjavafirstclient.blog.dto.BlogDto;
import com.mdau.momentspackagingbackendjavafirstclient.blog.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/blogs")
@RequiredArgsConstructor
public class PublicBlogController {

    private final BlogService blogService;

    @GetMapping
    public ResponseEntity<List<BlogDto>> getBlogs(
            @RequestParam(required = false) String  template,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(blogService.getPublishedBlogs(template, limit));
    }

    @GetMapping("/latest")
    public ResponseEntity<List<BlogDto>> getLatest(
            @RequestParam(defaultValue = "3") int limit) {
        return ResponseEntity.ok(blogService.getLatest(limit));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<BlogDto> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(blogService.getPublishedBySlug(slug));
    }

    @GetMapping("/{slug}/related")
    public ResponseEntity<List<BlogDto>> getRelated(@PathVariable String slug) {
        return ResponseEntity.ok(blogService.getRelated(slug));
    }
}