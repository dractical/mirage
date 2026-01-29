package dev.fembyte.mirage.command.subcommands;

import dev.fembyte.mirage.command.MirageSubcommand;
import dev.fembyte.mirage.command.annotations.MirageCommand;
import dev.fembyte.mirage.config.ConfigManager;
import dev.fembyte.mirage.config.modules.ConfigSystemConfig;
import dev.fembyte.mirage.util.MirageText;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

@MirageCommand(
    name = "reload",
    description = "Reload Mirage configuration",
    usage = "reload",
    permission = "mirage.command.reload",
    order = 10
)
public final class ReloadCommand implements MirageSubcommand {
    @Override
    public int execute(CommandSender sender, String[] args) {
        ConfigManager manager = ConfigManager.global();
        if (manager == null) {
            Component message = MirageText.prefixed("<red>Config system is not initialized.</red>");
            MirageText.broadcast(sender, message);
            return 0;
        }
        long start = System.nanoTime();
        boolean ok = manager.reload();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        ConfigSystemConfig systemConfig = manager.get(ConfigSystemConfig.class);
        manager.setWatchingEnabled(systemConfig.watchFileChanges);

        Component message;
        if (ok) {
            message = MirageText.prefixed("<green>Configuration reloaded</green> <gray>(" + elapsedMs + " ms)</gray>");
        } else {
            message = MirageText.prefixed("<red>Configuration reload failed</red> <gray>(" + elapsedMs + " ms)</gray>");
        }
        MirageText.broadcast(sender, message);
        return ok ? 1 : 0;
    }
}
