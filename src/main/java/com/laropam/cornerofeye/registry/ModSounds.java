package com.laropam.cornerofeye.registry;

import com.laropam.cornerofeye.CornerOfTheEye;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CornerOfTheEye.MODID);

    public static final RegistryObject<SoundEvent> BREATH = register("the_one_breath");
    public static final RegistryObject<SoundEvent> SKITTER = register("the_one_skitter");
    public static final RegistryObject<SoundEvent> SCREAM = register("the_one_scream");
    public static final RegistryObject<SoundEvent> WAIL = register("the_one_wail");
    public static final RegistryObject<SoundEvent> VANISH = register("the_one_vanish");
    public static final RegistryObject<SoundEvent> BANISH = register("the_one_banish");
    public static final RegistryObject<SoundEvent> WHISPER = register("whisper");
    public static final RegistryObject<SoundEvent> HEARTBEAT = register("heartbeat");
    public static final RegistryObject<SoundEvent> DRONE = register("drone");
    public static final RegistryObject<SoundEvent> DISTANT = register("distant");
    public static final RegistryObject<SoundEvent> LANTERN_CHARGE = register("lantern_charge");
    public static final RegistryObject<SoundEvent> LANTERN_FLASH = register("lantern_flash");
    public static final RegistryObject<SoundEvent> GLIMPSE = register("glimpse");
    public static final RegistryObject<SoundEvent> PLAYER_BREATH = register("player_breath");
    public static final RegistryObject<SoundEvent> TINNITUS = register("tinnitus");
    public static final RegistryObject<SoundEvent> DREAD_LOOP = register("dread_loop");
    public static final RegistryObject<SoundEvent> TIMER_TICK = register("timer_tick");
    public static final RegistryObject<SoundEvent> NIGHT_BELL = register("night_bell");
    public static final RegistryObject<SoundEvent> CRACK = register("crack");
    public static final RegistryObject<SoundEvent> WARDING_BELL = register("warding_bell");
    public static final RegistryObject<SoundEvent> CLAW_SCRAPE = register("claw_scrape");
    public static final RegistryObject<SoundEvent> CLAW_GLASS = register("claw_glass");
    public static final RegistryObject<SoundEvent> GLASS_TAP = register("glass_tap");
    public static final RegistryObject<SoundEvent> BELL_STRING = register("bell_string");
    public static final RegistryObject<SoundEvent> FLESH = register("flesh");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(CornerOfTheEye.id(name)));
    }

    private ModSounds() {
    }
}
