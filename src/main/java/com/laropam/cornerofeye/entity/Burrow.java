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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

/**
 * What it does with its hands on the way to you (Navigator decides where): doors it opens, slowly; walls
 * it tears through, one block at a time, in front of it, above its hands when it has to climb, or below
 * them when it has to go down. From the other side you hear the scraping, see the cracks spread, and the
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
    /** A hand on a door, then opening it. */
    public static final int OPEN = 5;
    public static final int OPEN_LENGTH = 30;
    /** When, into opening, the door actually moves. */
    public static final int OPEN_AT = 16;

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
    @Nullable
    private BlockPos openPos;
    private int openTicks;

    Burrow(TheOneEntity one) {
        this.one = one;
    }

    /** Whether it may tear through blocks here at all (config, the mobGriefing rule). */
    static boolean allowed(ServerLevel level, TheOneEntity one) {
        return HorrorConfig.ALLOW_BLOCK_CHANGES.get() && HorrorConfig.DIG_THROUGH_WALLS.get() && ForgeEventFactory.getMobGriefingEvent(level, one);
    }

    boolean isBusy() {
        return this.digPos != null || this.openPos != null || this.pause > 0;
    }

    /** Keeps working on what it started. Call while {@link #isBusy()}. */
    void tick(ServerLevel level) {
        if (this.pause > 0) {
            if (--this.pause == 0) {
                this.one.setDig(NONE, 0, 0, false);
            }
            return;
        }
        if (this.openPos != null) {
            this.tickOpen(level);
            return;
        }
        if (this.digPos != null) {
            this.tickDig(level);
        }
    }

    /** Starts tearing at {@code cell}. */
    void dig(ServerLevel level, BlockPos cell, int direction) {
        if (!this.isBusy()) {
            this.start(level, cell, direction);
        }
    }

    /**
     * Opens the door, gate or trapdoor at {@code pos}: a hand on it first, still, as if listening, and then
     * slowly, with a long creak. @return true if it started
     */
    boolean open(ServerLevel level, BlockPos pos) {
        if (this.isBusy()) {
            return false;
        }
        this.openPos = pos.immutable();
        this.openTicks = 0;
        this.one.face(Vec3.atCenterOf(pos));
        this.one.setDig(OPEN, ++this.sequence, OPEN_LENGTH, false);
        return true;
    }

    private void tickOpen(ServerLevel level) {
        BlockPos pos = this.openPos;
        this.openTicks++;
        if (this.openTicks == OPEN_AT) {
            BlockState state = level.getBlockState(pos);
            if (Navigator.isOpenable(state) && !state.getValue(BlockStateProperties.OPEN)) {
                setOpen(level, pos, state, true);
            }
        }
        if (this.openTicks >= OPEN_LENGTH) {
            this.openPos = null;
            this.one.setDig(NONE, 0, 0, false);
        }
    }

    /** Opens or shuts it the way a hand would, only slower and lower. */
    static void setOpen(ServerLevel level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        SoundEvent sound;
        if (state.getBlock() instanceof DoorBlock door) {
            sound = open ? door.type().doorOpen() : door.type().doorClose();
        } else if (state.getBlock() instanceof TrapDoorBlock) {
            sound = open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE;
        } else {
            sound = open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE;
        }
        level.playSound(null, pos, sound, SoundSource.BLOCKS, open ? 1.0F : 0.5F, open ? 0.45F : 0.6F);
        level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
    }

    /** Stops whatever it was doing to a block: the cracks heal. */
    void cancel(ServerLevel level) {
        if (this.digPos != null) {
            level.destroyBlockProgress(this.one.getId(), this.digPos, -1);
        }
        this.digPos = null;
        this.digState = null;
        this.openPos = null;
        if (this.pause > 0 || this.one.getDigDirection() != NONE) {
            this.one.setDig(NONE, 0, 0, false);
        }
        this.pause = 0;
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
