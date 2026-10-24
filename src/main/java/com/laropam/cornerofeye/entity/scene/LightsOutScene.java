package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.Challenge;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.haunt.Lights;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Every light around you goes out, one after another, even the ones you are looking at. Then it starts
 * walking out of the dark towards you. Light something before it gets there.
 * Steps: 0 lights going out, 1 coming for you.
 */
public final class LightsOutScene implements SceneRunner {
    private static final int GAP = 5;

    private final List<BlockPos> lights;
    private Vec3 challengeStart = Vec3.ZERO;
    private int challengeLength;
    private int challengeAge = -1;

    public LightsOutScene(List<BlockPos> lights) {
        this.lights = lights;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setAnim(0);
        one.face(victim.position());
        HorrorNetwork.playSoundTo(victim, ModSounds.DRONE.get(), SoundSource.HOSTILE, victim.getEyePosition(), 0.8F, 0.7F);
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        int doneAt = this.lights.size() * GAP;
        if (age < doneAt) {
            if (age % GAP == 0) {
                BlockPos light = this.lights.get(age / GAP);
                if (Lights.isSnuffable(level.getBlockState(light))) {
                    Lights.snuff(level, light);
                }
            }
            return;
        }
        if (age == doneAt + 10) {
            if (TheOneEntity.isLit(level, victim, HorrorConfig.SAFE_LIGHT_LEVEL.get())) {
                // Something that doesn't burn is still lighting you. It can't do anything about that.
                one.leave(HauntManager.Exit.SCARED);
                return;
            }
            one.setAnim(1);
            this.challengeStart = one.position();
            this.challengeLength = one.getStage() >= HauntData.HUNTING ? 160 : 200;
            this.challengeAge = 0;
            one.startChallenge(victim, Challenge.LIGHT_PLACE, this.challengeLength);
        } else if (this.challengeAge >= 0) {
            this.challengeAge++;
            double t = Math.min(1.0D, this.challengeAge / (double) this.challengeLength);
            one.stepTo(this.challengeStart.lerp(victim.position(), t), false);
            one.face(victim.position());
            if (this.challengeAge % 30 == 0) {
                HorrorNetwork.playSoundTo(victim, ModSounds.BREATH.get(), SoundSource.HOSTILE, one.getEyePosition(), 0.9F, 0.8F);
            }
        }
    }
}
