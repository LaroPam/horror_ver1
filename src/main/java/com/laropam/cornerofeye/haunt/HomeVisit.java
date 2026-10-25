package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.block.MarkBlock;
import com.laropam.cornerofeye.block.UnlitTorchBlock;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * While you are away, it goes into your home. Nothing tells you; you find out when you come back. The doors you shut
 * are open. The torches are off the walls and stand burnt out in a row at the foot of your bed. One thing from your
 * chest lies on your pillow. There are claw marks over the headboard, and handprints on the floor from the door to
 * the bed.
 */
public final class HomeVisit {
    private static final String LAST = "CornerOfEyeHomeVisit";
    private static final String UNSEEN = "CornerOfEyeHomeUnseen";
    /** At least this many haunt ticks between visits to the same player's home. */
    private static final long BETWEEN = 3L * 24000L;

    /** Called once a second for every player. */
    static void tick(ServerPlayer player, HauntData data, int stage) {
        if (stage < HauntData.APPROACHING || !HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return;
        }
        BlockPos bed = bedOf(player);
        if (bed == null) {
            return;
        }
        // Kept through death, like everything else it knows about you.
        CompoundTag tag = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, tag);
        double dx = player.getX() - bed.getX() - 0.5D;
        double dz = player.getZ() - bed.getZ() - 0.5D;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (tag.getBoolean(UNSEEN)) {
            if (dist < 7.0D && player.level().dimension() == Level.OVERWORLD) {
                // Home. Something is wrong, and your heart knows before you do.
                tag.remove(UNSEEN);
                HorrorNetwork.send(player, HorrorPacket.Type.DREAD, 45);
            }
            return;
        }
        // Always the same bed: it knows where you live, and goes there more often.
        float sameBed = Habits.get(player, Habits.Habit.BED);
        if (dist < 48.0D || dist > 160.0D || tag.contains(LAST) && data.getTicks() - tag.getLong(LAST) < BETWEEN * (1.0F - 0.6F * sameBed)) {
            return;
        }
        // About once in a quarter of an hour of being away.
        if (player.getRandom().nextInt((int) (900 / (1.0F + 2.0F * sameBed))) != 0) {
            return;
        }
        if (visit(player.server.overworld(), bed)) {
            tag.putLong(LAST, data.getTicks());
            tag.putBoolean(UNSEEN, true);
        }
    }

    /** The head of the bed the player sleeps in, if it is still there. */
    @Nullable
    public static BlockPos bedOf(ServerPlayer player) {
        BlockPos pos = player.getRespawnPosition();
        if (pos == null || player.getRespawnDimension() != Level.OVERWORLD) {
            return null;
        }
        ServerLevel level = player.server.overworld();
        if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof BedBlock)) {
            return null;
        }
        return pos;
    }

    /** Goes through the home around the bed whose head is at {@code head}. @return false if nobody's home was touched */
    public static boolean visit(ServerLevel level, BlockPos head) {
        BlockState bed = level.getBlockState(head);
        if (!(bed.getBlock() instanceof BedBlock) || !HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return false;
        }
        Vec3 home = Vec3.atCenterOf(head);
        if (level.getNearestPlayer(home.x, home.y, home.z, 24.0D, false) != null || !level.isAreaLoaded(head, 15)) {
            // Someone is home (or the house isn't all there: it doesn't load the world to get in).
            return false;
        }
        Direction facing = bed.getValue(BedBlock.FACING);
        BlockPos foot = head.relative(facing.getOpposite());
        RandomSource random = level.random;
        int done = 0;

        BlockPos door = null;
        for (BlockPos pos : near(level, head, 14, 3, state -> state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                && !state.getValue(DoorBlock.OPEN) && ((DoorBlock) state.getBlock()).type().canOpenByHand())) {
            if (door == null) {
                door = pos;
            }
            BlockState state = level.getBlockState(pos);
            ((DoorBlock) state.getBlock()).setOpen(null, level, state, pos, true);
            done++;
        }

        // The torches, off the walls and burnt out, standing in a row at the foot of the bed.
        List<BlockPos> row = row(level, foot, facing, random);
        List<BlockPos> torches = Lights.findAll(level, head, 10, 4, false);
        torches.removeIf(pos -> !Lights.isTorch(level.getBlockState(pos)));
        for (int i = 0; i < Math.min(torches.size(), row.size()); i++) {
            BlockState torch = level.getBlockState(torches.get(i));
            boolean soul = torch.is(Blocks.SOUL_TORCH) || torch.is(Blocks.SOUL_WALL_TORCH);
            level.setBlock(torches.get(i), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(row.get(i), ModBlocks.UNLIT_TORCH.get().defaultBlockState().setValue(UnlitTorchBlock.SOUL, soul), Block.UPDATE_ALL);
            done++;
        }

        // One thing out of your chest, on your pillow.
        ItemStack taken = ItemStack.EMPTY;
        for (BlockPos pos : near(level, head, 10, 3, state -> state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL))) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (!(entity instanceof ChestBlockEntity) && !(entity instanceof BarrelBlockEntity)) {
                continue;
            }
            Container container = (Container) entity;
            // Something of yours rather than just something: a blade, a tool, armour, if there is one.
            List<Integer> filled = new ArrayList<>();
            List<Integer> personal = new ArrayList<>();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty()) {
                    filled.add(slot);
                    if (stack.getMaxStackSize() == 1) {
                        personal.add(slot);
                    }
                }
            }
            if (!personal.isEmpty()) {
                filled = personal;
            }
            if (!filled.isEmpty()) {
                taken = container.removeItem(filled.get(random.nextInt(filled.size())), 1);
                container.setChanged();
                break;
            }
        }
        if (!taken.isEmpty()) {
            ItemEntity item = new ItemEntity(level, head.getX() + 0.5D, head.getY() + 0.6D, head.getZ() + 0.5D, taken);
            item.setDeltaMovement(Vec3.ZERO);
            item.setUnlimitedLifetime();
            level.addFreshEntity(item);
            done++;
        }

        // Claws dragged down the wall over the headboard.
        BlockPos above = head.above();
        if (Marks.markOn(level, above, facing.getOpposite(), MarkBlock.SCRATCH)) {
            Marks.markOn(level, above.above(), facing.getOpposite(), MarkBlock.SCRATCH);
            done++;
        }

        // Its hands on the floor, from the door to the bed.
        if (door != null) {
            Vec3 from = Vec3.atBottomCenterOf(door);
            Vec3 to = Vec3.atBottomCenterOf(foot);
            double length = from.distanceTo(to);
            for (double d = 1.5D; d < length - 1.0D; d += 1.4D + random.nextDouble() * 0.6D) {
                BlockPos at = BlockPos.containing(from.lerp(to, d / length));
                if (Marks.markOn(level, at, Direction.UP, random.nextInt(4) == 0 ? MarkBlock.SMEAR : MarkBlock.HAND)) {
                    done++;
                }
            }
        }
        return done > 0;
    }

    /** Floor spots in front of the foot of the bed and either side of it, for the torches it stands up. */
    private static List<BlockPos> row(ServerLevel level, BlockPos foot, Direction facing, RandomSource random) {
        List<BlockPos> row = new ArrayList<>();
        BlockPos front = foot.relative(facing.getOpposite());
        Direction side = facing.getClockWise();
        for (int k : new int[]{0, 1, -1, 2, -2, 3, -3}) {
            BlockPos pos = front.relative(side, k);
            if (level.getBlockState(pos).isAir() && ModBlocks.UNLIT_TORCH.get().defaultBlockState().canSurvive(level, pos)) {
                row.add(pos);
            }
        }
        // Not in a neat line from one end: from the middle outwards.
        row.sort(Comparator.comparingInt(pos -> pos.distManhattan(front)));
        return row;
    }

    private static List<BlockPos> near(ServerLevel level, BlockPos center, int radius, int height, java.util.function.Predicate<BlockState> what) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -height, -radius), center.offset(radius, height, radius))) {
            if (what.test(level.getBlockState(pos))) {
                found.add(pos.immutable());
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return found;
    }

    private HomeVisit() {
    }
}
