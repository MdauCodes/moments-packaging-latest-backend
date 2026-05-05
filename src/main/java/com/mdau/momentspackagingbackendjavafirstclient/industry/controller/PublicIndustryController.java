package com.mdau.momentspackagingbackendjavafirstclient.industry.controller;

import com.mdau.momentspackagingbackendjavafirstclient.industry.dto.IndustryDto;
import com.mdau.momentspackagingbackendjavafirstclient.industry.service.IndustryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/industries")
@RequiredArgsConstructor
public class PublicIndustryController {

    private final IndustryService industryService;

    @GetMapping
    public ResponseEntity<List<IndustryDto>> getAllIndustries() {
        return ResponseEntity.ok(industryService.getAllIndustries());
    }
}