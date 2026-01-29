package dev.fembyte.mirage.system.randomtick;

public record RandomTickStatus(
        String worldName,
        boolean enabled,
        int lastTickSpeed,
        int lastBudget,
        int lastSpent,
        int lastSections,
        int lastChunksTicked,
        int lastChunksSkipped,
        int tokens,
        int maxBurst,
        float fluidChance,
        float vineChance,
        float snowyDirtChance
) {
    public double utilization() {
        if (lastBudget <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) lastSpent / (double) lastBudget);
    }
}
