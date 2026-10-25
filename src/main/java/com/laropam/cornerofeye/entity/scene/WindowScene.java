package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.haunt.Marks;
import com.laropam.cornerofeye.haunt.Sight;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Your room is lit, so it can't come in. It comes to the window instead: bent double to get its face down to the
 * glass, palms flat on it either side of its head, watching you. Now and then its claws drag slowly down the glass.
 * If you haven't noticed it, it taps. Look straight at it, or walk up to the window, and it is gone, leaving its
 * hands on the glass.
 * Steps: 0 pressed to the glass, 1 claws dragging down it, 2 snapping back out of sight.
 */
public final class WindowScene implements SceneRunner {
    public static final int SCRAPE = 40;
    public static final int SNAP_BACK = 4;
    /** From where it stands to the glass, in blocks: its face is this far ahead of its feet when it bends down. */
    public static final double REACH = 1.15D;
    /** Eye contact this long (in ticks) and it is gone. */
    private static final int STARE_LIMIT = 8;

    /** The glass at the height of its face. */
    private final BlockPos window;
    /** From the glass towards it, away from you. */
    private final Direction out;
    /** Full glass blocks keep its handprints; panes are too thin to hold them. */
    private final boolean holdsHands;
    private int staredAt;
    private int snapAge = -1;
    private int scrapeAge = -1;
    private boolean tapped;

    public WindowScene(BlockPos window, Direction out, boolean holdsHands) {
        this.window = window;
        this.out = out;
        this.holdsHands = holdsHands;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setCrawling(false);
        one.setAnim(0);
        one.face(Vec3.atCenterOf(this.window));
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        one.setDeltaMovement(Vec3.ZERO);
        one.face(Vec3.atCenterOf(this.window));
        Vec3 glass = Vec3.atCenterOf(this.window);
        if (this.snapAge >= 0) {
            if (age - this.snapAge >= SNAP_BACK) {
                if (this.holdsHands && HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
                    Marks.handOn(level, this.window.relative(this.out), this.out);
                }
                one.leave(HauntManager.Exit.TIMEOUT);
            }
            return;
        }
        if (age % 50 == 10) {
            // Its breath on the glass.
            HorrorNetwork.playSoundTo(victim, ModSounds.BREATH.get(), SoundSource.HOSTILE, glass, 0.5F, 0.6F + level.random.nextFloat() * 0.1F);
        }
        if (!this.tapped && age > 60 && Sight.gazeDot(victim, glass) < 0.6D) {
            // You haven't seen it yet. Tap. Tap. Tap.
            this.tapped = true;
            for (int i = 0; i < 3; i++) {
                HauntManager.schedule(i * 9, () -> HorrorNetwork.playSoundTo(victim, ModSounds.GLASS_TAP.get(), SoundSource.HOSTILE, glass, 0.9F, 1.4F));
            }
        }
        if (this.scrapeAge < 0 && age > 40 && age % 90 == 45) {
            this.scrapeAge = age;
            one.setAnim(1);
            HorrorNetwork.playSoundTo(victim, ModSounds.CLAW_GLASS.get(), SoundSource.HOSTILE, glass, 0.45F, 1.6F);
            HorrorNetwork.playSoundTo(victim, SoundEvents.GLASS_STEP, SoundSource.HOSTILE, glass, 0.6F, 0.5F);
        }
        if (this.scrapeAge >= 0 && age - this.scrapeAge >= SCRAPE) {
            this.scrapeAge = -1;
            one.setAnim(0);
        }
        // The glass itself is in the way of an ordinary line of sight, so the glass is what you have to be looking at.
        if (age > 25 && Sight.gazeDot(victim, glass) > 0.96D && Sight.clearLineToBlock(level, victim, this.window)) {
            if (++this.staredAt == 1) {
                HorrorNetwork.send(victim, HorrorPacket.Type.DREAD, 40);
            }
        }
        boolean close = victim.position().distanceToSqr(glass) < 2.5D * 2.5D;
        if (this.staredAt > STARE_LIMIT || close || age > 600) {
            this.snapAge = age;
            one.setAnim(2);
            HorrorNetwork.playSoundTo(victim, ModSounds.SKITTER.get(), SoundSource.HOSTILE, one.getEyePosition(), 0.8F, 1.3F);
        }
    }
}
