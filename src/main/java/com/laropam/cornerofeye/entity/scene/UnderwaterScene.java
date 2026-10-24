package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.entity.Challenge;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * You are swimming. Below you, out of the dark water, something is coming up with its arms raised.
 * Get out of the water before it reaches you, or it takes you down.
 * Steps: 0 rising.
 */
public final class UnderwaterScene implements SceneRunner {
    private Vec3 start = Vec3.ZERO;
    private int length;

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setAnim(0);
        this.start = one.position();
        this.length = one.getStage() >= HauntData.HUNTING ? 140 : 180;
        HorrorNetwork.playSoundTo(victim, ModSounds.DRONE.get(), SoundSource.HOSTILE, victim.getEyePosition(), 0.9F, 0.6F);
        one.startChallenge(victim, Challenge.LEAVE_WATER, this.length);
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        double t = Math.min(1.0D, age / (double) this.length);
        // Its hands reach your feet as the time runs out.
        Vec3 end = victim.position().subtract(0.0D, 3.0D, 0.0D);
        one.stepTo(this.start.lerp(end, t), false);
        one.face(victim.position());
        if (age % 35 == 10) {
            HorrorNetwork.playSoundTo(victim, ModSounds.SKITTER.get(), SoundSource.HOSTILE, one.position(), 0.9F, 0.6F);
        }
    }
}
