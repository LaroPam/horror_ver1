package com.laropam.cornerofeye.entity;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.Sight;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

/**
 * How it gets through walls: it doesn't pass through them, it tears its way through, one block at a time.
 * Whatever is in front of it, above it when it has to climb, or below it when it has to go down, gets
 * clawed at until it gives. From the other side you hear the scraping, see the cracks spread, and the
 * claws come through first.
 */
public final class Burrow {
    public static final int NONE = 0;
    public static final int FORWARD = 1;
    public static final int UP = 2;
    public static final int DOWN = 3;
    /** Just broke through: it stops in the hole and looks at you. */
    public static final int THROUGH = 4;
    /** Before each block it goes still with its face pressed to it, listening, then starts. */
    public static final int LISTEN = 22;
    /** How far ahead of its hips its claws reach when it crawls (times its size), where it stops in front of a block. */
    public static final float REACH = 2.9F;
    /** Where its head is, ahead of its hips, when it lies flat. */
    private static final double HEAD = 1.15D;

    private static final int STROKE = 7;

    private final TheOneEntity one;
    @Nullable
    private BlockPos digPos;
    @Nullable
    private BlockState digState;
    private int digTicks;
    private int digLength;
    /** Ticks it spends listening before this block: only before the first block of a run. */
    private int listen;
    private long lastBroke = -1000L;
    private int pause;
    private int sequence;

    Burrow(TheOneEntity one) {
        this.one = one;
    }

    /** Whether it may tear through blocks here at all (config, the mobGriefing rule). */
    static boolean allowed(ServerLevel level, TheOneEntity one) {
        return HorrorConfig.ALLOW_BLOCK_CHANGES.get() && HorrorConfig.DIG_THROUGH_WALLS.get() && ForgeEventFactory.getMobGriefingEvent(level, one);
    }

    boolean isBusy() {
        return this.digPos != null || this.pause > 0;
    }

    /**
     * Called before each step towards {@code next}. Digs when something is in the way.
     *
     * @return true if it is free to make the step this tick, false while it is digging or staring
     */
    boolean clear(ServerLevel level, Vec3 next) {
        if (this.pause > 0) {
            if (--this.pause == 0) {
                this.one.setDig(NONE, 0, 0, false);
            }
            return false;
        }
        if (this.digPos != null) {
            this.tickDig(level);
            return false;
        }
        Obstacle obstacle = this.findObstacle(level, next);
        if (obstacle == null) {
            return true;
        }
        this.start(level, obstacle.cell(), obstacle.direction());
        return false;
    }

    /** Whether a step to {@code next} would run into something solid (for moves that never dig). */
    boolean wouldHit(ServerLevel level, Vec3 next) {
        return this.findObstacle(level, next) != null;
    }

    /** Stops whatever it was doing to a block: the cracks heal. */
    void cancel(ServerLevel level) {
        if (this.digPos != null) {
            level.destroyBlockProgress(this.one.getId(), this.digPos, -1);
        }
        this.digPos = null;
        this.digState = null;
        if (this.pause > 0 || this.one.getDigDirection() != NONE) {
            this.one.setDig(NONE, 0, 0, false);
        }
        this.pause = 0;
    }

    /**
     * The first solid block in its way on the way to {@code next}: where its outstretched claws reach, over
     * its head when it is climbing, in front of and below its head when it is going down, or around its body.
     * Blocks it is already inside (spawned in a wall) don't count, so it can never get stuck in one.
     */
    private record Obstacle(BlockPos cell, int direction) {
    }

    @Nullable
    private Obstacle findObstacle(ServerLevel level, Vec3 next) {
        Vec3 pos = this.one.position();
        Vec3 move = next.subtract(pos);
        Vec3 flat = new Vec3(move.x, 0.0D, move.z);
        Vec3 ahead = flat.lengthSqr() > 1.0E-6D ? flat.normalize() : Vec3.directionFromRotation(0.0F, this.one.getYRot());
        // Along the way it is going, slope included: a tunnel up or down a hill follows the slope.
        double steep = flat.lengthSqr() > 1.0E-6D ? move.y / flat.length() : 0.0D;
        Vec3 along = steep > 2.0D || steep < -2.0D ? ahead : new Vec3(ahead.x, steep, ahead.z).normalize();
        double scale = this.one.getVisualScale();
        BlockPos insideLow = BlockPos.containing(pos.add(0.0D, 0.1D, 0.0D));
        BlockPos insideHigh = BlockPos.containing(pos.add(0.0D, 0.6D, 0.0D));
        Vec3 hips = next.add(0.0D, 0.45D, 0.0D);
        Vec3[] probes = {
                hips.add(along.scale(REACH * scale)),
                hips.add(along.scale(HEAD * scale)),
                // Straight up or down a shaft.
                steep > 2.0D ? hips.add(ahead.scale(0.4D)).add(0.0D, 1.0D, 0.0D) : null,
                steep < -2.0D ? hips.add(ahead.scale(0.4D)).add(0.0D, -1.0D, 0.0D) : null,
                hips,
        };
        for (Vec3 probe : probes) {
            if (probe == null) {
                continue;
            }
            BlockPos cell = BlockPos.containing(probe);
            if (cell.equals(insideLow) || cell.equals(insideHigh)) {
                continue;
            }
            BlockState state = level.getBlockState(cell);
            if (!state.getCollisionShape(level, cell).isEmpty() && state.getDestroySpeed(level, cell) >= 0.0F
                    && ForgeEventFactory.onEntityDestroyBlock(this.one, cell, state)) {
                // Right over or under it: a shaft. Anywhere ahead, even up or down a slope: in front of its face.
                Vec3 to = Vec3.atCenterOf(cell).subtract(hips);
                boolean shaft = to.horizontalDistance() < 1.2D;
                int direction = shaft && to.y > 0.6D ? UP : shaft && to.y < -0.6D ? DOWN : FORWARD;
                return new Obstacle(cell, direction);
            }
        }
        return null;
    }

