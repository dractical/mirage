package dev.fembyte.mirage.config.modules;

import dev.fembyte.mirage.config.ConfigModule;
import dev.fembyte.mirage.config.annotations.Comment;
import dev.fembyte.mirage.config.annotations.ConfigSpec;

@ConfigSpec(name = "config", category = "mirage")
@Comment({"Mirage configuration system settings."})
public final class ConfigSystemConfig implements ConfigModule {
    @Comment({"Enable watching the config file for hot reloads.", "Default: false"})
    public boolean watchFileChanges = false;
}
