package dev.fembyte.mirage.system.randomtick;

import dev.fembyte.mirage.config.Configs;
import dev.fembyte.mirage.config.modules.RandomTickConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class RandomTickWorld {
    private static final class SectionCache {
        private int count;
        private long lastRefresh;
    }

    private final ServerLevel level;
    private final java.util.Map<LevelChunk, SectionCache> sectionCache = new java.util.WeakHashMap<>();

    private long lastGameTime = Long.MIN_VALUE;
    private boolean schedulerEnabled;
    private float fluidChance = 1.0f;
    private float vineChance = 1.0f;
    private float snowyDirtChance = 1.0f;
    private int sectionRefreshInterval = 20;

    private int tokens;
    private int maxBurst;
    private int currentBudget;
    private int lastBudget;

    private int currentSpent;
    private int lastSpent;

    private int currentSections;
    private int lastSections;

    private int currentChunksTicked;
    private int lastChunksTicked;

    private int currentChunksSkipped;
    private int lastChunksSkipped;

    private int currentTickSpeed;
    private int lastTickSpeed;

    RandomTickWorld(ServerLevel level) {
        this.level = level;
    }

    public RandomTickDecision decide(LevelChunk chunk, int randomTickSpeed, long gameTime) {
        beginTick(gameTime, randomTickSpeed);

        if (randomTickSpeed <= 0) {
            return schedulerEnabled ? RandomTickDecision.skipped(fluidChance, vineChance, snowyDirtChance) : RandomTickDecision.disabled(randomTickSpeed);
        }

        if (!schedulerEnabled) {
            currentChunksTicked++;
            return RandomTickDecision.disabled(randomTickSpeed);
        }

        int sections = getSectionCount(chunk, gameTime);
        if (sections <= 0) {
            currentChunksSkipped++;
            return RandomTickDecision.skipped(fluidChance, vineChance, snowyDirtChance);
        }

        int affordableSpeed = tokens / sections;
        if (affordableSpeed <= 0) {
            currentChunksSkipped++;
            currentSections += sections;
            return RandomTickDecision.skipped(fluidChance, vineChance, snowyDirtChance);
        }

        int speed = Math.min(randomTickSpeed, affordableSpeed);
        int cost = speed * sections;
        tokens -= cost;
        currentSpent += cost;
        currentChunksTicked++;
        return new RandomTickDecision(true, speed, fluidChance, vineChance, snowyDirtChance, sections, cost);
    }

    public RandomTickStatus snapshot() {
        String worldName = level.getWorld() != null
                ? level.getWorld().getName()
                : level.dimension().identifier().toString();
        return new RandomTickStatus(
                worldName,
                schedulerEnabled,
                lastTickSpeed,
                lastBudget,
                lastSpent,
                lastSections,
                lastChunksTicked,
                lastChunksSkipped,
                tokens,
                maxBurst,
                fluidChance,
                vineChance,
                snowyDirtChance
        );
    }

    public void record(LevelChunk chunk, RandomTickDecision decision, int actualSections, long gameTime) {
        SectionCache cache = sectionCache.computeIfAbsent(chunk, key -> new SectionCache());
        cache.count = actualSections;
        cache.lastRefresh = gameTime;

        currentSections += actualSections;
        if (!decision.schedulerEnabled() || decision.tickSpeed() <= 0) {
            return;
        }

        int deltaSections = decision.estimatedSections() - actualSections;
        if (deltaSections == 0) {
            return;
        }
        int delta = deltaSections * decision.tickSpeed();
        tokens += delta;
        if (tokens > maxBurst) {
            tokens = maxBurst;
        } else if (tokens < 0) {
            tokens = 0;
        }
        currentSpent = Math.max(0, currentSpent - delta);
    }

    private void beginTick(long gameTime, int randomTickSpeed) {
        if (gameTime == lastGameTime) {
            return;
        }

        if (lastGameTime != Long.MIN_VALUE) {
            lastBudget = currentBudget;
            lastSpent = currentSpent;
            lastSections = currentSections;
            lastChunksTicked = currentChunksTicked;
            lastChunksSkipped = currentChunksSkipped;
            lastTickSpeed = currentTickSpeed;
        }

        currentBudget = 0;
        currentSpent = 0;
        currentSections = 0;
        currentChunksTicked = 0;
        currentChunksSkipped = 0;
        currentTickSpeed = randomTickSpeed;

        RandomTickConfig config = Configs.get(RandomTickConfig.class);
        schedulerEnabled = config.enabled;
        fluidChance = clampChance((float) config.fluidMultiplier);
        vineChance = clampChance((float) config.vineMultiplier);
        snowyDirtChance = clampChance((float) config.snowyDirtMultiplier);
        sectionRefreshInterval = Math.max(1, config.sectionRefreshInterval);

        if (!schedulerEnabled) {
            tokens = 0;
            maxBurst = 0;
            lastGameTime = gameTime;
            return;
        }

        int baseBudget = computeBudget(randomTickSpeed, config);
        currentBudget = baseBudget;
        maxBurst = computeMaxBurst(baseBudget, config);
        if (maxBurst < baseBudget) {
            maxBurst = baseBudget;
        }
        if (maxBurst < 0) {
            maxBurst = 0;
        }
        tokens = Math.min(maxBurst, tokens + baseBudget);
        lastGameTime = gameTime;
    }

    private int computeBudget(int randomTickSpeed, RandomTickConfig config) {
        if (config.fixedBudget > 0) {
            return config.fixedBudget;
        }
        int sections = lastSections;
        if (sections <= 0) {
            return Math.max(0, config.bootstrapBudget);
        }
        double budget = sections * (double) randomTickSpeed * config.budgetMultiplier;
        if (budget <= 0.0) {
            return 0;
        }
        return (int) Math.round(budget);
    }

    private int computeMaxBurst(int baseBudget, RandomTickConfig config) {
        if (config.maxBurst > 0) {
            return config.maxBurst;
        }
        double burst = baseBudget * config.burstMultiplier;
        if (burst <= 0.0) {
            return baseBudget;
        }
        return (int) Math.round(burst);
    }

    private static float clampChance(float value) {
        if (value <= 0.0f) {
            return 0.0f;
        }
        if (value >= 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private int getSectionCount(LevelChunk chunk, long gameTime) {
        SectionCache cache = sectionCache.get(chunk);
        if (cache == null || gameTime - cache.lastRefresh >= sectionRefreshInterval) {
            int count = countTickingSections(chunk);
            if (cache == null) {
                cache = new SectionCache();
                sectionCache.put(chunk, cache);
            }
            cache.count = count;
            cache.lastRefresh = gameTime;
        }
        return cache.count;
    }

    private static int countTickingSections(LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        int ticking = 0;
        for (LevelChunkSection section : sections) {
            if (section != null && section.isRandomlyTickingBlocks()) {
                ticking++;
            }
        }
        return ticking;
    }
}
