package com.laropam.cornerofeye.haunt;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** "Is anyone looking at this?" checks. */
public final class Sight {
    /** Roughly the part of the screen a player is paying attention to. */
    public static final double ON_SCREEN = 0.55D;

    /** Cosine of the angle between where the player looks and the point: 1 = dead center, below 0 = behind. */
    public static double gazeDot(Player player, Vec3 point) {
        Vec3 to = point.subtract(player.getEyePosition());
        if (to.lengthSqr() < 1.0E-6D) {
            return 1.0D;
        }
        return player.getViewVector(1.0F).dot(to.normalize());
    }

    /** True if no block is in the way between the player's eyes and the point. */
    public static boolean clearLine(Level level, Player player, Vec3 point) {
        return level.clip(new ClipContext(player.getEyePosition(), point, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    /** Like {@link #clearLine} but hitting the target block itself still counts as seeing it. */
    public static boolean clearLineToBlock(Level level, Player player, BlockPos target) {
        BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(), Vec3.atCenterOf(target),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target);
    }

    public static boolean canSee(Level level, Player player, Vec3 point, double minDot) {
        return gazeDot(player, point) > minDot && clearLine(level, player, point);
    }

    /** True if no player nearby has this block on screen. Lights only ever go out when this is true. */
    public static boolean blockHiddenFromEveryone(Level level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        for (Player player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(center) > 80.0D * 80.0D) {
                continue;
            }
            if (gazeDot(player, center) > ON_SCREEN && clearLineToBlock(level, player, pos)) {
                return false;
            }
        }
        return true;
    }

    /** True if no player nearby has the point on screen with a clear line to it. */
    public static boolean hiddenFromEveryone(Level level, Vec3 point) {
        for (Player player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(point) < 80.0D * 80.0D && canSee(level, player, point, ON_SCREEN)) {
                return false;
            }
        }
        return true;
    }

    private Sight() {
    }
}
