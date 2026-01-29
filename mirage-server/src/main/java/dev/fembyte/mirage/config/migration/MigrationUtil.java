package dev.fembyte.mirage.config.migration;

import dev.fembyte.mirage.config.util.ConfigPath;
import java.util.Map;

public final class MigrationUtil {
    private MigrationUtil() {
    }

    public static void rename(Map<String, Object> root, String from, String to) {
        move(root, from, to);
    }

    public static void move(Map<String, Object> root, String from, String to) {
        Object value = ConfigPath.remove(root, from);
        if (value != null) {
            ConfigPath.set(root, to, value);
        }
    }

    public static void delete(Map<String, Object> root, String path) {
        ConfigPath.remove(root, path);
    }

    public static void setDefault(Map<String, Object> root, String path, Object value) {
        if (ConfigPath.get(root, path) == null) {
            ConfigPath.set(root, path, value);
        }
    }
}
