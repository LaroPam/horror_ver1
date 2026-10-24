package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.haunt.Sight;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModItems;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Behind a tree trunk. A head leans out sideways, too far, fingers curling around the bark. It stays there
 * looking at you until you stare back for too long, then snaps out of sight.
 * Steps: 0 leaning out, 1 watching, 2 snapping back.
 */
public final class PeekScene implements SceneRunner {
    public static final int LEAN_OUT = 50;
    public static final int SNAP_BACK = 5;
    /** Eye contact this long (in ticks) and it is gone. */
    public static final int STARE_LIMIT = 12;

    private final boolean leanLeft;
    private int staredAt;
    private int snapAge = -1;
    private boolean approached;

    public PeekScene(boolean leanLeft) {
        this.leanLeft = leanLeft;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setLeaningLeft(this.leanLeft);
        one.setAnim(0);
        one.face(victim.position());
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        one.setDeltaMovement(Vec3.ZERO);
        if (this.snapAge >= 0) {
            if (age - this.snapAge >= SNAP_BACK) {
                if (this.approached && (one.getStage() < HauntData.HUNTING || level.random.nextFloat() < 0.3F)) {
                    ItemEntity item = new ItemEntity(level, one.getX(), one.getY() + 0.2D, one.getZ(), new ItemStack(ModItems.SHADOW_SCRAP.get()));
                    item.setDeltaMovement(Vec3.ZERO);
                    level.addFreshEntity(item);
                }
                one.leave(this.approached ? HauntManager.Exit.APPROACHED : HauntManager.Exit.TIMEOUT);
            }
            return;
        }
        if (age == LEAN_OUT) {
            one.setAnim(1);
            HorrorNetwork.playSoundTo(victim, ModSounds.GLIMPSE.get(), SoundSource.HOSTILE, one.getEyePosition(), 1.0F, 0.9F);
            HorrorNetwork.send(victim, HorrorPacket.Type.DREAD, 35);
        }
        double dx = victim.getX() - one.getX();
        double dz = victim.getZ() - one.getZ();
        this.approached = dx * dx + dz * dz < 9.0D * 9.0D;
        Vec3 head = one.position().add(0.0D, 2.4D, 0.0D);
        if (age > LEAN_OUT && Sight.canSee(level, victim, head, 0.95D)) {
            this.staredAt++;
        }
        if (this.approached || this.staredAt > STARE_LIMIT || age > 320) {
            this.snapAge = age;
            one.setAnim(2);
            HorrorNetwork.playSoundTo(victim, ModSounds.SKITTER.get(), SoundSource.HOSTILE, one.getEyePosition(), 1.0F, 1.2F);
        }
    }
}
