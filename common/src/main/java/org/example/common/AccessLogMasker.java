package org.example.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class AccessLogMasker {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "token", "credential", "secretkey", "secretid",
            "authorization", "code", "accesskey", "privatekey"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([\\w.%+-]{1,3})[\\w.%+-]*(@[\\w.-]+\\.[A-Za-z]{2,})");
    private static final Pattern SENSITIVE_KV_PATTERN = Pattern.compile(
            "(?i)(password|secret|token|credential|authorization|accesskey|privatekey|secretkey|secretid)\\s*=\\s*[^&\\s\"']+");

    private AccessLogMasker() {
    }

    public static String maskAndTruncate(String raw, int maxLength) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String masked = maskJsonOrPlain(raw);
        if (masked.length() > maxLength) {
            return masked.substring(0, maxLength) + "...[truncated]";
        }
        return masked;
    }

    private static String maskJsonOrPlain(String raw) {
        try {
            JsonNode node = MAPPER.readTree(raw);
            maskNode(node);
            return MAPPER.writeValueAsString(node);
        } catch (Exception ignored) {
            return maskPlainText(raw);
        }
    }

    private static void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (value.isValueNode() && isSensitiveKey(key)) {
                    obj.put(key, "***");
                } else if (value.isValueNode() && value.isTextual()) {
                    obj.put(key, maskPlainText(value.asText()));
                } else {
                    maskNode(value);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskNode(child);
            }
        }
    }

    private static boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.contains(key.toLowerCase());
    }

    private static String maskPlainText(String text) {
        if (text == null) {
            return null;
        }
        String result = PHONE_PATTERN.matcher(text).replaceAll("$1****$2");
        result = EMAIL_PATTERN.matcher(result).replaceAll("$1***$2");
        result = SENSITIVE_KV_PATTERN.matcher(result).replaceAll("$1=***");
        return result;
    }
}
