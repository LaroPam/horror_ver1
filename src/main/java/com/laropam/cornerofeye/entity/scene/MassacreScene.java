package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.CorpseEntity;
import com.laropam.cornerofeye.entity.Scene;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.Blood;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.haunt.Lights;
import com.laropam.cornerofeye.haunt.Marks;
import com.laropam.cornerofeye.haunt.Sight;
import com.laropam.cornerofeye.haunt.SpotFinder;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.registry.ModEntities;
import com.laropam.cornerofeye.registry.ModItems;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A village at night. The lights go out one by one, and then the villagers, one at a time, always where
 * nobody is looking. You hear them. In the morning there are bodies and blood; the children are just gone.
 * When it is done, it comes for you. It is never seen in the village.
 * Steps: 0 working, 1 done and quiet.
 */
public final class MassacreScene implements SceneRunner {
    private static final int GIVE_UP = 3600;
    private static final int PAUSE_BEFORE_CHASE = 120;

    private final List<UUID> targets;
    private final List<BlockPos> lights;
    private int nextKill = 100;
    private int quietSince = -1;
    private boolean dollLeft;

    public MassacreScene(List<UUID> targets, List<BlockPos> lights) {
        this.targets = new ArrayList<>(targets);
        this.lights = new ArrayList<>(lights);
    }

    @Override
    public void start(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        one.setAnim(0);
    }

