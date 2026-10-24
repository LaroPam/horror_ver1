package com.laropam.cornerofeye.item;

import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Hold it up and look at what is behind you. It is the one way to see it straight on, but only over your
 * shoulder, and every time it turns up in there the glass cracks a little more (MirrorOverlay draws the mirror).
 */
public class HandMirrorItem extends Item {
    /** How far behind you the mirror shows. */
    public static final double RANGE = 32.0D;
    /** Whether it was in each player's mirror at the last look, so each sighting cracks it only once. */
    private static final Map<Player, Boolean> IN_MIRROR = new WeakHashMap<>();

    public HandMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (user instanceof Player player) {
            IN_MIRROR.remove(player);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player) || remaining % 5 != 0) {
            return;
        }
        TheOneEntity one = HauntManager.findTheOne(server);
        boolean seen = one != null && isBehind(player, one);
        boolean before = Boolean.TRUE.equals(IN_MIRROR.put(player, seen));
        if (seen && !before) {
            level.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(), ModSounds.CRACK.get(), SoundSource.PLAYERS, 0.5F, 1.4F);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
        }
    }

    /** Close enough and in the half of the world behind you, where a mirror held up in front of your face looks. */
    public static boolean isBehind(Player player, TheOneEntity one) {
        Vec3 to = one.getEyePosition().subtract(player.getEyePosition());
        double dist = to.length();
        return dist < RANGE && dist > 0.01D && to.normalize().dot(player.getLookAngle()) < -0.35D;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cornerofeye.hand_mirror.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
