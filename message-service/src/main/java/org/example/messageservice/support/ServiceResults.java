package org.example.messageservice.support;

import java.util.HashMap;
import java.util.Map;

public final class ServiceResults {

    private ServiceResults() {
    }

    public static Map<String, Object> ok() {
        return ok(null);
    }

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "ok");
        if (data != null) {
            result.put("data", data);
        }
        return result;
    }

    public static Map<String, Object> error(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        return result;
    }
}
