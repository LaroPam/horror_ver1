package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.registry.ModBlocks;
import com.laropam.cornerofeye.registry.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THE_ONE.get(), TheOneRenderer::new);
        event.registerEntityRenderer(ModEntities.CORPSE.get(), CorpseRenderer::new);
        event.registerEntityRenderer(ModEntities.MIMIC.get(), MimicRenderer::new);
        event.registerEntityRenderer(ModEntities.FLARE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        event.registerBlockEntityRenderer(ModBlocks.WRITING_ENTITY.get(), WritingRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(TheOneModel.LAYER, TheOneModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Dread darkens the edges under the HUD; flashes and jumpscares cover everything.
        event.registerBelowAll("dread", ClientHorror::renderDread);
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "mirror", MirrorOverlay::render);
        event.registerAboveAll("scare", ClientHorror::renderScare);
    }

    private ClientSetup() {
    }
}
