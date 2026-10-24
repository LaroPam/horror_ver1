package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.CorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;

/** A villager lying on its back, pale, neck at the wrong angle, clothes soaked through. */
public class CorpseRenderer extends EntityRenderer<CorpseEntity> {
    private static final ResourceLocation BASE = new ResourceLocation("textures/entity/villager/villager.png");
    private static final ResourceLocation BLOOD = CornerOfTheEye.id("textures/entity/corpse_blood.png");

    private final VillagerModel<CorpseEntity> model;

    public CorpseRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(CorpseEntity corpse, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - corpse.getYRot()));
        // On its back, centered on where it fell.
        poseStack.translate(0.0D, 0.28D, -0.9D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.501D, 0.0D);

        this.model.setupAnim(corpse, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        // A hat brim would stick straight up from a body on its back.
        this.model.hatVisible(false);
        this.model.getHead().yRot = ((corpse.getId() * 37) % 100 / 100.0F - 0.5F) * 1.8F;

        VillagerData data = corpse.getVillagerData();
        this.layer(poseStack, buffers, packedLight, BASE);
        ResourceLocation type = BuiltInRegistries.VILLAGER_TYPE.getKey(data.getType());
        this.layer(poseStack, buffers, packedLight, new ResourceLocation(type.getNamespace(), "textures/entity/villager/type/" + type.getPath() + ".png"));
        if (data.getProfession() != VillagerProfession.NONE) {
            ResourceLocation profession = BuiltInRegistries.VILLAGER_PROFESSION.getKey(data.getProfession());
            if ("minecraft".equals(profession.getNamespace())) {
                this.layer(poseStack, buffers, packedLight, new ResourceLocation("textures/entity/villager/profession/" + profession.getPath() + ".png"));
            }
        }
        this.layer(poseStack, buffers, packedLight, BLOOD);
        poseStack.popPose();
        super.render(corpse, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private void layer(PoseStack poseStack, MultiBufferSource buffers, int packedLight, ResourceLocation texture) {
        // Drained of colour.
        this.model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight,
                OverlayTexture.NO_OVERLAY, 0.8F, 0.72F, 0.7F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseEntity corpse) {
        return BASE;
    }
}
