package com.laropam.cornerofeye.entity;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.haunt.Doll;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.haunt.LightSuppression;
import com.laropam.cornerofeye.haunt.Lights;
import com.laropam.cornerofeye.haunt.Nightmares;
import com.laropam.cornerofeye.haunt.Sight;
import com.laropam.cornerofeye.haunt.SpotFinder;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModEntities;
import com.laropam.cornerofeye.registry.ModItems;
import com.laropam.cornerofeye.registry.ModSounds;
import com.laropam.cornerofeye.entity.scene.SceneRunner;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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
    /** Haunt stage when it appeared: decides how far it has rotted. */
    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.INT);
    /** Days into the haunt when it appeared: it grows a little every day. */
    private static final EntityDataAccessor<Integer> DATA_DAYS =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.INT);
    /**
     * Burrow.NONE/FORWARD/UP/DOWN/THROUGH in the low 3 bits, whether it listens first in bit 3, and a counter
     * above them, so each new block restarts the animation.
     */
    private static final EntityDataAccessor<Integer> DATA_DIG =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.INT);
    /** Ticks the current block takes to break (or to stare, for THROUGH). */
    private static final EntityDataAccessor<Integer> DATA_DIG_LENGTH =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.INT);
    /** Leans to its own left (true) or right while peeking. */
    private static final EntityDataAccessor<Boolean> DATA_SIDE =
            SynchedEntityData.defineId(TheOneEntity.class, EntityDataSerializers.BOOLEAN);

    /** Looking this directly at it (about 20 degrees) for a moment makes it move somewhere else. */
    private static final double DIRECT_GAZE = 0.93D;
    /** The longest it keeps going straight through a wall the old way before it looks for a path again. */
    private static final int SEEP_TICKS = 80;
    /** How long it stands below someone it can't climb to, looking up, before it stops waiting. */
    private static final int STARE_TICKS = 100;
    /** How long it stands behind you, still, once it has come up after you. */
    private static final int LURK_TICKS = 40;

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
    /** Lights it has put out on this visit: nobody can light them again until it leaves. */
    private final List<BlockPos> smothered = new ArrayList<>();
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
    /** Client-side: blocks of room above it, smoothed, so it stoops or goes down on all fours under a low ceiling. */
    private float headroom = 6.0F;
    private float headroomO = 6.0F;
    /** Client-side: 1 squeezed flat into a one-block space, 0 crawling with its limbs spread; smoothed. */
    private float prone;
    private float proneO;
    /** Client-side: how steeply it is going up (positive) or down a tunnel, in radians; smoothed. */
    private float climb;
    private float climbO;
    private final Burrow burrow = new Burrow(this);
    private final Navigator navigator = new Navigator(this);
    /** Has torn its way through something on this hunt: from now on, wherever there is room, it gets up and walks. */
    private boolean upright;
    /** Server side: a door just ahead, so it walks up to it upright instead of crawling with its arms through the wall. */
    private boolean goingToDoor;
    /** Ticks left of getting up after crawling out: it doesn't move until it has unfolded. */
    private int riseTicks;
    /** Ticks left of going straight through a wall it can neither get round nor break, and whether it is in it yet. */
    private int seepTicks;
    private boolean seepIn;
    /** Hunting someone up in the open air it has nothing to climb to: how long it has stood below, looking up. */
    private int stareTicks;
    /** Where below it stands to be seen looking up, and where you were when it went there. */
    @Nullable
    private Vec3 stareSpot;
    private Vec3 stareFrom = Vec3.ZERO;
    /** Ticks left of standing still behind you, breathing, before it moves again. */
    private int lurkTicks;
    /** Client-side: ticks since the dig state last changed. */
    private int digAge;
    private int lastDig;
    /** One of its empty copies on the last night: catching it wastes a scrap. Server side only. */
    private boolean decoy;
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
        one.setStage(level, stage);
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
        one.setStage(level, stage);
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
        this.entityData.define(DATA_DIG, 0);
        this.entityData.define(DATA_DIG_LENGTH, 1);
        this.entityData.define(DATA_STAGE, HauntData.WATCHING);
        this.entityData.define(DATA_DAYS, 0);
    }

    /**
     * Nothing about a visit survives a reload. These keys are only read, for testing and for posing it with
     * {@code /summon cornerofeye:the_one ~ ~ ~ {NoAI:1b,Stage:3,Days:9,Crawling:1b,Phase:"SCENE",Scene:"PEEK",Anim:1}}
     * (trailers, screenshots); the pose keys need NoAI so it doesn't try to act out a scene it doesn't have.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Stage")) {
            this.stage = Mth.clamp(tag.getInt("Stage"), HauntData.WATCHING, HauntData.HUNTING);
            this.entityData.set(DATA_STAGE, this.stage);
        }
        if (tag.contains("Days")) {
            this.entityData.set(DATA_DAYS, Math.max(0, tag.getInt("Days")));
        }
        if (!this.isNoAi() && tag.contains("Phase")) {
            // /summon cornerofeye:the_one ~ ~ ~ {Phase:"HUNT"}: comes for the nearest player right away.
            for (Phase phase : new Phase[]{Phase.WATCH, Phase.STALK, Phase.HUNT}) {
                if (phase.name().equalsIgnoreCase(tag.getString("Phase"))) {
                    this.startPhase(phase);
                }
            }
        }
        if (this.isNoAi()) {
            this.setCrawling(tag.getBoolean("Crawling"));
            for (Phase phase : Phase.values()) {
                if (phase.name().equalsIgnoreCase(tag.getString("Phase"))) {
                    this.entityData.set(DATA_PHASE, phase.ordinal());
                }
            }
            for (Scene scene : Scene.values()) {
                if (scene.name().equalsIgnoreCase(tag.getString("Scene"))) {
                    this.entityData.set(DATA_SCENE, scene.ordinal());
                }
            }
            this.setAnim(tag.getInt("Anim"));
            if (tag.contains("Dig")) {
                int dig = Mth.clamp(tag.getInt("Dig"), Burrow.NONE, Burrow.THROUGH);
                this.setDig(dig, 1, Math.max(Burrow.LISTEN + 1, tag.getInt("DigLength")), dig != Burrow.THROUGH);
            }
        }
    }

    private void setStage(ServerLevel level, int stage) {
        this.stage = stage;
        this.entityData.set(DATA_STAGE, stage);
        this.entityData.set(DATA_DAYS, (int) Math.min(Integer.MAX_VALUE, HauntData.get(level.getServer()).getDays()));
    }

    /** Client side: 1-3, which version of it to draw. */
    public int getVisualStage() {
        return Mth.clamp(this.entityData.get(DATA_STAGE), HauntData.WATCHING, HauntData.HUNTING);
    }

    /** 0 on its first night, 1 once the hunt is on and it has had a week: taller, twitchier, worse. */
    public float getMenace() {
        float byStage = (this.getVisualStage() - 1) / 2.0F;
        float byDays = this.entityData.get(DATA_DAYS) / 8.0F;
        return Mth.clamp(Math.max(byStage, byDays), 0.0F, 1.0F);
    }

    /** Its voice gets deeper as it gets worse. */
    public float voicePitch() {
        return 1.0F - 0.08F * (this.stage - 1);
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
        if (this.level() instanceof ServerLevel level) {
            this.burrow.cancel(level);
        }
        this.entityData.set(DATA_PHASE, phase.ordinal());
        this.upright = false;
        this.seepTicks = 0;
        this.stareTicks = 0;
        this.stareSpot = null;
        this.lurkTicks = 0;
        this.navigator.reset();
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
            this.headroomO = this.headroom;
            int dig = this.entityData.get(DATA_DIG);
            this.digAge = dig == this.lastDig ? this.digAge + 1 : 0;
            this.lastDig = dig;
            float room = this.measureHeadroom();
            this.headroom = this.tickCount <= 1 ? room : this.headroom + Mth.clamp(room - this.headroom, -0.3F, 0.3F);
            // Flat in anything lower than its crawl or narrower than its spread arms; unfolding slowly as it gets out.
            this.proneO = this.prone;
            float squeezed = this.fitsToRoom() && (room < 1.5F * this.getVisualScale() || !this.hasRoomToSpread()) ? 1.0F : 0.0F;
            this.prone = this.tickCount <= 1 ? squeezed : this.prone + Mth.clamp(squeezed - this.prone, -0.06F, 0.12F);
            this.climbO = this.climb;
            double rise = this.getY() - this.yo;
            double run = Math.sqrt((this.getX() - this.xo) * (this.getX() - this.xo) + (this.getZ() - this.zo) * (this.getZ() - this.zo));
            // Kept while it stops to dig in a tunnel, so it stays lying along its slope; out in the open it levels out.
            float slope = rise * rise + run * run > 1.0E-4D ? (float) Mth.clamp(Math.atan2(rise, run), -1.45D, 1.45D)
                    : this.prone > 0.5F ? this.climb : this.climb * 0.8F;
            this.climb += (slope - this.climb) * 0.25F;
            float target = this.isCrawling() || this.mustCrawl() ? 1.0F : 0.0F;
            // Unfolding to full height after coming through a door takes its time.
            // Unfolding after crawling out of a hole it dug is just as slow.
            boolean slowRise = this.getScene() == Scene.DOOR || this.getPhase() == Phase.HUNT;
            float rate = slowRise && target < this.crawl ? 0.035F : 0.15F;
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

    void setDig(int direction, int sequence, int length, boolean listens) {
        this.entityData.set(DATA_DIG, direction == Burrow.NONE ? 0 : direction | (listens ? 8 : 0) | (sequence & 0xFFFF) << 4);
        this.entityData.set(DATA_DIG_LENGTH, Math.max(1, length));
    }

    /** Burrow.NONE, FORWARD, UP, DOWN, or THROUGH right after breaking a block. */
    public int getDigDirection() {
        return this.entityData.get(DATA_DIG) & 7;
    }

    /** Ticks it spends listening before the current block (none once it is tunnelling). */
    public int getDigListen() {
        return (this.entityData.get(DATA_DIG) & 8) != 0 ? Burrow.LISTEN : 0;
    }

    /** Client side: ticks into the current block. */
    public float getDigTime(float partialTick) {
        return this.digAge + partialTick;
    }

    /** Client side: 0-1 through the current block, after the moment it spends listening. */
    public float getDigProgress(float partialTick) {
        int length = this.entityData.get(DATA_DIG_LENGTH);
        int listen = this.getDigListen();
        return Mth.clamp((this.getDigTime(partialTick) - listen) / Math.max(1, length - listen), 0.0F, 1.0F);
    }

    /**
     * Crawls towards {@code target} at {@code speed}: straight there if nothing is in the way, otherwise by its
     * own way round (doors, gaps), tearing through only what it must. Without digging it passes through walls.
     *
     * @return false while it is busy with a door or a block
     */
    public boolean crawlTowards(Vec3 target, double speed) {
        this.setCrawling(true);
        Navigator.Result result = this.level() instanceof ServerLevel level && Burrow.allowed(level, this)
                ? this.navigator.follow(level, this.burrow, target, speed, true) : Navigator.Result.SEEP;
        if (result == Navigator.Result.SEEP) {
            // Not allowed to dig, or nothing between it can break: straight through, the old way.
            Vec3 delta = target.subtract(this.position());
            double length = delta.length();
            if (length > 1.0E-3D) {
                this.stepTo(this.position().add(delta.scale(Math.min(speed, length) / length)), true);
            }
            return true;
        }
        return result != Navigator.Result.BUSY;
    }

    void setGoingToDoor(boolean goingToDoor) {
        this.goingToDoor = goingToDoor;
    }

    /** Called by Burrow when a block gives way: once it has come through, it stands up wherever it can. */
    void brokeThrough() {
        this.upright = true;
    }

    /** Server side: open blocks for its full height, here and a step ahead. */
    private boolean hasRoomToStand(Vec3 at) {
        int height = Mth.ceil(2.3F * this.getVisualScale());
        Vec3 ahead = at.add(Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.8D));
        for (Vec3 column : new Vec3[]{at, ahead}) {
            BlockPos feet = BlockPos.containing(column.x, column.y + 0.05D, column.z);
            for (int i = 0; i < height; i++) {
                BlockPos pos = feet.above(i);
                if (!this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Server side: something solid right over it, so it is squeezing through a tunnel or a crawlspace. */
    private boolean isSqueezed() {
        BlockPos above = BlockPos.containing(this.position().add(Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.8D)).add(0.0D, 1.2D, 0.0D));
        return !this.level().getBlockState(above).getCollisionShape(this.level(), above).isEmpty();
    }

    /** Whether it can get from here to {@code spot} without going through anything solid. */
    private boolean openPathTo(Vec3 spot) {
        Vec3 from = this.position().add(0.0D, 0.5D, 0.0D);
        return this.level().clip(new ClipContext(from, spot.add(0.0D, 0.5D, 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    /** How much taller it gets over the days; the renderer scales the model by this. */
    public float getVisualScale() {
        return 1.0F + 0.22F * this.getMenace();
    }

    /**
     * Client side: room between its feet and whatever is above, up to 6 blocks. Inside a wall (passing
     * through it) there is no telling, so it keeps what it had.
     */
    private float measureHeadroom() {
        BlockPos feet = BlockPos.containing(this.getX(), this.getY() + 0.05D, this.getZ());
        for (int i = 0; i <= 6; i++) {
            BlockPos pos = feet.above(i);
            VoxelShape shape = this.level().getBlockState(pos).getCollisionShape(this.level(), pos);
            if (!shape.isEmpty()) {
                return i == 0 ? this.headroom : (float) (pos.getY() + shape.min(Direction.Axis.Y) - this.getY());
            }
        }
        return 6.0F;
    }

    /** Client side: free blocks to both sides of its middle, where its spread arms and knees go. */
    private boolean hasRoomToSpread() {
        Vec3 ahead = Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(1.0D * this.getVisualScale());
        Vec3 side = new Vec3(-ahead.z, 0.0D, ahead.x).normalize();
        Vec3 middle = this.position().add(ahead).add(0.0D, 0.2D, 0.0D);
        for (Vec3 at : new Vec3[]{middle.add(side), middle.subtract(side)}) {
            for (int up = 0; up <= 1; up++) {
                BlockPos pos = BlockPos.containing(at).above(up);
                if (!this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 0-1: how squeezed it is, flat on the ground with everything tucked in to fit a one-block tunnel. */
    public float getProne(float partialTick) {
        return Mth.lerp(partialTick, this.proneO, this.prone);
    }

    /** Free height above it, in blocks (the renderer squashes it a little to fit a tunnel). */
    public float getHeadroom(float partialTick) {
        return Mth.lerp(partialTick, this.headroomO, this.headroom);
    }

    /** Radians up (positive) or down along the tunnel it is crawling through. */
    public float getClimb(float partialTick) {
        return Mth.lerp(partialTick, this.climbO, this.climb);
    }

    /** Scenes with poses of their own don't fold to fit the room. */
    private boolean fitsToRoom() {
        Scene scene = this.getScene();
        return scene != Scene.CEILING && scene != Scene.PEEK && scene != Scene.UNDERWATER && this.getPhase() != Phase.REVEALED;
    }

    /** Under about two blocks of ceiling (more as it grows) it can only move on all fours. */
    public boolean mustCrawl() {
        return this.fitsToRoom() && this.headroom < 2.3F * this.getVisualScale();
    }

    /** 0 standing tall, 1 hunched right down with bent knees: a three-block room is not enough for it. */
    public float getStoop(float partialTick) {
        if (!this.fitsToRoom()) {
            return 0.0F;
        }
        float scale = this.getVisualScale();
        return Mth.clamp((3.3F * scale - Mth.lerp(partialTick, this.headroomO, this.headroom)) / scale, 0.0F, 1.0F);
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
            if (this.level() instanceof ServerLevel level) {
                this.setStage(level, HauntData.HUNTING);
            }
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
                ItemEntity item = new ItemEntity(level, this.getX(), this.getY() + 0.2D, this.getZ(), scraps(victim));
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
            this.driftTowards(this.waypoint, 0.14D, safe, false);
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
        if (this.burrow.isBusy()) {
            // Head down in the wall, or a hand on a door: nothing distracts it.
            this.burrow.tick(level);
            this.ambientSounds(victim, 18.0D);
            return;
        }

        if (this.isGazedAt(level) && this.relocateCooldown == 0) {
            // You can't face it. It is just somewhere else now, and a little closer.
            this.relocateCooldown = 20;
            Vec3 spot = this.findPeripheralSpot(level, victim, Math.max(2.5D, dist * 0.6D), Math.max(4.0D, dist * 0.85D), safe);
            // When it has to dig its way to you, it can't skip a wall by being somewhere else. Up in the open air
            // there is no wall to skip, only the height.
            if (spot != null && (!Burrow.allowed(level, this) || this.openPathTo(spot)
                    || this.stareTicks > 0 && Sight.clearLine(level, victim, spot.add(0.0D, 1.0D, 0.0D)))) {
                this.blinkTo(spot);
                return;
            }
        }

        if (this.lurkTicks > 0) {
            this.lurkTicks--;
            this.setDeltaMovement(Vec3.ZERO);
            this.face(victim.position());
            this.ambientSounds(victim, 18.0D);
            return;
        }

        boolean victimInLight = isLit(level, victim, safe);
        // Its reach, not its eyes, decides: hunched in a low room its head is up in the ceiling.
        if (dist < 1.8D && !victimInLight && (!Burrow.allowed(level, this) || this.openPathTo(victim.position()))) {
            this.touch(victim);
            return;
        }

        Vec3 target = victim.position();
        double speed = (this.stage >= HauntData.HUNTING ? 0.24D : 0.18D) * (HauntData.get(level.getServer()).isBloodMoon() ? 1.3D : 1.0D);
        boolean blocked;
        if (this.seepTicks > 0) {
            // Through what it couldn't get round, the old way, until it is out the other side.
            this.seepTicks--;
            boolean inside = Navigator.blocksBody(level, this.position());
            this.seepIn |= inside;
            blocked = !this.driftTowards(target, speed, safe, true);
            if (blocked || this.seepIn && !inside) {
                this.seepTicks = 0;
                this.navigator.reset();
            }
        } else {
            boolean canDig = Burrow.allowed(level, this);
            if (this.stareSpot != null && victim.position().distanceToSqr(this.stareFrom) > 9.0D) {
                // You moved: maybe there is a way to you now.
                this.stareSpot = null;
            }
            if (this.stareSpot != null) {
                // Out of reach up there: it goes to stand where you can see it, and looks up at you.
                Navigator.Result result = this.navigator.follow(level, this.burrow, this.stareSpot, speed, canDig);
                if (result != Navigator.Result.MOVING && result != Navigator.Result.BUSY) {
                    this.stareUp(level, victim, safe);
                }
                blocked = true;
            } else {
                Navigator.Result result = this.navigator.follow(level, this.burrow, target, speed, canDig);
                blocked = this.huntStep(level, victim, result, canDig, victimInLight, speed, safe);
            }
        }
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

    /** What it does with how getting to you went this tick. @return whether it is held back (by walls, light, height) */
    private boolean huntStep(ServerLevel level, ServerPlayer victim, Navigator.Result result, boolean canDig, boolean victimInLight,
                             double speed, int safe) {
        Vec3 target = victim.position();
        boolean blocked = result == Navigator.Result.STUCK || result == Navigator.Result.SEEP || result == Navigator.Result.OUT_OF_REACH
                || result == Navigator.Result.ARRIVED && victimInLight;
        if ((result == Navigator.Result.SEEP || result == Navigator.Result.STUCK && !canDig) && !victimInLight) {
            // No way it can walk, open or dig (not allowed to dig, or bedrock, or something else it can't
            // break): it goes through the wall, the old way.
            this.seepTicks = SEEP_TICKS;
            this.seepIn = false;
            blocked = !this.driftTowards(target, speed, safe, true);
        }
        if (result == Navigator.Result.OUT_OF_REACH) {
            if (this.stareTicks % 20 == 0 && !this.hasLineOfSight(victim)) {
                // Right under you it can't see you, and you can't see it. It wants you to: it goes to stand
                // down there in front of where you are looking, far enough out to be seen past your edge.
                double height = victim.getY() - this.getY();
                this.stareSpot = SpotFinder.find(level, victim, 3.0D, 6.0D + height * 1.2D, 0.0F, 70.0F, (int) height + 6, 32,
                        at -> at.y < victim.getY() - 2.0D && SpotFinder.isDark(level, at, safe)
                                && Sight.clearLine(level, victim, at.add(0.0D, 1.5D, 0.0D)));
                this.stareFrom = victim.position();
            }
            this.stareUp(level, victim, safe);
        } else {
            this.stareTicks = 0;
        }
        return blocked;
    }

    /**
     * You are up in the open air where it can't climb (a platform, a sky base). It stands below you with its head
     * tipped back and looks up at you, and once you have looked away it is up there with you, somewhere dark behind
     * your back.
     */
    private void stareUp(ServerLevel level, ServerPlayer victim, int safe) {
        this.setDeltaMovement(Vec3.ZERO);
        this.setCrawling(!this.hasRoomToStand(this.position()));
        this.face(victim.position());
        if (++this.stareTicks < STARE_TICKS || Sight.canSee(level, victim, this.position().add(0.0D, 2.5D, 0.0D), 0.5D)) {
            return;
        }
        Vec3 spot = SpotFinder.find(level, victim, 1.5D, 4.0D, 150.0F, 30.0F, 3, 20,
                at -> SpotFinder.isDark(level, at, safe) && Sight.clearLine(level, victim, at.add(0.0D, 1.0D, 0.0D))
                        && !Sight.canSee(level, victim, at.add(0.0D, 1.6D, 0.0D), 0.3D));
        if (spot != null) {
            this.blinkTo(spot);
            // It doesn't take you at once. It stands there behind you, breathing, and lets you turn round.
            this.lurkTicks = LURK_TICKS;
            this.setCrawling(false);
            this.face(victim.position());
            this.playSoundAt(ModSounds.BREATH.get(), 1.0F);
        }
    }

    /** Far enough to look straight up at you, or straight down. */
    @Override
    public int getMaxHeadXRot() {
        return 85;
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

    public boolean isDecoy() {
        return this.decoy;
    }

    public void makeDecoy() {
        this.decoy = true;
    }

    /** Switches to {@code phase} for exactly {@code ticks}: in a nightmare it doesn't wander off on its own. */
    public void holdPhase(Phase phase, int ticks) {
        this.startPhase(phase);
        this.phaseTicks = ticks;
    }

    /** Puts a countdown on the victim's screen. Failing it means being touched. */
    public void startChallenge(ServerPlayer victim, Challenge type, int ticks) {
        this.challenge = type;
        this.challengeTicks = ticks;
        this.challengeGrace = 25;
        this.victimLastPos = victim.position();
        HorrorNetwork.send(victim, HorrorPacket.Type.CHALLENGE, new BlockPos(ticks, HorrorConfig.SAFE_LIGHT_LEVEL.get(), 0), type.ordinal());
    }

    /** Moves on to another scene without leaving, e.g. from the village to you. */
    public void switchScene(ServerLevel level, ServerPlayer victim, Scene scene, SceneRunner runner) {
        this.entityData.set(DATA_SCENE, scene.ordinal());
        this.sceneRunner = runner;
        this.sceneAge = 0;
        this.phaseTicks = 1800;
        this.setAnim(0);
        runner.start(this, level, victim);
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
            case LEAVE_WATER -> {
                if (!victim.isInWater()) {
                    survived = true;
                } else if (this.challengeTicks <= 0) {
                    survived = false;
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
            ItemEntity item = new ItemEntity(level, this.getX(), this.getY() + 0.2D, this.getZ(), scraps(victim));
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

    /** What it leaves behind: one scrap, or two for whoever carries the doll. */
    private static ItemStack scraps(ServerPlayer victim) {
        return new ItemStack(ModItems.SHADOW_SCRAP.get(), Doll.carries(victim) ? 2 : 1);
    }

    public void rememberSmothered(BlockPos pos) {
        this.smothered.add(pos.immutable());
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.level() instanceof ServerLevel level) {
            this.burrow.cancel(level);
        }
        if (this.level() instanceof ServerLevel level && !this.smothered.isEmpty()) {
            // Gone. What it put out can be lit again now, but nothing comes back on by itself.
            LightSuppression.unlock(level, this.smothered);
            this.smothered.clear();
        }
        super.remove(reason);
    }

    public void touch(ServerPlayer victim) {
        if (Nightmares.isDream(this.level())) {
            // In a dream its touch doesn't kill. It wakes you.
            this.discard();
            Nightmares.end(victim, false);
            return;
        }
        if (this.decoy) {
            // Nothing there. A cold draught, and it is gone.
            this.playSoundAt(ModSounds.VANISH.get(), 1.0F);
            this.discard();
            return;
        }
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
        if (this.getScene() == Scene.UNDERWATER) {
            // It takes you down with it.
            victim.setDeltaMovement(0.0D, -1.4D, 0.0D);
            victim.hurtMarked = true;
        }
        HauntManager.award(victim, "touched");
        this.leave(HauntManager.Exit.TOUCHED);
    }

    // -------------------------------------------------------------- revealed

    /** Called when the Lantern of Revelation catches it. */
    public void reveal(ServerPlayer catcher) {
        if (!(this.level() instanceof ServerLevel level) || this.getPhase() == Phase.REVEALED) {
            return;
        }
        if (this.decoy) {
            // Caught nothing: an empty copy, laughing as it comes apart.
            this.playSoundAt(ModSounds.WAIL.get(), 1.2F);
            level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.6D, this.getZ(), 40, 0.4D, 1.0D, 0.4D, 0.05D);
            catcher.displayClientMessage(Component.translatable("cornerofeye.message.decoy").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            this.discard();
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
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.6D, this.getZ(), 240, 0.6D, 1.6D, 0.6D, 0.12D);
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.6D, this.getZ(), 80, 0.6D, 1.6D, 0.6D, 0.08D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 2.5D, this.getZ(), 40, 0.3D, 2.0D, 0.3D, 0.1D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BANISH.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
        // Lightning without fire, and a flash that everyone close by sees.
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(this.getX(), this.getY(), this.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        for (ServerPlayer player : level.players()) {
            double dist = player.distanceTo(this);
            if (dist < 64.0D) {
                HorrorNetwork.send(player, HorrorPacket.Type.FLASH, (int) (100 * (1.0D - dist / 64.0D)));
            }
        }
    }

    /** The lantern flash didn't catch it, but it doesn't like light this bright. */
    public void scareAway() {
        this.playSoundAt(ModSounds.VANISH.get(), 1.0F);
        this.leave(HauntManager.Exit.SCARED);
    }

    // ------------------------------------------------------------- movement

    /**
     * Straight at {@code target}, refusing to step into light. Where digging is allowed it never passes through
     * a wall: it is simply somewhere else, unless it {@code seeps}, which is for walls nothing can get through
     * any other way. @return false if light is in the way
     */
    private boolean driftTowards(Vec3 target, double speed, int safeLight, boolean seep) {
        Vec3 delta = target.subtract(this.position());
        double length = delta.length();
        if (length < 0.05D) {
            this.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        Vec3 step = delta.scale(Math.min(speed, length) / length);
        Vec3 next = this.position().add(step);
        if (this.isLitAt(next, safeLight)) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setCrawling(false);
            return false;
        }
        if (!seep && this.level() instanceof ServerLevel level && Burrow.allowed(level, this) && Navigator.blocksBody(level, next)) {
            this.blinkTo(target);
            return true;
        }
        this.setDeltaMovement(step);
        this.setCrawling(true);
        float yaw = yawTowards(this.position(), target);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        return true;
    }

    private boolean isLitAt(Vec3 at, int safeLight) {
        return this.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(at)) >= safeLight
                || this.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(at.add(0.0D, 2.0D, 0.0D))) >= safeLight;
    }

    /**
     * One step along its own path (Navigator): crawling where it must, and, once it has torn its way through
     * something, getting up wherever there is room and walking. @return false if light is in the way
     */
    boolean walkStep(Vec3 waypoint, double speed) {
        if (this.riseTicks > 0) {
            // Unfolding to its full height, slowly, before it takes a step.
            this.riseTicks--;
            this.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        if (this.isSqueezed()) {
            // Dragging itself through a tunnel is slow.
            speed *= 0.45D;
        } else if (this.upright || this.goingToDoor) {
            // Upright it doesn't hurry.
            speed *= 0.45D;
        }
        Vec3 delta = waypoint.subtract(this.position());
        double length = delta.length();
        if (length < 1.0E-3D) {
            this.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        Vec3 step = delta.scale(Math.min(speed, length) / length);
        Vec3 next = this.position().add(step);
        if (this.level().getBrightness(LightLayer.BLOCK, BlockPos.containing(next.add(0.0D, 0.2D, 0.0D))) >= HorrorConfig.SAFE_LIGHT_LEVEL.get()) {
            this.setDeltaMovement(Vec3.ZERO);
            return false;
        }
        this.setDeltaMovement(step);
        // Only once its hips are out of the hole: the whole of it has to fit where it stands up.
        boolean crawl = !((this.upright || this.goingToDoor) && this.hasRoomToStand(this.position()) && this.hasRoomToStand(next));
        if (!crawl && this.isCrawling()) {
            this.riseTicks = 34;
            this.setDeltaMovement(Vec3.ZERO);
            this.setCrawling(false);
            this.playSoundAt(ModSounds.BREATH.get(), 0.9F);
            return true;
        }
        this.setCrawling(crawl);
        if (step.horizontalDistanceSqr() > 1.0E-6D) {
            float yaw = yawTowards(this.position(), this.position().add(step));
            this.setYRot(yaw);
            this.setYBodyRot(yaw);
        }
        return true;
    }

    private void blinkTo(Vec3 spot) {
        this.teleportTo(spot.x, spot.y, spot.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.relocateCooldown = 30;
        this.gazeTicks = 0;
        this.seepTicks = 0;
        this.stareTicks = 0;
        this.stareSpot = null;
        this.lurkTicks = 0;
        this.navigator.reset();
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
        Lights.smotherOne(level, victim.blockPosition(), 14, 6, this);
    }

    private void ambientSounds(ServerPlayer victim, double range) {
        if (--this.soundCooldown > 0 || this.distanceTo(victim) > range) {
            return;
        }
        boolean breath = this.random.nextBoolean();
        HorrorNetwork.playSoundTo(victim, breath ? ModSounds.BREATH.get() : ModSounds.SKITTER.get(), SoundSource.HOSTILE,
                this.getEyePosition(), breath ? 0.7F : 0.5F, (0.8F + this.random.nextFloat() * 0.25F) * this.voicePitch());
        this.soundCooldown = 70 + this.random.nextInt(80);
    }

    public void playSoundAt(SoundEvent sound, float volume) {
        this.level().playSound(null, this.getX(), this.getY() + 1.5D, this.getZ(), sound, SoundSource.HOSTILE, volume,
                (0.85F + this.random.nextFloat() * 0.2F) * this.voicePitch());
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
        if (this.level() instanceof ServerLevel level && !this.decoy) {
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
