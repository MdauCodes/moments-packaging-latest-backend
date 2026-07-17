package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto;

import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Subcategory;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class SubcategoryDto {

    private final UUID id;
    private final UUID categoryId;
    private final String categoryName;
    private final UUID segmentId;
    private final String segmentName;
    private final String name;
    private final String slug;
    private final String description;
    private final Integer sortOrder;
    /** Industries this subcategory is directly tagged with — independent of its category's own tags. */
    private final List<UUID> industryIds;
    private final List<String> industryNames;

    public SubcategoryDto(Subcategory subcategory) {
        this.id           = subcategory.getId();
        this.categoryId   = subcategory.getCategory().getId();
        this.categoryName = subcategory.getCategory().getName();
        this.segmentId    = subcategory.getCategory().getSegment().getId();
        this.segmentName  = subcategory.getCategory().getSegment().getName();
        this.name         = subcategory.getName();
        this.slug         = subcategory.getSlug();
        this.description  = subcategory.getDescription();
        this.sortOrder    = subcategory.getSortOrder();
        this.industryIds   = subcategory.getIndustries().stream().map(Industry::getId).collect(Collectors.toList());
        this.industryNames = subcategory.getIndustries().stream().map(Industry::getName).collect(Collectors.toList());
    }
}
