package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.Scene;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.entity.scene.BehindScene;
import com.laropam.cornerofeye.entity.scene.CeilingScene;
import com.laropam.cornerofeye.entity.scene.ChaseScene;
import com.laropam.cornerofeye.entity.scene.MassacreScene;
import com.laropam.cornerofeye.entity.scene.UnderwaterScene;
import com.laropam.cornerofeye.entity.scene.DoorScene;
import com.laropam.cornerofeye.entity.scene.LightsOutScene;
import com.laropam.cornerofeye.entity.scene.PeekScene;
import com.laropam.cornerofeye.entity.scene.StillScene;
import com.laropam.cornerofeye.entity.scene.WindowScene;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
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
            TheOneEntity one = start(victim, stage, scene, false);
            if (one != null) {
                return one;
            }
        }
        return null;
    }

    /**
     * @param forced started by a command: skip the dice roll on the rare ones
     * @return null if the surroundings don't allow this moment right now
     */
    @Nullable
    public static TheOneEntity start(ServerPlayer victim, int stage, Scene scene, boolean forced) {
        if (victim.isPassenger() || victim.getAbilities().flying || (victim.isInWater() && scene != Scene.UNDERWATER)) {
            return null;
        }
        ServerLevel level = victim.serverLevel();
        return switch (scene) {
            case DOOR -> door(level, victim, stage);
            case PEEK -> peek(level, victim, stage);
            case BEHIND -> behind(level, victim, stage);
            case LIGHTS_OUT -> lightsOut(level, victim, stage);
            case STILL -> still(level, victim, stage);
            case CEILING -> ceiling(level, victim, stage);
            case UNDERWATER -> underwater(level, victim, stage);
            case MASSACRE -> massacre(level, victim, stage, forced);
            case CHASE -> chase(level, victim, stage);
            case WINDOW -> window(level, victim, stage);
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
            return TheOneEntity.spawnForScene(level, victim, outside, stage, Scene.DOOR, new DoorScene(door, outside, doorway, inside));
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
        List<BlockPos> lights = Lights.findAll(level, victim.blockPosition(), 10, 4, true);
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

    // ------------------------------------------------------------------ ceiling

    @Nullable
    private static TheOneEntity ceiling(ServerLevel level, ServerPlayer victim, int stage) {
        BlockPos eyes = BlockPos.containing(victim.getEyePosition());
        if (level.getBrightness(LightLayer.SKY, eyes) >= 4) {
            return null;
        }
        RandomSource random = victim.getRandom();
        for (int attempt = 0; attempt < 40; attempt++) {
            double yaw = Math.toRadians(victim.getYRot() + (random.nextFloat() * 2.0F - 1.0F) * 35.0F);
            double dist = 4.0D + random.nextDouble() * 6.0D;
            BlockPos column = BlockPos.containing(victim.getX() - Math.sin(yaw) * dist, victim.getY(), victim.getZ() + Math.cos(yaw) * dist);
            BlockPos floor = null;
            for (int dy = 0; dy <= 6 && floor == null; dy = dy <= 0 ? 1 - dy : -dy) {
                BlockPos pos = column.above(dy);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                    floor = pos;
                }
            }
            if (floor == null) {
                continue;
            }
            int gap = 1;
            while (gap <= 9 && level.getBlockState(floor.above(gap)).isAir()) {
                gap++;
            }
            BlockPos roof = floor.above(gap);
            if (gap < 3 || gap > 9 || !level.getBlockState(roof).isSolidRender(level, roof)) {
                continue;
            }
            Vec3 hangPoint = new Vec3(floor.getX() + 0.5D, roof.getY() - 1.0D, floor.getZ() + 0.5D);
            if (!Sight.clearLine(level, victim, hangPoint)) {
                continue;
            }
            return TheOneEntity.spawnForScene(level, victim, new Vec3(hangPoint.x, roof.getY(), hangPoint.z), stage, Scene.CEILING,
                    new CeilingScene(roof.getY()));
        }
        return null;
    }

    // --------------------------------------------------------------- underwater

    @Nullable
    private static TheOneEntity underwater(ServerLevel level, ServerPlayer victim, int stage) {
        if (!victim.isInWater()) {
            return null;
        }
        BlockPos feet = victim.blockPosition();
        int depth = 0;
        while (depth < 12 && level.getFluidState(feet.below(depth + 1)).is(FluidTags.WATER)) {
            depth++;
        }
        if (depth < 3) {
            return null;
        }
        // Below you and a little ahead, where you'd see it if you looked down.
        Vec3 ahead = victim.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D).normalize().scale(1.5D);
        Vec3 start = victim.position().add(ahead).subtract(0.0D, Math.min(depth, 8) + 2.0D, 0.0D);
        return TheOneEntity.spawnForScene(level, victim, start, stage, Scene.UNDERWATER, new UnderwaterScene());
    }

    // ----------------------------------------------------------------- massacre

    @Nullable
    private static TheOneEntity massacre(ServerLevel level, ServerPlayer victim, int stage, boolean forced) {
        HauntData data = HauntData.get(level.getServer());
        if (!HorrorConfig.VILLAGE_MASSACRE.get() || (!forced && data.getTicks() < data.getNextMassacre())) {
            return null;
        }
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, victim.getBoundingBox().inflate(48.0D, 16.0D, 48.0D));
        if (villagers.size() < 3) {
            return null;
        }
        if (!forced && victim.getRandom().nextFloat() > (stage >= HauntData.HUNTING ? 0.6F : 0.35F)) {
            return null;
        }
        Vec3 center = Vec3.ZERO;
        for (Villager villager : villagers) {
            center = center.add(villager.position());
        }
        center = center.scale(1.0D / villagers.size());
        BlockPos ground = SpotFinder.findGround(level, BlockPos.containing(center), 8);
        Vec3 spot = ground != null ? Vec3.atBottomCenterOf(ground) : center;
        data.setNextMassacre(data.getTicks() + HorrorConfig.DAYS_BETWEEN_MASSACRES.get() * 24000L);
        return TheOneEntity.spawnForScene(level, victim, spot, stage, Scene.MASSACRE,
                new MassacreScene(MassacreScene.targetsAround(level, center), Lights.findAll(level, BlockPos.containing(center), 32, 8, true)));
    }

    // -------------------------------------------------------------------- chase

    @Nullable
    private static TheOneEntity chase(ServerLevel level, ServerPlayer victim, int stage) {
        int safe = HorrorConfig.SAFE_LIGHT_LEVEL.get();
        // Out of the dark ahead of you, so you see it coming.
        Vec3 spot = SpotFinder.find(level, victim, 18.0D, 26.0D, 15.0F, 25.0F, 8, 24,
                pos -> SpotFinder.isDark(level, pos, safe) && Sight.clearLine(level, victim, pos.add(0.0D, 1.0D, 0.0D)));
        if (spot == null) {
            return null;
        }
        return TheOneEntity.spawnForScene(level, victim, spot, stage, Scene.CHASE, new ChaseScene(ChaseScene.defaultLength(stage)));
    }

    // ------------------------------------------------------------------- window

    /**
     * You are indoors and can see out of a window: it comes to the window, outside in the dark, with the bottom of
     * the glass a block above the ground it stands on, so bending down puts its face right against it.
     */
    @Nullable
    private static TheOneEntity window(ServerLevel level, ServerPlayer victim, int stage) {
        if (level.canSeeSky(BlockPos.containing(victim.getEyePosition()))) {
            return null;
        }
        int safe = HorrorConfig.SAFE_LIGHT_LEVEL.get();
        List<BlockPos> windows = new ArrayList<>();
        List<Vec3> spots = new ArrayList<>();
        List<Direction> outs = new ArrayList<>();
        BlockPos center = victim.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-10, -2, -10), center.offset(10, 3, 10))) {
            BlockState state = level.getBlockState(pos);
            boolean pane = state.getBlock() instanceof IronBarsBlock && !state.is(Blocks.IRON_BARS);
            if (!pane && !(state.getBlock() instanceof AbstractGlassBlock && !state.is(Blocks.TINTED_GLASS))) {
                continue;
            }
            Vec3 glass = Vec3.atCenterOf(pos);
            double dist = glass.distanceTo(victim.getEyePosition());
            // At the edge of your sight, where you would notice it, through glass you can actually see.
            double gaze = Sight.gazeDot(victim, glass);
            if (dist < 3.0D || dist > 12.0D || gaze < 0.2D || gaze > 0.88D || !Sight.clearLineToBlock(level, victim, pos)) {
                continue;
            }
            Direction out = Direction.getNearest(glass.x - victim.getX(), 0.0D, glass.z - victim.getZ());
            if (!open(level, pos.relative(out)) || !open(level, pos.relative(out.getOpposite()))) {
                // Not glass you look through from where you are (a pane seen edge on, glass against a wall).
                continue;
            }
            Vec3 stand = glass.add(out.getStepX() * ((pane ? 0.0625D : 0.5D) + WindowScene.REACH), 0.0D,
                    out.getStepZ() * ((pane ? 0.0625D : 0.5D) + WindowScene.REACH));
            stand = new Vec3(stand.x, pos.getY() - 1, stand.z);
            BlockPos feet = BlockPos.containing(stand);
            BlockPos below = pos.relative(out).below();
            if (!open(level, feet) || !open(level, feet.above()) || !open(level, feet.above(2)) || !open(level, below)
                    || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                    || !SpotFinder.isDark(level, stand, safe)) {
                continue;
            }
            windows.add(pos.immutable());
            spots.add(stand);
            outs.add(out);
        }
        if (windows.isEmpty()) {
            return null;
        }
        int pick = victim.getRandom().nextInt(windows.size());
        BlockPos window = windows.get(pick);
        boolean pane = level.getBlockState(window).getBlock() instanceof IronBarsBlock;
        return TheOneEntity.spawnForScene(level, victim, spots.get(pick), stage, Scene.WINDOW,
                new WindowScene(window, outs.get(pick), !pane));
    }

    /** Nothing in the way of a body there. */
    private static boolean open(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private Moments() {
    }
}
