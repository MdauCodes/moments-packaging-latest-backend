package com.mdau.momentspackagingbackendjavafirstclient.settings.controller;

import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.PublicConfigDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/config")
@RequiredArgsConstructor
public class PublicConfigController {

    @Value("${app.features.blogs-enabled}")
    private Boolean blogsEnabled;

    @Value("${app.features.email-capture-enabled}")
    private Boolean emailCaptureEnabled;

    @Value("${app.company.whatsapp-number}")
    private String whatsappNumber;

    @Value("${app.company.email}")
    private String companyEmail;

    @Value("${app.company.phone}")
    private String companyPhone;

    @Cacheable("public-config")
    @GetMapping
    public ResponseEntity<PublicConfigDto> getConfig() {
        return ResponseEntity.ok(new PublicConfigDto(
                blogsEnabled,
                emailCaptureEnabled,
                whatsappNumber,
                companyEmail,
                companyPhone
        ));
    }
}