package com.laropam.cornerofeye.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Left behind where it stood. Crafting material and fuel for the Lantern of Revelation. */
public class ShadowScrapItem extends Item {
    public ShadowScrapItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cornerofeye.shadow_scrap.tooltip").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
