package com.laropam.cornerofeye.entity;

import com.laropam.cornerofeye.HorrorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * How it finds its way to you. It thinks like something that has all night: it would rather walk round,
 * open the door, come through the open window or down the cave than claw through a wall, and it only digs
 * where going round would take far longer. It climbs walls like a spider and drops from ledges, it never
 * steps into light, and when it can't get all the way to you it gets as close as it can and waits.
 * <p>
 * The plan is an A* search over block cells, where each cell costs what it would take it to be there:
 * open air is cheap, a closed door costs the moment it takes to open it, and solid blocks cost the time
 * to tear through them, so a nearby door always wins over a wall.
 */
final class Navigator {
    enum Kind {
        WALK, OPEN, DIG
    }

    record Step(BlockPos pos, Kind kind) {
    }

    enum Result {
        MOVING, BUSY, ARRIVED, STUCK,
        /** Walled off by something it can neither get round nor break (bedrock): only the old way through is left. */
        SEEP,
        /** The target is up in the open air, with nothing between to climb on (a floating platform, a sky base). */
        OUT_OF_REACH
    }

    private enum Cell {
        AIR, LOW, WATER, DOOR, SOLID, BLOCKED
    }

    private record Info(Cell cell, float hardness, boolean lit) {
        boolean walkable() {
            return (this.cell == Cell.AIR || this.cell == Cell.LOW || this.cell == Cell.WATER) && !this.lit;
        }
    }

    private static final int MAX_NODES = 3500;
    private static final int MAX_RANGE = 40;
    private static final int REPLAN_TICKS = 40;
    private static final double WALK = 1.0D;
    private static final double DIAGONAL = 1.414D;
    private static final double CLIMB = 1.6D;
    private static final double DROP = 0.9D;
    /** Opening a door costs about as much as a few steps. */
    private static final double OPEN = 4.0D;
    /** Tearing through a block costs as much as a long walk round, more for harder blocks. */
    private static final double DIG = 12.0D;
    private static final double DIG_PER_HARDNESS = 6.0D;

    private final TheOneEntity one;
    private List<Step> path = Collections.emptyList();
    private int index;
    @Nullable
    private BlockPos goal;
    private boolean reachesGoal;
    /** The way straight at the target starts with something it can't break. */
    private boolean walledOff;
    /** The way straight at the target starts with open air it has nothing to hold on to in. */
    private boolean outOfReach;
    private long plannedAt = -1000L;
    /** A door it opened and will quietly close behind itself once through. */
    @Nullable
    private BlockPos doorToClose;

    Navigator(TheOneEntity one) {
        this.one = one;
    }

    void reset() {
        this.path = Collections.emptyList();
        this.index = 0;
        this.goal = null;
    }

