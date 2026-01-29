package dev.fembyte.mirage.config.io;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class YamlConfigLoader {
    private final Yaml yaml;

    public YamlConfigLoader() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(50);
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> load(Path path) throws IOException {
        if (Files.notExists(path)) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            Object loaded = yaml.load(reader);
            if (loaded == null) {
                return new LinkedHashMap<>();
            }
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new IOException("Root YAML document must be a map");
            }
            return (Map<String, Object>) map;
        }
    }
}
