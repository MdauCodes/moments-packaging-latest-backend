package com.mdau.momentspackagingbackendjavafirstclient.tag.controller;

import com.mdau.momentspackagingbackendjavafirstclient.tag.dto.TagDto;
import com.mdau.momentspackagingbackendjavafirstclient.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/tags")
@RequiredArgsConstructor
public class PublicTagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<List<TagDto>> getAllTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }
}
