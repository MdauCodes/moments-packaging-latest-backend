package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto;

import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Subcategory;
import lombok.Getter;

import java.util.UUID;

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
    }
}
