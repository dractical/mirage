package dev.fembyte.mirage.config;

import dev.fembyte.mirage.config.annotations.Experimental;
import dev.fembyte.mirage.config.io.YamlConfigLoader;
import dev.fembyte.mirage.config.io.YamlConfigWriter;
import dev.fembyte.mirage.config.migration.ConfigMigration;
import dev.fembyte.mirage.config.migration.MigrationRegistry;
import dev.fembyte.mirage.config.util.ConfigPath;
import dev.fembyte.mirage.config.util.TypeConverter;
import dev.fembyte.mirage.config.validation.ConfigValidator;
import dev.fembyte.mirage.config.validation.ValidationResult;
import dev.fembyte.mirage.config.validation.ValidationUtil;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigManager {
    private static final String VERSION_KEY = "config-version";
    private static ConfigManager global;

    private final Path configPath;
    private final int currentVersion;
    private final ConfigRegistry registry;
    private final MigrationRegistry migrations = new MigrationRegistry();
    private final YamlConfigLoader loader = new YamlConfigLoader();
    private final YamlConfigWriter writer = new YamlConfigWriter();
    private final Logger logger;
    private final Map<Class<?>, ConfigModuleInfo> modulesByClass = new HashMap<>();
    private final Set<String> experimentalWarnings = ConcurrentHashMap.newKeySet();
    private final Duration reloadDebounce = Duration.ofMillis(250);
    private volatile Throwable lastError;

    private WatchService watchService;
    private ExecutorService watchExecutor;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pendingReload;

    public ConfigManager(Path configPath, int currentVersion, String scanPackage) {
        this.configPath = configPath;
        this.currentVersion = currentVersion;
        this.registry = ConfigRegistry.scan(scanPackage);
        this.logger = Logger.getLogger("MirageConfig");
        for (ConfigModuleInfo module : registry.modules()) {
            modulesByClass.put(module.type(), module);
        }
    }

    public static ConfigManager global() {
        return global;
    }

    public void setGlobal() {
        global = this;
    }

    public void registerMigration(ConfigMigration migration) {
        migrations.register(migration);
    }

    public <T extends ConfigModule> T get(Class<T> type) {
        ConfigModuleInfo info = modulesByClass.get(type);
        if (info == null) {
            throw new IllegalArgumentException("No config module registered for " + type.getName());
        }
        return type.cast(info.instance());
    }

    public List<ConfigModuleInfo> modules() {
        return registry.modules();
    }

    public synchronized boolean load() {
        return loadInternal(false);
    }

    public synchronized boolean reload() {
        return loadInternal(true);
    }

    public synchronized boolean save() {
        try {
            Files.createDirectories(configPath.getParent());
            String yaml = writer.write(currentVersion, registry.modules());
            Files.writeString(configPath, yaml);
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to save config", ex);
            if (lastError == null) {
                lastError = ex;
            }
            return false;
        }
    }

    public void startWatching() {
        if (watchService != null) {
            return;
        }
        try {
            Files.createDirectories(configPath.getParent());
            watchService = FileSystems.getDefault().newWatchService();
            configPath.getParent().register(watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to start config watcher", ex);
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Mirage-Config-Reload"));
        watchExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Mirage-Config-Watch"));
        watchExecutor.submit(this::watchLoop);
    }

    public void stopWatching() {
        if (watchService == null) {
            return;
        }
        try {
            watchService.close();
        } catch (IOException ignored) {
        }
        watchService = null;
        if (watchExecutor != null) {
            watchExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    public void setWatchingEnabled(boolean enabled) {
        if (enabled) {
            startWatching();
        } else {
            stopWatching();
        }
    }

    private void watchLoop() {
        while (watchService != null) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Object context = event.context();
                    if (context == null) {
                        continue;
                    }
                    Path changed = (Path) context;
                    if (!changed.getFileName().equals(configPath.getFileName())) {
                        continue;
                    }
                    scheduleReload();
                }
                key.reset();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Config watcher error", ex);
                return;
            }
        }
    }

    private void scheduleReload() {
        if (scheduler == null) {
            return;
        }
        if (pendingReload != null) {
            pendingReload.cancel(false);
        }
        pendingReload = scheduler.schedule(this::reload, reloadDebounce.toMillis(), TimeUnit.MILLISECONDS);
    }

    private boolean loadInternal(boolean isReload) {
        lastError = null;
        boolean ok = true;
        Map<String, Object> root;
        boolean fileMissing = Files.notExists(configPath);
        try {
            root = loader.load(configPath);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to load config", ex);
            lastError = ex;
            return false;
        }

        int fileVersion = readVersion(root);
        if (fileVersion > currentVersion) {
            logger.warning("Config version " + fileVersion + " is newer than supported " + currentVersion);
        }
        boolean migrated = false;
        if (fileVersion < currentVersion) {
            migrated = migrations.applyMigrations(root, fileVersion, currentVersion);
            root.put(VERSION_KEY, currentVersion);
        }

        boolean dirty = fileMissing || migrated;
        for (ConfigModuleInfo module : registry.modules()) {
            if (isReload && !module.reloadable()) {
                continue;
            }
            boolean moduleDirty = applyModule(root, module);
            dirty = dirty || moduleDirty;
        }

        if (dirty) {
            ok = save();
        }
        return ok;
    }

    private boolean applyModule(Map<String, Object> root, ConfigModuleInfo module) {
        boolean dirty = false;
        for (ConfigFieldInfo fieldInfo : module.fields()) {
            Field field = fieldInfo.field();
            Object defaultValue;
            try {
                defaultValue = field.get(module.defaultInstance());
            } catch (IllegalAccessException ex) {
                logger.log(Level.WARNING, "Failed to read default for " + field.getName(), ex);
                continue;
            }
            String path = buildPath(module, fieldInfo.key());
            Object raw = ConfigPath.get(root, path);
            Object value = defaultValue;
            if (raw != null) {
                try {
                    value = TypeConverter.convert(raw, field.getType(), field.getGenericType());
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Invalid value for " + path + " (" + formatValue(raw) + "): " + ex.getMessage());
                    dirty = true;
                }
            } else {
                dirty = true;
            }
            ValidationResult validation = ValidationUtil.validateAnnotation(value, fieldInfo.validate());
            if (!validation.valid()) {
                logger.warning("Invalid value for " + path + " (" + formatValue(value) + "): " + validation.message());
                value = defaultValue;
                dirty = true;
            }
            ConfigValidator<?> customValidator = fieldInfo.newValidator();
            if (customValidator != null) {
                @SuppressWarnings("unchecked")
                ValidationResult customResult = ((ConfigValidator<Object>) customValidator).validate(value);
                if (!customResult.valid()) {
                    logger.warning("Invalid value for " + path + " (" + formatValue(value) + "): " + customResult.message());
                    value = defaultValue;
                    dirty = true;
                }
            }
            try {
                field.set(module.instance(), value);
            } catch (IllegalAccessException ex) {
                logger.log(Level.WARNING, "Failed to set value for " + path, ex);
                continue;
            }
            if (field.isAnnotationPresent(Experimental.class)) {
                warnExperimental(module, fieldInfo, value, defaultValue);
            }
        }
        return dirty;
    }

    private void warnExperimental(ConfigModuleInfo module, ConfigFieldInfo fieldInfo, Object value, Object defaultValue) {
        if (!isExperimentalEnabled(value, defaultValue)) {
            return;
        }
        String key = module.name() + "." + fieldInfo.key();
        if (experimentalWarnings.add(key)) {
            logger.warning("Experimental config enabled: " + key);
        }
    }

    private boolean isExperimentalEnabled(Object value, Object defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0.0;
        }
        if (value instanceof String string) {
            return !string.isBlank();
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        return !Objects.equals(value, defaultValue);
    }

    private int readVersion(Map<String, Object> root) {
        Object raw = root.get(VERSION_KEY);
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String buildPath(ConfigModuleInfo module, String key) {
        if (module.category() == null || module.category().isEmpty()) {
            return module.name() + '.' + key;
        }
        return module.category() + '.' + module.name() + '.' + key;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "'" + string + "' (String)";
        }
        return value + " (" + value.getClass().getSimpleName() + ")";
    }
}
