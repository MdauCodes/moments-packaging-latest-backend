package com.mdau.momentspackagingbackendjavafirstclient.industry.dto;

import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import lombok.Getter;

import java.util.UUID;

@Getter
public class IndustryDto {

    private final UUID id;
    private final String name;
    private final String slug;
    private final String description;
    private final String iconUrl;

    public IndustryDto(Industry industry) {
        this.id          = industry.getId();
        this.name        = industry.getName();
        this.slug        = industry.getSlug();
        this.description = industry.getDescription();
        this.iconUrl     = industry.getIconUrl();
    }
}