package com.laropam.dontturnaround.haunt;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;

/**
 * Everything an event needs to know about the player it is about to scare.
 *
 * @param forced run by a command: skip the "is this a good moment" checks
 */
public record HauntContext(ServerPlayer player, ServerLevel level, int stage, long hauntTicks,
                           boolean night, boolean underground, boolean dark, boolean forced,
                           HauntManager.PlayerState state) {

    public static HauntContext of(ServerPlayer player, HauntData data, HauntManager.PlayerState state, boolean forced) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = BlockPos.containing(player.getEyePosition());
        boolean underground = level.getBrightness(LightLayer.SKY, pos) < 4 && player.getY() < level.getSeaLevel();
        boolean dark = level.getRawBrightness(pos, level.getSkyDarken()) < 7;
        return new HauntContext(player, level, data.getStage(), data.getTicks(), level.isNight(), underground, dark, forced, state);
    }

    public RandomSource random() {
        return this.player.getRandom();
    }
}
