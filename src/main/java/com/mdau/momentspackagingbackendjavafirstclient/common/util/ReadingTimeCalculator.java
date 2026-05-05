package com.mdau.momentspackagingbackendjavafirstclient.common.util;

import com.fasterxml.jackson.databind.JsonNode;

public final class ReadingTimeCalculator {

    private static final int WORDS_PER_MINUTE = 200;

    private ReadingTimeCalculator() {}

    public static int calculate(JsonNode body) {
        if (body == null) return 1;
        String text = extractText(body);
        if (text.isBlank()) return 1;
        String[] words = text.trim().split("\\s+");
        int minutes = (int) Math.ceil((double) words.length / WORDS_PER_MINUTE);
        return Math.max(1, minutes);
    }

    private static String extractText(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        if (node.isTextual()) {
            sb.append(node.asText()).append(" ");
        } else if (node.isArray() || node.isObject()) {
            node.forEach(child -> sb.append(extractText(child)));
        }
        return sb.toString();
    }
}