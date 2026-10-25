package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.CornerOfTheEye;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * It learns from you. Whatever keeps you alive, it remembers, and stops letting it work:
 * <ul>
 *     <li>you hide in the light: it puts lights out sooner and from further away;</li>
 *     <li>you get up high where it can't climb: it waits below for longer, and comes straight to the foot of it;</li>
 *     <li>you always sleep in the same bed: it comes to that bed;</li>
 *     <li>you stare it down: it keeps behind you.</li>
 * </ul>
 * Each habit is a 0-1 score kept with the player (through death too). It fades a little every night, so whatever
 * you stop doing it slowly forgets. When a habit gets strong, nightfall tells you it has noticed.
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID)
public final class Habits {
    public enum Habit {
        LIGHT, HEIGHT, BED, GAZE;

        String key() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    private static final String TAG = "CornerOfEyeHabits";
    /** A habit this strong is one it has noticed. */
    public static final float NOTICED = 0.5F;

    private static CompoundTag tag(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag habits = persisted.getCompound(TAG);
        persisted.put(TAG, habits);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return habits;
    }

    /** How strong this habit is, 0-1. */
    public static float get(Player player, Habit habit) {
        return tag(player).getFloat(habit.key());
    }

    /** Sets a habit outright, for testing. */
    public static void set(Player player, Habit habit, float value) {
        tag(player).putFloat(habit.key(), Math.max(0.0F, Math.min(1.0F, value)));
    }

    /** Forgets everything it learned about {@code player}: a fresh start. */
    public static void forget(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.remove(TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Saw you do it again. */
    public static void note(Player player, Habit habit, float amount) {
        CompoundTag tag = tag(player);
        tag.putFloat(habit.key(), Math.min(1.0F, tag.getFloat(habit.key()) + amount));
    }

    /**
     * Nightfall: everything fades a little, and the strongest habit it has newly noticed is put to you, once, as
     * something it says.
     */
    static void nightfall(ServerPlayer player) {
        CompoundTag tag = tag(player);
        Habit strongest = null;
        for (Habit habit : Habit.values()) {
            float value = tag.getFloat(habit.key()) * 0.85F;
            tag.putFloat(habit.key(), value);
            boolean told = tag.getBoolean(habit.key() + "Told");
            if (value < NOTICED * 0.6F && told) {
                // Forgotten; if you take it up again, it will notice again.
                tag.remove(habit.key() + "Told");
            } else if (value >= NOTICED && !told && (strongest == null || value > tag.getFloat(strongest.key()))) {
                strongest = habit;
            }
        }
        if (strongest != null) {
            tag.putBoolean(strongest.key() + "Told", true);
            player.sendSystemMessage(Component.translatable("cornerofeye.learned." + strongest.key())
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }

    /** Woke up in a bed: if it is the same one as last time, that is a habit. */
    static void sleptIn(ServerPlayer player, BlockPos bed) {
        CompoundTag tag = tag(player);
        long last = tag.getLong("LastBed");
        if (tag.contains("LastBed") && last == bed.asLong()) {
            note(player, Habit.BED, 0.18F);
        }
        tag.putLong("LastBed", bed.asLong());
    }

    /** Every bright light you put down. */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getPlacedBlock().getLightEmission(event.getLevel(), event.getPos()) >= 10) {
            note(player, Habit.LIGHT, 0.006F);
        }
    }

    private Habits() {
    }
}
