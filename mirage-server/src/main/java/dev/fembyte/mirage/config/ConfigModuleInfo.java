package dev.fembyte.mirage.config;

import java.util.List;

public record ConfigModuleInfo(
    Class<?> type,
    ConfigModule instance,
    ConfigModule defaultInstance,
    String name,
    String category,
    boolean reloadable,
    int order,
    List<ConfigFieldInfo> fields
) {
}
