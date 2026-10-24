package com.laropam.cornerofeye.item;

import com.laropam.cornerofeye.haunt.Doll;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A rag doll with a scrap of its shadow sewn in. While you carry it, it leaves twice the scraps, and it comes
 * for you more often. Throw it away and it is back in your pocket; lock it in a chest and it is back by
 * nightfall. Only fire gets rid of it (see Doll).
 */
public class CursedDollItem extends Item {
    public CursedDollItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player && level.getGameTime() % 20 == 0) {
            Doll.tickInInventory(stack, player);
        }
    }

    /** Nothing but fire hurts it. */
    @Override
    public boolean canBeHurtBy(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE);
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        Doll.burned(entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cornerofeye.cursed_doll.tooltip").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
