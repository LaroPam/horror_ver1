package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.SenseItGoal;
import com.laropam.cornerofeye.entity.TheOneEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Animals know where it is before you do. Dogs growl at an empty corner, cats hiss and bolt, horses rear,
 * cattle freeze and stare and then run; villagers shake their heads at a blank wall. Later on, every animal
 * around sometimes turns to stare at the same empty spot, and then, all together, at you.
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID)
public final class Animals {
    /** How far away they feel it. */
    private static final double RANGE = 24.0D;
    /** Stares reach further, so that everything around the player joins in. */
    private static final double STARE_RANGE = 32.0D;
    private static final int STARE_LENGTH = 200;
    /** For the last two seconds of a stare they look at the player instead. */
    private static final int STARE_TURN = 40;

    /**
     * Something they react to: a point to look at. Stares are nothing at all; nobody runs from those.
     */
    public record Sense(Vec3 pos, boolean real, double range) {
    }

    private record Stare(Vec3 pos, UUID player, long until) {
    }

    private static final Map<ResourceKey<Level>, List<Sense>> SENSES = new HashMap<>();
    private static final Map<ResourceKey<Level>, List<Stare>> STARES = new HashMap<>();

    /** The closest thing {@code mob} can feel right now, if any. */
    @Nullable
    public static Sense find(Mob mob) {
        List<Sense> senses = SENSES.get(mob.level().dimension());
        if (senses == null) {
            return null;
        }
        Sense best = null;
        double bestDist = Double.MAX_VALUE;
        for (Sense sense : senses) {
            double dist = mob.distanceToSqr(sense.pos());
            if (dist < sense.range() * sense.range() && dist < bestDist) {
                best = sense;
                bestDist = dist;
            }
        }
        return best;
    }

    /** Every animal around turns to the same empty spot to the player's side or behind, and stares. */
    public static boolean stareAtNothing(HauntContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = context.level();
        List<Mob> watchers = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20.0D, 8.0D, 20.0D),
                mob -> mob instanceof Animal || mob instanceof Villager);
        if (watchers.size() < (context.forced() ? 1 : 2)) {
            return false;
        }
        Vec3 spot = SpotFinder.find(level, player, 8.0D, 16.0D, 110.0F, 50.0F, 6, 20, pos -> true);
        if (spot == null) {
            return false;
        }
        STARES.computeIfAbsent(level.dimension(), key -> new ArrayList<>())
                .add(new Stare(spot.add(0.0D, 1.6D, 0.0D), player.getUUID(), level.getGameTime() + STARE_LENGTH));
        return true;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.getGameTime() % 10 != 0) {
            return;
        }
        List<Sense> senses = new ArrayList<>();
        TheOneEntity one = HauntManager.findTheOne(level);
        if (one != null) {
            senses.add(new Sense(one.getEyePosition(), true, RANGE));
        }
        List<Stare> stares = STARES.get(level.dimension());
        if (stares != null) {
            stares.removeIf(stare -> stare.until() <= level.getGameTime());
            for (Stare stare : stares) {
                Player player = level.getPlayerByUUID(stare.player());
                boolean turned = player != null && stare.until() - level.getGameTime() <= STARE_TURN;
                senses.add(new Sense(turned ? player.getEyePosition() : stare.pos(), false, STARE_RANGE));
            }
        }
        if (senses.isEmpty()) {
            SENSES.remove(level.dimension());
            return;
        }
        SENSES.put(level.dimension(), senses);
        for (Sense sense : senses) {
            villagers(level, sense);
        }
    }

    /** Villagers think with a brain rather than goals: hand it something to look at. */
    private static void villagers(ServerLevel level, Sense sense) {
        double range = sense.range() * 0.75D;
        for (Villager villager : level.getEntitiesOfClass(Villager.class, new AABB(sense.pos(), sense.pos()).inflate(range))) {
            if (villager.isSleeping()) {
                continue;
            }
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(sense.pos()), 20L);
            if (sense.real() && villager.getRandom().nextInt(12) == 0) {
                // Shakes its head at it.
                villager.setUnhappyCounter(40);
                villager.playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.8F);
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Animal animal
                && animal.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof SenseItGoal)) {
            animal.goalSelector.addGoal(1, new SenseItGoal(animal));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SENSES.clear();
        STARES.clear();
    }

    private Animals() {
    }
}
