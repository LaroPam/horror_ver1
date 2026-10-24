package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.block.WritingBlock;
import com.laropam.cornerofeye.block.WritingBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Words in blood on a wall, written by something that has only seen writing, never done it: letters lurch
 * up and down, change size, some are upside down, and the blood runs down from them.
 */
public class WritingRenderer implements BlockEntityRenderer<WritingBlockEntity> {
    private static final float SCALE = 0.03F;
    private final Font font;

    public WritingRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(WritingBlockEntity writing, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction facing = writing.getBlockState().getValue(WritingBlock.FACING);
        RandomSource random = RandomSource.create(writing.getSeed());
        List<String> lines = lines(mangle(I18n.get("cornerofeye.writing." + writing.getMessage()), random));

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        // On the wall's surface, facing out of it.
        poseStack.translate(0.0D, 0.15D * lines.size(), -0.49D);
        poseStack.mulPose(Axis.ZP.rotationDegrees((random.nextFloat() - 0.5F) * 10.0F));
        poseStack.scale(SCALE, -SCALE, SCALE);

        float y = -lines.size() * 6.0F;
        for (String line : lines) {
            float x = -this.font.width(line) / 2.0F;
            for (int i = 0; i < line.length(); i++) {
                String letter = String.valueOf(line.charAt(i));
                float width = this.font.width(letter) * (0.9F + random.nextFloat() * 0.4F);
                if (!letter.isBlank()) {
                    this.letter(poseStack, buffers, packedLight, random, letter, x, y, width);
                }
                x += width + random.nextFloat() * 1.2F;
            }
            y += 13.0F + random.nextFloat() * 3.0F;
        }
        poseStack.popPose();
    }

    private void letter(PoseStack poseStack, MultiBufferSource buffers, int light, RandomSource random, String letter, float x, float y, float width) {
        int red = 0x78 + random.nextInt(0x30);
        int color = 0xFF000000 | red << 16 | random.nextInt(10) << 8 | random.nextInt(10);
        float size = width / this.font.width(letter);
        poseStack.pushPose();
        poseStack.translate(x + width / 2.0F, y + (random.nextFloat() - 0.5F) * 3.5F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees((random.nextFloat() - 0.5F) * 18.0F));
        poseStack.scale(size, size, 1.0F);
        float half = this.font.width(letter) / 2.0F;
        poseStack.pushPose();
        if (random.nextFloat() < 0.07F) {
            // Now and then a letter is upside down. (Mirroring would turn it away from the viewer and cull it.)
            poseStack.translate(0.0F, 3.5F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.translate(0.0F, -3.5F, 0.0F);
        }
        this.font.drawInBatch(letter, -half, 0.0F, color, false, poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);
        poseStack.popPose();
        if (random.nextFloat() < 0.4F) {
            // The blood runs down from it.
            poseStack.translate(random.nextFloat() * half - half / 2.0F, 5.0F, 0.0F);
            poseStack.scale(1.0F, 0.6F + random.nextFloat() * 1.2F, 1.0F);
            int darker = 0xFF000000 | (red * 4 / 5) << 16;
            this.font.drawInBatch("|", -0.5F, 0.0F, darker, false, poseStack.last().pose(), buffers,
                    Font.DisplayMode.POLYGON_OFFSET, 0, light);
        }
        poseStack.popPose();
    }

    /** Case that flips at random, and a letter now and then said twice. */
    private static String mangle(String text, RandomSource random) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            String s = String.valueOf(c);
            out.append(random.nextFloat() < 0.3F ? (Character.isUpperCase(c) ? s.toLowerCase(Locale.ROOT) : s.toUpperCase(Locale.ROOT)) : s);
            if (Character.isLetter(c) && random.nextFloat() < 0.06F) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** One line, or two split near the middle. */
    private static List<String> lines(String text) {
        List<String> lines = new ArrayList<>();
        if (text.length() <= 16) {
            lines.add(text);
            return lines;
        }
        int split = text.lastIndexOf(' ', text.length() / 2 + 3);
        if (split <= 0) {
            split = text.indexOf(' ');
        }
        if (split <= 0) {
            lines.add(text);
        } else {
            lines.add(text.substring(0, split));
            lines.add(text.substring(split + 1));
        }
        return lines;
    }

    @Override
    public boolean shouldRenderOffScreen(WritingBlockEntity writing) {
        return true;
    }
}
