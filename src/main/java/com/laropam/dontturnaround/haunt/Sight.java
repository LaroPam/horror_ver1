package com.laropam.dontturnaround.haunt;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Helpers for "is the player looking at this?" checks. */
public final class Sight {
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

    /** True if the point is roughly on the player's screen and not hidden behind blocks. */
    public static boolean canSee(Level level, Player player, Vec3 point, double minDot) {
        return gazeDot(player, point) > minDot && clearLine(level, player, point);
    }

    /** True if the point is off-screen or blocked from view. */
    public static boolean isHidden(Level level, Player player, Vec3 point) {
        return gazeDot(player, point) < 0.25D || !clearLine(level, player, point);
    }

    private Sight() {
    }
}
