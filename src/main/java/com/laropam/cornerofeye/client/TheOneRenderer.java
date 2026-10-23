package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class TheOneRenderer extends MobRenderer<TheOneEntity, TheOneModel> {
    static final ResourceLocation TEXTURE = CornerOfTheEye.id("textures/entity/the_one.png");

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
        if (visibility > 0.5F) {
            ClientHorror.onGlimpse(entity);
        }
        this.getModel().setAlpha(visibility);
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Nullable
    @Override
    protected RenderType getRenderType(TheOneEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(TEXTURE);
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
        return TEXTURE;
    }
}
