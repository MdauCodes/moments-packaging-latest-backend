package com.mdau.momentspackagingbackendjavafirstclient.blog.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.mdau.momentspackagingbackendjavafirstclient.blog.entity.Blog;
import com.mdau.momentspackagingbackendjavafirstclient.blog.entity.BlogStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
public class BlogDto {

    private final UUID        id;
    private final String      slug;
    private final String      title;
    private final String      excerpt;
    private final String      template;
    private final BlogStatus  status;
    private final CoverImage  coverImage;
    private final String      secondaryImage;
    private final JsonNode    body;
    private final String      author;
    private final List<String> tags;
    private final Integer     readingTimeMin;
    private final Instant     publishedAt;
    private final Instant     createdAt;
    private final Instant     updatedAt;

    public BlogDto(Blog blog) {
        this.id             = blog.getId();
        this.slug           = blog.getSlug();
        this.title          = blog.getTitle();
        this.excerpt        = blog.getExcerpt();
        this.template       = blog.getTemplate();
        this.status         = blog.getStatus();
        this.secondaryImage = blog.getSecondaryImageUrl();
        this.body           = blog.getBody();
        this.author         = blog.getAuthor();
        this.tags           = blog.getTags();
        this.readingTimeMin = blog.getReadingTimeMin();
        this.publishedAt    = blog.getPublishedAt();
        this.createdAt      = blog.getCreatedAt();
        this.updatedAt      = blog.getUpdatedAt();
        this.coverImage     = new CoverImage(
                blog.getCoverImageUrl(),
                blog.getCoverImageAlt(),
                blog.getCoverImageCaption()
        );
    }

    @Getter
    public static class CoverImage {
        private final String url;
        private final String alt;
        private final String caption;

        public CoverImage(String url, String alt, String caption) {
            this.url     = url;
            this.alt     = alt;
            this.caption = caption;
        }
    }
}