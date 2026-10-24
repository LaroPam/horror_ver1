package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModEntities;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The last night, and what comes after. Once it has been caught all but one time, the last catch only counts
 * under a blood moon. That night every light around the players goes out at once, it comes hunting, and it
 * brings copies of itself that are nothing at all, so a flash at the wrong one burns a scrap for nothing.
 * Catch the real one and it comes apart: what it put out burns again, and the sun comes up. After that
 * there are only echoes, and, if the server wants it, one day it comes back.
 */
public final class Finale {
    private static final int DECOYS = 2;
    private static final HauntEvent[] ECHOES = {HauntEvent.FOOTSTEPS, HauntEvent.DISTANT_SOUND, HauntEvent.WHISPER, HauntEvent.STARE};
    /** Lights put out as the last night falls, locked until dawn. */
    private static final Map<ResourceKey<Level>, List<BlockPos>> LOCKED_TONIGHT = new HashMap<>();

    /** One wound left: the next night is the last one, under a blood moon. */
    public static boolean isDue(HauntData data) {
        return !data.isBanished() && data.getWounds() >= HorrorConfig.WOUNDS_TO_BANISH.get() - 1 && data.getWounds() > 0;
    }

    /** Whether a catch right now would be the last one. */
    public static boolean isTonight(HauntData data) {
        return isDue(data) && data.isBloodMoon();
    }

    /** Nightfall on the last night: every light around everyone goes out, and a little later it comes. */
    static void beginNight(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            List<BlockPos> tonight = LOCKED_TONIGHT.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
            int delay = 0;
            for (ServerPlayer player : level.players()) {
                for (BlockPos pos : Lights.findAll(level, player.blockPosition(), 24, 8, true)) {
                    tonight.add(pos);
                    // A wave of them, going out one after another.
                    HauntManager.schedule(delay++ % 80, () -> Lights.smother(level, pos, null));
                }
            }
        }
        HauntManager.schedule(300, () -> HauntManager.huntNow(server));
    }

    /** It never comes alone on the last night. The copies are empty: a flash at one only wastes a scrap. */
    static void spawnDecoys(ServerLevel level, ServerPlayer victim, int stage) {
        int safe = HorrorConfig.SAFE_LIGHT_LEVEL.get();
        for (int i = 0; i < DECOYS; i++) {
            Vec3 spot = SpotFinder.find(level, victim, 10.0D, 22.0D, 90.0F, 90.0F, 12, 24, pos -> SpotFinder.isDark(level, pos, safe));
            TheOneEntity decoy = spot == null ? null : TheOneEntity.spawn(level, victim, spot, stage, Phase.STALK);
            if (decoy != null) {
                decoy.makeDecoy();
            }
        }
    }

    /** The real one was caught for the last time. */
    static void victory(ServerLevel level, TheOneEntity one) {
        MinecraftServer server = level.getServer();
        for (ServerLevel each : server.getAllLevels()) {
            for (TheOneEntity other : each.getEntities(ModEntities.THE_ONE.get(), other -> other != one)) {
                other.discard();
            }
        }
        HauntData data = HauntData.get(server);
        data.setBloodMoon(false);
        data.setBanishedAt(server.overworld().getGameTime());
        LightSuppression.relightAll(server);
        LOCKED_TONIGHT.clear();
        ServerLevel overworld = server.overworld();
        if (Nights.isNightTime(overworld)) {
            // The sun comes up.
            overworld.setDayTime(overworld.getDayTime() + (24000L - overworld.getDayTime() % 24000L));
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Nights.sync(player, data);
            HorrorNetwork.send(player, HorrorPacket.Type.NIGHT_CARD, new BlockPos(Nights.CARD_VICTORY, 0, 0), 0);
        }
    }

    /** Morning after a last night that didn't end it: what went out can be lit again. */
    static void dawn(MinecraftServer server) {
        for (Map.Entry<ResourceKey<Level>, List<BlockPos>> entry : LOCKED_TONIGHT.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level != null) {
                LightSuppression.unlock(level, entry.getValue());
            }
        }
        LOCKED_TONIGHT.clear();
    }

    /** Once a second while it is gone: now and then an echo of it, and maybe, one day, the real thing again. */
    static void afterwards(MinecraftServer server, HauntData data) {
        ServerLevel overworld = server.overworld();
        if (data.getBanishedAt() == 0L) {
            data.setBanishedAt(overworld.getGameTime());
        }
        int days = HorrorConfig.RETURN_AFTER_DAYS.get();
        boolean night = Nights.isNightTime(overworld);
        if (days > 0 && night && overworld.getGameTime() - data.getBanishedAt() >= days * 24000L) {
            data.revive(HorrorConfig.DAYS_UNTIL_WATCHING.get() * 24000L);
            Component message = Component.translatable("cornerofeye.message.returned").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
                HorrorNetwork.playSoundTo(player, ModSounds.DRONE.get(), SoundSource.AMBIENT, player.getEyePosition(), 0.8F, 0.6F);
            }
            return;
        }
        if (!HorrorConfig.ECHOES.get() || !night) {
            return;
        }
        for (ServerPlayer player : overworld.players()) {
            // Roughly once every half hour of night: just enough to never quite be sure.
            if (player.getRandom().nextInt(1800) == 0) {
                ECHOES[player.getRandom().nextInt(ECHOES.length)].run(HauntContext.of(player, HauntData.APPROACHING, true));
            }
        }
    }

    static void reset() {
        LOCKED_TONIGHT.clear();
    }

    private Finale() {
    }
}
