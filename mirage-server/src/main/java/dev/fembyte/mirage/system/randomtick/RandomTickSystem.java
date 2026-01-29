package dev.fembyte.mirage.system.randomtick;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RandomTickSystem {
    private static final Map<ServerLevel, RandomTickWorld> WORLDS = Collections.synchronizedMap(new WeakHashMap<>());

    private RandomTickSystem() {
    }

    public static RandomTickWorld get(ServerLevel level) {
        return WORLDS.computeIfAbsent(level, RandomTickWorld::new);
    }

    public static List<RandomTickWorld> snapshotWorlds() {
        synchronized (WORLDS) {
            return new ArrayList<>(WORLDS.values());
        }
    }
}
