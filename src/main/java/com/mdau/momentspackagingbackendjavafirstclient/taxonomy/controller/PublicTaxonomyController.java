package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.controller;

import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.CategoryDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SegmentDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SubcategoryDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service.CategoryService;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service.SegmentService;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service.SubcategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Read-only endpoints for the storefront to render the Segment -> Category -> Subcategory browsing tree. */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicTaxonomyController {

    private final SegmentService segmentService;
    private final CategoryService categoryService;
    private final SubcategoryService subcategoryService;

    @GetMapping("/segments")
    public ResponseEntity<List<SegmentDto>> getAllSegments() {
        return ResponseEntity.ok(segmentService.getAllSegments());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories(@RequestParam(required = false) UUID segmentId) {
        if (segmentId != null) {
            return ResponseEntity.ok(categoryService.getBySegment(segmentId));
        }
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/subcategories")
    public ResponseEntity<List<SubcategoryDto>> getSubcategories(@RequestParam(required = false) UUID categoryId) {
        if (categoryId != null) {
            return ResponseEntity.ok(subcategoryService.getByCategory(categoryId));
        }
        return ResponseEntity.ok(subcategoryService.getAllSubcategories());
    }
}
