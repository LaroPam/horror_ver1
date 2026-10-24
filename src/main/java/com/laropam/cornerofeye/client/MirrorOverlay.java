package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.item.HandMirrorItem;
import com.laropam.cornerofeye.registry.ModItems;
import com.laropam.cornerofeye.registry.ModSounds;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Quaternionf;

/**
 * The hand mirror held up in front of you. It shows what is behind you, which is mostly darkness; when it is
 * back there, it is in the mirror, as plainly as it ever gets to be seen. The glass shows every crack it has.
 */
final class MirrorOverlay {
    private static boolean sawIt;

    static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !player.isUsingItem() || !player.getUseItem().is(ModItems.HAND_MIRROR.get())) {
            sawIt = false;
            return;
        }
        float raise = Math.min(1.0F, (player.getTicksUsingItem() + partialTick) / 6.0F);
        int h = (int) (height * 0.46F);
        int w = (int) (h * 0.7F);
        int x = width / 2 - w / 2;
        int y = height - 34 - (int) (h * raise);

        graphics.fill(x - 4, y - 4, x + w + 4, y + h + 4, 0xFF231E1A);
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF7A6F62);
        graphics.fillGradient(x, y, x + w, y + h, 0xFF131921, 0xFF05070A);
        graphics.enableScissor(x, y, x + w, y + h);
        boolean sees = false;
        TheOneEntity one = nearestBehind(player);
        if (one != null) {
            sees = drawIt(graphics, player, one, x, y, w, h, partialTick);
        }
        // Old, dim glass, with a streak of light across it.
        graphics.fill(x, y, x + w, y + h, 0x50000000);
        graphics.pose().pushPose();
        graphics.pose().translate(x + w * 0.2F, y, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(25.0F));
        graphics.fill(0, 0, w / 7, h * 2, 0x12FFFFFF);
        graphics.pose().popPose();
        int damage = player.getUseItem().getDamageValue();
        for (int i = 0; i < damage; i++) {
            Cracks.fromRandomEdge(RandomSource.create(7919L * (i + 1)), 0.5F + (i % 3) * 0.15F)
                    .draw(graphics, x, y, w, h, Float.MAX_VALUE);
        }
        graphics.disableScissor();

        if (sees && !sawIt) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.GLIMPSE.get(), 1.0F, 0.8F));
        }
        sawIt = sees;
    }

    private static TheOneEntity nearestBehind(LocalPlayer player) {
        TheOneEntity best = null;
        for (TheOneEntity one : player.level().getEntitiesOfClass(TheOneEntity.class, player.getBoundingBox().inflate(HandMirrorItem.RANGE))) {
            if (HandMirrorItem.isBehind(player, one) && (best == null || player.distanceToSqr(one) < player.distanceToSqr(best))) {
                best = one;
            }
        }
        return best;
    }

    /** Where it would be in a mirror held in front of your face: behind-left shows on the left. */
    private static boolean drawIt(GuiGraphics graphics, LocalPlayer player, TheOneEntity one, int x, int y, int w, int h, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 to = one.getPosition(partialTick).subtract(eye);
        float yaw = player.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        double behind = -(to.x * fx + to.z * fz);
        double left = to.x * fz - to.z * fx;
        if (behind < 0.5D) {
            return false;
        }
        double dist = to.length();
        int scale = (int) Mth.clamp(h * 0.8D / dist, 4.0D, h * 0.6D);
        int px = (int) (x + w / 2.0F - (float) (left / behind) * w * 0.9F);
        int horizon = y + (int) (h * 0.45F);
        int feet = horizon - (int) (to.y * scale);

        // Facing you, the way the inventory turns its model to the viewer; put back afterwards.
        float bodyRot = one.yBodyRot;
        float yRot = one.getYRot();
        float xRot = one.getXRot();
        float headRot = one.yHeadRot;
        float headRotO = one.yHeadRotO;
        one.yBodyRot = 180.0F;
        one.setYRot(180.0F);
        one.setXRot(0.0F);
        one.yHeadRot = 180.0F;
        one.yHeadRotO = 180.0F;
        InventoryScreen.renderEntityInInventory(graphics, px, feet, scale, new Quaternionf().rotateZ((float) Math.PI), null, one);
        one.yBodyRot = bodyRot;
        one.setYRot(yRot);
        one.setXRot(xRot);
        one.yHeadRot = headRot;
        one.yHeadRotO = headRotO;
        return true;
    }

    private MirrorOverlay() {
    }
}
