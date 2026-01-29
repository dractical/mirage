package dev.fembyte.mirage.command;

import net.minecraft.server.MinecraftServer;

public final class MirageCommands {
    private MirageCommands() {
    }

    public static void registerCommands(MinecraftServer server) {
        MirageCommandRegistry registry = MirageCommandRegistry.scan("dev.fembyte.mirage.command.subcommands");
        MirageRootCommand command = new MirageRootCommand(registry);
        server.server.getCommandMap().register("mirage", "Mirage", command);
    }
}
