package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.mixin.AbstractFurnaceBlockEntityAccessor;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Putting out lights. Torches burn out for good and have to be lit again with flint and steel.
 * While it hunts it puts out anything that glows: furnaces stop dead, glowstone and lanterns go dark,
 * and none of it comes back on by itself.
 */
public final class Lights {
    public static boolean isTorch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
    }

    /** What can go out even when it isn't around: things with a flame. */
    public static boolean isSnuffable(BlockState state) {
        if (isTorch(state) || state.is(Blocks.JACK_O_LANTERN)) {
            return true;
        }
        boolean flame = state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.CANDLES) || state.is(BlockTags.CANDLE_CAKES);
        return flame && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    /** Anything that gives off light and isn't already out: what it puts out while hunting. */
    public static boolean isLightSource(ServerLevel level, BlockPos pos, BlockState state) {
        if (isSnuffable(state)) {
            return true;
        }
        if (!state.getFluidState().isEmpty() || state.getBlock() instanceof BaseFireBlock || state.getLightEmission(level, pos) <= 0) {
            return false;
        }
        return !LightSuppression.isSmothered(level, pos);
    }

    /**
     * Lights around {@code center}, closest first.
     *
     * @param anything every glowing block (it is hunting), or only flames (it isn't here)
     */
    public static List<BlockPos> findAll(ServerLevel level, BlockPos center, int radius, int height, boolean anything) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -height, -radius), center.offset(radius, height, radius))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (anything ? isLightSource(level, pos, state) : isSnuffable(state)) {
                found.add(pos.immutable());
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return found;
    }

    /** Like {@link #findAll}, minus anything a player is looking at. */
    public static List<BlockPos> findHidden(ServerLevel level, BlockPos center, int radius, int height, boolean anything) {
        List<BlockPos> found = findAll(level, center, radius, height, anything);
        found.removeIf(pos -> !Sight.blockHiddenFromEveryone(level, pos));
        return found;
    }

    /** A flame goes out on its own. Torches burn out; campfires and candles can simply be lit again. */
    public static void snuff(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isTorch(state)) {
            burnOutTorch(level, pos, state);
        } else if (state.is(Blocks.JACK_O_LANTERN)) {
            level.setBlock(pos, Blocks.CARVED_PUMPKIN.defaultBlockState()
                    .setValue(CarvedPumpkinBlock.FACING, state.getValue(CarvedPumpkinBlock.FACING)), Block.UPDATE_ALL);
        } else if (state.hasProperty(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        } else {
            return;
        }
        effects(level, pos);
    }

    /**
     * It puts out whatever is there, and nobody can light it again until it leaves. After that it has to be
     * lit by hand: nothing comes back on by itself.
     */
    public static void smother(ServerLevel level, BlockPos pos, @Nullable TheOneEntity by) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (isSnuffable(state)) {
            snuff(level, pos);
        } else if (state.getBlock() instanceof AbstractFurnaceBlock && state.getValue(AbstractFurnaceBlock.LIT)) {
            // The fire dies with the fuel it was burning, and the furnace won't start again until relit.
            if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace) {
                ((AbstractFurnaceBlockEntityAccessor) furnace).cornerofeye$setLitTime(0);
                furnace.setChanged();
            }
            level.setBlock(pos, state.setValue(AbstractFurnaceBlock.LIT, false), Block.UPDATE_ALL);
            LightSuppression.freeze(level, pos, state.getBlock());
            effects(level, pos);
        } else if (isLightSource(level, pos, state)) {
            LightSuppression.darken(level, pos, state.getBlock());
            effects(level, pos);
        } else {
            return;
        }
        LightSuppression.lock(level, pos);
        if (by != null) {
            by.rememberSmothered(pos);
        }
    }

    /** Puts out the closest glowing thing around {@code center} that nobody is watching. */
    public static boolean smotherOne(ServerLevel level, BlockPos center, int radius, int height, TheOneEntity by) {
        for (BlockPos pos : findAll(level, center, radius, height, true)) {
            if (Sight.blockHiddenFromEveryone(level, pos)) {
                smother(level, pos, by);
                return true;
            }
        }
        return false;
    }

    private static void burnOutTorch(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState burnt = state.getBlock() instanceof WallTorchBlock
                ? ModBlocks.UNLIT_WALL_TORCH.get().defaultBlockState().setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING))
                : ModBlocks.UNLIT_TORCH.get().defaultBlockState();
        level.setBlock(pos, burnt, Block.UPDATE_ALL);
    }

    private static void effects(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.35F, 1.7F + level.random.nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D, 8, 0.08D, 0.12D, 0.08D, 0.01D);
        // A light going out next to you makes your heart jump.
        for (ServerPlayer player : level.players()) {
            double dist = Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(pos)));
            if (dist < 16.0D) {
                HorrorNetwork.send(player, HorrorPacket.Type.DREAD, (int) (35 * (1.0D - dist / 16.0D)));
            }
        }
    }

    private Lights() {
    }
}