    /**
     * One tick of getting to {@code target}: walking the plan, opening what is in the way, digging if it has to.
     *
     * @param canDig whether it may tear through blocks; if not, a walled-in target is simply out of reach
     */
    Result follow(ServerLevel level, Burrow burrow, Vec3 target, double speed, boolean canDig) {
        this.closeDoorBehind(level);
        if (burrow.isBusy()) {
            burrow.tick(level);
            return Result.BUSY;
        }
        BlockPos goalCell = feet(target);
        Step current = this.index < this.path.size() ? this.path.get(this.index) : null;
        boolean stale = level.getGameTime() - this.plannedAt > REPLAN_TICKS;
        boolean moved = this.goal == null || this.goal.distSqr(goalCell) > 4.0D;
        // Found no way at all a moment ago: don't search the whole area again every tick.
        boolean hopeless = this.path.isEmpty() && !moved && level.getGameTime() - this.plannedAt < 10L;
        if (!hopeless && (current == null || stale || moved || !this.stillValid(level, current, canDig))) {
            this.plan(level, goalCell, canDig);
            current = this.index < this.path.size() ? this.path.get(this.index) : null;
        }
        if (current == null) {
            this.one.setDeltaMovement(Vec3.ZERO);
            return this.reachesGoal ? Result.ARRIVED : this.walledOff ? Result.SEEP : this.outOfReach ? Result.OUT_OF_REACH : Result.STUCK;
        }

        Vec3 hips = this.one.position().add(0.0D, 0.45D, 0.0D);
        // Something to do further along? Stop where its arms reach it.
        Step action = this.nextAction(level, canDig);
        // A door ahead: it gets up and goes to it on its feet, the way a person would.
        this.one.setGoingToDoor(action != null && action.kind() == Kind.OPEN
                && Vec3.atCenterOf(action.pos()).distanceTo(hips) < 5.0D);
        if (action != null) {
            Vec3 at = Vec3.atCenterOf(action.pos());
            Vec3 to = at.subtract(hips);
            if (action.kind() == Kind.OPEN && to.horizontalDistance() <= 1.4D && Math.abs(to.y) < 2.0D && !this.one.isCrawling()) {
                BlockPos door = lowerHalf(level, action.pos());
                if (burrow.open(level, door) && level.random.nextFloat() < 0.6F) {
                    this.doorToClose = door;
                }
                this.markWalkable(action);
                return Result.BUSY;
            }
            if (action.kind() == Kind.DIG) {
                BlockPos feet = feet(this.one.position());
                boolean shaft = action.pos().getX() == feet.getX() && action.pos().getZ() == feet.getZ();
                double reach = shaft ? 1.6D : Burrow.REACH * this.one.getVisualScale() + 0.5D;
                // Only what its arms can get at in a straight line: not round a corner, through another wall. A
                // block below its level it digs from right above, head down, not by reaching down and forward.
                boolean inReach = (shaft || action.pos().getY() >= feet.getY()) && to.length() <= reach
                        && this.openUpTo(level, hips, action.pos());
                if (inReach || this.path.indexOf(action) == this.index) {
                    if (!shaft && this.easeBack(level, at, hips, speed)) {
                        return Result.MOVING;
                    }
                    int direction = !shaft ? Burrow.FORWARD : to.y > 0.0D ? Burrow.UP : Burrow.DOWN;
                    burrow.dig(level, action.pos(), direction);
                    this.markWalkable(action);
                    return Result.BUSY;
                }
            }
        }

        // Cut corners where nothing is in the way, so it moves in lines, not along the grid. Only on the level and
        // over a floor: up a pillar or a wall it goes hand over hand, not through the air.
        int floorY = feet(this.one.position()).getY();
        while (this.index + 1 < this.path.size() && this.path.get(this.index + 1).kind() == Kind.WALK
                && this.path.get(this.index + 1).pos().getY() == floorY
                && this.clearWalk(level, hips, Vec3.atBottomCenterOf(this.path.get(this.index + 1).pos()).add(0.0D, 0.45D, 0.0D))) {
            this.index++;
        }
        current = this.path.get(this.index);
        Vec3 waypoint = Vec3.atBottomCenterOf(current.pos());
        if (!this.one.walkStep(waypoint, speed)) {
            // Light stepped into its way: think again.
            this.plannedAt = -1000L;
            this.one.setDeltaMovement(Vec3.ZERO);
            return Result.STUCK;
        }
        if (this.one.position().distanceToSqr(waypoint) < 0.3D * 0.3D) {
            this.index++;
        }
        return Result.MOVING;
    }

    // ------------------------------------------------------------------ planning

