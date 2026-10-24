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
 * It is standing right behind you, bent over you, breathing on your neck. It keeps the same spot behind you
 * if you walk, but if you turn around you will see it. Don't.
 * Steps: 0 bending over you.
 */
public final class BehindScene implements SceneRunner {
    private final Vec3 offset;

    /** @param offset where it stands relative to the victim's feet, fixed in world space */
    public BehindScene(Vec3 offset) {
        this.offset = offset;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setAnim(0);
        one.face(victim.position());
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        Vec3 target = victim.position().add(this.offset);
        one.stepTo(one.position().lerp(target, 0.35D), false);
        one.face(victim.position());
        if (age % 45 == 0) {
            Vec3 neck = victim.getEyePosition().add(this.offset.normalize().scale(0.6D));
            HorrorNetwork.playSoundTo(victim, ModSounds.BREATH.get(), SoundSource.HOSTILE, neck, 1.0F, 0.75F + level.random.nextFloat() * 0.1F);
        }
        if (age == 30) {
            one.startChallenge(victim, Challenge.DONT_LOOK, one.getStage() >= HauntData.HUNTING ? 200 : 160);
        }
        if (age % 50 == 25) {
            HorrorNetwork.playSoundTo(victim, ModSounds.WHISPER.get(), SoundSource.HOSTILE, victim.getEyePosition().add(this.offset.normalize()),
                    0.8F, 0.9F);
        }
    }
}
