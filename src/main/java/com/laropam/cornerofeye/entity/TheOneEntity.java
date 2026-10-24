package com.laropam.cornerofeye.entity;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.haunt.Lights;
import com.laropam.cornerofeye.haunt.Sight;
import com.laropam.cornerofeye.haunt.SpotFinder;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModEntities;
import com.laropam.cornerofeye.registry.ModItems;
import com.laropam.cornerofeye.registry.ModSounds;
import com.laropam.cornerofeye.entity.scene.SceneRunner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The One in the Corner. There is only ever one, it is never saved to disk, and every appearance is
 * staged by {@link HauntManager}. It drifts through blocks, can only be seen from the corner of the eye
 * (see the client renderer) and puts out the lights nobody is looking at.
 */
public class TheOneEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CRAWLING =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SCENE =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.INT);
    /** Which step of its scene it is on; each scene numbers its own steps. */
    private static final EntityDataAccessor<Integer> DATA_ANIM =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.INT);
    /** Leans to its own left (true) or right while peeking. */
    private static final EntityDataAccessor<Boolean> DATA_SIDE =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.BOOLEAN);

    /** Looking this directly at it (about 20 degrees) for a moment makes it move somewhere else. */
    private static final double DIRECT_GAZE = 0.93D;

    @Nullable
    private UUID victimId;
    private int stage = HauntData.WATCHING;
    private int phaseTicks = 2400;
    private int gazeTicks;
    private int relocateCooldown;
    private int snuffCooldown = 100;
    private int soundCooldown = 60;
    private int holdTicks;
    @Nullable
    private Vec3 waypoint;
    @Nullable
    private SceneRunner sceneRunner;
    private int sceneAge;
    @Nullable
    private Challenge challenge;
    private int challengeTicks;
    private int challengeGrace;
    @Nullable
    private Vec3 victimLastPos;
    /** Client-side: smoothed 0 = standing, 1 = on all fours. */
    private float crawl;
    private float crawlO;
    /** Client-side: ticks since the scene step last changed. */
    private int animTicks;
    private int lastAnimSeen = -1;
    private Scene lastSceneSeen = Scene.NONE;

    public TheOneEntity(EntityType<? extends TheOneEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 666.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 96.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** Puts it into the world facing its victim. */
    @Nullable
    public static TheOneEntity spawn(ServerLevel level, ServerPlayer victim, Vec3 pos, int stage, Phase phase) {
        TheOneEntity one = ModEntities.THE_ONE.get().create(level);
        if (one == null) {
            return null;
        }
        float yaw = yawTowards(pos, victim.position());
        one.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        one.setYHeadRot(yaw);
        one.setYBodyRot(yaw);
        one.victimId = victim.getUUID();
        one.stage = stage;
        one.startPhase(phase);
        level.addFreshEntity(one);
        return one;
    }

    /** Puts it into the world to play out a scripted moment. */
    @Nullable
    public static TheOneEntity spawnForScene(ServerLevel level, ServerPlayer victim, Vec3 pos, int stage, Scene scene, SceneRunner runner) {
        TheOneEntity one = ModEntities.THE_ONE.get().create(level);
        if (one == null) {
            return null;
        }
        float yaw = yawTowards(pos, victim.position());
        one.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        one.setYHeadRot(yaw);
        one.setYBodyRot(yaw);
        one.victimId = victim.getUUID();
        one.stage = stage;
        one.entityData.set(DATA_SCENE, scene.ordinal());
        one.sceneRunner = runner;
        one.startPhase(Phase.SCENE);
        level.addFreshEntity(one);
        runner.start(one, level, victim);
        return one;
    }

    public static float yawTowards(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, Phase.WATCH.ordinal());
        this.entityData.define(DATA_CRAWLING, false);
        this.entityData.define(DATA_SCENE, Scene.NONE.ordinal());
        this.entityData.define(DATA_ANIM, 0);
        this.entityData.define(DATA_SIDE, false);
    }

    public Phase getPhase() {
        return Phase.byId(this.entityData.get(DATA_PHASE));
    }

    public boolean isCrawling() {
        return this.entityData.get(DATA_CRAWLING);
    }

    public void setCrawling(boolean crawling) {
        this.entityData.set(DATA_CRAWLING, crawling);
    }

    public Scene getScene() {
        return this.getPhase() == Phase.SCENE ? Scene.byId(this.entityData.get(DATA_SCENE)) : Scene.NONE;
    }

    public int getAnim() {
        return this.entityData.get(DATA_ANIM);
    }

    public void setAnim(int step) {
        this.entityData.set(DATA_ANIM, step);
    }

    public boolean isLeaningLeft() {
        return this.entityData.get(DATA_SIDE);
    }

    public void setLeaningLeft(boolean left) {
        this.entityData.set(DATA_SIDE, left);
    }

    /** Client-side: ticks (with partial tick) since the current scene step began. */
    public float getAnimTime(float partialTick) {
        return this.animTicks + partialTick;
    }

    public int getStage() {
        return this.stage;
    }

    public boolean isHaunting(Player player) {
        return player.getUUID().equals(this.victimId);
    }

    private void startPhase(Phase phase) {
        this.entityData.set(DATA_PHASE, phase.ordinal());
        this.gazeTicks = 0;
        this.waypoint = null;
        this.setDeltaMovement(Vec3.ZERO);
        this.phaseTicks = switch (phase) {
            case WATCH -> switch (this.stage) {
                case HauntData.WATCHING -> 1800 + this.random.nextInt(1200);
                case HauntData.APPROACHING -> 600 + this.random.nextInt(600);
                default -> 300 + this.random.nextInt(300);
            };
            case STALK -> this.stage >= HauntData.HUNTING ? 600 + this.random.nextInt(600) : 1200 + this.random.nextInt(1200);
            case HUNT -> 3600;
            case REVEALED -> 60;
            // Scenes end themselves; this is only a safety net.
            case SCENE -> 1800;
        };
        this.setCrawling(phase == Phase.HUNT);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        // Walls don't stop it.
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;
        this.setNoGravity(true);
        if (this.level().isClientSide) {
            this.crawlO = this.crawl;
            float target = this.isCrawling() ? 1.0F : 0.0F;
            // Unfolding to full height after coming through a door takes its time.
            float rate = this.getScene() == Scene.DOOR && target < this.crawl ? 0.035F : 0.15F;
            if (this.tickCount <= 1) {
                this.crawl = target;
            }
            this.crawl += Mth.clamp(target - this.crawl, -rate, rate);
            Scene scene = this.getScene();
            if (this.getAnim() != this.lastAnimSeen || scene != this.lastSceneSeen) {
                this.lastAnimSeen = this.getAnim();
                this.lastSceneSeen = scene;
                this.animTicks = 0;
            } else {
                this.animTicks++;
            }
        }
    }

    public float getCrawlAmount(float partialTick) {
        return Mth.lerp(partialTick, this.crawlO, this.crawl);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer victim = this.resolveVictim();
        if (victim == null) {
            this.leave(HauntManager.Exit.LOST);
            return;
        }
        Phase phase = this.getPhase();
        if (phase != Phase.REVEALED && phase != Phase.SCENE && this.isDaylightAround(victim)) {
            this.leave(HauntManager.Exit.DAYLIGHT);
            return;
        }
        if (--this.phaseTicks <= 0) {
            this.nextPhase(victim);
            return;
        }
        if (this.relocateCooldown > 0) {
            this.relocateCooldown--;
        }
        if (phase != Phase.REVEALED) {
            this.getLookControl().setLookAt(victim, 180.0F, 180.0F);
        }
        switch (phase) {
            case WATCH -> this.tickWatch(level, victim);
            case STALK -> this.tickStalk(level, victim);
            case HUNT -> this.tickHunt(level, victim);
            case REVEALED -> this.tickRevealed(level);
            case SCENE -> this.tickScene(level, victim);
        }
    }

    @Nullable
    private ServerPlayer resolveVictim() {
        if (this.victimId == null) {
            // Summoned or hatched from a spawn egg: whoever is closest.
            Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 64.0D, EntitySelector.NO_SPECTATORS);
            if (nearest == null) {
                return null;
            }
            this.victimId = nearest.getUUID();
            this.stage = HauntData.HUNTING;
        }
        Player player = this.level().getPlayerByUUID(this.victimId);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.isAlive() && !serverPlayer.isSpectator()
                && serverPlayer.distanceToSqr(this) < 96.0D * 96.0D) {
            return serverPlayer;
        }
        return null;
    }

    private boolean isDaylightAround(ServerPlayer victim) {
        return this.level().isDay() && this.level().canSeeSky(victim.blockPosition()) && this.level().canSeeSky(this.blockPosition());
    }

    private void nextPhase(ServerPlayer victim) {
        switch (this.getPhase()) {
            case WATCH -> {
                if (this.stage >= HauntData.APPROACHING) {
                    this.startPhase(Phase.STALK);
                } else {
                    this.leave(HauntManager.Exit.TIMEOUT);
                }
            }
            case STALK -> {
                if (this.stage >= HauntData.HUNTING || (this.stage == HauntData.APPROACHING && this.random.nextBoolean())) {
                    this.startPhase(Phase.HUNT);
                    HorrorNetwork.playSoundTo(victim, ModSounds.DRONE.get(), SoundSource.HOSTILE, victim.getEyePosition(), 0.7F, 0.8F);
                } else {
                    this.leave(HauntManager.Exit.TIMEOUT);
                }
            }
            case HUNT, SCENE -> this.leave(HauntManager.Exit.TIMEOUT);
            case REVEALED -> this.leave(HauntManager.Exit.WOUNDED);
        }
    }

    // ----------------------------------------------------------------- watch

    private void tickWatch(ServerLevel level, ServerPlayer victim) {
        this.setDeltaMovement(Vec3.ZERO);
        double dx = victim.getX() - this.getX();
        double dz = victim.getZ() - this.getZ();
        if (dx * dx + dz * dz < 10.0D * 10.0D) {
            // You walked up to where it was standing. Nothing there but this.
            boolean scrap = this.stage < HauntData.HUNTING || this.random.nextFloat() < 0.3F;
            if (scrap) {
                ItemEntity item = new ItemEntity(level, this.getX(), this.getY() + 0.2D, this.getZ(), new ItemStack(ModItems.SHADOW_SCRAP.get()));
                item.setDeltaMovement(Vec3.ZERO);
                item.setExtendedLifetime();
                level.addFreshEntity(item);
                level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.5D, this.getZ(), 20, 0.3D, 0.8D, 0.3D, 0.01D);
            }
            this.playSoundAt(ModSounds.VANISH.get(), 0.6F);
            this.leave(HauntManager.Exit.APPROACHED);
        }
    }

    // ----------------------------------------------------------------- stalk

    private void tickStalk(ServerLevel level, ServerPlayer victim) {
        int safe = HorrorConfig.SAFE_LIGHT_LEVEL.get();
        if (this.isGazedAt(level) && this.relocateCooldown == 0) {
            this.relocateCooldown = 20;
            Vec3 spot = this.findPeripheralSpot(level, victim, 8.0D, 16.0D, safe);
            if (spot != null) {
                this.waypoint = spot;
                this.blinkTo(spot);
            }
        }

        boolean arrived = this.waypoint != null && this.holdTicks <= 0 && this.position().distanceToSqr(this.waypoint) < 1.0D;
        if (arrived || (this.waypoint == null && this.tickCount % 10 == 0)) {
            this.waypoint = this.findPeripheralSpot(level, victim, 8.0D, 16.0D, safe);
            this.holdTicks = 60 + this.random.nextInt(100);
        }
        if (this.waypoint != null && this.position().distanceToSqr(this.waypoint) > 1.0D) {
            this.driftTowards(this.waypoint, 0.14D, safe);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            this.setCrawling(false);
            this.holdTicks--;
        }

        if (--this.snuffCooldown <= 0) {
            this.snuffNear(level, victim);
            this.snuffCooldown = this.stage >= HauntData.HUNTING ? 40 + this.random.nextInt(60) : 100 + this.random.nextInt(100);
        }
        this.ambientSounds(victim, 14.0D);
    }

    // ------------------------------------------------------------------ hunt

    private void tickHunt(ServerLevel level, ServerPlayer victim) {
        int safe = HorrorConfig.SAFE_LIGHT_LEVEL.get();
        double dist = this.distanceTo(victim);

        if (this.isGazedAt(level) && this.relocateCooldown == 0) {
            // You can't face it. It is just somewhere else now, and a little closer.
            this.relocateCooldown = 20;
            Vec3 spot = this.findPeripheralSpot(level, victim, Math.max(2.5D, dist * 0.6D), Math.max(4.0D, dist * 0.85D), safe);
            if (spot != null) {
                this.blinkTo(spot);
                return;
            }
        }

        boolean victimInLight = isLit(level, victim, safe);
        if (dist < 1.8D && !victimInLight) {
            this.touch(victim);
            return;
        }

        Vec3 target = victim.position();
        boolean blocked = !this.driftTowards(target, this.stage >= HauntData.HUNTING ? 0.24D : 0.18D, safe);
        if (blocked || victimInLight) {
            // Waits at the edge of the light and works on the light instead.
            if (--this.snuffCooldown <= 0) {
                this.snuffNear(level, victim);
                this.snuffCooldown = 30 + this.random.nextInt(40);
            }
        } else if (--this.snuffCooldown <= 0) {
            this.snuffNear(level, victim);
            this.snuffCooldown = 80 + this.random.nextInt(60);
        }
        this.ambientSounds(victim, 18.0D);
    }

    // ----------------------------------------------------------------- scenes

    private void tickScene(ServerLevel level, ServerPlayer victim) {
        if (this.sceneRunner == null) {
            this.leave(HauntManager.Exit.LOST);
            return;
        }
        this.sceneRunner.tick(this, level, victim, this.sceneAge++);
        if (this.isAlive() && this.challenge != null) {
            this.tickChallenge(level, victim);
        }
    }

    /** Puts a countdown on the victim's screen. Failing it means being touched. */
    public void startChallenge(ServerPlayer victim, Challenge type, int ticks) {
        this.challenge = type;
        this.challengeTicks = ticks;
        this.challengeGrace = 25;
        this.victimLastPos = victim.position();
        HorrorNetwork.send(victim, HorrorPacket.Type.CHALLENGE, new BlockPos(ticks, 0, 0), type.ordinal());
    }

    public boolean hasChallenge() {
        return this.challenge != null;
    }

    /** 0 at the start of the countdown, 1 when it runs out. */
    public float getChallengeProgress(int length) {
        return this.challenge == null ? 0.0F : Mth.clamp(1.0F - (float) this.challengeTicks / length, 0.0F, 1.0F);
    }

    private void tickChallenge(ServerLevel level, ServerPlayer victim) {
        Challenge type = this.challenge;
        if (type == null) {
            return;
        }
        this.challengeTicks--;
        Boolean survived = null;
        switch (type) {
            case LIGHT_RUN, LIGHT_PLACE -> {
                if (isLit(level, victim, HorrorConfig.SAFE_LIGHT_LEVEL.get())) {
                    survived = true;
                } else if (this.challengeTicks <= 0) {
                    survived = false;
                }
            }
            case DONT_LOOK -> {
                if (this.isSeenBy(level, victim, Sight.ON_SCREEN)) {
                    survived = false;
                } else if (this.challengeTicks <= 0) {
                    survived = true;
                }
            }
            case STILL -> {
                Vec3 now = victim.position();
                double moved = this.victimLastPos == null ? 0.0D : now.subtract(this.victimLastPos).horizontalDistance();
                this.victimLastPos = now;
                if (this.challengeGrace > 0) {
                    this.challengeGrace--;
                } else if (!victim.isShiftKeyDown() || moved > 0.02D) {
                    survived = false;
                }
                if (survived == null && this.challengeTicks <= 0) {
                    survived = true;
                }
            }
        }
        if (survived != null) {
            this.endChallenge(victim, survived);
        }
    }

    private void endChallenge(ServerPlayer victim, boolean survived) {
        this.challenge = null;
        HorrorNetwork.send(victim, HorrorPacket.Type.CHALLENGE_END, survived ? 1 : 0);
        if (!survived) {
            this.touch(victim);
            return;
        }
        // It lets you go this time, and leaves something behind.
        if (this.level() instanceof ServerLevel level && (this.stage < HauntData.HUNTING || this.random.nextBoolean())) {
            ItemEntity item = new ItemEntity(level, this.getX(), this.getY() + 0.2D, this.getZ(), new ItemStack(ModItems.SHADOW_SCRAP.get()));
            item.setDeltaMovement(Vec3.ZERO);
            item.setExtendedLifetime();
            level.addFreshEntity(item);
        }
        this.playSoundAt(ModSounds.VANISH.get(), 1.0F);
        victim.displayClientMessage(Component.translatable("cornerofeye.message.survived").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        HauntManager.award(victim, "survived");
        this.leave(HauntManager.Exit.SURVIVED);
    }

    /** True if the player has it on screen with nothing in between. */
    public boolean isSeenBy(ServerLevel level, Player player, double minDot) {
        Vec3 head = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.85D, this.getZ());
        Vec3 middle = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ());
        return Sight.canSee(level, player, head, minDot) || Sight.canSee(level, player, middle, minDot);
    }

    /** Moves straight to a point this tick, through anything, facing where it goes. */
    public void stepTo(Vec3 target, boolean faceIt) {
        Vec3 from = this.position();
        this.setPos(target.x, target.y, target.z);
        this.setDeltaMovement(Vec3.ZERO);
        if (faceIt && from.distanceToSqr(target) > 1.0E-4D) {
            this.face(target.add(target.subtract(from)));
        }
    }

    public void face(Vec3 target) {
        float yaw = yawTowards(this.position(), target);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
    }

    public void touch(ServerPlayer victim) {
        HorrorNetwork.send(victim, HorrorPacket.Type.JUMPSCARE, 18);
        this.level().playSound(victim, this.getX(), this.getY(), this.getZ(), ModSounds.SCREAM.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        float damage = (float) (victim.getMaxHealth() * HorrorConfig.TOUCH_DAMAGE_FRACTION.get());
        if (this.stage < HauntData.HUNTING) {
            // Before the hunt begins it only shows you what it could do.
            damage = Math.min(damage, victim.getHealth() - 1.0F);
        }
        if (damage > 0.0F) {
            victim.invulnerableTime = 0;
            victim.hurt(this.damageSources().mobAttack(this), damage);
        }
        victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0), this);
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), this);
        HauntManager.award(victim, "touched");
        this.leave(HauntManager.Exit.TOUCHED);
    }

    // -------------------------------------------------------------- revealed

    /** Called when the Lantern of Revelation catches it. */
    public void reveal(ServerPlayer catcher) {
        if (!(this.level() instanceof ServerLevel level) || this.getPhase() == Phase.REVEALED) {
            return;
        }
        Player victim = this.victimId == null ? null : level.getPlayerByUUID(this.victimId);
        if (this.challenge != null && victim instanceof ServerPlayer serverVictim) {
            // Caught in the middle of its moment: that counts as surviving it.
            this.challenge = null;
            HorrorNetwork.send(serverVictim, HorrorPacket.Type.CHALLENGE_END, 1);
        }
        this.startPhase(Phase.REVEALED);
        float yaw = yawTowards(this.position(), catcher.position());
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.playSoundAt(ModSounds.WAIL.get(), 2.0F);
        HauntManager.onWounded(level, this, catcher);
    }

    private void tickRevealed(ServerLevel level) {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.6D, this.getZ(), 3, 0.3D, 0.9D, 0.3D, 0.02D);
        }
    }

    /** The last wound: it comes apart. */
    public void playBanishEffects(ServerLevel level) {
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.6D, this.getZ(), 120, 0.5D, 1.2D, 0.5D, 0.08D);
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.6D, this.getZ(), 40, 0.5D, 1.2D, 0.5D, 0.05D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BANISH.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
    }

    /** The lantern flash didn't catch it, but it doesn't like light this bright. */
    public void scareAway() {
        this.playSoundAt(ModSounds.VANISH.get(), 1.0F);
        this.leave(HauntManager.Exit.SCARED);
    }

    // ------------------------------------------------------------- movement

    /** Moves straight at the target, through blocks. Refuses to drift into light. @return false if light is in the way. */
    private boolean driftTowards(Vec3 target, double speed, int safeLight) {
        Vec3 delta = target.subtract(this.position());
        double length = delta.length();
        if (length < 0.05D) {
            this.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        Vec3 step = delta.scale(Math.min(speed, length) / length);
        Vec3 next = this.position().add(step);
        if (this.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(next)) >= safeLight
                || this.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(next.add(0.0D, 2.0D, 0.0D))) >= safeLight) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setCrawling(false);
            return false;
        }
        this.setDeltaMovement(step);
        this.setCrawling(true);
        float yaw = yawTowards(this.position(), target);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        return true;
    }

    private void blinkTo(Vec3 spot) {
        this.teleportTo(spot.x, spot.y, spot.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.relocateCooldown = 30;
        this.gazeTicks = 0;
        if (this.random.nextInt(3) == 0) {
            this.playSoundAt(ModSounds.SKITTER.get(), 0.8F);
        }
    }

    @Nullable
    private Vec3 findPeripheralSpot(ServerLevel level, ServerPlayer victim, double minDist, double maxDist, int safe) {
        // 55-110 degrees off where you are looking: the edge of the screen, or just past it.
        return SpotFinder.find(level, victim, minDist, maxDist, 82.0F, 27.0F, 6, 16,
                spot -> SpotFinder.isDark(level, spot, safe) && !Sight.canSee(level, victim, spot.add(0.0D, 1.6D, 0.0D), DIRECT_GAZE));
    }

    private void snuffNear(ServerLevel level, ServerPlayer victim) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return;
        }
        Lights.snuffOne(level, victim.blockPosition(), 14, 6);
    }

    private void ambientSounds(ServerPlayer victim, double range) {
        if (--this.soundCooldown > 0 || this.distanceTo(victim) > range) {
            return;
        }
        boolean breath = this.random.nextBoolean();
        HorrorNetwork.playSoundTo(victim, breath ? ModSounds.BREATH.get() : ModSounds.SKITTER.get(), SoundSource.HOSTILE,
                this.getEyePosition(), breath ? 0.7F : 0.5F, 0.8F + this.random.nextFloat() * 0.25F);
        this.soundCooldown = 70 + this.random.nextInt(80);
    }

    public void playSoundAt(SoundEvent sound, float volume) {
        this.level().playSound(null, this.getX(), this.getY() + 1.5D, this.getZ(), sound, SoundSource.HOSTILE, volume,
                0.85F + this.random.nextFloat() * 0.2F);
    }

    // --------------------------------------------------------------- helpers

    public static boolean isLit(ServerLevel level, Player player, int safe) {
        return level.getBrightness(LightLayer.BLOCK, player.blockPosition()) >= safe
                || level.getBrightness(LightLayer.BLOCK, BlockPos.containing(player.getEyePosition())) >= safe;
    }

    /** True once somebody has been looking straight at it (with nothing in between) for a moment. */
    private boolean isGazedAt(ServerLevel level) {
        Vec3 head = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.85D, this.getZ());
        Vec3 middle = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ());
        boolean seen = false;
        for (Player player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(this) < 64.0D * 64.0D
                    && (Sight.canSee(level, player, head, DIRECT_GAZE) || Sight.canSee(level, player, middle, DIRECT_GAZE))) {
                seen = true;
                break;
            }
        }
        this.gazeTicks = seen ? this.gazeTicks + 1 : 0;
        return this.gazeTicks > 8;
    }

    /** Whether the lantern flash from this player catches it. */
    public boolean isCaughtBy(Player player, double radius) {
        if (this.getPhase() == Phase.REVEALED || this.distanceTo(player) > radius) {
            return false;
        }
        Level level = this.level();
        Vec3 head = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.85D, this.getZ());
        Vec3 middle = new Vec3(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ());
        // Anywhere on screen counts, the corners included.
        return Sight.canSee(level, player, head, 0.25D) || Sight.canSee(level, player, middle, 0.25D);
    }

    public void leave(HauntManager.Exit exit) {
        if (this.challenge != null && this.victimId != null && this.level().getPlayerByUUID(this.victimId) instanceof ServerPlayer victim) {
            HorrorNetwork.send(victim, HorrorPacket.Type.CHALLENGE_END, 2);
        }
        this.challenge = null;
        if (this.level() instanceof ServerLevel level) {
            HauntManager.onGone(level, this, exit);
        }
        this.discard();
    }

    // ------------------------------------------------------------- overrides

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        // Swords go straight through. Only the lantern can do anything to it.
        if (!this.level().isClientSide && source.getEntity() instanceof Player && this.getPhase() != Phase.REVEALED) {
            this.playSoundAt(ModSounds.VANISH.get(), 1.0F);
            this.leave(HauntManager.Exit.SCARED);
        }
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void checkDespawn() {
        // Every appearance ends on its own.
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
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Nothing. You won't hear it walk.
    }

    @Override
    public int getMaxHeadYRot() {
        // Further than any neck should turn.
        return 150;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 2.9F;
    }
}
