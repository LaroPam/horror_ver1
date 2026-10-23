package com.laropam.cornerofeye;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-player presentation settings. */
public final class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue JUMPSCARES = BUILDER
            .comment("Show full-screen jumpscares. When off, you only hear them.")
            .define("jumpscares", true);
    public static final ForgeConfigSpec.BooleanValue FOG = BUILDER
            .comment("Thick fog at night in later stages and while it is close.")
            .define("fog", true);
    public static final ForgeConfigSpec.BooleanValue SCREEN_EFFECTS = BUILDER
            .comment("Darkening screen edges, camera shake and the lantern flash.")
            .define("screenEffects", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
