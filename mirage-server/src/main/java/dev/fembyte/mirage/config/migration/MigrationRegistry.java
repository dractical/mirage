package dev.fembyte.mirage.config.migration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MigrationRegistry {
    private final List<ConfigMigration> migrations = new ArrayList<>();

    public void register(ConfigMigration migration) {
        migrations.add(migration);
    }

    public boolean applyMigrations(Map<String, Object> root, int fromVersion, int toVersion) {
        if (fromVersion >= toVersion) {
            return false;
        }
        boolean migrated = false;
        int current = fromVersion;
        List<ConfigMigration> ordered = migrations.stream()
            .sorted(Comparator.comparingInt(ConfigMigration::fromVersion))
            .toList();
        for (ConfigMigration migration : ordered) {
            if (migration.fromVersion() != current) {
                continue;
            }
            if (migration.toVersion() > toVersion) {
                break;
            }
            migration.apply(root);
            current = migration.toVersion();
            migrated = true;
        }
        return migrated;
    }
}
