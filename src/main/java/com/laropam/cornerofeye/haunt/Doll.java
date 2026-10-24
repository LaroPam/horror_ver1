package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.registry.ModItems;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * The cursed doll and the player it has chosen. A doll is bound to whoever first carries it; the player
 * remembers the bond. Thrown away, it is back in their pocket within a minute; left anywhere else, a new one
 * is there at nightfall and the old one falls apart the moment anyone picks it up. Burning it breaks the bond.
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID)
public final class Doll {
    private static final String PLAYER_BOND = "cornerofeye.dollBond";
    private static final String STACK_BOND = "Bond";

    /** Whether {@code player} has a doll on them, anywhere in their inventory. */
    public static boolean carries(Player player) {
        for (List<ItemStack> list : List.of(player.getInventory().items, player.getInventory().offhand, player.getInventory().armor)) {
            for (ItemStack stack : list) {
                if (stack.is(ModItems.CURSED_DOLL.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean anyoneCarries(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().anyMatch(Doll::carries);
    }

    private static long bond(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getLong(PLAYER_BOND);
    }

    private static void setBond(Player player, long bond) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putLong(PLAYER_BOND, bond);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static long newBond(ServerPlayer player) {
        long bond;
        do {
            bond = player.getRandom().nextLong();
        } while (bond == 0L);
        return bond;
    }

    /** Once a second while a doll is in someone's inventory. */
    public static void tickInInventory(ItemStack stack, ServerPlayer player) {
        long mine = stack.getOrCreateTag().getLong(STACK_BOND);
        long theirs = bond(player);
        if (mine == 0L) {
            // A new doll: it is theirs now.
            if (theirs == 0L) {
                theirs = newBond(player);
                setBond(player, theirs);
            }
            stack.getOrCreateTag().putLong(STACK_BOND, theirs);
        } else if (theirs == 0L) {
            setBond(player, mine);
        } else if (mine != theirs) {
            // An old one, left behind somewhere. It only wants one of you.
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("cornerofeye.message.doll_crumbled").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            return;
        }
        if (Nights.isNightTime(player.level()) && player.getRandom().nextInt(150) == 0) {
            // Very quietly, right next to you, and only you hear it.
            HorrorNetwork.playSoundTo(player, ModSounds.WHISPER.get(), SoundSource.HOSTILE, player.position().add(0.0D, 0.5D, 0.0D), 0.35F, 1.6F);
        }
    }

    /** Thrown away: give it a minute. If it is still lying there, it isn't. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemEntity entity = event.getEntity();
        if (!entity.getItem().is(ModItems.CURSED_DOLL.get()) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        UUID id = player.getUUID();
        MinecraftServer server = player.getServer();
        HauntManager.schedule(600 + player.getRandom().nextInt(600), () -> {
            ServerPlayer owner = server.getPlayerList().getPlayer(id);
            if (owner != null && entity.isAlive()) {
                ItemStack doll = entity.getItem().copy();
                entity.discard();
                giveBack(owner, doll);
            }
        });
    }

    /** At nightfall a doll you got rid of without fire is back. */
    static void nightfall(ServerPlayer player) {
        if (bond(player) == 0L || carries(player)) {
            return;
        }
        long bond = newBond(player);
        setBond(player, bond);
        ItemStack doll = new ItemStack(ModItems.CURSED_DOLL.get());
        doll.getOrCreateTag().putLong(STACK_BOND, bond);
        giveBack(player, doll);
    }

    private static void giveBack(ServerPlayer player, ItemStack doll) {
        if (!player.getInventory().add(doll)) {
            // Straight at their feet, not through a toss (that would only start it all again).
            ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), doll);
            entity.setNoPickUpDelay();
            player.level().addFreshEntity(entity);
        }
        player.displayClientMessage(Component.translatable("cornerofeye.message.doll_back").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WHISPER.get(), SoundSource.HOSTILE, 0.5F, 1.5F);
    }

    /** In fire or lava. It screams; whoever it belonged to is free of it. */
    public static void burned(ItemEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.SCREAM.get(), SoundSource.HOSTILE, 0.9F, 1.7F);
        level.sendParticles(ParticleTypes.SOUL, entity.getX(), entity.getY() + 0.3D, entity.getZ(), 12, 0.2D, 0.3D, 0.2D, 0.02D);
        long bond = entity.getItem().getOrCreateTag().getLong(STACK_BOND);
        if (bond == 0L) {
            return;
        }
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (bond(player) == bond) {
                setBond(player, 0L);
                player.displayClientMessage(Component.translatable("cornerofeye.message.doll_burned").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }
    }

    private Doll() {
    }
}
