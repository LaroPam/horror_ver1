package com.laropam.cornerofeye.entity;

import com.laropam.cornerofeye.registry.ModEntities;
import com.laropam.cornerofeye.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * A thrown flare. Where it lands it burns red for half a minute, lighting the place up with an invisible
 * light block; it doesn't count as a light it can smother (see Lights.isLightSource), so it is real, fresh
 * light that holds for as long as it burns.
 */
public class FlareEntity extends ThrowableItemProjectile {
    private static final int BURN_TICKS = 600;
    private static final EntityDataAccessor<Boolean> DATA_LANDED = SynchedEntityData.defineId(FlareEntity.class, EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0F, 0.12F, 0.08F), 1.3F);

    private int burnLeft = BURN_TICKS;
    @Nullable
    private BlockPos lightPos;

    public FlareEntity(EntityType<? extends FlareEntity> type, Level level) {
        super(type, level);
    }

    public FlareEntity(Level level, LivingEntity thrower) {
        super(ModEntities.FLARE.get(), thrower, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_LANDED, false);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FLARE.get();
    }

    public boolean isLanded() {
        return this.entityData.get(DATA_LANDED);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        // Just in front of whatever it hit, so the light goes in the open block.
        Vec3 at = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.1D));
        this.setPos(at.x, at.y, at.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.entityData.set(DATA_LANDED, true);
        if (!this.level().isClientSide) {
            this.level().playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        // Bounces off and drops; whatever it hit gets a moment of fire.
        if (!(hit.getEntity() instanceof TheOneEntity)) {
            hit.getEntity().setSecondsOnFire(2);
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.1D, 0.1D, -0.1D));
    }

    @Override
    public void tick() {
        if (!this.isLanded()) {
            super.tick();
            return;
        }
        this.baseTick();
        this.setDeltaMovement(Vec3.ZERO);
        Level level = this.level();
        if (level.isClientSide) {
            level.addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.1D, this.getZ(), 0.0D, 0.02D, 0.0D);
            for (int i = 0; i < 2; i++) {
                level.addParticle(RED, this.getX() + (this.random.nextDouble() - 0.5D) * 0.3D, this.getY() + 0.15D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 0.3D, 0.0D, 0.05D, 0.0D);
            }
            if (this.tickCount % 3 == 0) {
                level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.25D, this.getZ(), 0.0D, 0.06D, 0.0D);
            }
            return;
        }
        this.keepLight();
        if (this.tickCount % 20 == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.9F, 1.5F);
        }
        if (--this.burnLeft <= 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
            this.discard();
        }
    }

    /**
     * An invisible full-strength light where it burns, in air or in still water; landed in grass, flowers or snow,
     * just above them.
     */
    private void keepLight() {
        BlockPos at = this.blockPosition();
        if (this.lightPos != null && !this.lightPos.equals(at) && !this.lightPos.equals(at.above())) {
            this.clearLight();
        }
        for (BlockPos pos : new BlockPos[]{at, at.above()}) {
            BlockState state = this.level().getBlockState(pos);
            if (state.is(Blocks.LIGHT)) {
                this.lightPos = pos;
                return;
            }
            BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
            if (state.isAir()) {
                this.level().setBlock(pos, light, Block.UPDATE_ALL);
            } else if (state.is(Blocks.WATER) && this.level().getFluidState(pos).is(Fluids.WATER) && this.level().getFluidState(pos).isSource()) {
                this.level().setBlock(pos, light.setValue(LightBlock.WATERLOGGED, true), Block.UPDATE_ALL);
            } else {
                continue;
            }
            this.lightPos = pos;
            return;
        }
    }

    private void clearLight() {
        if (this.lightPos == null) {
            return;
        }
        BlockState state = this.level().getBlockState(this.lightPos);
        if (state.is(Blocks.LIGHT)) {
            this.level().setBlock(this.lightPos, state.getValue(LightBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL);
        }
        this.lightPos = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        // Saved with its chunk it keeps its light; burnt out or killed, the light goes with it.
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            this.clearLight();
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Landed", this.isLanded());
        tag.putInt("Burn", this.burnLeft);
        if (this.lightPos != null) {
            tag.put("Light", NbtUtils.writeBlockPos(this.lightPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_LANDED, tag.getBoolean("Landed"));
        if (tag.contains("Burn")) {
            this.burnLeft = tag.getInt("Burn");
        }
        if (tag.contains("Light")) {
            this.lightPos = NbtUtils.readBlockPos(tag.getCompound("Light"));
        }
    }
}
