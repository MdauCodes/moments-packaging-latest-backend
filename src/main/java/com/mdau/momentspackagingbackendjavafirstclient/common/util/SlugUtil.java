package com.mdau.momentspackagingbackendjavafirstclient.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {

    private static final Pattern NON_LATIN      = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE      = Pattern.compile("[\\s]+");
    private static final Pattern LEADING_TRAILING = Pattern.compile("^-|-$");
    private static final Pattern MULTIPLE_DASHES  = Pattern.compile("-{2,}");

    private SlugUtil() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = normalized.toLowerCase(Locale.ENGLISH);
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");
        slug = LEADING_TRAILING.matcher(slug).replaceAll("");
        return slug;
    }
}