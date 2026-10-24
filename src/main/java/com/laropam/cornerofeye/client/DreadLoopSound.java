package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** A low hum under everything that swells as it gets closer and fades when it is gone. */
final class DreadLoopSound extends AbstractTickableSoundInstance {
    private int quietTicks;

    DreadLoopSound() {
        super(ModSounds.DREAD_LOOP.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
        this.volume = 0.0F;
    }

    @Override
    public void tick() {
        float dread = ClientHorror.dreadLevel();
        this.volume = Math.min(1.0F, dread * 0.9F);
        this.pitch = 0.9F + dread * 0.15F;
        this.quietTicks = dread < 0.03F ? this.quietTicks + 1 : 0;
        if (this.quietTicks > 60) {
            this.stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
