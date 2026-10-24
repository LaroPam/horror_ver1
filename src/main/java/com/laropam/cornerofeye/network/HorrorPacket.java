package com.laropam.cornerofeye.network;

import com.laropam.cornerofeye.client.ClientHorror;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: effects that only exist on one player's screen or in their headphones. */
public record HorrorPacket(Type type, BlockPos pos, int data) {
    public enum Type {
        /** data = haunt stage, or {@link #DEFEATED} once it is gone for good. */
        SYNC_STAGE,
        /** data = number of steps. */
        FOOTSTEPS,
        /** pos = block being "mined". */
        MINING,
        /** pos = door being knocked on. */
        KNOCK,
        /** data = duration in ticks. */
        JUMPSCARE,
        /** data = brightness, 0-100. */
        FLASH,
        /** A sudden jolt of fear, e.g. a light going out right next to you. data = 0-100. */
        DREAD,
        /** A countdown appears: data = {@link com.laropam.cornerofeye.entity.Challenge} id, pos.x = length in ticks. */
        CHALLENGE,
        /** The countdown is over: data = 1 survived, 0 failed, 2 cancelled. */
        CHALLENGE_END,
        /** A light it put out (data = 1), lit again (0), or forget them all (2). */
        SMOTHER,
        /** Nightfall card: data = night number, pos.x = 1 under a blood moon. */
        NIGHT_CARD,
        /** data = 1 while the moon is red. */
        BLOOD_MOON
    }

    public static final int DEFEATED = 99;

    public static void encode(HorrorPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.data);
    }

    public static HorrorPacket decode(FriendlyByteBuf buf) {
        return new HorrorPacket(buf.readEnum(Type.class), buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(HorrorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHorror.handle(msg));
    }
}
