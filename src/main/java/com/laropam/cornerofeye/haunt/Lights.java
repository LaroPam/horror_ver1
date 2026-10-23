package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lights it can put out. Only things that burn: torches, lanterns, campfires, candles, jack o'lanterns.
 * Glowstone, sea lanterns, shroomlights and the like don't burn, so it can't touch them.
 */
public final class Lights {
    public static boolean isSnuffable(BlockState state) {
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN) || state.is(Blocks.JACK_O_LANTERN)) {
            return true;
        }
        boolean litFire = state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.CANDLES) || state.is(BlockTags.CANDLE_CAKES);
        return litFire && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    /** Snuffable lights around {@code center} that nobody is looking at, closest first. */
    public static List<BlockPos> findHidden(ServerLevel level, BlockPos center, int radius, int height) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -height, -radius), center.offset(radius, height, radius))) {
            if (level.hasChunkAt(pos) && isSnuffable(level.getBlockState(pos))) {
                found.add(pos.immutable());
            }
        }
        found.removeIf(pos -> !Sight.blockHiddenFromEveryone(level, pos));
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return found;
    }

    public static void snuff(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) {
            // Knocked down rather than destroyed.
            level.destroyBlock(pos, true);
        } else if (state.is(Blocks.JACK_O_LANTERN)) {
            level.setBlock(pos, Blocks.CARVED_PUMPKIN.defaultBlockState()
                    .setValue(CarvedPumpkinBlock.FACING, state.getValue(CarvedPumpkinBlock.FACING)), Block.UPDATE_ALL);
        } else if (state.hasProperty(BlockStateProperties.LIT)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
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

    /** Puts out the nearest light nobody is watching. */
    public static boolean snuffOne(ServerLevel level, BlockPos center, int radius, int height) {
        List<BlockPos> lights = findHidden(level, center, radius, height);
        if (lights.isEmpty()) {
            return false;
        }
        snuff(level, lights.get(0));
        return true;
    }

    private Lights() {
    }
}
