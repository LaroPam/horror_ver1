package com.laropam.cornerofeye.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Lighting a burnt-out torch again: it takes flint and steel or a fire charge. */
final class Relight {
    /**
     * Flint on a torch it put out. The wick is gone: nothing lights it again. Only a new torch is light, so every
     * torch it takes is one torch fewer.
     */
    static InteractionResult torch(Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
            player.displayClientMessage(Component.translatable("cornerofeye.message.dead_torch").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private Relight() {
    }
}
