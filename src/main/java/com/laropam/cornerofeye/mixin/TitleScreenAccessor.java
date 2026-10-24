package com.laropam.cornerofeye.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

@Mixin(TitleScreen.class)
public interface TitleScreenAccessor {
    @Accessor("splash")
    void cornerofeye$setSplash(@Nullable SplashRenderer splash);
}
