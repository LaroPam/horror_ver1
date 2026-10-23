package com.laropam.dontturnaround.haunt;

import com.laropam.dontturnaround.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/** Finds places around a player where something tall could be standing. */
public final class SpawnFinder {
    /**
     * @param yawOffset  0 = in front of the player, 180 = behind
     * @param yawSpread  random spread around {@code yawOffset}, in degrees
     * @param yRange     how far up/down from the player's feet to search for ground
     */
    @Nullable
    public static Vec3 find(ServerLevel level, ServerPlayer player, double minDist, double maxDist,
                            float yawOffset, float yawSpread, int yRange, int attempts, Predicate<Vec3> filter) {
        RandomSource random = player.getRandom();
        for (int i = 0; i < attempts; i++) {
            double yaw = Math.toRadians(player.getYRot() + yawOffset + (random.nextFloat() * 2.0F - 1.0F) * yawSpread);
            double dist = minDist + random.nextDouble() * (maxDist - minDist);
            double x = player.getX() - Math.sin(yaw) * dist;
            double z = player.getZ() + Math.cos(yaw) * dist;
            BlockPos ground = findGround(level, BlockPos.containing(x, player.getY(), z), yRange);
            if (ground == null) {
                continue;
            }
            Vec3 spot = Vec3.atBottomCenterOf(ground);
            if (filter.test(spot)) {
                return spot;
            }
        }
        return null;
    }

    /** Searches up and down from {@code start}, closest levels first. */
    @Nullable
    public static BlockPos findGround(ServerLevel level, BlockPos start, int yRange) {
        for (int i = 0; i <= yRange * 2; i++) {
            int dy = (i % 2 == 0) ? i / 2 : -(i + 1) / 2;
            BlockPos pos = start.above(dy);
            if (isStandable(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    public static boolean isStandable(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }
        return level.noCollision(ModEntities.FACELESS.get().getAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
    }

    private SpawnFinder() {
    }
}
