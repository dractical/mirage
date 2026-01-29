package dev.fembyte.mirage.command;

import java.util.List;
import org.bukkit.command.CommandSender;

public interface MirageSubcommand {
    int execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
