package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fuzzy name-matching between Riseller catalog items and admin-created products.
 *
 * Matching tiers (checked in order, returns on first hit):
 *   1. Riseller code is already in the product's keywords list → score 1.0
 *   2. Riseller code is in the product's tags list            → score 0.98
 *   3. Riseller code appears as a word token in product name  → score 0.95
 *   4. Jaccard token similarity on normalized names           → score ≥ threshold
 *
 * Name normalization strips pack-size parentheticals like "(100PCS)", "(1*10KGS)",
 * lowercases everything, and splits on whitespace / special characters.
 */
public final class RisellerNameMatcher {

    // Strips things like (100PCS), (1*10KGS), (30PKTS*50PCS), (1*4PCS)
    private static final Pattern PACK_PARENTHETICAL = Pattern.compile("\\([^)]{1,50}\\)");
    private static final Pattern SPECIAL_CHARS      = Pattern.compile("[#*@&$%!+=|<>\\\\/_]");
    private static final Pattern MULTI_SPACE        = Pattern.compile("\\s+");

    // Very generic words that add noise without helping discrimination
    private static final Set<String> STOP = Set.of("no", "and", "or", "the", "of", "in", "for",
            "to", "by", "with", "is", "at", "on", "an", "it");

    private RisellerNameMatcher() {}

    /**
     * Normalise a product name into a set of meaningful tokens.
     *
     * Single-letter/digit tokens are KEPT because they often identify product variants:
     *   "DESSERT CUPS E" → {dessert, cups, e}   ← 'e' distinguishes Cup E from Cup F
     *   "TECH 2CUP CARRIER(100PCS)" → {tech, 2cup, carrier}   ← pack size stripped
     *   "Baraka Khaki Bag No. 2"    → {baraka, khaki, bag, no., 2}
     */
    public static Set<String> tokenize(String name) {
        if (name == null || name.isBlank()) return Collections.emptySet();
        String s = name.toLowerCase(Locale.ROOT);
        s = PACK_PARENTHETICAL.matcher(s).replaceAll(" ");
        s = SPECIAL_CHARS.matcher(s).replaceAll(" ");
        s = MULTI_SPACE.matcher(s.trim()).replaceAll(" ");
        return Arrays.stream(s.split("\\s+"))
                .filter(t -> !t.isEmpty())
                .filter(t -> !STOP.contains(t))
                // keep single chars only if alphabetic (variant codes like 'e', 'f', 'g')
                // or single digits (size/variant numbers like '2', '4')
                .filter(t -> t.length() >= 2
                        || Character.isLetter(t.charAt(0))
                        || Character.isDigit(t.charAt(0)))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Strip pack-size parentheticals for use as a clean display name. */
    public static String cleanDisplayName(String risellerName) {
        if (risellerName == null || risellerName.isBlank()) return "Unnamed Product";
        String s = PACK_PARENTHETICAL.matcher(risellerName).replaceAll("").trim();
        s = MULTI_SPACE.matcher(s).replaceAll(" ").trim();
        return s.isBlank() ? risellerName.trim() : s;
    }

    /** Jaccard similarity on tokenised names. 0.0 = no overlap, 1.0 = identical. */
    public static double jaccardScore(String a, String b) {
        Set<String> ta = tokenize(a);
        Set<String> tb = tokenize(b);
        if (ta.isEmpty() && tb.isEmpty()) return 1.0;
        if (ta.isEmpty() || tb.isEmpty()) return 0.0;
        long intersection = ta.stream().filter(tb::contains).count();
        long union = (long) ta.size() + tb.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    public record MatchResult(Product product, double score, String reason) {}

    /**
     * Outcome of a match attempt — either a single clear winner, multiple ambiguous
     * candidates (none auto-linked), or nothing found.
     */
    public sealed interface MatchOutcome permits
            MatchOutcome.Clear, MatchOutcome.Ambiguous, MatchOutcome.NotFound {

        /** Single product scored above threshold AND is clearly ahead of any runner-up. */
        record Clear(MatchResult winner) implements MatchOutcome {}

        /**
         * Multiple products scored above threshold with scores too close to pick safely.
         * The system should NOT auto-link; instead it should flag for manual review.
         */
        record Ambiguous(List<MatchResult> candidates) implements MatchOutcome {}

        /** No product reached the minimum threshold. */
        record NotFound() implements MatchOutcome {}
    }

    /**
     * Resolve a Riseller catalog item against a pool of unlinked DB products.
     *
     * Rules:
     *   1. Exact-match tiers (keyword / tag / code-in-name) always win immediately.
     *   2. If only ONE product passes {@code threshold} → Clear win.
     *   3. If MULTIPLE products pass {@code threshold} AND the gap between the top two
     *      scores is >= {@code minGap} → Clear win (best is unambiguously ahead).
     *   4. If MULTIPLE products pass with gap < {@code minGap} → Ambiguous; admin must decide.
     *
     * @param risellerCode numeric code string, e.g. "634"
     * @param risellerName display name, e.g. "TECH 2CUP CARRIER(100PCS)"
     * @param candidates   pool of unlinked DB products to search
     * @param threshold    minimum Jaccard score to be considered (e.g. 0.55)
     * @param minGap       minimum score gap to declare a clear winner (e.g. 0.15)
     */
    public static MatchOutcome resolve(
            String risellerCode,
            String risellerName,
            List<Product> candidates,
            double threshold,
            double minGap) {

        // ── Exact-match tiers (deterministic, no ambiguity possible) ─────────────
        for (Product p : candidates) {
            if (p.getKeywords() != null
                    && p.getKeywords().stream().anyMatch(k -> k.equalsIgnoreCase(risellerCode))) {
                return new MatchOutcome.Clear(new MatchResult(p, 1.0, "keyword-exact"));
            }
            if (p.getTags() != null
                    && p.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(risellerCode))) {
                return new MatchOutcome.Clear(new MatchResult(p, 0.98, "tag-exact"));
            }
            String pName = p.getName() == null ? "" : p.getName();
            Set<String> nameTokens = new HashSet<>(Arrays.asList(
                    pName.toLowerCase(Locale.ROOT).split("[\\s\\-_/#.,]")));
            if (nameTokens.contains(risellerCode.toLowerCase(Locale.ROOT))) {
                return new MatchOutcome.Clear(new MatchResult(p, 0.95, "code-in-name"));
            }
        }

        // ── Fuzzy name scoring — collect ALL candidates above threshold ───────────
        List<MatchResult> hits = candidates.stream()
                .map(p -> {
                    double score = jaccardScore(risellerName, p.getName() == null ? "" : p.getName());
                    return new MatchResult(p, score, String.format("jaccard(%.2f)", score));
                })
                .filter(m -> m.score() >= threshold)
                .sorted(Comparator.comparingDouble(MatchResult::score).reversed())
                .toList();

        if (hits.isEmpty()) {
            return new MatchOutcome.NotFound();
        }

        if (hits.size() == 1) {
            return new MatchOutcome.Clear(hits.get(0));
        }

        // Multiple hits — check if best is clearly ahead of the runner-up
        double best   = hits.get(0).score();
        double second = hits.get(1).score();
        if (best - second >= minGap) {
            return new MatchOutcome.Clear(hits.get(0));
        }

        // Scores too close — don't guess; surface all candidates for admin review
        return new MatchOutcome.Ambiguous(hits);
    }
}
