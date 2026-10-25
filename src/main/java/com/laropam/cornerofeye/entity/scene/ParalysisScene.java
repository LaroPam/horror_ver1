package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Sleep paralysis. You wake up and can't move, can't look away. It is standing at the foot of the bed. It leans
 * slowly closer, one hand coming up towards your face, and when you finally manage to blink it is gone.
 * The client holds you still for the whole of it (HorrorPacket.Type.PARALYSIS). Steps: 0.
 */
public final class ParalysisScene implements SceneRunner {
    public static final int LENGTH = 90;

    private final Vec3 from;
    private final Vec3 to;

    /** @param from where it stands at first, @param to where it ends up, closer */
    public ParalysisScene(Vec3 from, Vec3 to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setCrawling(false);
        one.setAnim(0);
        one.face(victim.position());
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        float t = Mth.clamp(age / (float) LENGTH, 0.0F, 1.0F);
        one.stepTo(this.from.lerp(this.to, t * t * (3.0F - 2.0F * t)), false);
        one.face(victim.position());
        Vec3 mouth = one.position().add(0.0D, 2.4D, 0.0D);
        if (age % 22 == 6) {
            HorrorNetwork.playSoundTo(victim, ModSounds.BREATH.get(), SoundSource.HOSTILE, mouth, 1.0F, 0.55F + level.random.nextFloat() * 0.1F);
        }
        if (age == 50) {
            HorrorNetwork.playSoundTo(victim, ModSounds.WHISPER.get(), SoundSource.HOSTILE, victim.getEyePosition(), 0.9F, 0.8F);
        }
        if (age >= LENGTH) {
            // You blink. It isn't there.
            one.leave(HauntManager.Exit.TIMEOUT);
        }
    }
}
