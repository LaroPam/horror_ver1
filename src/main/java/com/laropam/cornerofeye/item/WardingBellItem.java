package com.laropam.cornerofeye.item;

import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.haunt.Nights;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A little bell on a string. In the hotbar or the off hand it rings when it is near, faster the closer it
 * gets, and everyone around hears it. Once the hunt is on it sometimes rings at night when nothing is there.
 * Nothing you can see, anyway.
 */
public class WardingBellItem extends Item {
    private static final double RANGE = 24.0D;
    /** Game time of each player's next ring, however many bells they carry. */
    private static final Map<Player, Long> NEXT_RING = new WeakHashMap<>();

    public WardingBellItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // Slots 0-8 are the hotbar (and the off hand is slot 0 of its own list).
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player) || slot >= 9 || level.getGameTime() % 2 != 0) {
            return;
        }
        long now = level.getGameTime();
        if (now < NEXT_RING.getOrDefault(player, 0L)) {
            return;
        }
        TheOneEntity one = HauntManager.findTheOne(server);
        double dist = one == null ? Double.MAX_VALUE : one.distanceTo(player);
        HauntData data = HauntData.get(server.getServer());
        if (dist < RANGE) {
            float closeness = (float) (1.0D - dist / RANGE);
            ring(server, player, 0.45F + closeness * 0.55F, 1.0F + closeness * 0.25F);
            NEXT_RING.put(player, now + (long) Mth.lerp(closeness, 50.0F, 6.0F));
        } else if (data.getStage() >= HauntData.HUNTING && !data.isBanished() && Nights.isNightTime(level)
                && server.random.nextInt(1200) == 0) {
            // Nothing there. Probably.
            ring(server, player, 0.25F, 0.85F);
            NEXT_RING.put(player, now + 80);
        } else {
            NEXT_RING.put(player, now + 10);
        }
    }

    private static void ring(ServerLevel level, Player player, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(), ModSounds.WARDING_BELL.get(), SoundSource.PLAYERS,
                volume, pitch * (0.97F + level.random.nextFloat() * 0.06F));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cornerofeye.warding_bell.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
