package dev.fembyte.mirage.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface MirageSubcommand {
    int execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    default boolean runWhenHasChildren() {
        return false;
    }
}
