package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto;

import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Segment;
import lombok.Getter;

import java.util.UUID;

@Getter
public class SegmentDto {

    private final UUID id;
    private final String name;
    private final String slug;
    private final String description;
    private final Integer sortOrder;

    public SegmentDto(Segment segment) {
        this.id          = segment.getId();
        this.name        = segment.getName();
        this.slug        = segment.getSlug();
        this.description = segment.getDescription();
        this.sortOrder   = segment.getSortOrder();
    }
}
