package dev.fembyte.mirage.config.modules;

import dev.fembyte.mirage.config.ConfigModule;
import dev.fembyte.mirage.config.annotations.Comment;
import dev.fembyte.mirage.config.annotations.ConfigSpec;

@ConfigSpec(name = "random-tick", category = "mirage")
@Comment({"Mirage random tick scheduler settings."})
public final class RandomTickConfig implements ConfigModule {
    @Comment({"Enable the Mirage random tick scheduler.", "Default: true"})
    public boolean enabled = true;

    @Comment({
            "Multiplier applied to the estimated vanilla random tick workload.",
            "Lower values do fewer random ticks per tick.",
            "Default: 0.9"
    })
    public double budgetMultiplier = 0.9;

    @Comment({
            "Fixed random tick budget per tick.",
            "Set to -1 to use the budgetMultiplier.",
            "Default: -1"
    })
    public int fixedBudget = -1;

    @Comment({
            "Maximum budget that can accumulate for bursts.",
            "Set to -1 to use burstMultiplier.",
            "Default: -1"
    })
    public int maxBurst = -1;

    @Comment({
            "Burst multiplier used when maxBurst is not set.",
            "Default: 2.0"
    })
    public double burstMultiplier = 2.0;

    @Comment({
            "Bootstrap budget used when no history is available.",
            "Default: 6000"
    })
    public int bootstrapBudget = 6000;

    @Comment({
            "Chance multiplier for fluid random ticks (0.0 - 1.0).",
            "Default: 0.6"
    })
    public double fluidMultiplier = 0.6;

    @Comment({
            "Chance multiplier for vine random ticks (0.0 - 1.0).",
            "Default: 0.7"
    })
    public double vineMultiplier = 0.7;

    @Comment({
            "Chance multiplier for spreading snowy dirt random ticks (0.0 - 1.0).",
            "Default: 0.7"
    })
    public double snowyDirtMultiplier = 0.7;

    @Comment({
            "How often to refresh per-chunk ticking section counts (in ticks).",
            "Lower values are more accurate, higher values save CPU.",
            "Default: 5"
    })
    public int sectionRefreshInterval = 5;
}
