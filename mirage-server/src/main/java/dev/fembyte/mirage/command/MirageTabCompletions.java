package dev.fembyte.mirage.command;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public final class MirageTabCompletions {
    private MirageTabCompletions() {
    }

    public static List<String> complete(CommandSender sender, MirageSubcommandInfo info, String[] args) {
        if (info.completions().isEmpty()) {
            return List.of();
        }
        int index = Math.max(0, args.length - 1);
        if (index >= info.completions().size()) {
            return List.of();
        }
        String pattern = info.completions().get(index);
        if (pattern == null || pattern.isEmpty()) {
            return List.of();
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1];
        List<String> raw = expandPattern(pattern, sender);
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(prefix, raw, matches);
        if (matches.isEmpty()) {
            return matches;
        }
        return new ArrayList<>(new LinkedHashSet<>(matches));
    }

    private static List<String> expandPattern(String pattern, CommandSender sender) {
        if (pattern.equalsIgnoreCase("<player>")) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }
        if (pattern.equalsIgnoreCase("<world>")) {
            List<String> worlds = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                worlds.add(world.getName());
            }
            return worlds;
        }
        if (pattern.equalsIgnoreCase("<boolean>")) {
            return List.of("true", "false");
        }
        if (pattern.contains("|")) {
            String[] parts = pattern.split("\\|");
            List<String> values = new ArrayList<>(parts.length);
            for (String part : parts) {
                values.add(part.trim());
            }
            return values;
        }
        return List.of(pattern);
    }
}
