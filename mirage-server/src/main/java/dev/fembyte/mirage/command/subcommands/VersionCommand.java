package dev.fembyte.mirage.command.subcommands;

import com.destroystokyo.paper.util.VersionFetcher;
import com.mojang.brigadier.Command;
import dev.fembyte.mirage.MirageVersionFetcher;
import dev.fembyte.mirage.command.MirageSubcommand;
import dev.fembyte.mirage.command.annotations.MirageCommand;
import dev.fembyte.mirage.util.MirageText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

@MirageCommand(
        name = "version",
        description = "Show Mirage version information",
        usage = "version",
        permission = "mirage.command.version",
        order = 20
)
public final class VersionCommand implements MirageSubcommand {
    private static final Component FETCHING = MirageText.mini("<gray><italic>Checking version, please wait...</italic></gray>");
    private static final Component FAILED = MirageText.mini("<red>Could not fetch version information.</red>");

    private final VersionFetcher versionFetcher = new MirageVersionFetcher();
    private CompletableFuture<ComputedVersion> computedVersion = CompletableFuture.completedFuture(new ComputedVersion(Component.empty(), -1));

    @Override
    public int execute(CommandSender sender, String[] args) {
        sendVersion(sender);
        return Command.SINGLE_SUCCESS;
    }

    private void sendVersion(CommandSender sender) {
        CompletableFuture<ComputedVersion> version = getVersionOrFetch();
        if (!version.isDone()) {
            sender.sendMessage(FETCHING);
        }

        version.whenComplete((computedVersion, throwable) -> {
            if (computedVersion != null) {
                sender.sendMessage(computedVersion.message);
            } else if (throwable != null) {
                sender.sendMessage(FAILED);
                MinecraftServer.LOGGER.warn("Could not fetch Mirage version information", throwable);
            }
        });
    }

    private CompletableFuture<ComputedVersion> getVersionOrFetch() {
        if (!this.computedVersion.isDone()) {
            return this.computedVersion;
        }

        if (this.computedVersion.isCompletedExceptionally()
                || System.currentTimeMillis() - this.computedVersion.resultNow().computedTime() > this.versionFetcher.getCacheTime()) {
            this.computedVersion = fetchVersionMessage();
        }

        return this.computedVersion;
    }

    private CompletableFuture<ComputedVersion> fetchVersionMessage() {
        return CompletableFuture.supplyAsync(() -> {
            Component header = MirageText.mini("<gradient:#71b7ff:#5af5a1><bold>Mirage</bold></gradient> <gray>version</gray>");
            Component message = Component.textOfChildren(
                    header,
                    Component.newline(),
                    Component.text(Bukkit.getVersionMessage(), NamedTextColor.WHITE),
                    Component.newline(),
                    this.versionFetcher.getVersionMessage()
            );

            Component clickable = message
                    .hoverEvent(Component.translatable("chat.copy.click", NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.copyToClipboard(PlainTextComponentSerializer.plainText().serialize(message)));

            return new ComputedVersion(clickable, System.currentTimeMillis());
        });
    }

    private record ComputedVersion(Component message, long computedTime) {
    }
}
