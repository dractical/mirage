package dev.fembyte.mirage.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public final class MirageText {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Component PREFIX = MINI.deserialize("<gradient:#71b7ff:#5af5a1><bold>Mirage</bold></gradient><gray> » </gray>");

    private MirageText() {
    }

    public static Component mini(String input) {
        return MINI.deserialize(input);
    }

    public static Component mini(String input, TagResolver resolver) {
        return MINI.deserialize(input, resolver);
    }

    public static Component prefixed(String input) {
        return Component.textOfChildren(PREFIX, mini(input));
    }

    public static void send(CommandSender sender, String input) {
        sender.sendMessage(mini(input));
    }

    public static void sendPrefixed(CommandSender sender, String input) {
        sender.sendMessage(prefixed(input));
    }

    public static void broadcast(CommandSender sender, Component message) {
        Command.broadcastCommandMessage(sender, message, true);
    }

    public static Component prefixedComponent(Component message) {
        return Component.textOfChildren(PREFIX, message);
    }
}
