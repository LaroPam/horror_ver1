package com.laropam.cornerofeye.mixin;

import com.laropam.cornerofeye.haunt.LightSuppression;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Glowing blocks are normally drawn at least as bright as their own glow. A smothered one is drawn by the
 * light actually around it, so it looks dead, and so do the faces of the blocks next to it.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"), cancellable = true)
    private static void cornerofeye$deadLight(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        // Faces of full blocks are lit from the neighbouring cell but with the block's own glow, so check around too.
        if (LightSuppression.isClientDark(pos) || LightSuppression.isClientDarkNextTo(level, state, pos)) {
            cir.setReturnValue(level.getBrightness(LightLayer.SKY, pos) << 20 | level.getBrightness(LightLayer.BLOCK, pos) << 4);
        }
    }
}
