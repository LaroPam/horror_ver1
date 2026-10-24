package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * It comes out of the dark on all fours, straight at you, through anything in the way.
 * Get into the light before it arrives.
 * Steps: 0 coming.
 */
public final class ChaseScene implements SceneRunner {
    private final int length;
    private Vec3 start = Vec3.ZERO;

    /** @param length ticks until it reaches you */
    public ChaseScene(int length) {
        this.length = length;
    }

    public static int defaultLength(int stage) {
        return stage >= HauntData.HUNTING ? 200 : 260;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setAnim(0);
        one.setCrawling(true);
        this.start = one.position();
        HorrorNetwork.playSoundTo(victim, ModSounds.SCREAM.get(), SoundSource.HOSTILE, one.getEyePosition(), 1.2F, 0.6F);
        DoorScene.startLightChallenge(one, level, victim, this.length);
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        // Slow at first, then faster and faster.
        double t = Math.min(1.0D, age / (double) this.length);
        one.stepTo(this.start.lerp(victim.position(), t * t), true);
        if (age % 10 == 0) {
            HorrorNetwork.playSoundTo(victim, ModSounds.SKITTER.get(), SoundSource.HOSTILE, one.position(), 1.0F, 0.9F + (float) t * 0.4F);
        }
    }
}
