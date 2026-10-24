package com.laropam.cornerofeye.entity.scene;

import com.laropam.cornerofeye.entity.TheOneEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server-side script for one scary moment. The client poses the model from the synced scene and step. */
public interface SceneRunner {
    /** Called once, right after it appears. */
    void start(TheOneEntity one, ServerLevel level, ServerPlayer victim);

    /** Called every tick; {@code age} counts ticks since the scene started. */
    void tick(TheOneEntity one, ServerLevel level, ServerPlayer victim, int age);
}
