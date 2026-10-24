package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.entity.Challenge;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * It circles you on all fours in the dark, sniffing, each lap a little tighter. It finds you by sound:
 * crouch and don't move until it gives up.
 * Steps: 0 circling.
 */
public final class StillScene implements SceneRunner {
    private final Vec3 center;
    private double angle;
    private final double direction;
    private int length;

    public StillScene(Vec3 center, double startAngle, boolean clockwise) {
        this.center = center;
        this.angle = startAngle;
        this.direction = clockwise ? 1.0D : -1.0D;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setAnim(0);
        one.setCrawling(true);
        this.length = one.getStage() >= HauntData.HUNTING ? 200 : 160;
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        double progress = Mth.clamp(age / (double) (this.length + 20), 0.0D, 1.0D);
        double radius = Mth.lerp(progress, 7.0D, 3.0D);
        this.angle += this.direction * 0.05D;
        Vec3 target = new Vec3(this.center.x + Math.cos(this.angle) * radius, this.center.y, this.center.z + Math.sin(this.angle) * radius);
        one.stepTo(target, true);
        if (age % 18 == 0) {
            HorrorNetwork.playSoundTo(victim, ModSounds.SKITTER.get(), SoundSource.HOSTILE, one.position(), 0.9F, 0.8F + level.random.nextFloat() * 0.3F);
        }
        if (age % 50 == 30) {
            HorrorNetwork.playSoundTo(victim, ModSounds.BREATH.get(), SoundSource.HOSTILE, one.getEyePosition(), 0.8F, 1.1F);
        }
        if (age == 20) {
            one.startChallenge(victim, Challenge.STILL, this.length);
        }
    }
}
