package com.laropam.cornerofeye.entity;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.haunt.Sight;
import com.laropam.cornerofeye.registry.ModEntities;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Someone you know, standing out in the dark: a friend's face and name (yours, if you play alone). It waves
 * and calls you over in chat. Walk up to it, or stare at it for too long, and its head turns slowly upside
 * down before it is gone. It can't hurt you. That isn't what it is for.
 */
public class MimicEntity extends PathfinderMob {
    public static final int SAID_LINES = 6;
    private static final EntityDataAccessor<Optional<UUID>> DATA_COPY = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** Ticks into the head turn; 0 while it is still pretending. */
    private static final EntityDataAccessor<Integer> DATA_TWIST = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WAVING = SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int TWIST_TICKS = 30;
    private static final int LIFETIME = 1500;

    @Nullable
    private UUID victimId;
    private int chatCooldown = 30;
    private int said;
    private int gazeTicks;

    public MimicEntity(EntityType<? extends MimicEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    /** Puts it at {@code pos}, wearing {@code copy}'s face, for {@code victim} to find. */
    @Nullable
    public static MimicEntity spawn(ServerLevel level, ServerPlayer victim, ServerPlayer copy, Vec3 pos) {
        MimicEntity mimic = ModEntities.MIMIC.get().create(level);
        if (mimic == null) {
            return null;
        }
        float yaw = TheOneEntity.yawTowards(pos, victim.position());
        mimic.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        mimic.setYHeadRot(yaw);
        mimic.setYBodyRot(yaw);
        mimic.victimId = victim.getUUID();
        mimic.entityData.set(DATA_COPY, Optional.of(copy.getUUID()));
        mimic.setCustomName(copy.getName());
        mimic.setCustomNameVisible(true);
        level.addFreshEntity(mimic);
        return mimic;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_COPY, Optional.empty());
        this.entityData.define(DATA_TWIST, 0);
        this.entityData.define(DATA_WAVING, false);
    }

    /** Whose face it is wearing. */
    public Optional<UUID> getCopied() {
        return this.entityData.get(DATA_COPY);
    }

    public boolean isWaving() {
        return this.entityData.get(DATA_WAVING);
    }

    /** 0 to 1: how far its head has turned. */
    public float getTwist(float partialTick) {
        int ticks = this.entityData.get(DATA_TWIST);
        return ticks == 0 ? 0.0F : Mth.clamp((ticks + partialTick) / TWIST_TICKS, 0.0F, 1.0F);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) this.level();
        ServerPlayer victim = this.victimId == null ? null : (ServerPlayer) level.getPlayerByUUID(this.victimId);
        if (victim == null || this.tickCount > LIFETIME) {
            this.vanish();
            return;
        }
        this.getLookControl().setLookAt(victim, 30.0F, 30.0F);
        this.setYBodyRot(TheOneEntity.yawTowards(this.position(), victim.position()));

        int twist = this.entityData.get(DATA_TWIST);
        if (twist > 0) {
            this.entityData.set(DATA_TWIST, twist + 1);
            if (twist + 1 >= TWIST_TICKS + 30) {
                this.vanish();
            }
            return;
        }
        // Waves now and then, as if it had been waiting for you.
        this.entityData.set(DATA_WAVING, this.tickCount % 120 < 50);
        if (HorrorConfig.ALLOW_FAKE_CHAT.get() && this.said < 2 && --this.chatCooldown <= 0) {
            this.chatCooldown = 220 + this.random.nextInt(120);
            this.said++;
            victim.sendSystemMessage(Component.translatable("chat.type.text", this.getName(),
                    Component.translatable("cornerofeye.mimic_said." + this.random.nextInt(SAID_LINES))));
        }
        // Too close, or looked at for too long: it stops pretending.
        boolean staring = Sight.gazeDot(victim, this.getEyePosition()) > 0.97D && Sight.clearLine(level, victim, this.getEyePosition());
        this.gazeTicks = staring ? this.gazeTicks + 1 : Math.max(0, this.gazeTicks - 1);
        if (this.distanceTo(victim) < 6.0D || this.gazeTicks > 80) {
            this.startTwist();
        }
    }

    private void startTwist() {
        this.entityData.set(DATA_TWIST, 1);
        this.entityData.set(DATA_WAVING, false);
        this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), ModSounds.CRACK.get(), SoundSource.HOSTILE, 1.0F, 0.45F);
    }

    private void vanish() {
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.VANISH.get(), SoundSource.HOSTILE, 0.8F, 1.0F);
            level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.0D, this.getZ(), 25, 0.25D, 0.6D, 0.25D, 0.01D);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Hit it and there is nothing there.
        if (!this.level().isClientSide && !this.isRemoved()) {
            this.vanish();
        }
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
