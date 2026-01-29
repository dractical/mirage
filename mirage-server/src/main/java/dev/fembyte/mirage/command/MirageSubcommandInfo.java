package dev.fembyte.mirage.command;

import java.util.List;

public record MirageSubcommandInfo(
        String name,
        String parent,
        String description,
        String usage,
        String permission,
        List<String> aliases,
        List<String> completions,
        int order,
        MirageSubcommand handler
) {
}
