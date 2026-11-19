package com.laropam.cornerofeye.network;

import com.laropam.cornerofeye.CornerOfTheEye;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

public final class HorrorNetwork {
    private static final int PROTOCOL = 7;
    public static final SimpleChannel CHANNEL = ChannelBuilder.named(CornerOfTheEye.id("main"))
            .networkProtocolVersion(PROTOCOL)
            .acceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(HorrorPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HorrorPacket::encode)
                .decoder(HorrorPacket::decode)
                .consumerMainThread(HorrorPacket::handle)
                .add();
    }

    public static void send(ServerPlayer player, HorrorPacket.Type type, BlockPos pos, int data) {
        CHANNEL.send(new HorrorPacket(type, pos, data), PacketDistributor.PLAYER.with(player));
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
