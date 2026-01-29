package dev.fembyte.mirage.command.subcommands.status;

import com.mojang.brigadier.Command;
import dev.fembyte.mirage.command.MirageSubcommand;
import dev.fembyte.mirage.command.annotations.MirageCommand;
import dev.fembyte.mirage.config.Configs;
import dev.fembyte.mirage.config.modules.RandomTickConfig;
import dev.fembyte.mirage.system.randomtick.RandomTickStatus;
import dev.fembyte.mirage.system.randomtick.RandomTickSystem;
import dev.fembyte.mirage.util.MirageText;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;

import java.util.Locale;

@MirageCommand(
        name = "status",
        description = "Show Mirage system status",
        usage = "status",
        permission = "mirage.command.status",
        order = 30
)
public final class StatusCommand implements MirageSubcommand {
    @Override
    public int execute(CommandSender sender, String[] args) {
        MirageText.sendPrefixed(sender, "<gray>Mirage system status overview</gray>");
        sendRandomTickSummary(sender);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public boolean runWhenHasChildren() {
        return true;
    }

    private void sendRandomTickSummary(CommandSender sender) {
        RandomTickConfig config = Configs.get(RandomTickConfig.class);
        int totalWorlds = Bukkit.getWorlds().size();
        int enabledWorlds = 0;
        int totalBudget = 0;
        int totalSpent = 0;
        int totalChunks = 0;
        int totalSkipped = 0;

        for (World world : Bukkit.getWorlds()) {
            ServerLevel level = ((CraftWorld) world).getHandle();
            RandomTickStatus status = RandomTickSystem.get(level).snapshot();
            if (status.enabled()) {
                enabledWorlds++;
            }
            totalBudget += status.lastBudget();
            totalSpent += status.lastSpent();
            totalChunks += status.lastChunksTicked() + status.lastChunksSkipped();
            totalSkipped += status.lastChunksSkipped();
        }

        String enabledLabel = config.enabled ? "<green>enabled</green>" : "<red>disabled</red>";
        String multiplier = String.format(Locale.ROOT, "%.2f", config.budgetMultiplier);
        String fluid = String.format(Locale.ROOT, "%.2f", config.fluidMultiplier);
        String vine = String.format(Locale.ROOT, "%.2f", config.vineMultiplier);
        String snow = String.format(Locale.ROOT, "%.2f", config.snowyDirtMultiplier);
        MirageText.send(sender,
                "<aqua>Random tick</aqua> <gray>- " + enabledLabel
                        + ", budget x" + multiplier
                        + ", fluids x" + fluid
                        + ", vines x" + vine
                        + ", snowy x" + snow
                        + ", worlds " + enabledWorlds + "/" + totalWorlds + "</gray>");

        if (totalBudget > 0) {
            double utilization = Math.min(1.0, (double) totalSpent / (double) totalBudget);
            String util = String.format(Locale.ROOT, "%.0f", utilization * 100.0);
            MirageText.send(sender,
                    "<gray>Last tick:</gray> <white>" + totalSpent + "</white>/<white>" + totalBudget
                            + "</white> used (<white>" + util + "%</white>), <white>" + totalSkipped
                            + "</white>/<white>" + totalChunks + "</white> chunks skipped");
        }
    }
}
