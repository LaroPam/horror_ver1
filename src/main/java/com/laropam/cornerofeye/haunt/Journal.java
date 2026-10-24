package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;

import java.util.Locale;

/**
 * Notes left by whoever lived here before you, found a few pages at a time. Each chapter turns up once the
 * player is ready for it (their first scrap, the lantern, the first catch), or after enough days if they are
 * stuck, so over time everyone learns how to drive it away for good.
 */
public final class Journal {
    private enum Chapter {
        /** How it behaves. */
        ARRIVAL(3),
        /** Where the scraps come from. */
        SCRAPS(3),
        /** The recipe. */
        LANTERN(1),
        /** How the lantern works. */
        FLASH(2),
        /** The moments and putting lights back on. */
        MOMENTS(2),
        /** Three times. */
        THREE(1),
        /** The seventh night. */
        HUNT(2);

        final int pages;

        Chapter(int pages) {
            this.pages = pages;
        }
    }

    private static final String TAG_CHAPTERS = "cornerofeye.chapters";
    private static final String TAG_SCRAP = "cornerofeye.seenScrap";
    private static final String TAG_LANTERN = "cornerofeye.madeLantern";
    private static final String TAG_LAST = "cornerofeye.lastPage";
    /** Never more than one new page this often (ticks of haunt time). */
    private static final long GAP = 600;

    /** Called every second for every player. Hands out at most one chapter at a time. */
    static void tick(ServerPlayer player, HauntData data, int stage) {
        if (stage < HauntData.WATCHING || !HorrorConfig.GIVE_JOURNAL.get()) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        boolean seenScrap = persisted.getBoolean(TAG_SCRAP) || count(player, ModItems.SHADOW_SCRAP.get()) > 0;
        boolean madeLantern = persisted.getBoolean(TAG_LANTERN) || count(player, ModItems.LANTERN_OF_REVELATION.get()) > 0;
        persisted.putBoolean(TAG_SCRAP, seenScrap);
        persisted.putBoolean(TAG_LANTERN, madeLantern);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        int delivered = persisted.getInt(TAG_CHAPTERS);
        long sinceLast = data.getTicks() - persisted.getLong(TAG_LAST);
        // (Negative if the haunt was reset since.)
        if (delivered != 0 && sinceLast >= 0 && sinceLast < GAP) {
            return;
        }
        long days = data.getDays();
        for (Chapter chapter : Chapter.values()) {
            if ((delivered & (1 << chapter.ordinal())) != 0) {
                continue;
            }
            boolean ready = switch (chapter) {
                case ARRIVAL -> true;
                case SCRAPS -> seenScrap || days >= HorrorConfig.DAYS_UNTIL_WATCHING.get() + 1;
                case LANTERN -> count(player, ModItems.SHADOW_SCRAP.get()) >= 2 || madeLantern || stage >= HauntData.APPROACHING;
                case FLASH -> madeLantern || days >= HorrorConfig.DAYS_UNTIL_APPROACHING.get() + 1;
                case MOMENTS -> stage >= HauntData.APPROACHING;
                case THREE -> data.getWounds() > 0 || stage >= HauntData.HUNTING;
                case HUNT -> stage >= HauntData.HUNTING;
            };
            if (!ready) {
                // Chapters come in order: the story doesn't skip ahead.
                return;
            }
            give(player, chapter);
            persisted.putInt(TAG_CHAPTERS, delivered | (1 << chapter.ordinal()));
            persisted.putLong(TAG_LAST, data.getTicks());
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            return;
        }
    }

    /** Forgets which pages this player has found, for /cornerofeye reset. */
    public static void reset(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.remove(TAG_CHAPTERS);
        persisted.remove(TAG_SCRAP);
        persisted.remove(TAG_LANTERN);
        persisted.remove(TAG_LAST);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static void give(ServerPlayer player, Chapter chapter) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, "...");
        tag.putString(WrittenBookItem.TAG_AUTHOR, "?");
        tag.putBoolean(WrittenBookItem.TAG_RESOLVED, true);
        ListTag pages = new ListTag();
        String key = "cornerofeye.journal." + chapter.name().toLowerCase(Locale.ROOT) + ".";
        for (int i = 0; i < chapter.pages; i++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(key + i))));
        }
        tag.put(WrittenBookItem.TAG_PAGES, pages);
        book.setHoverName(Component.translatable("cornerofeye.journal.title", chapter.ordinal() + 1).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.sendSystemMessage(Component.translatable(chapter == Chapter.ARRIVAL ? "cornerofeye.message.journal" : "cornerofeye.message.page")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static int count(Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private Journal() {
    }
}
