package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.registry.ModItems;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * You left the doll out for it, hung up in a frame somewhere dark. It can't help itself: before it comes for you it
 * goes to the doll, crouches under it and sniffs at it, head jerking, for a long while. Long enough to be waiting
 * there with the lantern. Then it tears the doll down, and comes for you.
 * Steps: 0 on its way, 1 crouched under the doll, 2 tearing it down.
 */
public final class LuredScene implements SceneRunner {
    public static final int SNIFF = 240;
    public static final int SNATCH = 12;
    /** Gives up on getting there after this long. */
    private static final int WAY = 500;

    private final ItemFrame frame;
    /** Where it crouches, just under the frame. */
    private final Vec3 spot;
    private int arrivedAt = -1;

    public LuredScene(ItemFrame frame, Vec3 spot) {
        this.frame = frame;
        this.spot = spot;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setCrawling(true);
        one.setAnim(0);
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        boolean dollThere = this.frame.isAlive() && this.frame.getItem().is(ModItems.CURSED_DOLL.get());
        if (!dollThere || this.arrivedAt < 0 && age > WAY) {
            // Someone took it down, or it can't get there: it comes for you instead.
            one.holdPhase(Phase.HUNT, 600);
            return;
        }
        Vec3 doll = this.frame.position();
        if (this.arrivedAt < 0) {
            one.crawlTowards(this.spot, 0.2D);
            // Where its path ends counts as there (it stops within a block and a half of where it is going).
            if (one.position().distanceToSqr(this.spot) < 1.6D * 1.6D) {
                this.arrivedAt = age;
                one.setAnim(1);
            }
            return;
        }
        one.setDeltaMovement(Vec3.ZERO);
        one.face(doll);
        int t = age - this.arrivedAt;
        if (t % 26 == 4) {
            // Sniffing at it. Anyone close enough hears it.
            level.playSound(null, doll.x, doll.y, doll.z, ModSounds.BREATH.get(), SoundSource.HOSTILE, 0.8F, 1.2F + level.random.nextFloat() * 0.2F);
        }
        if (t == SNIFF / 2) {
            level.playSound(null, doll.x, doll.y, doll.z, ModSounds.WHISPER.get(), SoundSource.HOSTILE, 0.7F, 0.7F);
        }
        if (t == SNIFF) {
            one.setCrawling(false);
            one.setAnim(2);
        }
        if (t == SNIFF + SNATCH / 2) {
            // Torn down. Somewhere in its hands the doll screams.
            this.frame.setItem(ItemStack.EMPTY);
            level.playSound(null, doll.x, doll.y, doll.z, ModSounds.SCREAM.get(), SoundSource.HOSTILE, 0.8F, 1.8F);
        }
        if (t >= SNIFF + SNATCH) {
            one.holdPhase(Phase.HUNT, 600);
        }
    }
}
