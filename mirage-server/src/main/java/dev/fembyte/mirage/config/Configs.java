package dev.fembyte.mirage.config;

public final class Configs {
    private Configs() {
    }

    public static <T extends ConfigModule> T get(Class<T> type) {
        ConfigManager manager = ConfigManager.global();
        if (manager == null) {
            throw new IllegalStateException("ConfigManager global instance is not set");
        }
        return manager.get(type);
    }
}
