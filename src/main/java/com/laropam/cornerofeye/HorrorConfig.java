package com.laropam.cornerofeye;

import net.minecraftforge.common.ForgeConfigSpec;

/** Gameplay settings, shared by the server and singleplayer worlds. */
public final class HorrorConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.comment("How fast it escalates. A day is 20 minutes spent in the world; sleeping through the night does not skip it.")
                .push("escalation");
    }

    public static final ForgeConfigSpec.IntValue DAYS_UNTIL_WATCHING = BUILDER
            .comment("Stage 1, Watching: it shows up far away in the dark and starts putting out distant lights.")
            .defineInRange("daysUntilWatching", 2, 0, 1000);
    public static final ForgeConfigSpec.IntValue DAYS_UNTIL_APPROACHING = BUILDER
            .comment("Stage 2, Approaching: it comes close at night, puts out lights around you and can hurt you, but not kill.")
            .defineInRange("daysUntilApproaching", 4, 0, 1000);
    public static final ForgeConfigSpec.IntValue DAYS_UNTIL_HUNTING = BUILDER
            .comment("Stage 3, Hunting: every night, through walls, and its touch can kill.")
            .defineInRange("daysUntilHunting", 6, 0, 1000);
    public static final ForgeConfigSpec.DoubleValue EVENT_FREQUENCY = BUILDER
            .comment("Multiplier for how often small events (sounds, whispers, lights going out) happen.")
            .defineInRange("eventFrequency", 1.0D, 0.05D, 20.0D);

    static {
        BUILDER.pop().push("monster");
    }

    public static final ForgeConfigSpec.IntValue SAFE_LIGHT_LEVEL = BUILDER
            .comment("Block light level it cannot step into and cannot touch you in. A torch gives 14 on its own block, minus 1 per block away.")
            .defineInRange("safeLightLevel", 9, 1, 15);
    public static final ForgeConfigSpec.DoubleValue TOUCH_DAMAGE_FRACTION = BUILDER
            .comment("Fraction of max health its touch takes in stage 3. In stage 2 it never takes your last heart.")
            .defineInRange("touchDamageFraction", 0.5D, 0.0D, 10.0D);
    public static final ForgeConfigSpec.IntValue WOUNDS_TO_BANISH = BUILDER
            .comment("How many times it has to be caught by the Lantern of Revelation before it leaves for good.")
            .defineInRange("woundsToBanish", 3, 1, 100);

    static {
        BUILDER.pop().push("events");
    }

    public static final ForgeConfigSpec.BooleanValue ALLOW_BLOCK_CHANGES = BUILDER
            .comment("Allow it to change the world: lights going out, doors opening, signs. Turning this off makes it much weaker.")
            .define("allowBlockChanges", true);
    public static final ForgeConfigSpec.BooleanValue ALLOW_FAKE_CHAT = BUILDER
            .comment("Allow fake chat messages.")
            .define("allowFakeChat", true);
    public static final ForgeConfigSpec.BooleanValue GIVE_JOURNAL = BUILDER
            .comment("Slip the previous owner's notes into each player's pocket when stage 1 begins.")
            .define("giveJournal", true);

    static {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private HorrorConfig() {
    }
}
