package com.laropam.cornerofeye.registry;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.item.CursedDollItem;
import com.laropam.cornerofeye.item.FlareItem;
import com.laropam.cornerofeye.item.HandMirrorItem;
import com.laropam.cornerofeye.item.LanternOfRevelationItem;
import com.laropam.cornerofeye.item.ShadowScrapItem;
import com.laropam.cornerofeye.item.WardingBellItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CornerOfTheEye.MODID);

    public static final RegistryObject<Item> SHADOW_SCRAP = ITEMS.register("shadow_scrap",
            () -> new ShadowScrapItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> LANTERN_OF_REVELATION = ITEMS.register("lantern_of_revelation",
            () -> new LanternOfRevelationItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> UNLIT_TORCH = ITEMS.register("unlit_torch",
            () -> new StandingAndWallBlockItem(ModBlocks.UNLIT_TORCH.get(), ModBlocks.UNLIT_WALL_TORCH.get(), new Item.Properties(), Direction.DOWN));
    public static final RegistryObject<Item> BELL_STRING = ITEMS.register("bell_string",
            () -> new BlockItem(ModBlocks.BELL_STRING.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("block.cornerofeye.bell_string.tooltip").withStyle(ChatFormatting.GRAY));
                }
            });
    public static final RegistryObject<Item> WARDING_BELL = ITEMS.register("warding_bell",
            () -> new WardingBellItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HAND_MIRROR = ITEMS.register("hand_mirror",
            () -> new HandMirrorItem(new Item.Properties().durability(6)));
    public static final RegistryObject<Item> FLARE = ITEMS.register("flare",
            () -> new FlareItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> CURSED_DOLL = ITEMS.register("cursed_doll",
            () -> new CursedDollItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> THE_ONE_SPAWN_EGG = ITEMS.register("the_one_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.THE_ONE, 0xC9C3B8, 0x0A0A0C, new Item.Properties()));

    private ModItems() {
    }
}
