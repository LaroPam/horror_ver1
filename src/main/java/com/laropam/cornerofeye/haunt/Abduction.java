package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.block.MarkBlock;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In co-op: wander off on your own at night and it may take you. One moment you are there, the next your friends
 * hear you scream and you are gone, and from where you stood a smear of handprints and blood leads off into the
 * dark. You are in its nightmare, looking for a bed in the dark. Your friends can hear you, muffled, from the end of
 * the trail, and if one of them brings light to it, it lets go of you and you are back, there. If nobody comes, you
 * have to find your own way out.
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID)
public final class Abduction {
    /** Taken player → where the trail ends, where they can be heard and brought back. */
    private static final Map<UUID, BlockPos> TAKEN = new HashMap<>();
    private static int ticks;

    /**
     * Takes {@code victim} if they are alone, out at night in the dark, with friends somewhere in the world.
     * @param forced by a command: no friends needed, no dice
     */
    public static boolean tryTake(ServerPlayer victim, int stage, boolean forced) {
        ServerLevel level = victim.serverLevel();
        if (stage < HauntData.APPROACHING || !HorrorConfig.NIGHTMARES.get() || level.dimension() != Level.OVERWORLD
                || TAKEN.containsKey(victim.getUUID()) || victim.isPassenger() || victim.getAbilities().flying) {
            return false;
        }
        if (!forced) {
            List<ServerPlayer> others = level.players().stream().filter(other -> other != victim && !other.isSpectator()).toList();
            boolean alone = others.stream().noneMatch(other -> other.distanceTo(victim) < 40.0D);
            if (others.isEmpty() || !alone || !level.isNight() || TheOneEntity.isLit(level, victim, HorrorConfig.SAFE_LIGHT_LEVEL.get())
                    || victim.getRandom().nextFloat() > 0.3F) {
                return false;
            }
        }
        BlockPos from = victim.blockPosition();
        // Dragged off along the ground: hands clawing at it, blood.
        Direction away = Direction.Plane.HORIZONTAL.getRandomDirection(victim.getRandom());
        BlockPos end = from;
        for (int step = 1; step <= 12; step++) {
            BlockPos ground = SpotFinder.findGround(level, from.relative(away, step).relative(away.getClockWise(), victim.getRandom().nextInt(3) - 1), 3);
            if (ground == null) {
                break;
            }
            end = ground;
            if (step % 2 == 0) {
                Marks.markOn(level, ground, Direction.UP, step % 4 == 0 ? MarkBlock.HAND : MarkBlock.SMEAR);
            }
        }
        if (HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            Blood.splatter(level, end, 4, 2);
        }
        HorrorNetwork.send(victim, HorrorPacket.Type.JUMPSCARE, 10);
        TAKEN.put(victim.getUUID(), end.immutable());
        if (!Nightmares.begin(victim, stage)) {
            TAKEN.remove(victim.getUUID());
            return false;
        }
        Nightmares.wakeAt(victim, end);
        Vec3 scream = Vec3.atCenterOf(from);
        level.playSound(null, scream.x, scream.y, scream.z, ModSounds.SCREAM.get(), SoundSource.HOSTILE, 1.4F, 1.3F);
        Component lost = Component.translatable("cornerofeye.message.taken", victim.getDisplayName()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
            if (other != victim) {
                other.sendSystemMessage(lost);
                HorrorNetwork.send(other, HorrorPacket.Type.DREAD, 60);
            }
        }
        return true;
    }

    /** Whoever was taken, can still be heard from the end of the trail, and is let go if light reaches it. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TAKEN.isEmpty() || ++ticks % 20 != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        List<ServerPlayer> freed = new ArrayList<>();
        for (Iterator<Map.Entry<UUID, BlockPos>> it = TAKEN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, BlockPos> entry = it.next();
            ServerPlayer taken = server.getPlayerList().getPlayer(entry.getKey());
            if (taken == null || !Nightmares.isDream(taken.level())) {
                it.remove();
                continue;
            }
            BlockPos end = entry.getValue();
            if (overworld.isLoaded(end) && overworld.getBrightness(LightLayer.BLOCK, end.above()) >= HorrorConfig.SAFE_LIGHT_LEVEL.get()) {
                // Light. It lets go.
                freed.add(taken);
                continue;
            }
            if (ticks % 80 == 0) {
                // Muffled, from somewhere under there.
                Vec3 at = Vec3.atCenterOf(end);
                for (ServerPlayer other : overworld.players()) {
                    if (other != taken && other.distanceToSqr(at) < 96.0D * 96.0D) {
                        HorrorNetwork.playSoundTo(other, SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, at, 0.7F, 0.55F + overworld.random.nextFloat() * 0.1F);
                        HorrorNetwork.playSoundTo(other, ModSounds.PLAYER_BREATH.get(), SoundSource.PLAYERS, at, 0.8F, 0.8F);
                    }
                }
            }
        }
        for (ServerPlayer taken : freed) {
            Nightmares.end(taken, true);
            Component back = Component.translatable("cornerofeye.message.taken_back", taken.getDisplayName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(back));
        }
    }

    /** Out of the dream, one way or the other. */
    static void released(ServerPlayer player) {
        TAKEN.remove(player.getUUID());
    }

    public static boolean isTaken(ServerPlayer player) {
        return TAKEN.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TAKEN.clear();
    }

    private Abduction() {
    }
}
