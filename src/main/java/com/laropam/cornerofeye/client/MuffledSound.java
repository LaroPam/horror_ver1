package com.laropam.cornerofeye.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/** Wraps a world sound so it plays quieter and a little slower, as if heard through fear. */
final class MuffledSound implements SoundInstance {
    private final SoundInstance delegate;
    private final float volume;
    private final float pitch;

    MuffledSound(SoundInstance delegate, float volume, float pitch) {
        this.delegate = delegate;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public ResourceLocation getLocation() {
        return this.delegate.getLocation();
    }

    @Nullable
    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        return this.delegate.resolve(manager);
    }

    @Override
    public Sound getSound() {
        return this.delegate.getSound();
    }

    @Override
    public SoundSource getSource() {
        return this.delegate.getSource();
    }

    @Override
    public boolean isLooping() {
        return this.delegate.isLooping();
    }

    @Override
    public boolean isRelative() {
        return this.delegate.isRelative();
    }

    @Override
    public int getDelay() {
        return this.delegate.getDelay();
    }

    @Override
    public float getVolume() {
        return this.delegate.getVolume() * this.volume;
    }

    @Override
    public float getPitch() {
        return this.delegate.getPitch() * this.pitch;
    }

    @Override
    public double getX() {
        return this.delegate.getX();
    }

    @Override
    public double getY() {
        return this.delegate.getY();
    }

    @Override
    public double getZ() {
        return this.delegate.getZ();
    }

    @Override
    public Attenuation getAttenuation() {
        return this.delegate.getAttenuation();
    }

    @Override
    public boolean canStartSilent() {
        return this.delegate.canStartSilent();
    }

    @Override
    public boolean canPlaySound() {
        return this.delegate.canPlaySound();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        return this.delegate.getStream(library, sound, looping);
    }
}
