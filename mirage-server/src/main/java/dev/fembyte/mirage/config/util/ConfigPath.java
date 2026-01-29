package dev.fembyte.mirage.config.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigPath {
    private ConfigPath() {
    }

    @SuppressWarnings("unchecked")
    public static Object get(Map<String, Object> root, String path) {
        if (root == null || path == null || path.isEmpty()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length; i++) {
            Object value = current.get(parts[i]);
            if (i == parts.length - 1) {
                return value;
            }
            if (!(value instanceof Map<?, ?>)) {
                return null;
            }
            current = (Map<String, Object>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void set(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1) {
                current.put(part, value);
                return;
            }
            Object existing = current.get(part);
            if (!(existing instanceof Map<?, ?>)) {
                Map<String, Object> child = new LinkedHashMap<>();
                current.put(part, child);
                current = child;
            } else {
                current = (Map<String, Object>) existing;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Object remove(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1) {
                return current.remove(part);
            }
            Object existing = current.get(part);
            if (!(existing instanceof Map<?, ?>)) {
                return null;
            }
            current = (Map<String, Object>) existing;
        }
        return null;
    }
}
