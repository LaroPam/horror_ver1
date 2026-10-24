package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.Scene;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class TheOneRenderer extends MobRenderer<TheOneEntity, TheOneModel> {
    private static final ResourceLocation[] TEXTURES = {
            CornerOfTheEye.id("textures/entity/the_one_stage1.png"),
            CornerOfTheEye.id("textures/entity/the_one_stage2.png"),
            CornerOfTheEye.id("textures/entity/the_one_stage3.png")};

    public TheOneRenderer(EntityRendererProvider.Context context) {
        // No shadow. Nothing about it should touch the ground properly.
        super(context, new TheOneModel(context.bakeLayer(TheOneModel.LAYER)), 0.0F);
        this.addLayer(new TheOneGlowLayer(this));
    }

    @Override
    public void render(TheOneEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float visibility = Peripheral.visibility(entity, partialTick);
        if (visibility < 0.01F) {
            return;
        }
        if (visibility > 0.5F && entity.getPhase() != Phase.SCENE) {
            ClientHorror.onGlimpse(entity);
        }
        this.getModel().setAlpha(visibility);
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    protected void setupRotations(TheOneEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        if (entity.getScene() == Scene.CEILING) {
            // Hanging head first out of the ceiling.
            poseStack.translate(0.0F, entity.getBbHeight(), 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }

    @Nullable
    @Override
    protected RenderType getRenderType(TheOneEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(this.getTextureLocation(entity));
    }

    @Override
    protected boolean isShaking(TheOneEntity entity) {
        return entity.getPhase() == Phase.REVEALED;
    }

    @Override
    protected boolean shouldShowName(TheOneEntity entity) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(TheOneEntity entity) {
        return TEXTURES[entity.getVisualStage() - 1];
    }

    @Override
    protected void scale(TheOneEntity entity, PoseStack poseStack, float partialTick) {
        // A little taller every day.
        float size = entity.getVisualScale();
        poseStack.scale(size, size, size);
    }
}
