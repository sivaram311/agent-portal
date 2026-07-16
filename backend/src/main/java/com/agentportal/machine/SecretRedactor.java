package com.agentportal.machine;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deep-redacts JSON-like maps/nodes whose keys look like secrets.
 */
public final class SecretRedactor {

    private static final String REDACTED = "[REDACTED]";
    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            ".*(token|secret|password|passwd|passphrase|api[_-]?key|private|credential|auth|jwt|bearer|ssh|cert|cookie|session|salt).*",
            Pattern.CASE_INSENSITIVE
    );

    private SecretRedactor() {
    }

    public static Object redact(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                if (isSensitiveKey(key)) {
                    out.put(key, REDACTED);
                } else {
                    out.put(key, redact(e.getValue()));
                }
            }
            return out;
        }
        if (value instanceof Iterable<?> it) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (Object o : it) {
                list.add(redact(o));
            }
            return list;
        }
        if (value instanceof JsonNode node) {
            return redactJsonNode(node);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> redactMap(Map<String, Object> input) {
        return (Map<String, Object>) redact(input);
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return SENSITIVE_KEY.matcher(key).matches();
    }

    private static Object redactJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> out = new java.util.LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                if (isSensitiveKey(e.getKey())) {
                    out.put(e.getKey(), REDACTED);
                } else {
                    out.put(e.getKey(), redactJsonNode(e.getValue()));
                }
            }
            return out;
        }
        if (node.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (JsonNode child : node) {
                list.add(redactJsonNode(child));
            }
            return list;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }

    public static Set<String> sensitiveKeyHints() {
        return Set.of("token", "secret", "password", "apiKey", "private", "credential", "auth", "jwt");
    }

    public static String redactedMarker() {
        return REDACTED;
    }

    public static boolean looksLikeSecretValue(String key, String value) {
        if (!isSensitiveKey(key)) {
            return false;
        }
        return value != null && !value.isBlank();
    }

    public static Locale locale() {
        return Locale.ROOT;
    }
}
