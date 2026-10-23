package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Its eyes and teeth catch light that isn't there, and the pale skin glows just enough to make out in the dark. */
public class TheOneGlowLayer extends RenderLayer<TheOneEntity, TheOneModel> {
    private static final RenderType GLOW = RenderType.eyes(CornerOfTheEye.id("textures/entity/the_one_glow.png"));

    public TheOneGlowLayer(RenderLayerParent<TheOneEntity, TheOneModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, TheOneEntity entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        TheOneModel model = this.getParentModel();
        // Additive blending: fade by darkening the color, not with alpha.
        float strength = model.getAlpha();
        model.setAlpha(1.0F);
        model.renderToBuffer(poseStack, buffers.getBuffer(GLOW), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                strength, strength, strength, 1.0F);
        model.setAlpha(strength);
    }
}
