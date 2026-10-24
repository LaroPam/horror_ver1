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
            .comment("Darkening screen edges, narrowing vision, camera shake and the lantern flash.")
            .define("screenEffects", true);
    public static final ForgeConfigSpec.BooleanValue MUFFLE_WORLD = BUILDER
            .comment("When it is close, the rest of the world goes quiet and slow, so you hear your heart and it.")
            .define("muffleWorld", true);
    public static final ForgeConfigSpec.BooleanValue MENU_CORRUPTION = BUILDER
            .comment("It follows you out of the world: the title screen, pause menu, inventory and tooltips change as it gets worse.")
            .define("menuCorruption", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
