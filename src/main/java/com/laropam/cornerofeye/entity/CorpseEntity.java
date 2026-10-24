package com.laropam.cornerofeye.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** What it leaves of a villager. Lies where it fell for a few days; hit it to clear it away. */
public class CorpseEntity extends Entity {
    private static final EntityDataAccessor<VillagerData> DATA_VILLAGER =
            SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.VILLAGER_DATA);
    private static final int LIFETIME = 72000;

    private int age;

    public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_VILLAGER, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
    }

    public VillagerData getVillagerData() {
        return this.entityData.get(DATA_VILLAGER);
    }

    public void setVillagerData(VillagerData data) {
        this.entityData.set(DATA_VILLAGER, data);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && ++this.age > LIFETIME) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || this.isRemoved()) {
            return false;
        }
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    this.getX(), this.getY() + 0.2D, this.getZ(), 20, 0.4D, 0.1D, 0.4D, 0.05D);
        }
        this.discard();
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        if (tag.contains("VillagerData")) {
            VillagerData.CODEC.parse(NbtOps.INSTANCE, tag.get("VillagerData")).result().ifPresent(this::setVillagerData);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, this.getVillagerData()).result().ifPresent(data -> tag.put("VillagerData", data));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
