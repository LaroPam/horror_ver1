package com.laropam.cornerofeye.mixin;

import com.laropam.cornerofeye.haunt.LightSuppression;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A smothered furnace stands still: no fire, no fuel burnt, no progress, until someone lights it again. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void cornerofeye$freeze(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        if (LightSuppression.isFrozen(level, pos, state)) {
            ci.cancel();
        }
    }
}
