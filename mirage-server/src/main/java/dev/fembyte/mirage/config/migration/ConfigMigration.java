package dev.fembyte.mirage.config.migration;

import java.util.Map;

public interface ConfigMigration {
    int fromVersion();

    int toVersion();

    void apply(Map<String, Object> root);
}