    private void plan(ServerLevel level, BlockPos goalCell, boolean canDig) {
        this.goal = goalCell;
        this.plannedAt = level.getGameTime();
        this.index = 0;
        this.path = Collections.emptyList();
        this.reachesGoal = false;
        this.walledOff = false;
        this.outOfReach = false;

        BlockPos start = feet(this.one.position());
        Map<Long, Info> cells = new HashMap<>();
        Map<Long, Double> cost = new HashMap<>();
        Map<Long, Long> from = new HashMap<>();
        Map<Long, Kind> kinds = new HashMap<>();
        PriorityQueue<double[]> open = new PriorityQueue<>((a, b) -> Double.compare(a[1], b[1]));
        long startKey = start.asLong();
        cost.put(startKey, 0.0D);
        kinds.put(startKey, Kind.WALK);
        open.add(new double[]{startKey, distance(start, goalCell)});
        long best = startKey;
        double bestDistance = distance(start, goalCell);
        int expanded = 0;

        while (!open.isEmpty() && expanded < MAX_NODES) {
            double[] top = open.poll();
            long key = (long) top[0];
            BlockPos pos = BlockPos.of(key);
            double here = cost.get(key);
            if (top[1] > here + distance(pos, goalCell) + 1.0E-6D) {
                continue;
            }
            expanded++;
            double left = distance(pos, goalCell);
            if (left < bestDistance) {
                best = key;
                bestDistance = left;
            }
            if (left <= 1.5D) {
                this.reachesGoal = true;
                best = key;
                break;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if ((dx | dy | dz) == 0 || (dx != 0 && dz != 0 && dy != 0)) {
                            continue;
                        }
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (Math.abs(next.getX() - start.getX()) > MAX_RANGE || Math.abs(next.getZ() - start.getZ()) > MAX_RANGE
                                || Math.abs(next.getY() - start.getY()) > MAX_RANGE) {
                            continue;
                        }
                        double step = this.stepCost(level, cells, pos, dx, dy, dz, canDig, kinds, next.equals(goalCell));
                        if (step < 0.0D) {
                            continue;
                        }
                        long nextKey = next.asLong();
                        double total = here + step;
                        Double known = cost.get(nextKey);
                        if (known == null || total < known - 1.0E-6D) {
                            cost.put(nextKey, total);
                            from.put(nextKey, key);
                            open.add(new double[]{nextKey, total + distance(next, goalCell)});
                        }
                    }
                }
            }
        }
        List<Step> steps = new ArrayList<>();
        for (long key = best; key != startKey; key = from.get(key)) {
            steps.add(new Step(BlockPos.of(key), kinds.getOrDefault(key, Kind.WALK)));
        }
        Collections.reverse(steps);
        if (!this.reachesGoal) {
            // No whole way found (too far, too hard, or walled off with something it can't break). It doesn't
            // start digging towards a guess, say under your floor: it walks as close as it can get, and from
            // there goes straight at you.
            for (int i = 0; i < steps.size(); i++) {
                if (steps.get(i).kind() == Kind.DIG) {
                    steps = new ArrayList<>(steps.subList(0, i));
                    break;
                }
            }
            if (steps.isEmpty() && canDig) {
                steps = this.bore(level, start, goalCell);
            }
        }
        this.path = steps;
    }

    /**
     * A few cells straight from {@code from} towards {@code to}, one axis at a time, opening and tearing through
     * whatever is in the way. Stops short of light and of what it can't break.
     */
    private List<Step> bore(ServerLevel level, BlockPos from, BlockPos to) {
        List<Step> steps = new ArrayList<>();
        Map<Long, Info> cells = new HashMap<>();
        BlockPos pos = from;
        for (int i = 0; i < 8 && distance(pos, to) > 1.5D; i++) {
            // Of the moves that bring it closer, an open one first (up the wall to the right height before
            // tearing in, rather than through the floor as well), then a door, then the longest way to go.
            BlockPos next = null;
            Info nextInfo = null;
            int bestRank = Integer.MAX_VALUE;
            int bestOrder = 3;
            int[] delta = {to.getX() - pos.getX(), to.getY() - pos.getY(), to.getZ() - pos.getZ()};
            for (int axis = 0; axis < 3; axis++) {
                if (delta[axis] == 0) {
                    continue;
                }
                int sign = Integer.signum(delta[axis]);
                BlockPos cell = pos.offset(axis == 0 ? sign : 0, axis == 1 ? sign : 0, axis == 2 ? sign : 0);
                Info info = this.info(level, cells, cell, true);
                // Open air it can only go up or along with something to hold on to.
                boolean open = info.walkable() && (axis == 1 && sign < 0 || this.supported(level, cells, cell, true));
                int order = open ? 0 : info.cell() == Cell.DOOR ? 1 : info.cell() == Cell.SOLID ? 2 : 3;
                // Horizontal before vertical when it is just as far, so it doesn't dig shafts it needn't.
                int rank = order * 1000 - Math.abs(delta[axis]) * 2 - (axis == 1 ? 0 : 1);
                if (rank < bestRank) {
                    bestRank = rank;
                    bestOrder = order;
                    next = cell;
                    nextInfo = info;
                }
            }
            Kind kind = switch (bestOrder) {
                case 0 -> Kind.WALK;
                case 1 -> Kind.OPEN;
                case 2 -> Kind.DIG;
                default -> null;
            };
            if (kind == null) {
                this.walledOff = steps.isEmpty() && nextInfo.cell() == Cell.BLOCKED;
                this.outOfReach = steps.isEmpty() && nextInfo.walkable();
                break;
            }
            steps.add(new Step(next, kind));
            pos = next;
        }
        return steps;
    }

    /**
     * What it costs to move from {@code pos} by (dx, dy, dz), or -1 if it can't. Records whether getting into
     * the new cell means opening or digging it.
     */
    private double stepCost(ServerLevel level, Map<Long, Info> cells, BlockPos pos, int dx, int dy, int dz, boolean canDig,
                            Map<Long, Kind> kinds, boolean isGoal) {
        BlockPos next = pos.offset(dx, dy, dz);
        Info info = this.info(level, cells, next, canDig);
        boolean horizontal = dx != 0 || dz != 0;
        // Corners and slopes only through open space: diagonals never squeeze past or dig into a corner.
        if (dx != 0 && dz != 0) {
            if (!info.walkable() || !this.info(level, cells, pos.offset(dx, 0, 0), canDig).walkable()
                    || !this.info(level, cells, pos.offset(0, 0, dz), canDig).walkable()) {
                return -1.0D;
            }
        }
        if (horizontal && dy != 0) {
            BlockPos through = dy > 0 ? pos.above() : pos.offset(dx, 0, dz);
            if (!info.walkable() || !this.info(level, cells, through, canDig).walkable()) {
                return -1.0D;
            }
        }
        double base = !horizontal ? (dy > 0 ? CLIMB : DROP) : (dx != 0 && dz != 0 ? DIAGONAL : WALK) + (dy > 0 ? 0.6D : 0.0D);
        Kind kind;
        double extra;
        switch (info.cell()) {
            case AIR, LOW, WATER -> {
                if (info.lit() && !isGoal) {
                    return -1.0D;
                }
                kind = Kind.WALK;
                extra = info.cell() == Cell.WATER ? 2.0D : info.cell() == Cell.LOW ? 0.3D : 0.0D;
            }
            case DOOR -> {
                kind = Kind.OPEN;
                extra = OPEN;
            }
            case SOLID -> {
                kind = Kind.DIG;
                extra = DIG + Math.min(300.0D, info.hardness() * DIG_PER_HARDNESS);
            }
            default -> {
                return -1.0D;
            }
        }
        // Up and along takes something to hold on to: a floor, a wall, a ceiling. Falling takes nothing.
        if (kind == Kind.WALK && dy >= 0 && !this.supported(level, cells, next, canDig)) {
            return -1.0D;
        }
        kinds.put(next.asLong(), kind);
        return base + extra;
    }

    private boolean supported(ServerLevel level, Map<Long, Info> cells, BlockPos pos, boolean canDig) {
        // In water it swims: up under your boat as easily as along the bottom.
        if (this.info(level, cells, pos, canDig).cell() == Cell.WATER) {
            return true;
        }
        if (this.holds(level, cells, pos.below(), canDig) || this.holds(level, cells, pos.above(), canDig)) {
            return true;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (this.holds(level, cells, pos.relative(side), canDig)) {
                return true;
            }
        }
        return false;
    }

    private boolean holds(ServerLevel level, Map<Long, Info> cells, BlockPos pos, boolean canDig) {
        Cell cell = this.info(level, cells, pos, canDig).cell();
        return cell == Cell.SOLID || cell == Cell.BLOCKED || cell == Cell.DOOR || cell == Cell.LOW;
    }

    private Info info(ServerLevel level, Map<Long, Info> cells, BlockPos pos, boolean canDig) {
        return cells.computeIfAbsent(pos.asLong(), key -> this.classify(level, pos, canDig));
    }

    private Info classify(ServerLevel level, BlockPos pos, boolean canDig) {
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) {
            return new Info(Cell.BLOCKED, 0.0F, false);
        }
        BlockState state = level.getBlockState(pos);
        boolean lit = level.getBrightness(LightLayer.BLOCK, pos) >= HorrorConfig.SAFE_LIGHT_LEVEL.get();
        if (state.getFluidState().is(FluidTags.LAVA)) {
            return new Info(Cell.BLOCKED, 0.0F, lit);
        }
        if (isOpenable(state)) {
            if (state.getValue(BlockStateProperties.OPEN)) {
                return new Info(Cell.AIR, 0.0F, lit);
            }
            if (HorrorConfig.ALLOW_BLOCK_CHANGES.get() && opensByHand(state)) {
                return new Info(Cell.DOOR, 0.0F, lit);
            }
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return new Info(state.getFluidState().is(FluidTags.WATER) ? Cell.WATER : Cell.AIR, 0.0F, lit);
        }
        if (shape.max(Direction.Axis.Y) <= 0.6D && !isOpenable(state)) {
            // Slabs, carpets, snow: it goes over them.
            return new Info(Cell.LOW, 0.0F, lit);
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (!canDig || hardness < 0.0F || !ForgeEventFactory.onEntityDestroyBlock(this.one, pos, state)) {
            return new Info(Cell.BLOCKED, 0.0F, lit);
        }
        return new Info(Cell.SOLID, hardness, lit);
    }

    // ------------------------------------------------------------------ following

    /** The first door or block to deal with on the path ahead, within a few blocks. */
    @Nullable
    private Step nextAction(ServerLevel level, boolean canDig) {
        double length = 0.0D;
        Vec3 last = this.one.position();
        for (int i = this.index; i < this.path.size() && length < 6.0D; i++) {
            Step step = this.path.get(i);
            Vec3 at = Vec3.atBottomCenterOf(step.pos());
            length += at.distanceTo(last);
            last = at;
            if (step.kind() != Kind.WALK) {
                Info info = this.classify(level, step.pos(), canDig);
                if (step.kind() == Kind.OPEN && info.cell() == Cell.DOOR || step.kind() == Kind.DIG && info.cell() == Cell.SOLID) {
                    return step;
                }
            }
        }
        return null;
    }

    /** Once a door is open or a block is gone, that step is just walking. */
    private void markWalkable(Step step) {
        int i = this.path.indexOf(step);
        if (i >= 0) {
            List<Step> copy = new ArrayList<>(this.path);
            copy.set(i, new Step(step.pos(), Kind.WALK));
            this.path = copy;
        }
    }

    private boolean stillValid(ServerLevel level, Step step, boolean canDig) {
        Info info = this.classify(level, step.pos(), canDig);
        return switch (step.kind()) {
            case WALK -> info.walkable() || step.pos().equals(this.goal) && info.cell() != Cell.SOLID && info.cell() != Cell.BLOCKED;
            case OPEN -> info.cell() == Cell.DOOR || info.walkable();
            case DIG -> info.cell() == Cell.SOLID || info.walkable();
        };
    }

    /** Nothing but open space between {@code from} and the block at {@code cell}. */
    private boolean openUpTo(ServerLevel level, Vec3 from, BlockPos cell) {
        Vec3 delta = Vec3.atCenterOf(cell).subtract(from);
        int samples = Math.max(1, (int) Math.ceil(delta.length() / 0.3D));
        for (int i = 1; i < samples; i++) {
            BlockPos at = BlockPos.containing(from.add(delta.scale(i / (double) samples)));
            if (!at.equals(cell) && !this.classify(level, at, false).walkable()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Too close to the block it is about to tear at (it got there round a corner, or up a wall): it eases back,
     * still facing it, until its claws are at the block's face and no further. Otherwise they would come out on
     * your side before it has even started. @return true while it is still easing back
     */
    private boolean easeBack(ServerLevel level, Vec3 block, Vec3 hips, double speed) {
        double want = Burrow.REACH * this.one.getVisualScale() + 0.3D;
        Vec3 away = new Vec3(hips.x - block.x, 0.0D, hips.z - block.z);
        double have = away.length();
        if (have >= want - 0.35D || have < 1.0E-3D) {
            return false;
        }
        away = away.scale(1.0D / have);
        double room = 0.0D;
        for (double k = 0.25D; k <= want - have + 1.0E-6D; k += 0.25D) {
            if (!this.classify(level, BlockPos.containing(hips.add(away.scale(k))), false).walkable()) {
                break;
            }
            room = k;
        }
        if (room < 0.25D) {
            return false;
        }
        this.one.setCrawling(true);
        this.one.face(block);
        this.one.setDeltaMovement(away.scale(Math.min(room, speed * 0.6D)));
        return true;
    }

    /** Open and dark all the way, with a floor under it (or water to swim in): it can go straight across. */
    private boolean clearWalk(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int samples = Math.max(1, (int) Math.ceil(delta.length() / 0.3D));
        for (int i = 1; i <= samples; i++) {
            BlockPos cell = BlockPos.containing(from.add(delta.scale(i / (double) samples)));
            Info info = this.classify(level, cell, false);
            if (!info.walkable()) {
                return false;
            }
            Cell below = this.classify(level, cell.below(), false).cell();
            if (info.cell() != Cell.WATER && below != Cell.BLOCKED && below != Cell.LOW && below != Cell.DOOR) {
                return false;
            }
        }
        return true;
    }

    /** A door it opened, closed again once it is a few steps past (and nobody is standing in it). */
    private void closeDoorBehind(ServerLevel level) {
        if (this.doorToClose == null) {
            return;
        }
        BlockPos door = this.doorToClose;
        double dist = this.one.position().distanceTo(Vec3.atBottomCenterOf(door));
        if (dist < 2.8D) {
            return;
        }
        this.doorToClose = null;
        BlockState state = level.getBlockState(door);
        if (dist > 12.0D || !isOpenable(state) || !state.getValue(BlockStateProperties.OPEN)
                || !level.getEntities(null, new net.minecraft.world.phys.AABB(door).inflate(0.3D, 1.0D, 0.3D)).isEmpty()) {
            return;
        }
        Burrow.setOpen(level, door, state, false);
    }

    // ------------------------------------------------------------------ blocks

    static boolean isOpenable(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock || state.getBlock() instanceof TrapDoorBlock;
    }

    /** Wooden doors, gates and trapdoors: anything a hand can open. Iron ones it has to tear out. */
    static boolean opensByHand(BlockState state) {
        if (state.getBlock() instanceof DoorBlock door) {
            return door.type().canOpenByHand();
        }
        if (state.getBlock() instanceof TrapDoorBlock) {
            return !state.is(Blocks.IRON_TRAPDOOR);
        }
        return state.getBlock() instanceof FenceGateBlock;
    }

    private static BlockPos lowerHalf(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    /** Whether something solid stands where its body would be at {@code at} (for moves that never dig). */
    static boolean blocksBody(ServerLevel level, Vec3 at) {
        for (double up : new double[]{0.45D, 1.45D}) {
            BlockPos cell = BlockPos.containing(at.add(0.0D, up, 0.0D));
            BlockState state = level.getBlockState(cell);
            if (!state.getCollisionShape(level, cell).isEmpty() && !(isOpenable(state) && state.getValue(BlockStateProperties.OPEN))) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos feet(Vec3 at) {
        return BlockPos.containing(at.x, at.y + 0.05D, at.z);
    }

    private static double distance(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }
}
