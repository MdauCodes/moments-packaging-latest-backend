package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SubcategoryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SubcategoryDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service.SubcategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subcategories")
@RequiredArgsConstructor
public class AdminSubcategoryController {

    private final SubcategoryService subcategoryService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<List<SubcategoryDto>> getAll(@RequestParam(required = false) UUID categoryId) {
        if (categoryId != null) {
            return ResponseEntity.ok(subcategoryService.getByCategory(categoryId));
        }
        return ResponseEntity.ok(subcategoryService.getAllSubcategories());
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<SubcategoryDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(subcategoryService.getById(id));
    }

    @IsStaffOrAdmin
    @PostMapping
    public ResponseEntity<SubcategoryDto> create(
            @Valid @RequestBody SubcategoryCreateRequest request) {
        SubcategoryDto created = subcategoryService.createSubcategory(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}")
    public ResponseEntity<SubcategoryDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody SubcategoryCreateRequest request) {
        return ResponseEntity.ok(subcategoryService.updateSubcategory(id, request));
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        subcategoryService.deleteSubcategory(id);
        return ResponseEntity.noContent().build();
    }
}
