package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Implementations of {@link HauntEvent}. Each returns false when it can't happen right now. */
final class HauntEvents {
    private static final int WHISPER_LINES = 12;
    private static final int SAID_LINES = 6;

    static boolean distantSound(HauntContext c) {
        RandomSource random = c.random();
        double angle = random.nextDouble() * Math.PI * 2.0D;
        Vec3 at = c.player().position().add(Math.cos(angle) * 24.0D, random.nextInt(9) - 4, Math.sin(angle) * 24.0D);
        HorrorNetwork.playSoundTo(c.player(), ModSounds.DISTANT.get(), SoundSource.AMBIENT, at, 2.5F, 0.8F + random.nextFloat() * 0.2F);
        return true;
    }

    /** A light at the far edge of your base goes out. You probably just forgot to place it. */
    static boolean farLightOut(HauntContext c) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return false;
        }
        BlockPos center = c.player().blockPosition();
        double minDist = c.stage() == HauntData.SILENCE ? 16.0D : 10.0D;
        List<BlockPos> lights = Lights.findHidden(c.level(), center, 24, 8, false);
        lights.removeIf(pos -> pos.distSqr(center) < minDist * minDist);
        if (lights.isEmpty()) {
            return false;
        }
        Lights.snuff(c.level(), lights.get(c.random().nextInt(lights.size())));
        return true;
    }

    static boolean footsteps(HauntContext c) {
        if (c.player().isPassenger() || c.player().isInWater()) {
            return false;
        }
        HorrorNetwork.send(c.player(), HorrorPacket.Type.FOOTSTEPS, 4 + c.random().nextInt(6));
        return true;
    }

    static boolean mining(HauntContext c) {
        if (!c.underground() && !c.forced()) {
            return false;
        }
        RandomSource random = c.random();
        ServerLevel level = c.level();
        for (int i = 0; i < 20; i++) {
            BlockPos pos = c.player().blockPosition().offset(random.nextInt(25) - 12, random.nextInt(9) - 4, random.nextInt(25) - 12);
            if (pos.distSqr(c.player().blockPosition()) > 36 && level.getBlockState(pos).isSolidRender(level, pos)
                    && Sight.blockHiddenFromEveryone(level, pos)) {
                HorrorNetwork.send(c.player(), HorrorPacket.Type.MINING, pos, 2 + random.nextInt(3));
                return true;
            }
        }
        return false;
    }

    /** Knocks on a nearby door, or quietly opens it. */
    static boolean door(HauntContext c) {
        ServerLevel level = c.level();
        List<BlockPos> doors = new ArrayList<>();
        BlockPos center = c.player().blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-12, -3, -12), center.offset(12, 3, 12))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                doors.add(pos.immutable());
            }
        }
        if (doors.isEmpty()) {
            return false;
        }
        BlockPos door = doors.get(c.random().nextInt(doors.size()));
        BlockState state = level.getBlockState(door);
        if (HorrorConfig.ALLOW_BLOCK_CHANGES.get() && !state.getValue(DoorBlock.OPEN) && ((DoorBlock) state.getBlock()).type().canOpenByHand()
                && c.random().nextInt(3) == 0
                && Sight.blockHiddenFromEveryone(level, door) && Sight.blockHiddenFromEveryone(level, door.above())) {
            ((DoorBlock) state.getBlock()).setOpen(null, level, state, door, true);
        } else {
            HorrorNetwork.send(c.player(), HorrorPacket.Type.KNOCK, door, 0);
        }
        return true;
    }

    static boolean whisper(HauntContext c) {
        ServerPlayer player = c.player();
        Vec3 behind = player.getEyePosition().subtract(player.getViewVector(1.0F).scale(1.2D));
        HorrorNetwork.playSoundTo(player, ModSounds.WHISPER.get(), SoundSource.HOSTILE, behind, 0.7F, 0.9F + c.random().nextFloat() * 0.2F);
        if (HorrorConfig.ALLOW_FAKE_CHAT.get() && c.random().nextInt(3) == 0) {
            player.sendSystemMessage(Component.translatable("cornerofeye.whisper." + c.random().nextInt(WHISPER_LINES))
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        return true;
    }

    /** Lights around you go out, one after another, wherever you are not looking. */
    static boolean lightsOut(HauntContext c) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return false;
        }
        List<BlockPos> lights = Lights.findHidden(c.level(), c.player().blockPosition(), 12, 5, false);
        if (lights.isEmpty()) {
            return false;
        }
        int count = Math.min(lights.size(), 1 + c.random().nextInt(3));
        for (int i = 0; i < count; i++) {
            BlockPos pos = lights.get(i);
            HauntManager.schedule(i * (15 + c.random().nextInt(25)), () -> {
                if (Lights.isSnuffable(c.level().getBlockState(pos)) && Sight.blockHiddenFromEveryone(c.level(), pos)) {
                    Lights.snuff(c.level(), pos);
                }
            });
        }
        return true;
    }

    /**
     * Somebody says something in chat that only you can see. In co-op it is usually one of your friends,
     * who never typed it.
     */
    static boolean fakeChat(HauntContext c) {
        if (!HorrorConfig.ALLOW_FAKE_CHAT.get()) {
            return false;
        }
        ServerPlayer player = c.player();
        List<ServerPlayer> others = new ArrayList<>(player.server.getPlayerList().getPlayers());
        others.remove(player);
        ServerPlayer speaker = others.isEmpty() ? player : others.get(c.random().nextInt(others.size()));
        Component line = Component.translatable("cornerofeye.said." + c.random().nextInt(SAID_LINES));
        player.sendSystemMessage(Component.translatable("chat.type.text", speaker.getDisplayName(), line));
        return true;
    }

    /** A sign shows up somewhere behind you. */
    static boolean sign(HauntContext c) {
        if (!HorrorConfig.ALLOW_BLOCK_CHANGES.get()) {
            return false;
        }
        ServerLevel level = c.level();
        ServerPlayer player = c.player();
        Vec3 spot = SpotFinder.find(level, player, 6.0D, 14.0D, 150.0F, 30.0F, 4, 20,
                pos -> Sight.hiddenFromEveryone(level, pos.add(0.0D, 0.5D, 0.0D))
                        && level.getBlockState(BlockPos.containing(pos)).canBeReplaced() && !level.getBlockState(BlockPos.containing(pos)).is(Blocks.LIGHT));
        if (spot == null) {
            return false;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(spot.z - player.getZ(), spot.x - player.getX())) - 90.0F;
        return Signs.place(level, BlockPos.containing(spot), yaw, Signs.randomMessage(c.random()));
    }

    private HauntEvents() {
    }
}
