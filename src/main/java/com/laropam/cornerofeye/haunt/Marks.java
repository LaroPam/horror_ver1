package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.block.MarkBlock;
import com.laropam.cornerofeye.block.WritingBlock;
import com.laropam.cornerofeye.block.WritingBlockEntity;
import com.laropam.cornerofeye.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Traces it leaves where nobody is looking: claw gouges, handprints, words on the walls. */
public final class Marks {
    public static final int MESSAGES = 16;

    record Wall(BlockPos pos, Direction facing) {
    }

    /** An empty spot in front of a solid wall around {@code center}, out of everyone's sight. */
    @Nullable
    static Wall findWall(ServerLevel level, BlockPos center, int minDist, int maxDist, int height, int attempts) {
        RandomSource random = level.random;
        List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = minDist + random.nextDouble() * (maxDist - minDist);
            BlockPos pos = center.offset((int) Math.round(Math.cos(angle) * dist), height, (int) Math.round(Math.sin(angle) * dist));
            if (!level.hasChunkAt(pos) || !isFree(level, pos)) {
                continue;
            }
            Collections.shuffle(sides, new Random(random.nextLong()));
            for (Direction side : sides) {
                BlockPos wall = pos.relative(side);
                if (level.getBlockState(wall).isFaceSturdy(level, wall, side.getOpposite()) && Sight.blockHiddenFromEveryone(level, pos)) {
                    return new Wall(pos, side.getOpposite());
                }
            }
        }
        return null;
    }

    /** Claw gouges dragged across a wall. */
    public static boolean scratches(ServerLevel level, BlockPos center) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return false;
        }
        Wall wall = findWall(level, center, 6, 16, level.random.nextInt(2), 60);
        if (wall == null) {
            return false;
        }
        place(level, wall.pos(), wall.facing(), MarkBlock.SCRATCH);
        // The gouges run on into the next block along, and the one above.
        Direction along = wall.facing().getClockWise();
        for (BlockPos next : new BlockPos[]{wall.pos().relative(along), wall.pos().above(), wall.pos().relative(along).above()}) {
            if (level.random.nextBoolean()) {
                place(level, next, wall.facing(), MarkBlock.SCRATCH);
            }
        }
        return true;
    }

    /** Handprints on the floor, every other block, coming out of the dark towards {@code target}. */
    public static boolean handprintTrail(ServerLevel level, Vec3 target) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return false;
        }
        RandomSource random = level.random;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        Vec3 dir = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        int placed = 0;
        for (double dist = 13.0D; dist >= 3.5D; dist -= 1.6D + random.nextDouble()) {
            Vec3 at = target.add(dir.scale(dist)).add(random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D);
            BlockPos ground = SpotFinder.findGround(level, BlockPos.containing(at), 3);
            if (ground != null && Sight.blockHiddenFromEveryone(level, ground)) {
                placed += place(level, ground, Direction.UP, random.nextInt(4) == 0 ? MarkBlock.SMEAR : MarkBlock.HAND) ? 1 : 0;
            }
        }
        return placed >= 2;
    }

    /** A long-fingered handprint where it pressed its palm: on a wall, a door, the outside of a window. */
    public static boolean handOn(ServerLevel level, BlockPos pos, Direction facing) {
        return HorrorConfig.ALLOW_BLOCK_CHANGES.get() && place(level, pos, facing, MarkBlock.HAND);
    }

    /** Something written in blood on a wall at eye height. */
    public static boolean writing(ServerLevel level, BlockPos center, int minDist, int maxDist) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return false;
        }
        Wall wall = null;
        for (int i = 0; i < 8 && !roomToWrite(level, wall); i++) {
            wall = findWall(level, center, minDist, maxDist, 1, 40);
        }
        if (!roomToWrite(level, wall)) {
            return false;
        }
        level.setBlock(wall.pos(), ModBlocks.WRITING.get().defaultBlockState().setValue(WritingBlock.FACING, wall.facing()), Block.UPDATE_ALL);
        if (level.getBlockEntity(wall.pos()) instanceof WritingBlockEntity writing) {
            writing.setMessage(level.random.nextInt(MESSAGES), level.random.nextLong());
            level.sendBlockUpdated(wall.pos(), level.getBlockState(wall.pos()), level.getBlockState(wall.pos()), Block.UPDATE_ALL);
        }
        // A smear of blood on the wall beside it.
        place(level, wall.pos().relative(wall.facing().getClockWise()), wall.facing(), MarkBlock.SMEAR);
        return true;
    }

    private static boolean place(ServerLevel level, BlockPos pos, Direction facing, int kind) {
        BlockState state = ModBlocks.MARK.get().defaultBlockState().setValue(MarkBlock.FACING, facing).setValue(MarkBlock.KIND, kind);
        if (!isFree(level, pos) || !state.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
        return true;
    }

    /**
     * The words run several blocks wide: the wall has to carry on to both sides, open in front, and nothing else
     * may already be written there.
     */
    private static boolean roomToWrite(ServerLevel level, @Nullable Wall wall) {
        if (wall == null) {
            return false;
        }
        Direction along = wall.facing().getClockWise();
        for (int k = -2; k <= 2; k++) {
            BlockPos spot = wall.pos().relative(along, k);
            BlockPos behind = spot.relative(wall.facing().getOpposite());
            if (!level.getBlockState(behind).isFaceSturdy(level, behind, wall.facing()) || level.getBlockState(spot).isSolidRender(level, spot)) {
                return false;
            }
        }
        for (BlockPos near : BlockPos.betweenClosed(wall.pos().offset(-5, -2, -5), wall.pos().offset(5, 2, 5))) {
            if (level.getBlockState(near).is(ModBlocks.WRITING.get())) {
                return false;
            }
        }
        return true;
    }

    /** Air, grass and the like, but never on top of something it already left. */
    private static boolean isFree(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() && level.getFluidState(pos).isEmpty()
                && !state.is(ModBlocks.MARK.get()) && !state.is(ModBlocks.WRITING.get());
    }

    private Marks() {
    }
}
