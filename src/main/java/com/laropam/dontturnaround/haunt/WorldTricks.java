package com.laropam.dontturnaround.haunt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Things it does to the world while you are not looking. */
public final class WorldTricks {
    /** Number of lines in each sign message; text lives in the lang files as dontturnaround.sign.N.L */
    private static final int[] SIGN_LINES = {1, 3, 2, 3, 1, 2, 3, 2, 2, 3};
    public static final int SIGN_WATCHED_YOU_SLEEP = 7;

    public static List<BlockPos> findBlocks(ServerLevel level, BlockPos center, int radius, int height, Predicate<BlockState> test) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -height, -radius), center.offset(radius, height, radius))) {
            if (level.hasChunkAt(pos) && test.test(level.getBlockState(pos))) {
                found.add(pos.immutable());
            }
        }
        return found;
    }

    public static boolean isTorch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
    }

    public static void snuffTorch(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F + level.random.nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D, 6, 0.08D, 0.1D, 0.08D, 0.01D);
    }

    public static int randomSignMessage(RandomSource random) {
        int index;
        do {
            index = random.nextInt(SIGN_LINES.length);
        } while (index == SIGN_WATCHED_YOU_SLEEP);
        return index;
    }

    /** Places a waxed sign that faces someone looking from {@code facingYaw}. */
    public static boolean placeSign(ServerLevel level, BlockPos pos, float viewerYaw, int message) {
        int rotation = RotationSegment.convertToSegment(viewerYaw + 180.0F);
        level.setBlock(pos, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return false;
        }
        int lines = SIGN_LINES[message];
        int first = (4 - lines) / 2;
        SignText text = sign.getFrontText();
        for (int i = 0; i < lines; i++) {
            text = text.setMessage(first + i, Component.translatable("dontturnaround.sign." + message + "." + i));
        }
        sign.setText(text, true);
        sign.setWaxed(true);
        return true;
    }

    /** A thin pillar of cobblestone with a redstone torch on top. Nobody built it. */
    public static void buildPillar(ServerLevel level, BlockPos base, RandomSource random) {
        int height = 3 + random.nextInt(2);
        for (int i = 0; i < height; i++) {
            BlockState block = random.nextInt(3) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
            level.setBlock(base.above(i), block, Block.UPDATE_ALL);
        }
        level.setBlock(base.above(height), Blocks.REDSTONE_TORCH.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static boolean isDiggable(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(Blocks.GRAVEL);
    }

    /** True if a 1x2 tunnel could start at {@code start} going {@code dir}. */
    public static boolean canTunnel(ServerLevel level, BlockPos start, Direction dir) {
        BlockPos first = start.relative(dir);
        return isDiggable(level.getBlockState(first)) && isDiggable(level.getBlockState(first.above()))
                && isDiggable(level.getBlockState(first.relative(dir))) && isDiggable(level.getBlockState(first.relative(dir).above()));
    }

    /** Digs a perfectly straight 1x2 tunnel into natural stone. Returns blocks dug. */
    public static int digTunnel(ServerLevel level, BlockPos start, Direction dir, int length) {
        int dug = 0;
        BlockPos pos = start;
        for (int i = 0; i < length; i++) {
            pos = pos.relative(dir);
            BlockState lower = level.getBlockState(pos);
            BlockState upper = level.getBlockState(pos.above());
            boolean lowerOk = lower.isAir() || isDiggable(lower);
            boolean upperOk = upper.isAir() || isDiggable(upper);
            if (!lowerOk || !upperOk || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()
                    || !level.getFluidState(pos.above(2)).isEmpty()) {
                break;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            dug++;
        }
        return dug;
    }

    private WorldTricks() {
    }
}
