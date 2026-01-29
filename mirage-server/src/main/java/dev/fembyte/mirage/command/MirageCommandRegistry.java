package dev.fembyte.mirage.command;

import dev.fembyte.mirage.command.annotations.MirageCommand;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MirageCommandRegistry {
    private static final Logger LOGGER = Logger.getLogger("MirageCommands");

    private final List<MirageSubcommandInfo> subcommands;
    private final Map<String, MirageCommandNode> lookup;
    private final MirageCommandNode root;

    private MirageCommandRegistry(List<MirageSubcommandInfo> subcommands, Map<String, MirageCommandNode> lookup, MirageCommandNode root) {
        this.subcommands = subcommands;
        this.lookup = lookup;
        this.root = root;
    }

    public static MirageCommandRegistry scan(String basePackage) {
        List<MirageSubcommandInfo> subcommands = new ArrayList<>();
        Map<String, MirageCommandNode> lookup = new HashMap<>();
        MirageCommandNode root = new MirageCommandNode(null);
        try (ScanResult scanResult = new ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .acceptPackages(basePackage)
            .scan()) {
            ClassInfoList classes = scanResult.getClassesWithAnnotation(MirageCommand.class.getName());
            for (ClassInfo classInfo : classes) {
                Class<?> type = classInfo.loadClass();
                if (!MirageSubcommand.class.isAssignableFrom(type)) {
                    continue;
                }
                MirageCommand annotation = type.getAnnotation(MirageCommand.class);
                MirageSubcommand handler = newInstance(type);
                MirageSubcommandInfo info = new MirageSubcommandInfo(
                    annotation.name(),
                    annotation.parent(),
                    annotation.description(),
                    annotation.usage(),
                    annotation.permission(),
                    List.of(annotation.aliases()),
                    List.of(annotation.completions()),
                    annotation.order(),
                    handler
                );
                subcommands.add(info);
                MirageCommandNode node = new MirageCommandNode(info);
                registerLookup(lookup, info.name(), node);
                for (String alias : info.aliases()) {
                    registerLookup(lookup, alias, node);
                }
            }
        }
        subcommands.sort(Comparator.comparingInt(MirageSubcommandInfo::order)
            .thenComparing(MirageSubcommandInfo::name));
        for (MirageSubcommandInfo info : subcommands) {
            MirageCommandNode node = lookup.get(info.name().toLowerCase());
            MirageCommandNode parent;
            if (info.parent() == null || info.parent().isEmpty()) {
                parent = root;
            } else {
                parent = resolveParent(info.parent(), lookup);
                if (parent == null) {
                    LOGGER.warning("Unknown parent '" + info.parent() + "' for Mirage subcommand " + info.name());
                    parent = root;
                }
            }
            parent.addChild(node);
        }
        return new MirageCommandRegistry(subcommands, lookup, root);
    }

    public List<MirageSubcommandInfo> subcommands() {
        return subcommands;
    }

    public MirageCommandNode root() {
        return root;
    }

    public MirageCommandNode find(String name) {
        return lookup.get(name.toLowerCase());
    }

    private static MirageSubcommand newInstance(Class<?> type) {
        try {
            return (MirageSubcommand) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create Mirage subcommand " + type.getName(), ex);
            throw new IllegalStateException("Failed to create Mirage subcommand " + type.getName(), ex);
        }
    }

    private static MirageCommandNode resolveParent(String parentName, Map<String, MirageCommandNode> lookup) {
        return lookup.get(parentName.toLowerCase());
    }

    private static void registerLookup(Map<String, MirageCommandNode> lookup, String key, MirageCommandNode node) {
        String lowered = key.toLowerCase();
        MirageCommandNode existing = lookup.putIfAbsent(lowered, node);
        if (existing != null && existing != node) {
            LOGGER.warning("Duplicate Mirage subcommand key '" + lowered + "' for " + node.info().handler().getClass().getName());
        }
    }
}
