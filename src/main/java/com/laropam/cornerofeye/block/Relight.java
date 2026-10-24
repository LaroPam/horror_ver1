package com.laropam.cornerofeye.block;

import com.laropam.cornerofeye.haunt.LightSuppression;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Lighting a burnt-out torch again: it takes flint and steel or a fire charge. */
final class Relight {
    static InteractionResult torch(Level level, BlockPos pos, Player player, InteractionHand hand, BlockState lit) {
        ItemStack stack = player.getItemInHand(hand);
        boolean flint = stack.is(Items.FLINT_AND_STEEL);
        if (!flint && !stack.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (LightSuppression.isLocked(level, pos)) {
            player.displayClientMessage(Component.translatable("cornerofeye.message.cant_relight").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.FAIL;
        }
        level.setBlock(pos, lit, Block.UPDATE_ALL);
        if (flint) {
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        } else {
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.CONSUME;
    }

    private Relight() {
    }
}
