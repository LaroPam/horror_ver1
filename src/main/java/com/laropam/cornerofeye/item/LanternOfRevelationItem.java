package com.laropam.cornerofeye.item;

import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModItems;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Hold to charge, release to flash. If it is anywhere on your screen when the light goes off,
 * the corner of your eye included, it is caught. Every flash burns one Shadow Scrap.
 */
public class LanternOfRevelationItem extends Item {
    public static final int CHARGE_TICKS = 30;
    private static final int COOLDOWN = 200;

    public LanternOfRevelationItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild && findFuel(player) == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.cornerofeye.lantern_of_revelation.no_fuel")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), ModSounds.LANTERN_CHARGE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide && this.getUseDuration(stack) - remaining == CHARGE_TICKS) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.getUseDuration(stack) - remaining < CHARGE_TICKS) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            ItemStack fuel = findFuel(player);
            if (fuel == null) {
                return;
            }
            fuel.shrink(1);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        flash(serverLevel, player);
    }

    private static void flash(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.LANTERN_FLASH.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        for (ServerPlayer other : level.players()) {
            double dist = other.distanceTo(player);
            if (dist < 32.0D) {
                HorrorNetwork.send(other, HorrorPacket.Type.FLASH, (int) (100 * (1.0D - dist / 32.0D)));
            }
        }

        TheOneEntity one = HauntManager.findTheOne(level);
        if (one == null) {
            return;
        }
        // Once the hunt begins it stays further out of reach.
        double radius = one.getStage() >= HauntData.HUNTING ? 12.0D : 20.0D;
        if (one.isCaughtBy(player, radius)) {
            one.reveal(player);
        } else if (one.distanceTo(player) < radius * 1.5D) {
            one.scareAway();
        }
    }

    @Nullable
    private static ItemStack findFuel(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.SHADOW_SCRAP.get())) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.SHADOW_SCRAP.get())) {
                return stack;
            }
        }
        return null;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cornerofeye.lantern_of_revelation.tooltip.0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.cornerofeye.lantern_of_revelation.tooltip.1").withStyle(ChatFormatting.DARK_GRAY));
    }
}
