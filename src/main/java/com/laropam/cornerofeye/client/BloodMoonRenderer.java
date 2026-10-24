package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** A red sky and a swollen red moon over the vanilla one. */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID, value = Dist.CLIENT)
public final class BloodMoonRenderer {
    private static final ResourceLocation MOON = CornerOfTheEye.id("textures/environment/blood_moon.png");

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        float partialTick = event.getPartialTick();
        float amount = ClientHorror.bloodMoonAmount(partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        if (amount < 0.01F || minecraft.level == null) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        // Wash the whole sky red.
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f pose = poseStack.last().pose();
        int alpha = (int) (amount * 95.0F);
        float s = 80.0F;
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float[][] faces = {
                {-s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s}, {-s, -s, s, -s, s, s, s, s, s, s, -s, s},
                {-s, -s, -s, -s, s, -s, -s, s, s, -s, -s, s}, {s, -s, -s, s, -s, s, s, s, s, s, s, -s},
                {-s, s, -s, s, s, -s, s, s, s, -s, s, s}, {-s, -s, -s, -s, -s, s, s, -s, s, s, -s, -s}};
        for (float[] face : faces) {
            for (int i = 0; i < 12; i += 3) {
                buffer.vertex(pose, face[i], face[i + 1], face[i + 2]).color(110, 0, 0, alpha).endVertex();
            }
        }
        BufferUploader.drawWithShader(buffer.end());

        // The moon, where the vanilla one is, bigger and red.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(minecraft.level.getTimeOfDay(partialTick) * 360.0F));
        Matrix4f moon = poseStack.last().pose();
        float size = 26.0F;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, MOON);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, amount);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(moon, -size, -100.0F, size).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(moon, size, -100.0F, size).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(moon, size, -100.0F, -size).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(moon, -size, -100.0F, -size).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private BloodMoonRenderer() {
    }
}
