package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.entity.MimicEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

/** Draws the mimic with the real skin of whoever it is copying, wide or slim arms to match. */
public class MimicRenderer extends LivingEntityRenderer<MimicEntity, MimicModel> {
    private final MimicModel wide;
    private final MimicModel slim;

    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context, new MimicModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = this.model;
        this.slim = new MimicModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(MimicEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        this.model = isSlim(entity) ? this.slim : this.wide;
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity entity) {
        UUID id = entity.getCopied().orElse(entity.getUUID());
        PlayerInfo info = info(id);
        return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
    }

    private static boolean isSlim(MimicEntity entity) {
        UUID id = entity.getCopied().orElse(entity.getUUID());
        PlayerInfo info = info(id);
        return "slim".equals(info != null ? info.getModelName() : DefaultPlayerSkin.getSkinModelName(id));
    }

    @Nullable
    private static PlayerInfo info(UUID id) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfo(id);
    }
}
