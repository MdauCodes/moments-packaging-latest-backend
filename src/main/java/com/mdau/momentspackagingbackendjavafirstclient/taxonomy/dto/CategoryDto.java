package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto;

import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Category;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class CategoryDto {

    private final UUID id;
    private final UUID segmentId;
    private final String segmentName;
    private final String name;
    private final String slug;
    private final String description;
    private final Integer sortOrder;
    private final List<UUID> industryIds;
    private final List<String> industryNames;

    public CategoryDto(Category category) {
        this.id          = category.getId();
        this.segmentId   = category.getSegment().getId();
        this.segmentName = category.getSegment().getName();
        this.name        = category.getName();
        this.slug        = category.getSlug();
        this.description = category.getDescription();
        this.sortOrder   = category.getSortOrder();
        this.industryIds = category.getIndustries().stream().map(Industry::getId).collect(Collectors.toList());
        this.industryNames = category.getIndustries().stream().map(Industry::getName).collect(Collectors.toList());
    }
}
