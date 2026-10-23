package com.laropam.dontturnaround.entity;

import com.laropam.dontturnaround.HorrorConfig;
import com.laropam.dontturnaround.haunt.Sight;
import com.laropam.dontturnaround.haunt.SpawnFinder;
import com.laropam.dontturnaround.haunt.WorldTricks;
import com.laropam.dontturnaround.network.HorrorNetwork;
import com.laropam.dontturnaround.network.HorrorPacket;
import com.laropam.dontturnaround.registry.ModEntities;
import com.laropam.dontturnaround.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The Faceless. Never saved to disk: every appearance is staged by the haunt and ends with it vanishing.
 */
public class FacelessEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(FacelessEntity.class, EntityDataSerializers.INT);

    /** Field-of-view cone that counts as "on screen" for freezing. */
    private static final double ON_SCREEN_DOT = 0.5D;

    @Nullable
    private UUID targetId;
    private int lifetime = 2400;
    private int observedTicks;
    private int revealTicks = 8;
    private int teleportCooldown = 60;
    private int stuckTicks;
    private int lurkTicks;
    private int snuffedTorches;
    private int soundCooldown = 40;

    public FacelessEntity(EntityType<? extends FacelessEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 96.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** Puts a new Faceless into the world, already facing its victim. */
    @Nullable
    public static FacelessEntity spawn(ServerLevel level, ServerPlayer target, Vec3 pos, FacelessMode mode, int lifetime) {
        FacelessEntity faceless = ModEntities.FACELESS.get().create(level);
        if (faceless == null) {
            return null;
        }
        float yaw = (float) (Mth.atan2(target.getZ() - pos.z, target.getX() - pos.x) * Mth.RAD_TO_DEG) - 90.0F;
        faceless.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        faceless.setYHeadRot(yaw);
        faceless.setYBodyRot(yaw);
        faceless.targetId = target.getUUID();
        faceless.setMode(mode);
        faceless.lifetime = lifetime;
        faceless.revealTicks = mode == FacelessMode.WATCHER ? 10 + level.random.nextInt(20) : 3 + level.random.nextInt(8);
        level.addFreshEntity(faceless);
        return faceless;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MODE, FacelessMode.STALKER.ordinal());
    }

    public FacelessMode getMode() {
        return FacelessMode.byId(this.entityData.get(DATA_MODE));
    }

    public void setMode(FacelessMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
    }

    public boolean isHaunting(Player player) {
        return player.getUUID().equals(this.targetId);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerPlayer target = this.resolveTarget();
        if (target == null) {
            this.discard();
            return;
        }
        if (--this.lifetime <= 0) {
            this.vanish(false);
            return;
        }
        this.getLookControl().setLookAt(target, 180.0F, 180.0F);
        switch (this.getMode()) {
            case WATCHER -> this.tickWatcher(target);
            case BEHIND -> this.tickBehind(target);
            case STALKER -> this.tickStalker(target);
        }
    }

    @Nullable
    private ServerPlayer resolveTarget() {
        if (this.targetId == null) {
            // Summoned or hatched from a spawn egg: pick whoever is closest.
            Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 64.0D, EntitySelector.NO_SPECTATORS);
            if (nearest == null) {
                return null;
            }
            this.targetId = nearest.getUUID();
        }
        Player player = this.level().getPlayerByUUID(this.targetId);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.isAlive() && !serverPlayer.isSpectator()) {
            return serverPlayer;
        }
        return null;
    }

    // ---------------------------------------------------------------- watcher

    private void tickWatcher(ServerPlayer target) {
        this.getNavigation().stop();
        if (this.distanceTo(target) < 18.0F) {
            this.vanish(false);
            return;
        }
        if (this.isSeenBy(target, 0.99D) && ++this.observedTicks > this.revealTicks) {
            this.vanish(false);
        }
    }

    // ----------------------------------------------------------------- behind

    private void tickBehind(ServerPlayer target) {
        this.getNavigation().stop();
        if (this.distanceTo(target) > 14.0F) {
            this.vanish(false);
            return;
        }
        if (--this.soundCooldown <= 0) {
            HorrorNetwork.playSoundTo(target, ModSounds.BREATH.get(), SoundSource.HOSTILE, this.getEyePosition(), 0.5F,
                    0.85F + this.random.nextFloat() * 0.2F);
            this.soundCooldown = 60 + this.random.nextInt(40);
        }
        if (this.isSeenBy(target, 0.55D) && ++this.observedTicks >= this.revealTicks) {
            if (this.random.nextDouble() < HorrorConfig.BEHIND_JUMPSCARE_CHANCE.get()) {
                HorrorNetwork.send(target, HorrorPacket.Type.JUMPSCARE, 14);
            }
            this.vanish(false);
        }
    }

    // ---------------------------------------------------------------- stalker

    private void tickStalker(ServerPlayer target) {
        int repel = HorrorConfig.LIGHT_REPEL_LEVEL.get();
        if (this.blockLight(this.blockPosition()) >= repel || this.blockLight(this.blockPosition().above()) >= repel) {
            this.vanish(true);
            return;
        }
        if (this.level().isDay() && this.level().canSeeSky(this.blockPosition())) {
            this.vanish(false);
            return;
        }

        if (this.isObservedByAnyone()) {
            // Frozen mid-step. It can still turn its head to look at you.
            this.observedTicks++;
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            return;
        }
        this.observedTicks = 0;

        double dist = this.distanceTo(target);
        if (dist < 2.2D && this.hasLineOfSight(target)) {
            this.strike(target);
            return;
        }
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }
        if (dist > 40.0D && this.teleportCooldown == 0 && this.teleportBehind(target)) {
            return;
        }

        if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
            double speed = dist > 16.0D ? 1.6D : dist > 8.0D ? 1.3D : 1.05D;
            this.getNavigation().moveTo(target, speed);
        }

        Path path = this.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            this.stuckTicks = 0;
            if (this.blockLight(path.getNextNodePos()) >= repel) {
                // Waits at the edge of the light. Sometimes the light goes out.
                this.getNavigation().stop();
                this.lurk(target);
                return;
            }
            this.lurkTicks = 0;
        } else if (dist > 3.0D && ++this.stuckTicks > 100 && this.teleportCooldown == 0) {
            this.teleportBehind(target);
        }

        if (dist < 12.0D && --this.soundCooldown <= 0) {
            HorrorNetwork.playSoundTo(target, ModSounds.BREATH.get(), SoundSource.HOSTILE, this.getEyePosition(), 0.6F,
                    0.75F + this.random.nextFloat() * 0.2F);
            this.soundCooldown = 80 + this.random.nextInt(60);
        }
    }

    private void lurk(ServerPlayer target) {
        this.lurkTicks++;
        if (this.lurkTicks % 80 == 0 && this.snuffedTorches < 3 && HorrorConfig.ALLOW_BLOCK_CHANGES.get()
                && this.level() instanceof ServerLevel level) {
            List<BlockPos> torches = WorldTricks.findBlocks(level, this.blockPosition(), 7, 4, WorldTricks::isTorch);
            torches.removeIf(pos -> !Sight.isHidden(level, target, Vec3.atCenterOf(pos)));
            if (!torches.isEmpty()) {
                WorldTricks.snuffTorch(level, torches.get(this.random.nextInt(torches.size())));
                this.snuffedTorches++;
            }
        }
        if (this.lurkTicks > 1200) {
            this.vanish(false);
        }
    }

    private void strike(ServerPlayer target) {
        HorrorNetwork.send(target, HorrorPacket.Type.JUMPSCARE, 18);
        this.level().playSound(target, this.getX(), this.getY(), this.getZ(), ModSounds.SCREAM.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        float damage = HorrorConfig.STALKER_DAMAGE.get().floatValue();
        if (damage > 0.0F) {
            target.hurt(this.damageSources().mobAttack(this), damage);
        }
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0), this);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this);
        this.vanish(false);
    }

    private boolean teleportBehind(ServerPlayer target) {
        this.teleportCooldown = 100;
        if (!(this.level() instanceof ServerLevel level)) {
            return false;
        }
        int repel = HorrorConfig.LIGHT_REPEL_LEVEL.get();
        Vec3 spot = SpawnFinder.find(level, target, 12.0D, 20.0D, 180.0F, 70.0F, 6, 16,
                pos -> Sight.isHidden(level, target, pos.add(0.0D, 1.5D, 0.0D)) && this.blockLight(BlockPos.containing(pos)) < repel);
        if (spot == null) {
            return false;
        }
        this.getNavigation().stop();
        this.teleportTo(spot.x, spot.y, spot.z);
        this.stuckTicks = 0;
        return true;
    }

    // ---------------------------------------------------------------- helpers

    private int blockLight(BlockPos pos) {
        return this.level().getBrightness(LightLayer.BLOCK, pos);
    }

    /** True if the player is looking roughly at it and nothing is in the way. */
    public boolean isSeenBy(Player player, double minDot) {
        Level level = this.level();
        Vec3 head = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.9D, this.getZ());
        Vec3 middle = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ());
        return Sight.canSee(level, player, head, minDot) || Sight.canSee(level, player, middle, minDot);
    }

    private boolean isObservedByAnyone() {
        for (Player player : this.level().players()) {
            if (!player.isSpectator() && player.isAlive() && player.distanceToSqr(this) < 96.0D * 96.0D
                    && this.isSeenBy(player, ON_SCREEN_DOT)) {
                return true;
            }
        }
        return false;
    }

    public void vanish(boolean withSound) {
        if (withSound) {
            this.level().playSound(null, this.getX(), this.getY() + 1.0D, this.getZ(), ModSounds.VANISH.get(), SoundSource.HOSTILE,
                    0.8F, 0.8F + this.random.nextFloat() * 0.2F);
        }
        this.discard();
    }

    // -------------------------------------------------------------- overrides

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        // You can't hurt it. It just isn't there anymore.
        if (!this.level().isClientSide && source.getEntity() != null) {
            this.vanish(true);
        }
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void checkDespawn() {
        // Every appearance ends on its own; see lifetime.
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // It makes no sound when it walks.
    }

    @Override
    public int getMaxHeadYRot() {
        // Further than any head should turn.
        return 140;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 2.45F;
    }
}
