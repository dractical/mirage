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
        name = "random-tick",
        parent = "status",
        description = "Show random tick scheduler status",
        usage = "status random-tick [world]",
        permission = "mirage.command.status.randomtick",
        completions = {"<world>"},
        order = 10
)
public final class RandomTickStatusCommand implements MirageSubcommand {
    @Override
    public int execute(CommandSender sender, String[] args) {
        if (args.length > 1) {
            MirageText.sendPrefixed(sender, "<red>Usage: /mirage status random-tick [world]</red>");
            return Command.SINGLE_SUCCESS;
        }

        if (args.length == 1) {
            World world = Bukkit.getWorld(args[0]);
            if (world == null) {
                MirageText.sendPrefixed(sender, "<red>Unknown world.</red>");
                return Command.SINGLE_SUCCESS;
            }
            sendWorldDetail(sender, world);
            return Command.SINGLE_SUCCESS;
        }

        MirageText.sendPrefixed(sender, "<gray>Random tick status by world</gray>");
        for (World world : Bukkit.getWorlds()) {
            sendWorldSummary(sender, world);
        }
        return Command.SINGLE_SUCCESS;
    }

    private void sendWorldSummary(CommandSender sender, World world) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        RandomTickStatus status = RandomTickSystem.get(level).snapshot();
        String enabledLabel = status.enabled() ? "<green>enabled</green>" : "<red>disabled</red>";
        String util = String.format(Locale.ROOT, "%.0f", status.utilization() * 100.0);
        MirageText.send(sender,
                "<aqua>" + world.getName() + "</aqua> <gray>- " + enabledLabel
                        + ", budget <white>" + status.lastBudget() + "</white>"
                        + " spent <white>" + status.lastSpent() + "</white> (<white>" + util + "%</white>)");
    }

    private void sendWorldDetail(CommandSender sender, World world) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        RandomTickStatus status = RandomTickSystem.get(level).snapshot();
        RandomTickConfig config = Configs.get(RandomTickConfig.class);

        MirageText.sendPrefixed(sender, "<gray>Random tick status for <aqua>" + world.getName() + "</aqua></gray>");
        MirageText.send(sender, "<gray>Scheduler:</gray> " + (status.enabled() ? "<green>enabled</green>" : "<red>disabled</red>"));
        MirageText.send(sender, "<gray>Last tick speed:</gray> <white>" + status.lastTickSpeed() + "</white>");
        MirageText.send(sender,
                "<gray>Budget:</gray> <white>" + status.lastSpent() + "</white>/<white>" + status.lastBudget()
                        + "</white> used (<white>" + String.format(Locale.ROOT, "%.0f", status.utilization() * 100.0)
                        + "%</white>)");
        MirageText.send(sender, "<gray>Ticking sections:</gray> <white>" + status.lastSections() + "</white>");
        MirageText.send(sender,
                "<gray>Chunks:</gray> <white>" + status.lastChunksTicked() + "</white> ticked, <white>"
                        + status.lastChunksSkipped() + "</white> skipped");
        MirageText.send(sender,
                "<gray>Tokens:</gray> <white>" + status.tokens() + "</white>/<white>" + status.maxBurst() + "</white>");
        MirageText.send(sender,
                "<gray>Fluid chance:</gray> <white>" + String.format(Locale.ROOT, "%.2f", status.fluidChance()) + "</white>");
        MirageText.send(sender,
                "<gray>Vine chance:</gray> <white>" + String.format(Locale.ROOT, "%.2f", status.vineChance()) + "</white>");
        MirageText.send(sender,
                "<gray>Snowy dirt chance:</gray> <white>" + String.format(Locale.ROOT, "%.2f", status.snowyDirtChance()) + "</white>");
        MirageText.send(sender,
                "<gray>Config:</gray> budget x<white>" + String.format(Locale.ROOT, "%.2f", config.budgetMultiplier)
                        + "</white>, fixed <white>" + config.fixedBudget + "</white>, burst x<white>"
                        + String.format(Locale.ROOT, "%.2f", config.burstMultiplier) + "</white>");
    }
}
