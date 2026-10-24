package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.block.BloodBlock;
import com.laropam.cornerofeye.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Splashes of blood on the floor around a spot. */
public final class Blood {
    public static void splatter(ServerLevel level, BlockPos center, int count, int radius) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return;
        }
        RandomSource random = level.random;
        int placed = 0;
        for (int attempt = 0; attempt < count * 4 && placed < count; attempt++) {
            BlockPos column = attempt == 0 ? center : center.offset(random.nextInt(radius * 2 + 1) - radius, 0, random.nextInt(radius * 2 + 1) - radius);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos pos = column.above(dy);
                BlockState state = level.getBlockState(pos);
                BlockPos below = pos.below();
                if (state.canBeReplaced() && !state.is(ModBlocks.BLOOD.get()) && level.getFluidState(pos).isEmpty()
                        && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    level.setBlock(pos, BloodBlock.random(ModBlocks.BLOOD.get(), random), Block.UPDATE_ALL);
                    placed++;
                    break;
                }
            }
        }
    }

    private Blood() {
    }
}
