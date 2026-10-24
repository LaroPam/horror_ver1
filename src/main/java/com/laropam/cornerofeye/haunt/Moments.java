package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.Scene;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.entity.scene.BehindScene;
import com.laropam.cornerofeye.entity.scene.DoorScene;
import com.laropam.cornerofeye.entity.scene.LightsOutScene;
import com.laropam.cornerofeye.entity.scene.PeekScene;
import com.laropam.cornerofeye.entity.scene.StillScene;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Finds a place for a scripted scary moment around a player and starts it. */
public final class Moments {
    /** Tries the moments that suit this stage in random order until one fits the surroundings. */
    @Nullable
    public static TheOneEntity startRandom(ServerPlayer victim, int stage) {
        List<Scene> scenes = new ArrayList<>();
        for (Scene scene : Scene.values()) {
            if (scene != Scene.NONE && stage >= scene.minStage) {
                scenes.add(scene);
            }
        }
        Collections.shuffle(scenes, new java.util.Random(victim.getRandom().nextLong()));
        for (Scene scene : scenes) {
            TheOneEntity one = start(victim, stage, scene);
            if (one != null) {
                return one;
            }
        }
        return null;
    }

    /** @return null if the surroundings don't allow this moment right now */
    @Nullable
    public static TheOneEntity start(ServerPlayer victim, int stage, Scene scene) {
        if (victim.isPassenger() || victim.isInWater() || victim.getAbilities().flying) {
            return null;
        }
        ServerLevel level = victim.serverLevel();
        return switch (scene) {
            case DOOR -> door(level, victim, stage);
            case PEEK -> peek(level, victim, stage);
            case BEHIND -> behind(level, victim, stage);
            case LIGHTS_OUT -> lightsOut(level, victim, stage);
            case STILL -> still(level, victim, stage);
            case NONE -> null;
        };
    }

    // --------------------------------------------------------------------- door

    @Nullable
    private static TheOneEntity door(ServerLevel level, ServerPlayer victim, int stage) {
        boolean mayOpen = HorrorConfig.ALLOW_BLOCK_CHANGES.get();
        Vec3 player = victim.position();
        List<BlockPos> doors = new ArrayList<>();
        BlockPos center = victim.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-10, -3, -10), center.offset(10, 3, 10))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && (mayOpen || state.getValue(DoorBlock.OPEN))) {
                doors.add(pos.immutable());
            }
        }
        doors.sort((a, b) -> Double.compare(a.distToCenterSqr(player), b.distToCenterSqr(player)));
        for (BlockPos door : doors) {
            Direction facing = level.getBlockState(door).getValue(DoorBlock.FACING);
            Vec3 doorway = Vec3.atBottomCenterOf(door);
            Vec3 axis = new Vec3(facing.getStepX(), 0.0D, facing.getStepZ());
            double side = player.subtract(doorway).dot(axis);
            if (Math.abs(side) < 1.5D) {
                // Standing in the doorway: no room for the scene.
                continue;
            }
            Vec3 in = axis.scale(Math.signum(side));
            Vec3 outside = doorway.subtract(in.scale(3.5D));
            Vec3 inside = doorway.add(in.scale(Math.min(1.8D, Math.abs(side) - 1.0D)));
            BlockPos above = BlockPos.containing(inside).above(2);
            boolean roomToStand = level.getBlockState(above).isAir() && level.getBlockState(above.above()).isAir();
            return TheOneEntity.spawnForScene(level, victim, outside, stage, Scene.DOOR,
                    new DoorScene(door, outside, doorway, inside, roomToStand));
        }
        return null;
    }

    // --------------------------------------------------------------------- peek

    @Nullable
    private static TheOneEntity peek(ServerLevel level, ServerPlayer victim, int stage) {
        List<BlockPos> trunks = new ArrayList<>();
        BlockPos center = victim.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-22, -3, -22), center.offset(22, 3, 22))) {
            if (!level.getBlockState(pos).is(BlockTags.LOGS) || level.getBlockState(pos.below()).is(BlockTags.LOGS)
                    || !level.getBlockState(pos.above()).is(BlockTags.LOGS) || !level.getBlockState(pos.above(2)).is(BlockTags.LOGS)) {
                continue;
            }
            Vec3 trunk = Vec3.atCenterOf(pos.above());
            double dist = trunk.distanceTo(victim.getEyePosition());
            // In front of you, where you will notice it, with a clear view of the trunk.
            if (dist > 6.0D && dist < 15.0D && Sight.gazeDot(victim, trunk) > 0.75D && Sight.clearLineToBlock(level, victim, pos.above())) {
                trunks.add(pos.immutable());
            }
        }
        if (trunks.isEmpty()) {
            return null;
        }
        BlockPos base = trunks.get(victim.getRandom().nextInt(trunks.size()));
        Vec3 trunk = Vec3.atBottomCenterOf(base);
        Vec3 away = trunk.subtract(victim.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 spot = trunk.add(away.scale(1.0D));
        return TheOneEntity.spawnForScene(level, victim, spot, stage, Scene.PEEK, new PeekScene(victim.getRandom().nextBoolean()));
    }

    // ------------------------------------------------------------------- behind

    @Nullable
    private static TheOneEntity behind(ServerLevel level, ServerPlayer victim, int stage) {
        Vec3 look = victim.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() < 1.0E-4D) {
            return null;
        }
        Vec3 offset = look.normalize().scale(-1.6D);
        return TheOneEntity.spawnForScene(level, victim, victim.position().add(offset), stage, Scene.BEHIND, new BehindScene(offset));
    }

    // --------------------------------------------------------------- lights out

    @Nullable
    private static TheOneEntity lightsOut(ServerLevel level, ServerPlayer victim, int stage) {
        int safe = HorrorConfig.SAFE_LIGHT_LEVEL.get();
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get() || !TheOneEntity.isLit(level, victim, safe) || !carriesLight(victim)) {
            return null;
        }
        List<BlockPos> lights = Lights.findAll(level, victim.blockPosition(), 10, 4);
        if (lights.size() < 2) {
            return null;
        }
        // Waits just past where the light reached, in front of you, so you watch it walk out of the dark.
        Vec3 spot = SpotFinder.find(level, victim, 9.0D, 13.0D, 22.0F, 18.0F, 6, 24,
                pos -> Sight.clearLine(level, victim, pos.add(0.0D, 2.0D, 0.0D)));
        if (spot == null) {
            spot = SpotFinder.find(level, victim, 9.0D, 13.0D, 90.0F, 90.0F, 6, 24, pos -> true);
        }
        if (spot == null) {
            return null;
        }
        Collections.reverse(lights);
        return TheOneEntity.spawnForScene(level, victim, spot, stage, Scene.LIGHTS_OUT, new LightsOutScene(lights));
    }

    /** It would be unfair to put out the lights of someone who can't light anything. */
    private static boolean carriesLight(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().getLightEmission() >= 10) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().getLightEmission() >= 10) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------- still

    @Nullable
    private static TheOneEntity still(ServerLevel level, ServerPlayer victim, int stage) {
        if (!victim.onGround() || TheOneEntity.isLit(level, victim, HorrorConfig.SAFE_LIGHT_LEVEL.get())) {
            return null;
        }
        RandomSource random = victim.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0D;
        Vec3 center = victim.position();
        Vec3 start = center.add(Math.cos(angle) * 7.0D, 0.0D, Math.sin(angle) * 7.0D);
        return TheOneEntity.spawnForScene(level, victim, start, stage, Scene.STILL, new StillScene(center, angle, random.nextBoolean()));
    }

    private Moments() {
    }
}
