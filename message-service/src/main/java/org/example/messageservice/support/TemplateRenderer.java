package org.example.messageservice.support;

import java.util.Map;

public final class TemplateRenderer {

    private TemplateRenderer() {
    }

    public static String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            rendered = rendered.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return rendered;
    }
}
