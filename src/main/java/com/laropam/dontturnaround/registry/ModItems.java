package com.laropam.dontturnaround.registry;

import com.laropam.dontturnaround.DontTurnAround;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DontTurnAround.MODID);

    public static final RegistryObject<Item> FACELESS_SPAWN_EGG = ITEMS.register("faceless_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FACELESS, 0x0E0E12, 0xD9D3C7, new Item.Properties()));

    private ModItems() {
    }
}
