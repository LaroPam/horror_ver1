package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.Challenge;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.Lights;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The door creaks open on its own. It comes through on all fours, head first, then unfolds to its full
 * height inside your room and starts walking towards you. Get into the light.
 * Steps: 0 crawling in, 1 standing up, 2 coming for you.
 */
public final class DoorScene implements SceneRunner {
    private static final int CRAWL_TO_DOOR = 50;
    private static final int CRAWL_INSIDE = 35;
    private static final int STAND_UP = 35;

    private final BlockPos door;
    private final Vec3 outside;
    private final Vec3 doorway;
    private final Vec3 inside;
    private Vec3 challengeStart = Vec3.ZERO;
    private int challengeLength;
    private int challengeAge = -1;

    public DoorScene(BlockPos door, Vec3 outside, Vec3 doorway, Vec3 inside) {
        this.door = door;
        this.outside = outside;
        this.doorway = doorway;
        this.inside = inside;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setCrawling(true);
        one.setAnim(0);
        one.face(this.doorway);
        BlockState state = level.getBlockState(this.door);
        if (state.getBlock() instanceof DoorBlock doorBlock && !state.getValue(DoorBlock.OPEN)) {
            doorBlock.setOpen(null, level, state, this.door, true);
        }
        // A long, slow creak.
        level.playSound(null, this.door, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.45F);
        HorrorNetwork.playSoundTo(victim, ModSounds.DRONE.get(), SoundSource.HOSTILE, victim.getEyePosition(), 0.7F, 0.75F);
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        if (age <= CRAWL_TO_DOOR) {
            one.stepTo(this.outside.lerp(this.doorway, age / (double) CRAWL_TO_DOOR), true);
            if (age % 12 == 0) {
                HorrorNetwork.playSoundTo(victim, ModSounds.SKITTER.get(), SoundSource.HOSTILE, one.position(), 0.8F, 0.8F);
            }
        } else if (age <= CRAWL_TO_DOOR + CRAWL_INSIDE) {
            one.stepTo(this.doorway.lerp(this.inside, (age - CRAWL_TO_DOOR) / (double) CRAWL_INSIDE), true);
        } else if (age == CRAWL_TO_DOOR + CRAWL_INSIDE + 1) {
            one.face(victim.position());
            // It gets up as far as the room lets it: on the client it stays on all fours under a two-block
            // ceiling and hunches under a three-block one (TheOneEntity.mustCrawl, getStoop).
            one.setCrawling(false);
            one.setAnim(1);
            HorrorNetwork.playSoundTo(victim, ModSounds.BREATH.get(), SoundSource.HOSTILE, one.getEyePosition(), 1.0F, 0.7F);
        } else if (age == CRAWL_TO_DOOR + CRAWL_INSIDE + STAND_UP) {
            this.beginChallenge(one, level, victim);
        } else if (this.challengeAge >= 0) {
            // It walks at you. Slowly enough that you could make it, if you move now.
            this.challengeAge++;
            double t = Math.min(1.0D, this.challengeAge / (double) this.challengeLength);
            Vec3 end = victim.position();
            one.stepTo(this.challengeStart.lerp(end, t * t), false);
            one.face(victim.position());
        }
    }

    private void beginChallenge(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setAnim(2);
        this.challengeStart = one.position();
        this.challengeLength = one.getStage() >= HauntData.HUNTING ? 120 : 160;
        this.challengeAge = 0;
        startLightChallenge(one, level, victim, this.challengeLength);
    }

    /** "Get into the light", or, if you are already in it, put it out first and make it "light something". */
    static void startLightChallenge(TheOneEntity one, ServerLevel level, ServerPlayer victim, int length) {
        Challenge challenge = Challenge.LIGHT_RUN;
        if (TheOneEntity.isLit(level, victim, HorrorConfig.SAFE_LIGHT_LEVEL.get()) && HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            // You were standing in the light. Not anymore.
            for (BlockPos light : Lights.findAll(level, victim.blockPosition(), 9, 4, true)) {
                Lights.smother(level, light, one);
            }
            challenge = Challenge.LIGHT_PLACE;
        }
        one.startChallenge(victim, challenge, length);
    }
}
