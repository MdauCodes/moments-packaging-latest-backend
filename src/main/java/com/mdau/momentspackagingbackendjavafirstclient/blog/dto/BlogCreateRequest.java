package com.mdau.momentspackagingbackendjavafirstclient.blog.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BlogCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String excerpt;

    private String template;

    private String coverImageUrl;

    private String coverImageAlt;

    private String coverImageCaption;

    private String secondaryImageUrl;

    private JsonNode body;

    private String author;

    private List<String> tags = new ArrayList<>();
}