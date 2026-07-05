package com.mdau.momentspackagingbackendjavafirstclient.tag.dto;

import com.mdau.momentspackagingbackendjavafirstclient.tag.entity.Tag;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TagDto {

    private final UUID id;
    private final String name;
    private final String slug;
    private final String description;

    public TagDto(Tag tag) {
        this.id          = tag.getId();
        this.name        = tag.getName();
        this.slug        = tag.getSlug();
        this.description = tag.getDescription();
    }
}
