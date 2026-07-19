package com.agentportal.integration.forgecity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public final class ForgeCityRewriteContract {

    private static final Set<String> FIELDS =
            Set.of("schemaVersion", "appLabel", "title", "text", "maxChars");
    private static final int MAX_APP_LABEL = 80;
    private static final int MAX_TITLE = 200;
    private static final int MAX_TEXT = 2_000;

    private ForgeCityRewriteContract() {
    }

    public static Request parse(ObjectMapper mapper, byte[] json, int configuredMaxChars) {
        if (json == null || json.length == 0 || json.length > 16_384) {
            throw new InvalidRequestException();
        }
        final JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (IOException e) {
            throw new InvalidRequestException();
        }
        if (root == null || !root.isObject() || root.size() != FIELDS.size()) {
            throw new InvalidRequestException();
        }
        Iterator<String> names = root.fieldNames();
        while (names.hasNext()) {
            if (!FIELDS.contains(names.next())) {
                throw new InvalidRequestException();
            }
        }
        JsonNode schemaVersion = root.get("schemaVersion");
        JsonNode maxChars = root.get("maxChars");
        if (schemaVersion == null || !schemaVersion.isIntegralNumber() || schemaVersion.asInt() != 1
                || maxChars == null || !maxChars.isIntegralNumber()) {
            throw new InvalidRequestException();
        }

        String appLabel = boundedText(root.get("appLabel"), MAX_APP_LABEL, false);
        String title = boundedText(root.get("title"), MAX_TITLE, true);
        String text = boundedText(root.get("text"), MAX_TEXT, true);
        int requestedMax = maxChars.asInt(-1);
        if (requestedMax < 1 || configuredMaxChars < 1 || requestedMax > configuredMaxChars
                || (title.isBlank() && text.isBlank())) {
            throw new InvalidRequestException();
        }
        return new Request(1, appLabel, title, text, requestedMax);
    }

    private static String boundedText(JsonNode node, int maxCodePoints, boolean blankAllowed) {
        if (node == null || !node.isTextual()) {
            throw new InvalidRequestException();
        }
        String value = node.asText().trim();
        if ((!blankAllowed && value.isBlank())
                || value.codePointCount(0, value.length()) > maxCodePoints
                || value.indexOf('\0') >= 0) {
            throw new InvalidRequestException();
        }
        return value;
    }

    public record Request(int schemaVersion, String appLabel, String title, String text, int maxChars) {
    }

    public static final class InvalidRequestException extends RuntimeException {
        public InvalidRequestException() {
            super("invalid request");
        }
    }
}
