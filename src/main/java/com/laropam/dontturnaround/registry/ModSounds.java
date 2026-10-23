package com.laropam.dontturnaround.registry;

import com.laropam.dontturnaround.DontTurnAround;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DontTurnAround.MODID);

    public static final RegistryObject<SoundEvent> BREATH = register("faceless_breath");
    public static final RegistryObject<SoundEvent> SCREAM = register("faceless_scream");
    public static final RegistryObject<SoundEvent> VANISH = register("faceless_vanish");
    public static final RegistryObject<SoundEvent> WHISPER = register("whisper");
    public static final RegistryObject<SoundEvent> HEARTBEAT = register("heartbeat");
    public static final RegistryObject<SoundEvent> DRONE = register("drone");
    public static final RegistryObject<SoundEvent> DISTANT = register("distant");
    public static final RegistryObject<SoundEvent> STATIC = register("static");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(DontTurnAround.id(name)));
    }

    private ModSounds() {
    }
}
