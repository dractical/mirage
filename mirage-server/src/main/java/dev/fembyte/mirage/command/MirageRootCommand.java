package dev.fembyte.mirage.command;

import dev.fembyte.mirage.util.MirageText;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class MirageRootCommand extends Command {
    private final MirageCommandRegistry registry;

    public MirageRootCommand(MirageCommandRegistry registry) {
        super("mirage");
        this.registry = registry;
        setDescription("Mirage command");
        setUsage("/mirage <subcommand>");
        setPermission("mirage.command");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender, registry.root());
            return true;
        }
        MirageCommandNode current = registry.root();
        int index = 0;
        while (index < args.length) {
            MirageCommandNode child = current.child(args[index]);
            if (child == null) {
                break;
            }
            MirageSubcommandInfo info = child.info();
            if (info != null && !info.permission().isEmpty() && !sender.hasPermission(info.permission())) {
                MirageText.sendPrefixed(sender, "<red>You do not have permission to use this command.</red>");
                return true;
            }
            current = child;
            index++;
        }

        if (current == registry.root()) {
            MirageText.sendPrefixed(sender, "<red>Unknown subcommand.</red>");
            sendHelp(sender, registry.root());
            return true;
        }

        if (!current.children().isEmpty() && index == args.length) {
            sendHelp(sender, current);
            return true;
        }

        if (!current.children().isEmpty()) {
            MirageText.sendPrefixed(sender, "<red>Unknown subcommand.</red>");
            sendHelp(sender, current);
            return true;
        }

        MirageSubcommandInfo info = current.info();
        String[] remainder = Arrays.copyOfRange(args, index, args.length);
        info.handler().execute(sender, remainder);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (!testPermissionSilent(sender)) {
            return List.of();
        }
        MirageCommandNode current = registry.root();
        int index = 0;
        while (index < args.length) {
            MirageCommandNode child = current.child(args[index]);
            if (child == null) {
                break;
            }
            MirageSubcommandInfo info = child.info();
            if (info != null && !info.permission().isEmpty() && !sender.hasPermission(info.permission())) {
                return List.of();
            }
            current = child;
            index++;
        }

        if (!current.children().isEmpty() && index == args.length) {
            return suggestChildren(sender, current, "");
        }
        if (!current.children().isEmpty() && index < args.length) {
            return suggestChildren(sender, current, args[index]);
        }

        if (current == registry.root()) {
            return suggestChildren(sender, current, args.length == 0 ? "" : args[0]);
        }

        MirageSubcommandInfo info = current.info();
        String[] remainder = Arrays.copyOfRange(args, index, args.length);
        List<String> handlerResults = info.handler().tabComplete(sender, remainder);
        if (!handlerResults.isEmpty()) {
            return dedupe(handlerResults);
        }
        return dedupe(MirageTabCompletions.complete(sender, info, remainder));
    }

    private void sendHelp(CommandSender sender, MirageCommandNode node) {
        MirageText.sendPrefixed(sender, "<gray>Available Mirage subcommands:</gray>");
        for (MirageCommandNode child : node.children()) {
            MirageSubcommandInfo info = child.info();
            if (info == null) {
                continue;
            }
            if (!info.permission().isEmpty() && !sender.hasPermission(info.permission())) {
                continue;
            }
            String usage = info.usage().isEmpty() ? info.name() : info.usage();
            String description = info.description().isEmpty() ? "" : " <dark_gray>-</dark_gray> <gray>" + info.description() + "</gray>";
            MirageText.send(sender, "<aqua>/mirage " + usage + "</aqua>" + description);
        }
    }

    private List<String> suggestChildren(CommandSender sender, MirageCommandNode node, String prefix) {
        Set<String> results = new LinkedHashSet<>();
        for (MirageCommandNode child : node.children()) {
            MirageSubcommandInfo info = child.info();
            if (info == null) {
                continue;
            }
            if (!info.permission().isEmpty() && !sender.hasPermission(info.permission())) {
                continue;
            }
            String name = info.name();
            if (name.toLowerCase().startsWith(prefix.toLowerCase())) {
                results.add(name);
            }
            for (String alias : info.aliases()) {
                if (alias.toLowerCase().startsWith(prefix.toLowerCase())) {
                    results.add(alias);
                }
            }
        }
        return new ArrayList<>(results);
    }

    private List<String> dedupe(List<String> input) {
        if (input.isEmpty()) {
            return input;
        }
        return new ArrayList<>(new LinkedHashSet<>(input));
    }
}
