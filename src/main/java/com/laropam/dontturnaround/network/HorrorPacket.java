package com.laropam.dontturnaround.network;

import com.laropam.dontturnaround.client.ClientHorror;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server → client: tells the client to play one of the purely client-side scares. */
public record HorrorPacket(Type type, BlockPos pos, int data) {
    public enum Type {
        /** data = current haunt stage. */
        SYNC_STAGE,
        /** data = number of steps. */
        FOOTSTEPS,
        /** pos = block being "mined". */
        MINING,
        /** pos = door being knocked on. */
        KNOCK,
        /** data = duration in ticks. */
        JUMPSCARE,
        /** data = duration in ticks. */
        GLITCH
    }

    public HorrorPacket(Type type, int data) {
        this(type, BlockPos.ZERO, data);
    }

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
