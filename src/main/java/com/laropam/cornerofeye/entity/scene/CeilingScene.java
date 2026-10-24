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
 * In a cave. Something lowers itself head first out of the rock ceiling, upside down, arms hanging,
 * turning its head to find you. Meet its eyes and it is pulled back up into the stone.
 * Steps: 0 coming down, 1 hanging, 2 pulled back up. The client draws it upside down.
 */
public final class CeilingScene implements SceneRunner {
    public static final int COME_DOWN = 60;
    private static final int PULL_UP = 6;
    /** How far below the ceiling it hangs. */
    private static final double DROP = 1.8D;

    private final double ceilingY;
    private int staredAt;
    private int pullAge = -1;
    private boolean approached;

    public CeilingScene(double ceilingY) {
        this.ceilingY = ceilingY;
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setAnim(0);
        one.face(victim.position());
        HorrorNetwork.playSoundTo(victim, ModSounds.SKITTER.get(), SoundSource.HOSTILE, one.position(), 1.0F, 0.7F);
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        one.face(victim.position());
        if (this.pullAge >= 0) {
            double t = Math.min(1.0D, (age - this.pullAge) / (double) PULL_UP);
            one.stepTo(new Vec3(one.getX(), this.ceilingY - DROP * (1.0D - t), one.getZ()), false);
            if (t >= 1.0D) {
                if (this.approached && (one.getStage() < HauntData.HUNTING || level.random.nextFloat() < 0.3F)) {
                    ItemEntity item = new ItemEntity(level, one.getX(), this.ceilingY - DROP, one.getZ(), new ItemStack(ModItems.SHADOW_SCRAP.get()));
                    level.addFreshEntity(item);
                }
                one.leave(this.approached ? HauntManager.Exit.APPROACHED : HauntManager.Exit.TIMEOUT);
            }
            return;
        }
        if (age <= COME_DOWN) {
            double t = age / (double) COME_DOWN;
            one.stepTo(new Vec3(one.getX(), this.ceilingY - DROP * t * t, one.getZ()), false);
        }
        if (age == COME_DOWN) {
            one.setAnim(1);
            HorrorNetwork.playSoundTo(victim, ModSounds.GLIMPSE.get(), SoundSource.HOSTILE, one.position(), 1.0F, 0.85F);
            HorrorNetwork.send(victim, HorrorPacket.Type.DREAD, 35);
        }
        if (age % 40 == 20) {
            HorrorNetwork.playSoundTo(victim, ModSounds.BREATH.get(), SoundSource.HOSTILE, one.position(), 0.8F, 0.75F);
        }
        double dx = victim.getX() - one.getX();
        double dz = victim.getZ() - one.getZ();
        this.approached = dx * dx + dz * dz < 4.0D * 4.0D;
        // Upside down, its head is at the bottom.
        Vec3 head = one.position().add(0.0D, 0.4D, 0.0D);
        if (age > COME_DOWN / 2 && Sight.canSee(level, victim, head, 0.95D)) {
            this.staredAt++;
        }
        if (this.approached || this.staredAt > PeekScene.STARE_LIMIT || age > 360) {
            this.pullAge = age;
            one.setAnim(2);
            HorrorNetwork.playSoundTo(victim, ModSounds.SKITTER.get(), SoundSource.HOSTILE, one.position(), 1.0F, 1.3F);
        }
    }
}