    private void start(ServerLevel level, BlockPos cell, int direction) {
        BlockState state = level.getBlockState(cell);
        float hardness = state.getDestroySpeed(level, cell);
        double ticks = 30.0D + hardness * 16.0D;
        if (this.one.getStage() >= HauntData.HUNTING) {
            ticks *= 0.8D;
        }
        if (HauntData.get(level.getServer()).isBloodMoon()) {
            ticks *= 0.8D;
        }
        // Once it is in the rock it doesn't stop to listen, and it goes faster.
        boolean continuing = level.getGameTime() - this.lastBroke < 60L;
        if (continuing) {
            ticks *= 0.3D;
        }
        this.listen = continuing ? 0 : LISTEN;
        this.digPos = cell.immutable();
        this.digState = state;
        this.digTicks = 0;
        this.digLength = this.listen + (int) Mth.clamp(ticks, 10.0D, 200.0D);

        if (direction == FORWARD) {
            this.one.face(Vec3.atCenterOf(cell));
        }
        // Down on its belly for it, whatever it was doing.
        this.one.setCrawling(true);
        this.one.setDig(direction, ++this.sequence, this.digLength, this.listen > 0);
    }

    private void tickDig(ServerLevel level) {
        BlockPos cell = this.digPos;
        BlockState state = level.getBlockState(cell);
        if (state != this.digState) {
            // Someone else got there first, or it changed: look again next tick.
            level.destroyBlockProgress(this.one.getId(), cell, -1);
            this.digPos = null;
            this.one.setDig(NONE, 0, 0, false);
            return;
        }
        this.digTicks++;
        Vec3 face = Vec3.atCenterOf(cell);
        if (this.digTicks <= this.listen) {
            if (this.digTicks == this.listen / 2) {
                level.playSound(null, this.one.getX(), this.one.getEyeY(), this.one.getZ(), ModSounds.BREATH.get(), SoundSource.HOSTILE, 0.6F, 0.5F);
            }
            return;
        }
        if ((this.digTicks - this.listen) % STROKE == 1) {
            // A stroke of the claws: grit, scraping, the block's own sound, deep and slow.
            level.playSound(null, face.x, face.y, face.z, ModSounds.CLAW_SCRAPE.get(), SoundSource.HOSTILE, 1.0F,
                    0.8F + level.random.nextFloat() * 0.35F);
            level.playSound(null, face.x, face.y, face.z, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 1.0F, 0.5F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), face.x, face.y, face.z, 8, 0.3D, 0.3D, 0.3D, 0.05D);
        }
        level.destroyBlockProgress(this.one.getId(), cell, Math.min(9, (this.digTicks - this.listen) * 10 / (this.digLength - this.listen)));
        if (this.digTicks < this.digLength) {
            return;
        }
        level.destroyBlockProgress(this.one.getId(), cell, -1);
        level.destroyBlock(cell, HorrorConfig.DUG_BLOCKS_DROP.get(), this.one);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), face.x, face.y, face.z, 30, 0.4D, 0.4D, 0.4D, 0.15D);
        this.digPos = null;
        this.digState = null;
        this.lastBroke = level.getGameTime();
        this.one.brokeThrough();
        // Through. If you can see the hole, it stops in it for a moment and looks at you.
        ServerPlayer watcher = null;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(face) < 24.0D * 24.0D && Sight.clearLine(level, player, face)) {
                watcher = player;
                break;
            }
        }
        if (watcher != null) {
            this.pause = 22;
            this.one.setDig(THROUGH, ++this.sequence, this.pause, false);
            level.playSound(null, this.one.getX(), this.one.getEyeY(), this.one.getZ(), ModSounds.BREATH.get(), SoundSource.HOSTILE, 1.2F, 0.6F);
        } else {
            this.one.setDig(NONE, 0, 0, false);
        }
    }
}
