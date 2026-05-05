package com.mdau.momentspackagingbackendjavafirstclient.blog.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blogs", indexes = {
        @Index(name = "idx_blogs_slug",         columnList = "slug",         unique = true),
        @Index(name = "idx_blogs_status",        columnList = "status"),
        @Index(name = "idx_blogs_published_at",  columnList = "published_at"),
        @Index(name = "idx_blogs_template",      columnList = "template")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(length = 100)
    private String template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BlogStatus status = BlogStatus.DRAFT;

    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    @Column(name = "cover_image_alt", length = 255)
    private String coverImageAlt;

    @Column(name = "cover_image_caption", length = 255)
    private String coverImageCaption;

    @Column(name = "secondary_image_url", length = 512)
    private String secondaryImageUrl;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode body;

    @Column(length = 100)
    private String author;

    @ElementCollection
    @CollectionTable(name = "blog_tags",
                     joinColumns = @JoinColumn(name = "blog_id"))
    @Column(name = "tag", length = 100)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "reading_time_min")
    @Builder.Default
    private Integer readingTimeMin = 1;

    @Column(name = "published_at")
    private Instant publishedAt;
}