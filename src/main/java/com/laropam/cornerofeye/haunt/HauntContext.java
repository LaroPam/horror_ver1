package com.laropam.cornerofeye.haunt;

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
public record HauntContext(ServerPlayer player, ServerLevel level, int stage, boolean night, boolean underground,
                           boolean dark, boolean forced) {

    public static HauntContext of(ServerPlayer player, int stage, boolean forced) {
        ServerLevel level = player.serverLevel();
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        boolean underground = level.getBrightness(LightLayer.SKY, eyes) < 4 && player.getY() < level.getSeaLevel();
        boolean dark = level.getMaxLocalRawBrightness(eyes) < 7;
        return new HauntContext(player, level, stage, level.isNight(), underground, dark, forced);
    }

    public RandomSource random() {
        return this.player.getRandom();
    }
}
