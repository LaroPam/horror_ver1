package com.laropam.cornerofeye.mixin;

import com.laropam.cornerofeye.haunt.LightSuppression;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lights it has smothered give off nothing, whatever block they are, until someone lights them again. */
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {
    @Inject(method = "getEmission", at = @At("HEAD"), cancellable = true)
    private void cornerofeye$smother(long pos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (LightSuppression.isDark(((LightEngineAccessor) this).cornerofeye$getChunkSource().getLevel(), pos, state)) {
            cir.setReturnValue(0);
        }
    }
}
