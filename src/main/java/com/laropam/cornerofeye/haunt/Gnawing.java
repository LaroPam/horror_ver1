package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.block.MarkBlock;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * It is in the wall right next to you. Scraping from inside the block, cracks creeping over it as if something were
 * clawing its way through... and then it stops, mid-stroke. Nothing comes out. Gouges stay on the stone, and the
 * cracks for a while. In the morning you may find the same on the wall by your bed.
 */
public final class Gnawing {
    /** Cracks stay on screen this long after it stops (the client forgets them unless reminded). */
    private static final int LINGER = 1200;

    /** Starts it on a block of wall a few steps from {@code player}, one they can see. @return false if there is none */
    public static boolean nearby(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();
        BlockPos feet = player.blockPosition();
        for (int attempt = 0; attempt < 80; attempt++) {
            Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos wall = feet.relative(side, 2 + random.nextInt(3)).relative(side.getClockWise(), random.nextInt(5) - 2).above(random.nextInt(2));
            Direction face = side.getOpposite();
            BlockPos front = wall.relative(face);
            BlockState state = level.getBlockState(wall);
            if (!state.isSolidRender(level, wall) || state.getDestroySpeed(level, wall) < 0.0F || !level.getBlockState(front).getCollisionShape(level, front).isEmpty()) {
                continue;
            }
            Vec3 center = Vec3.atCenterOf(wall);
            // On your screen, but not where you are looking: you notice the cracks before you notice the sound.
            double gaze = Sight.gazeDot(player, center);
            if (gaze < 0.68D || gaze > 0.95D || !Sight.clearLineToBlock(level, player, wall)) {
                continue;
            }
            gnaw(level, player, wall, face, 50 + random.nextInt(40), 5 + random.nextInt(3));
            return true;
        }
        return false;
    }

    /** In the morning: cracks and gouges on the wall right by the bed whose head is at {@code head}. */
    public static boolean byTheBed(ServerLevel level, BlockPos head) {
        BlockState bed = level.getBlockState(head);
        if (!(bed.getBlock() instanceof BedBlock)) {
            return false;
        }
        Direction facing = bed.getValue(BedBlock.FACING);
        BlockPos foot = head.relative(facing.getOpposite());
        // The wall behind the headboard, then the walls along either side of the bed.
        for (BlockPos part : new BlockPos[]{head, foot}) {
            for (Direction side : new Direction[]{facing, facing.getClockWise(), facing.getCounterClockWise()}) {
                if (part == foot && side == facing) {
                    continue;
                }
                for (int up = 0; up <= 1; up++) {
                    BlockPos wall = part.relative(side).above(up);
                    BlockPos front = part.above(up);
                    if (level.getBlockState(wall).isSolidRender(level, wall) && level.getBlockState(wall).getDestroySpeed(level, wall) >= 0.0F
                            && (up == 0 || level.getBlockState(front).isAir())) {
                        crack(level, wall, 6 + level.random.nextInt(3));
                        // Up the wall from there, claw marks over the cracks.
                        BlockPos higher = wall.above();
                        if (up == 0 && level.getBlockState(higher).isSolidRender(level, higher) && level.getBlockState(higher).getDestroySpeed(level, higher) >= 0.0F) {
                            crack(level, higher, 4 + level.random.nextInt(3));
                        }
                        Marks.markOn(level, part.above(up == 0 ? 1 : up), side.getOpposite(), MarkBlock.SCRATCH);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Scraping inside {@code wall} for {@code length} ticks while cracks spread to {@code depth} (of 9), then silence.
     * {@code face} is the side of the block you are on.
     */
    static void gnaw(ServerLevel level, ServerPlayer player, BlockPos wall, Direction face, int length, int depth) {
        BlockState state = level.getBlockState(wall);
        Vec3 inside = Vec3.atCenterOf(wall).subtract(Vec3.atLowerCornerOf(face.getNormal()).scale(0.3D));
        HorrorNetwork.send(player, HorrorPacket.Type.DREAD, 20);
        int id = breakerId(wall);
        for (int t = 0; t <= length; t += 6) {
            int at = t;
            HauntManager.schedule(at, () -> {
                if (level.getBlockState(wall) != state) {
                    return;
                }
                float pitch = 0.65F + level.random.nextFloat() * 0.2F;
                HorrorNetwork.playSoundTo(player, ModSounds.CLAW_SCRAPE.get(), SoundSource.HOSTILE, inside, 0.55F, pitch);
                if (at % 12 == 0) {
                    HorrorNetwork.playSoundTo(player, state.getSoundType().getHitSound(), SoundSource.BLOCKS, inside, 0.6F, 0.5F);
                }
                level.destroyBlockProgress(id, wall, Math.min(depth, at * (depth + 1) / Math.max(1, length)));
            });
        }
        HauntManager.schedule(length + 2, () -> {
            if (level.getBlockState(wall) != state) {
                level.destroyBlockProgress(id, wall, -1);
                return;
            }
            // It stopped. Right in the middle. Your heart doesn't.
            HorrorNetwork.send(player, HorrorPacket.Type.DREAD, 35);
            Marks.markOn(level, wall.relative(face), face, MarkBlock.SCRATCH);
            crack(level, wall, depth);
        });
    }

    /** Leaves cracks of {@code depth} on the block for a while, then lets them fade. */
    static void crack(ServerLevel level, BlockPos wall, int depth) {
        int id = breakerId(wall);
        BlockState state = level.getBlockState(wall);
        for (int t = 0; t < LINGER; t += 300) {
            HauntManager.schedule(t, () -> {
                if (level.getBlockState(wall) == state) {
                    level.destroyBlockProgress(id, wall, depth);
                }
            });
        }
        HauntManager.schedule(LINGER, () -> level.destroyBlockProgress(id, wall, -1));
    }

    /** Cracks are drawn per breaker: one made-up breaker per block, so they never mix with a player's own digging. */
    private static int breakerId(BlockPos pos) {
        return -1000 - (int) (pos.asLong() & 0x3FFFFFFFL);
    }

    private Gnawing() {
    }
}
