package com.mdau.momentspackagingbackendjavafirstclient.blog.controller;

import com.mdau.momentspackagingbackendjavafirstclient.blog.dto.BlogCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.blog.dto.BlogDto;
import com.mdau.momentspackagingbackendjavafirstclient.blog.dto.BlogUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.blog.service.BlogService;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/blogs")
@RequiredArgsConstructor
public class AdminBlogController {

    private final BlogService blogService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<List<BlogDto>> getAllBlogs(
            @RequestParam(required = false) String  template,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(blogService.getAllBlogs(template, limit));
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<BlogDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(blogService.getById(id));
    }

    @IsStaffOrAdmin
    @PostMapping
    public ResponseEntity<BlogDto> createBlog(
            @Valid @RequestBody BlogCreateRequest request) {
        BlogDto created = blogService.createBlog(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}")
    public ResponseEntity<BlogDto> updateBlog(
            @PathVariable UUID id,
            @Valid @RequestBody BlogUpdateRequest request) {
        return ResponseEntity.ok(blogService.updateBlog(id, request));
    }

    @IsStaffOrAdmin
    @PostMapping("/{id}/publish")
    public ResponseEntity<BlogDto> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(blogService.publish(id));
    }

    @IsStaffOrAdmin
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<BlogDto> unpublish(@PathVariable UUID id) {
        return ResponseEntity.ok(blogService.unpublish(id));
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlog(@PathVariable UUID id) {
        blogService.deleteBlog(id);
        return ResponseEntity.noContent().build();
    }
}