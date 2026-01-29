package dev.fembyte.mirage.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MirageCommandNode {
    private final MirageSubcommandInfo info;
    private final Map<String, MirageCommandNode> lookup = new HashMap<>();
    private final List<MirageCommandNode> children = new ArrayList<>();

    MirageCommandNode(MirageSubcommandInfo info) {
        this.info = info;
    }

    public MirageSubcommandInfo info() {
        return info;
    }

    public List<MirageCommandNode> children() {
        return children;
    }

    public MirageCommandNode child(String name) {
        return lookup.get(name.toLowerCase());
    }

    void addChild(MirageCommandNode child) {
        children.add(child);
        registerKey(child.info().name(), child);
        for (String alias : child.info().aliases()) {
            registerKey(alias, child);
        }
    }

    private void registerKey(String key, MirageCommandNode child) {
        String lowered = key.toLowerCase();
        lookup.putIfAbsent(lowered, child);
    }
}
