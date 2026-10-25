package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.Scene;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModEntities;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the haunt: decides when it appears and to whom, fires the small events in between,
 * and keeps score of how close the players are to getting rid of it.
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID)
public final class HauntManager {
    /** Why it left. Decides how long until it comes back. */
    public enum Exit {
        LOST, DAYLIGHT, TIMEOUT, APPROACHED, SCARED, SURVIVED, TOUCHED, WOUNDED
    }

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    private static final List<Task> TASKS = new ArrayList<>();
    private static int lastStage = -1;

    private static final class PlayerState {
        int syncedStage = -1;
    }

    private record Task(long runAt, Runnable action) {
    }

    private static long serverTicks;

    /** Runs {@code action} on the server thread after {@code delay} ticks. */
    public static void schedule(int delay, Runnable action) {
        TASKS.add(new Task(serverTicks + delay, action));
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        serverTicks++;
        runTasks();

        HauntData data = HauntData.get(server);
        if (data.isBanished()) {
            if (serverTicks % 20 == 0) {
                server.getPlayerList().getPlayers().forEach(player -> syncStage(player, HorrorPacket.DEFEATED));
                Finale.afterwards(server, data);
            }
            return;
        }
        data.tick();
        if (serverTicks % 20 != 0) {
            return;
        }

        int stage = data.getStage();
        Nights.tick(server, data, stage);
        announceStageChange(server, data, stage);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncStage(player, stage);
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            Journal.tick(player, data, stage);
            HomeVisit.tick(player, data, stage);
            tickEvents(player, stage);
        }
        directTheOne(server, data, stage);
    }

    private static void runTasks() {
        if (TASKS.isEmpty()) {
            return;
        }
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task task = it.next();
            if (task.runAt <= serverTicks) {
                due.add(task);
                it.remove();
            }
        }
        due.forEach(task -> task.action.run());
    }

    private static PlayerState state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new PlayerState());
    }

    private static void syncStage(ServerPlayer player, int stage) {
        PlayerState state = state(player);
        if (state.syncedStage != stage) {
            state.syncedStage = stage;
            HorrorNetwork.send(player, HorrorPacket.Type.SYNC_STAGE, stage);
        }
    }

    private static void announceStageChange(MinecraftServer server, HauntData data, int stage) {
        if (lastStage >= 0 && stage > lastStage && !data.isStageOverridden() && stage >= HauntData.APPROACHING) {
            Component message = Component.translatable("cornerofeye.message.stage." + stage)
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
                HorrorNetwork.playSoundTo(player, ModSounds.DRONE.get(), SoundSource.AMBIENT, player.getEyePosition(), 0.6F, 0.7F);
            }
        }
        lastStage = stage;
    }

    private static void tickEvents(ServerPlayer player, int stage) {
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        Director.tick(player, stage);
    }

    // ---------------------------------------------------------------- the one

    @Nullable
    public static TheOneEntity findTheOne(ServerLevel level) {
        for (TheOneEntity one : level.getEntities(ModEntities.THE_ONE.get(), entity -> entity.isAlive() && !entity.isDecoy())) {
            return one;
        }
        return null;
    }

    private static void directTheOne(MinecraftServer server, HauntData data, int stage) {
        if (stage < HauntData.WATCHING) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (data.getTicks() < data.getNextAppearance() || findTheOne(overworld) != null) {
            return;
        }
        RandomSource random = overworld.random;
        if (random.nextInt(20) != 0) {
            return;
        }
        ServerPlayer victim = pickVictim(overworld);
        if (victim == null) {
            return;
        }
        // A doll left out for it in the dark: it can't help going to that first.
        if (Moments.lureNear(victim) != null && Moments.start(victim, stage, Scene.LURED, false) != null) {
            return;
        }
        // Every so often, instead of just watching, it stages one of its moments. More often the further it gets.
        if (data.getNextMoment() < 0) {
            data.setNextMoment(data.getTicks() + momentInterval(stage, random));
        } else if (data.getTicks() >= data.getNextMoment() && Moments.startRandom(victim, stage) != null) {
            data.setNextMoment(data.getTicks() + momentInterval(stage, random));
            return;
        }
        appear(victim, stage, Phase.WATCH);
    }

    /** True once it is free to show up again after it last left. */
    static boolean appearanceDue(MinecraftServer server) {
        HauntData data = HauntData.get(server);
        return data.getTicks() >= data.getNextAppearance();
    }

    /** Ticks until the next moment: the configured average for this stage, give or take a quarter. */
    private static long momentInterval(int stage, RandomSource random) {
        int minutes = switch (stage) {
            case HauntData.WATCHING -> HorrorConfig.MINUTES_BETWEEN_MOMENTS_WATCHING.get();
            case HauntData.APPROACHING -> HorrorConfig.MINUTES_BETWEEN_MOMENTS_APPROACHING.get();
            default -> HorrorConfig.MINUTES_BETWEEN_MOMENTS_HUNTING.get();
        };
        long ticks = (long) (minutes * 1200L * (0.75D + random.nextDouble() * 0.5D));
        // Under a blood moon it doesn't wait as long, nor while someone carries the doll.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (Doll.anyoneCarries(server)) {
            ticks = ticks * 7 / 10;
        }
        return HauntData.get(server).isBloodMoon() ? ticks / 2 : ticks;
    }

    /** It prefers whoever is alone in the dark. */
    @Nullable
    private static ServerPlayer pickVictim(ServerLevel level) {
        List<ServerPlayer> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            HauntContext context = HauntContext.of(player, 0, false);
            if (!context.night() && !(context.underground() && context.dark())) {
                continue;
            }
            // Only once the build-up for them has run its course.
            if (!Director.wantsEncounter(player)) {
                continue;
            }
            boolean alone = level.getNearestPlayer(player.getX(), player.getY(), player.getZ(), 24.0D,
                    other -> other != player && !other.isSpectator()) == null;
            candidates.add(player);
            // ...and whoever carries the doll.
            weights.add(1 + (context.dark() ? 2 : 0) + (alone ? 2 : 0) + (Doll.carries(player) ? 6 : 0));
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int roll = level.random.nextInt(weights.stream().mapToInt(Integer::intValue).sum());
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return candidates.get(i);
            }
        }
        return candidates.get(0);
    }

    /** Makes it show up for {@code victim}. Used by the director and by commands. */
    @Nullable
    public static TheOneEntity appear(ServerPlayer victim, int stage, Phase phase) {
        ServerLevel level = victim.serverLevel();
        int safe = HorrorConfig.SAFE_LIGHT_LEVEL.get();
        double min = stage <= HauntData.WATCHING ? 24.0D : stage == HauntData.APPROACHING ? 16.0D : 13.0D;
        double max = stage <= HauntData.WATCHING ? 40.0D : stage == HauntData.APPROACHING ? 28.0D : 22.0D;
        if (phase == Phase.HUNT) {
            min = 10.0D;
            max = 16.0D;
        }
        Vec3 spot = null;
        BlockPos below = SpotFinder.findGround(level, victim.blockPosition().below(), 2) == null
                ? SpotFinder.findGround(level, victim.blockPosition().below(12), 10) : null;
        if (below != null && victim.getY() - below.getY() >= 5.0D && Habits.get(victim, Habits.Habit.HEIGHT) >= 0.4F) {
            // It knows you go up high. It comes straight to the foot of wherever you are, and looks up.
            double height = victim.getY() - below.getY();
            spot = SpotFinder.find(level, victim, 2.0D, 4.0D + height, 0.0F, 180.0F, (int) height + 4, 32,
                    pos -> pos.y < victim.getY() - 4.0D && SpotFinder.isDark(level, pos, safe) && Sight.clearLine(level, victim, pos.add(0.0D, 1.5D, 0.0D)));
        }
        if (spot == null) {
            // At the edge of the screen, somewhere you could see it if you happened to glance that way; further round
            // behind you if it has learned that you look for it.
            float offset = 60.0F + 70.0F * Habits.get(victim, Habits.Habit.GAZE);
            spot = SpotFinder.find(level, victim, min, max, offset, 20.0F, 12, 24,
                    pos -> SpotFinder.isDark(level, pos, safe) && Sight.clearLine(level, victim, pos.add(0.0D, 2.6D, 0.0D))
                            && Sight.gazeDot(victim, pos.add(0.0D, 1.6D, 0.0D)) < 0.9D);
        }
        if (spot == null) {
            spot = SpotFinder.find(level, victim, min, max, 90.0F, 90.0F, 12, 24, pos -> SpotFinder.isDark(level, pos, safe));
        }
        if (spot == null) {
            return null;
        }
        TheOneEntity one = TheOneEntity.spawn(level, victim, spot, stage, phase);
        if (one != null && Finale.isTonight(HauntData.get(level.getServer()))) {
            Finale.spawnDecoys(level, victim, stage);
        }
        return one;
    }

    /** Comes straight for someone, right now: the last night doesn't wait. */
    static void huntNow(MinecraftServer server) {
        HauntData data = HauntData.get(server);
        ServerLevel overworld = server.overworld();
        if (data.isBanished() || findTheOne(overworld) != null) {
            return;
        }
        ServerPlayer victim = pickVictim(overworld);
        if (victim != null) {
            appear(victim, data.getStage(), Phase.HUNT);
        }
    }

    /** Called when it leaves the world for any reason. */
    public static void onGone(ServerLevel level, TheOneEntity one, Exit exit) {
        HauntData data = HauntData.get(level.getServer());
        ServerPlayer victim = one.getVictimId() == null ? null : level.getServer().getPlayerList().getPlayer(one.getVictimId());
        if (victim != null) {
            Director.encounterEnded(victim, exit == Exit.TOUCHED || exit == Exit.WOUNDED);
        }
        RandomSource random = level.random;
        int stage = one.getStage();
        long delay = switch (exit) {
            case TOUCHED -> stage >= HauntData.HUNTING ? 1200 + random.nextInt(1200) : ticksUntilNextNight(level) + 600;
            case WOUNDED -> stage >= HauntData.HUNTING ? 2400 : 6000;
            case LOST, DAYLIGHT -> 1200;
            default -> switch (stage) {
                case HauntData.WATCHING -> 3600 + random.nextInt(3000);
                case HauntData.APPROACHING -> 2400 + random.nextInt(2400);
                default -> 1200 + random.nextInt(1200);
            };
        };
        if (Doll.anyoneCarries(level.getServer())) {
            delay = delay * 3 / 5;
        }
        data.setNextAppearance(data.getTicks() + (data.isBloodMoon() ? delay / 2 : delay));

        if (exit == Exit.WOUNDED && data.isBanished()) {
            one.playBanishEffects(level);
            Finale.victory(level, one);
            Component message = Component.translatable("cornerofeye.message.banished").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC);
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(message);
                award(player, "banished");
            }
        }
    }

    /** Called the moment the Lantern of Revelation catches it. */
    public static void onWounded(ServerLevel level, TheOneEntity one, ServerPlayer catcher) {
        HauntData data = HauntData.get(level.getServer());
        if (Finale.isDue(data) && !data.isBloodMoon()) {
            // The last one only counts on the last night.
            Component laugh = Component.translatable("cornerofeye.message.not_tonight").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(one) < 64.0D * 64.0D) {
                    player.displayClientMessage(laugh, true);
                }
            }
            return;
        }
        data.addWound();
        int left = Math.max(0, HorrorConfig.WOUNDS_TO_BANISH.get() - data.getWounds());
        Component message = left > 0
                ? Component.translatable("cornerofeye.message.wounded", left).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                : Component.translatable("cornerofeye.message.final_wound").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(one) < 64.0D * 64.0D) {
                player.displayClientMessage(message, true);
                award(player, "caught");
            }
        }
    }

    private static long ticksUntilNextNight(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        long until = (13000L - time + 24000L) % 24000L;
        return until == 0 ? 24000L : until;
    }

    public static void award(ServerPlayer player, String advancement) {
        Advancement adv = player.server.getAdvancements().getAdvancement(CornerOfTheEye.id(advancement));
        if (adv != null) {
            player.getAdvancements().award(adv, "code");
        }
    }

    // -------------------------------------------------------------- sleeping

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.getSleepTimer() == 70) {
            // Deeper asleep: sometimes the room is still there, but it isn't your room.
            HauntData data = HauntData.get(player.server);
            float chance = data.getStage() >= HauntData.HUNTING ? 0.45F : 0.3F;
            if (!data.isBanished() && data.getStage() >= HauntData.APPROACHING && player.getRandom().nextFloat() < chance) {
                Nightmares.begin(player, data.getStage());
            }
            return;
        }
        if (player.getSleepTimer() != 40) {
            return;
        }
        HauntData data = HauntData.get(player.server);
        if (data.isBanished() || data.getStage() < HauntData.APPROACHING || player.getRandom().nextFloat() > 0.2F) {
            return;
        }
        // Something sat down on the edge of the bed.
        player.stopSleepInBed(true, true);
        HorrorNetwork.playSoundTo(player, ModSounds.BREATH.get(), SoundSource.HOSTILE,
                player.getEyePosition().add(player.getViewVector(1.0F).scale(-0.8D)), 1.0F, 0.7F);
        player.sendSystemMessage(Component.translatable("cornerofeye.sleep." + player.getRandom().nextInt(3))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.wakeImmediately() || event.updateLevel()) {
            return;
        }
        // Woke up in the morning after a full night.
        HauntData data = HauntData.get(player.server);
        BlockPos sleptIn = HomeVisit.bedOf(player);
        if (sleptIn != null) {
            Habits.sleptIn(player, sleptIn);
        }
        // The same bed every night: it knows where to find you in the morning.
        float sameBed = Habits.get(player, Habits.Habit.BED);
        if (data.isBanished() || data.getStage() < HauntData.APPROACHING || player.getRandom().nextFloat() > 0.35F + 0.35F * sameBed) {
            return;
        }
        if (findTheOne(player.serverLevel()) == null && player.getRandom().nextFloat() < 0.45F + 0.3F * sameBed) {
            // You are awake and you can't move.
            schedule(10, () -> Moments.start(player, data.getStage(), Scene.PARALYSIS, true));
            return;
        }
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return;
        }
        BlockPos bed = HomeVisit.bedOf(player);
        if (bed != null && player.getRandom().nextBoolean()) {
            // Cracks in the wall by the bed, claw marks over them. It was right here, on the other side.
            schedule(5, () -> Gnawing.byTheBed(player.serverLevel(), bed));
            return;
        }
        schedule(5, () -> {
            ServerLevel level = player.serverLevel();
            for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-2, -1, -2), player.blockPosition().offset(2, 1, 2))) {
                if (level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty()
                        && level.getBlockState(pos.below()).isSolid() && !pos.equals(player.blockPosition())
                        && !pos.equals(player.blockPosition().above())) {
                    float yaw = (float) Math.toDegrees(Math.atan2(pos.getZ() + 0.5D - player.getZ(), pos.getX() + 0.5D - player.getX())) - 90.0F;
                    Signs.place(level, pos.immutable(), yaw, Signs.WATCHED_YOU_SLEEP);
                    return;
                }
            }
        });
    }

    // -------------------------------------------------------------- lifecycle

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        state(event.getEntity()).syncedStage = -1;
        if (event.getEntity() instanceof ServerPlayer player) {
            Nights.sync(player, HauntData.get(player.server));
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        state(event.getEntity()).syncedStage = -1;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
        TASKS.clear();
        lastStage = -1;
        Director.reset();
        Nights.reset();
        Finale.reset();
    }

    /** Runs one event right now, for commands. */
    public static boolean forceEvent(ServerPlayer player, HauntEvent event, int stage) {
        return event.run(HauntContext.of(player, stage, true));
    }

    private HauntManager() {
    }
}
