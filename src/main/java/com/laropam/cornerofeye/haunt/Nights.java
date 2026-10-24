package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Nightfall: a bell, "NIGHT 3" across the screen, and sometimes the moon comes up red. On a blood moon it
 * comes more often and moves faster.
 */
public final class Nights {
    private static Boolean wasNight;

    public static boolean isNightTime(Level level) {
        long time = level.getDayTime() % 24000L;
        return time >= 12500L && time < 23200L;
    }

    /** Called every second while the haunt is going. */
    static void tick(MinecraftServer server, HauntData data, int stage) {
        boolean night = isNightTime(server.overworld());
        if (wasNight != null && night != wasNight) {
            if (night) {
                nightFalls(server, data, stage);
            } else {
                data.setBloodMoon(false);
                server.getPlayerList().getPlayers().forEach(player -> sync(player, data));
            }
        }
        wasNight = night;
    }

    private static void nightFalls(MinecraftServer server, HauntData data, int stage) {
        ServerLevel overworld = server.overworld();
        float chance = stage >= HauntData.HUNTING ? 0.35F : stage == HauntData.APPROACHING ? 0.15F : 0.0F;
        // Dawn always clears it, so if it is already set now, /cornerofeye bloodmoon on asked for tonight.
        boolean blood = data.isBloodMoon() || Finale.isDue(data) || overworld.random.nextFloat() < chance;
        data.setBloodMoon(blood);
        int night = (int) Math.min(999, data.getDays() + 1);
        boolean newNight = night != data.getLastCardNight();
        if (newNight) {
            data.setLastCardNight(night);
        }
        for (ServerPlayer player : overworld.players()) {
            sync(player, data);
            if (newNight || blood) {
                HorrorNetwork.send(player, HorrorPacket.Type.NIGHT_CARD, new BlockPos(blood ? 1 : 0, 0, 0), night);
                HorrorNetwork.playSoundTo(player, ModSounds.NIGHT_BELL.get(), SoundSource.AMBIENT, player.getEyePosition(), 1.0F,
                        blood ? 0.8F : 1.0F);
            }
        }
    }

    public static void sync(ServerPlayer player, HauntData data) {
        HorrorNetwork.send(player, HorrorPacket.Type.BLOOD_MOON, data.isBloodMoon() && !data.isBanished() ? 1 : 0);
    }

    static void reset() {
        wasNight = null;
    }

    private Nights() {
    }
}
