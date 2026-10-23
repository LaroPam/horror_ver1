package com.laropam.cornerofeye.haunt;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;

/**
 * Notes left by whoever lived in this world before you. The only manual the player gets:
 * how it behaves, what it leaves behind and how to get rid of it.
 */
public final class Journal {
    private static final int PAGES = 10;

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, "...");
        tag.putString(WrittenBookItem.TAG_AUTHOR, "?");
        tag.putBoolean(WrittenBookItem.TAG_RESOLVED, true);
        ListTag pages = new ListTag();
        for (int i = 0; i < PAGES; i++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("cornerofeye.journal.page." + i))));
        }
        tag.put(WrittenBookItem.TAG_PAGES, pages);
        book.setHoverName(Component.translatable("cornerofeye.journal.title").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return book;
    }

    private Journal() {
    }
}
