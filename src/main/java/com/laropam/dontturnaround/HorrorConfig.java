package com.laropam.dontturnaround;

import net.minecraftforge.common.ForgeConfigSpec;

/** Gameplay settings, shared by the server and singleplayer worlds. */
public final class HorrorConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("How fast the haunting escalates. A day is 20 minutes spent in the world (sleeping does not count).")
                .push("escalation");
    }

    public static final ForgeConfigSpec.IntValue DAYS_UNTIL_STAGE_1 = BUILDER
            .comment("Days before the first quiet signs: footsteps, distant mining, a figure on the horizon.")
            .defineInRange("daysUntilStage1", 1, 0, 1000);
    public static final ForgeConfigSpec.IntValue DAYS_UNTIL_STAGE_2 = BUILDER
            .comment("Days before it gets bolder: whispers, torches going out, things built near you, fake chat.")
            .defineInRange("daysUntilStage2", 3, 0, 1000);
    public static final ForgeConfigSpec.IntValue DAYS_UNTIL_STAGE_3 = BUILDER
            .comment("Days before it starts hunting at night.")
            .defineInRange("daysUntilStage3", 6, 0, 1000);
    public static final ForgeConfigSpec.DoubleValue EVENT_FREQUENCY = BUILDER
            .comment("Multiplier for how often events happen. 2.0 = twice as often.")
            .defineInRange("eventFrequency", 1.0D, 0.05D, 20.0D);

    static {
        BUILDER.pop().push("events");
    }

    public static final ForgeConfigSpec.BooleanValue ALLOW_BLOCK_CHANGES = BUILDER
            .comment("Allow events that change the world: torches going out, doors opening, signs, pillars and tunnels.")
            .define("allowBlockChanges", true);
    public static final ForgeConfigSpec.BooleanValue ALLOW_FAKE_CHAT = BUILDER
            .comment("Allow fake chat messages and fake 'joined the game' notices.")
            .define("allowFakeChat", true);
    public static final ForgeConfigSpec.DoubleValue SLEEP_INTERRUPT_CHANCE = BUILDER
            .comment("Chance (stage 2+) that sleeping gets interrupted by a visitor.")
            .defineInRange("sleepInterruptChance", 0.2D, 0.0D, 1.0D);
    public static final ForgeConfigSpec.DoubleValue BEHIND_JUMPSCARE_CHANCE = BUILDER
            .comment("Chance that turning around to face the thing behind you ends in a jumpscare.")
            .defineInRange("behindJumpscareChance", 0.5D, 0.0D, 1.0D);

    static {
        BUILDER.pop().push("stalker");
    }

    public static final ForgeConfigSpec.BooleanValue ENABLE_STALKER = BUILDER
            .comment("Stage 3: at night it hunts you. It only moves while nobody is looking at it.")
            .define("enableStalker", true);
    public static final ForgeConfigSpec.DoubleValue STALKER_DAMAGE = BUILDER
            .comment("Damage dealt when it reaches you (2 = one heart).")
            .defineInRange("stalkerDamage", 8.0D, 0.0D, 1000.0D);
    public static final ForgeConfigSpec.IntValue LIGHT_REPEL_LEVEL = BUILDER
            .comment("Block light level it refuses to step into. Torches give 14 right next to them.")
            .defineInRange("lightRepelLevel", 11, 1, 16);

    static {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private HorrorConfig() {
    }
}
