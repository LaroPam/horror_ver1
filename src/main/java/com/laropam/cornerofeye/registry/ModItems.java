package com.laropam.cornerofeye.registry;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.item.LanternOfRevelationItem;
import com.laropam.cornerofeye.item.ShadowScrapItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CornerOfTheEye.MODID);

    public static final RegistryObject<Item> SHADOW_SCRAP = ITEMS.register("shadow_scrap",
            () -> new ShadowScrapItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> LANTERN_OF_REVELATION = ITEMS.register("lantern_of_revelation",
            () -> new LanternOfRevelationItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> THE_ONE_SPAWN_EGG = ITEMS.register("the_one_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.THE_ONE, 0xC9C3B8, 0x0A0A0C, new Item.Properties()));

    private ModItems() {
    }
}
