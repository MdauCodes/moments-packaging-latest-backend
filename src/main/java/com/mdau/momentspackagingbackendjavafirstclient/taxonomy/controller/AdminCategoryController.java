package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.CategoryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.CategoryDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAll(@RequestParam(required = false) UUID segmentId) {
        if (segmentId != null) {
            return ResponseEntity.ok(categoryService.getBySegment(segmentId));
        }
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @IsStaffOrAdmin
    @PostMapping
    public ResponseEntity<CategoryDto> create(
            @Valid @RequestBody CategoryCreateRequest request) {
        CategoryDto created = categoryService.createCategory(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID reassignTo,
            @RequestParam(required = false, defaultValue = "false") boolean cascade) {
        categoryService.deleteCategory(id, reassignTo, cascade);
        return ResponseEntity.noContent().build();
    }
}
