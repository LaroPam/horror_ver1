package com.laropam.cornerofeye.network;

import com.laropam.cornerofeye.CornerOfTheEye;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

public final class HorrorNetwork {
    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CornerOfTheEye.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.messageBuilder(HorrorPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HorrorPacket::encode)
                .decoder(HorrorPacket::decode)
                .consumerMainThread(HorrorPacket::handle)
                .add();
    }

    public static void send(ServerPlayer player, HorrorPacket.Type type, BlockPos pos, int data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HorrorPacket(type, pos, data));
    }

    public static void send(ServerPlayer player, HorrorPacket.Type type, int data) {
        send(player, type, BlockPos.ZERO, data);
    }

    /** Plays a sound that only this player can hear. */
    public static void playSoundTo(ServerPlayer player, SoundEvent sound, SoundSource source, Vec3 at, float volume, float pitch) {
        Holder<SoundEvent> holder = ForgeRegistries.SOUND_EVENTS.getHolder(sound).orElseGet(() -> Holder.direct(sound));
        player.connection.send(new ClientboundSoundPacket(holder, source, at.x, at.y, at.z, volume, pitch,
                player.getRandom().nextLong()));
    }

    private HorrorNetwork() {
    }
}
