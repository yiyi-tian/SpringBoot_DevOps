package org.example.messageservice.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ConfigMasker {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secretKey", "secretId", "accessKey", "accessKeySecret"
    );

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ConfigMasker() {
    }

    public static String maskJson(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return configJson;
        }
        try {
            Map<String, Object> map = MAPPER.readValue(configJson, new TypeReference<>() {});
            return MAPPER.writeValueAsString(maskMap(map));
        } catch (Exception e) {
            return configJson;
        }
    }

    private static Map<String, Object> maskMap(Map<String, Object> source) {
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) nested;
                masked.put(key, maskMap(nestedMap));
            } else if (SENSITIVE_KEYS.contains(key) && value != null) {
                masked.put(key, "******");
            } else {
                masked.put(key, value);
            }
        }
        return masked;
    }
}
