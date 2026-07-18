package com.mdau.momentspackagingbackendjavafirstclient.blog.repository;

import com.mdau.momentspackagingbackendjavafirstclient.blog.entity.Blog;
import com.mdau.momentspackagingbackendjavafirstclient.blog.entity.BlogStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogRepository extends JpaRepository<Blog, UUID> {

    Optional<Blog> findBySlugAndStatus(String slug, BlogStatus status);

    Optional<Blog> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
        SELECT b FROM Blog b
        WHERE (:status IS NULL OR b.status = :status)
        AND (:template IS NULL OR b.template = :template)
        ORDER BY b.createdAt DESC
        """)
    List<Blog> findAllByStatusAndTemplate(
            @Param("status")   BlogStatus status,
            @Param("template") String template,
            Pageable pageable
    );

    @Query("""
        SELECT b FROM Blog b
        WHERE b.status = 'PUBLISHED'
        ORDER BY b.publishedAt DESC
        """)
    List<Blog> findLatestPublished(Pageable pageable);

    @Query("""
        SELECT b FROM Blog b
        WHERE b.status = 'PUBLISHED'
        AND b.slug != :slug
        ORDER BY FUNCTION('RANDOM')
        """)
    List<Blog> findRelated(@Param("slug") String slug, Pageable pageable);
}