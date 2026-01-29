package dev.fembyte.mirage.system.randomtick;

public record RandomTickDecision(
        boolean schedulerEnabled,
        int tickSpeed,
        float fluidChance,
        float vineChance,
        float snowyDirtChance,
        int estimatedSections,
        int reservedCost
) {
    public static RandomTickDecision disabled(int tickSpeed) {
        return new RandomTickDecision(false, tickSpeed, 1.0f, 1.0f, 1.0f, 0, 0);
    }

    public static RandomTickDecision skipped(float fluidChance, float vineChance, float snowyDirtChance) {
        return new RandomTickDecision(true, 0, fluidChance, vineChance, snowyDirtChance, 0, 0);
    }
}
