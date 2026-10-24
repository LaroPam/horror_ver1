package com.laropam.cornerofeye.item;

import com.laropam.cornerofeye.entity.FlareEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Struck and thrown. Burns red where it lands, and that is one fire it cannot put out. */
public class FlareItem extends Item {
    public FlareItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.4F);
        if (!level.isClientSide) {
            FlareEntity flare = new FlareEntity(level, player);
            flare.setItem(stack);
            flare.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.1F, 1.0F);
            level.addFreshEntity(flare);
        }
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cornerofeye.flare.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
