package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;

    private static final int W_NAME_EXACT        = 1000;
    private static final int W_NAME_STARTS       = 600;
    private static final int W_NAME_CONTAINS     = 400;
    private static final int W_NAME_TOKEN        = 220;

    private static final int W_INDUSTRY_EXACT    = 180;
    private static final int W_INDUSTRY_CONTAINS = 120;
    private static final int W_INDUSTRY_KEYWORD  = 90;

    private static final int W_DESC_PHRASE       = 80;
    private static final int W_KEYWORD           = 70;
    private static final int W_CATEGORY          = 60;
    private static final int W_TAG               = 50;
    private static final int W_MATERIAL          = 45;
    private static final int W_DESC_TOKEN        = 35;
    private static final int W_FINISH            = 30;
    private static final int W_SIZE              = 25;

    private static final int POPULARITY_DIVISOR  = 50;
    private static final int POPULARITY_MAX      = 25;

    @Transactional(readOnly = true)
    public List<ProductDto> search(String query, int limit) {
        int cappedLimit = Math.min(limit, 50);
        if (query == null || query.isBlank()) return List.of();

        String   q      = query.trim().toLowerCase();
        String[] tokens = q.split("\\s+");

        List<Product> all = productRepository.findAll();

        return all.stream()
                .map(p -> new ScoredProduct(p, score(p, q, tokens)))
                .filter(sp -> sp.score > 0)
                .sorted(Comparator.comparingInt(ScoredProduct::getScore).reversed())
                .limit(cappedLimit)
                .map(sp -> new ProductDto(sp.product))
                .collect(Collectors.toList());
    }

    private int score(Product p, String q, String[] tokens) {
        int    score    = 0;
        String name     = lower(p.getName());
        String desc     = lower(p.getDescription());
        String category = lower(p.getCategory());
        String material = lower(p.getMaterial());
        String finish   = lower(p.getFinish());

        // ── Name ────────────────────────────────────────────────────────────
        if (name.equals(q))          score += W_NAME_EXACT;
        else if (name.startsWith(q)) score += W_NAME_STARTS;
        else if (name.contains(q))   score += W_NAME_CONTAINS;
        else {
            for (String t : tokens) {
                if (name.contains(t)) score += W_NAME_TOKEN;
            }
        }

        // ── Industry ────────────────────────────────────────────────────────
        for (var industry : p.getIndustries()) {
            String iName = lower(industry.getName());
            if (iName.equals(q))           score += W_INDUSTRY_EXACT;
            else if (iName.contains(q))    score += W_INDUSTRY_CONTAINS;
            else {
                for (String t : tokens) {
                    if (iName.contains(t)) score += W_INDUSTRY_KEYWORD;
                }
            }
        }

        // ── Description ─────────────────────────────────────────────────────
        if (!desc.isEmpty()) {
            if (desc.contains(q)) score += W_DESC_PHRASE;
            else {
                for (String t : tokens) {
                    if (desc.contains(t)) score += W_DESC_TOKEN;
                }
            }
        }

        // ── Category ────────────────────────────────────────────────────────
        if (!category.isEmpty() && category.contains(q)) score += W_CATEGORY;

        // ── Keywords ────────────────────────────────────────────────────────
        for (String kw : p.getKeywords()) {
            String k = lower(kw);
            if (k.equals(q) || k.contains(q))   score += W_KEYWORD;
            else {
                for (String t : tokens) {
                    if (k.contains(t)) score += W_KEYWORD / 2;
                }
            }
        }

        // ── Tags ────────────────────────────────────────────────────────────
        for (String tag : p.getTags()) {
            String t = lower(tag);
            if (t.equals(q) || t.contains(q)) score += W_TAG;
        }

        // ── Material ────────────────────────────────────────────────────────
        if (!material.isEmpty() && material.contains(q)) score += W_MATERIAL;

        // ── Finish ──────────────────────────────────────────────────────────
        if (!finish.isEmpty() && finish.contains(q)) score += W_FINISH;

        // ── Sizes (list) ────────────────────────────────────────────────────
        for (String sz : p.getSizes()) {
            if (lower(sz).contains(q)) score += W_SIZE;
        }

        // ── Popularity boost ────────────────────────────────────────────────
        if (score > 0) {
            int boost = (int) Math.min(
                    p.getMonthlyClicks() / POPULARITY_DIVISOR,
                    POPULARITY_MAX);
            score += boost;
        }

        return score;
    }

    private String lower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private static class ScoredProduct {
        final Product product;
        final int     score;

        ScoredProduct(Product product, int score) {
            this.product = product;
            this.score   = score;
        }

        int getScore() { return score; }
    }
}