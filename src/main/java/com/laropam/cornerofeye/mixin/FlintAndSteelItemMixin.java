package com.laropam.cornerofeye.mixin;

import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Flint and steel is worn out after a few strikes: with it around, a light you can strike again is worth something,
 * and so is the steel.
 */
@Mixin(FlintAndSteelItem.class)
public abstract class FlintAndSteelItemMixin {
    /** Overrides Forge's per-stack maximum (IForgeItem#getMaxDamage), which every durability check goes through. */
    public int getMaxDamage(ItemStack stack) {
        return 4;
    }
}
