package com.mdau.momentspackagingbackendjavafirstclient.tag.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.tag.dto.TagCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.tag.dto.TagDto;
import com.mdau.momentspackagingbackendjavafirstclient.tag.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagService tagService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<List<TagDto>> getAll() {
        return ResponseEntity.ok(tagService.getAllTags());
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<TagDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(tagService.getById(id));
    }

    @IsStaffOrAdmin
    @PostMapping
    public ResponseEntity<TagDto> create(@Valid @RequestBody TagCreateRequest request) {
        TagDto created = tagService.createTag(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}")
    public ResponseEntity<TagDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody TagCreateRequest request) {
        return ResponseEntity.ok(tagService.updateTag(id, request));
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