    @Override
    public void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age) {
        if (this.quietSince >= 0) {
            if (age - this.quietSince >= PAUSE_BEFORE_CHASE) {
                this.comeForVictim(one, level, victim);
            }
            return;
        }
        if (age % 8 == 0 && HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            this.smotherAnUnwatchedLight(one, level);
        }
        this.targets.removeIf(id -> !(level.getEntity(id) instanceof LivingEntity living) || !living.isAlive());
        if (age >= this.nextKill) {
            this.nextKill = age + 40 + level.random.nextInt(60);
            List<LivingEntity> unseen = new ArrayList<>();
            for (UUID id : this.targets) {
                if (level.getEntity(id) instanceof LivingEntity living && Sight.hiddenFromEveryone(level, living.getEyePosition())) {
                    unseen.add(living);
                }
            }
            if (!unseen.isEmpty()) {
                this.kill(level, unseen.get(level.random.nextInt(unseen.size())));
            }
        }
        if (this.targets.isEmpty() || age > GIVE_UP) {
            this.quietSince = age;
            Marks.writing(level, one.blockPosition(), 2, 20);
            Marks.writing(level, one.blockPosition(), 2, 20);
            Marks.scratches(level, one.blockPosition());
            one.setAnim(1);
            victim.sendSystemMessage(Component.translatable("cornerofeye.message.massacre_done").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            HorrorNetwork.playSoundTo(victim, ModSounds.DRONE.get(), SoundSource.HOSTILE, victim.getEyePosition(), 0.9F, 0.55F);
        }
    }

    private void smotherAnUnwatchedLight(TheOneEntity one, ServerLevel level) {
        for (int i = 0; i < this.lights.size(); i++) {
            BlockPos pos = this.lights.get(i);
            if (!Lights.isLightSource(level, pos, level.getBlockState(pos))) {
                this.lights.remove(i--);
            } else if (Sight.blockHiddenFromEveryone(level, pos)) {
                Lights.smother(level, pos, one);
                this.lights.remove(i);
                return;
            }
        }
    }

    private void kill(ServerLevel level, LivingEntity target) {
        Vec3 pos = target.position();
        this.targets.remove(target.getUUID());
        // Asleep in bed: remember the bed, then free it so it isn't left "occupied" forever.
        BlockPos bed = target.isSleeping() ? target.getSleepingPos().orElse(null) : null;
        Direction bedFacing = bed != null ? target.getBedOrientation() : null;
        if (bed != null) {
            target.stopSleeping();
        }
        if (target instanceof Villager villager && villager.isBaby()) {
            // No body. No blood. Just an empty bed, and once, a doll lying in it.
            villager.discard();
            if (bed != null && !this.dollLeft) {
                this.dollLeft = true;
                ItemEntity doll = new ItemEntity(level, bed.getX() + 0.5D, bed.getY() + 0.6D, bed.getZ() + 0.5D, new ItemStack(ModItems.CURSED_DOLL.get()));
                doll.setDeltaMovement(Vec3.ZERO);
                doll.setUnlimitedLifetime();
                level.addFreshEntity(doll);
            }
            return;
        }
        level.playSound(null, pos.x, pos.y + 1.0D, pos.z, target instanceof Villager ? SoundEvents.VILLAGER_HURT : SoundEvents.IRON_GOLEM_HURT,
                SoundSource.NEUTRAL, 1.5F, 0.5F);
        Blood.splatter(level, BlockPos.containing(pos), 4 + level.random.nextInt(4), 2);
        if (target instanceof Villager villager) {
            CorpseEntity corpse = ModEntities.CORPSE.get().create(level);
            if (corpse != null) {
                if (bed != null && bedFacing != null) {
                    // Killed in its sleep: it stays lying in its bed, head on the pillow.
                    Vec3 middle = Vec3.atBottomCenterOf(bed).subtract(bedFacing.getStepX() * 0.5D, 0.0D, bedFacing.getStepZ() * 0.5D);
                    corpse.moveTo(middle.x, bed.getY() + 0.5625D, middle.z, bedFacing.getOpposite().toYRot(), 0.0F);
                    soakBed(level, bed, bedFacing);
                } else {
                    corpse.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
                }
                corpse.setVillagerData(villager.getVillagerData());
                level.addFreshEntity(corpse);
            }
            villager.discard();
        } else {
            target.kill();
        }
    }

    /** The sheets are red now. Both halves are swapped together so neither one breaks the other. */
    private static void soakBed(ServerLevel level, BlockPos head, Direction facing) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return;
        }
        for (BlockPos part : new BlockPos[]{head, head.relative(facing.getOpposite())}) {
            BlockState state = level.getBlockState(part);
            if (state.getBlock() instanceof BedBlock && !state.is(Blocks.RED_BED)) {
                level.setBlock(part, Blocks.RED_BED.defaultBlockState()
                        .setValue(BedBlock.FACING, state.getValue(BedBlock.FACING))
                        .setValue(BedBlock.PART, state.getValue(BedBlock.PART)), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private void comeForVictim(TheOneEntity one, ServerLevel level, ServerPlayer victim) {
        int safe = HorrorConfig.SAFE_LIGHT_LEVEL.get();
        Vec3 spot = SpotFinder.find(level, victim, 18.0D, 26.0D, 15.0F, 25.0F, 8, 24,
                pos -> SpotFinder.isDark(level, pos, safe) && Sight.clearLine(level, victim, pos.add(0.0D, 1.0D, 0.0D)));
        if (spot == null) {
            spot = SpotFinder.find(level, victim, 18.0D, 26.0D, 90.0F, 90.0F, 8, 24, pos -> SpotFinder.isDark(level, pos, safe));
        }
        if (spot == null) {
            one.leave(HauntManager.Exit.TIMEOUT);
            return;
        }
        one.teleportTo(spot.x, spot.y, spot.z);
        one.switchScene(level, victim, Scene.CHASE, new ChaseScene(ChaseScene.defaultLength(one.getStage())));
    }

    /** Everyone it is going to visit: villagers and the golems that would have protected them. */
    public static List<UUID> targetsAround(ServerLevel level, Vec3 center) {
        List<UUID> ids = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, new AABB(center, center).inflate(48.0D, 16.0D, 48.0D),
                entity -> entity instanceof Villager || entity instanceof IronGolem)) {
            ids.add(entity.getUUID());
        }
        return ids;
    }
}
