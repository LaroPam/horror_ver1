package com.laropam.dontturnaround.haunt;

import com.laropam.dontturnaround.HorrorConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-wide haunt progress. Counts real ticks spent in the world instead of the day/night clock,
 * so /time set and sleeping don't reset or skip it.
 */
public class HauntData extends SavedData {
    private static final String NAME = "dontturnaround_haunt";
    public static final int MAX_STAGE = 3;

    private long ticks;
    private int stageOverride = -1;

    public static HauntData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(HauntData::load, HauntData::new, NAME);
    }

    private static HauntData load(CompoundTag tag) {
        HauntData data = new HauntData();
        data.ticks = tag.getLong("Ticks");
        data.stageOverride = tag.contains("StageOverride") ? tag.getInt("StageOverride") : -1;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("Ticks", this.ticks);
        tag.putInt("StageOverride", this.stageOverride);
        return tag;
    }

    public void tick() {
        this.ticks++;
        if (this.ticks % 200 == 0) {
            this.setDirty();
        }
    }

    public long getTicks() {
        return this.ticks;
    }

    public long getDays() {
        return this.ticks / 24000L;
    }

    public int getStage() {
        if (this.stageOverride >= 0) {
            return this.stageOverride;
        }
        long days = this.getDays();
        if (days >= HorrorConfig.DAYS_UNTIL_STAGE_3.get()) {
            return 3;
        }
        if (days >= HorrorConfig.DAYS_UNTIL_STAGE_2.get()) {
            return 2;
        }
        if (days >= HorrorConfig.DAYS_UNTIL_STAGE_1.get()) {
            return 1;
        }
        return 0;
    }

    public boolean isStageOverridden() {
        return this.stageOverride >= 0;
    }

    /** -1 returns to automatic progression. */
    public void setStageOverride(int stage) {
        this.stageOverride = stage;
        this.setDirty();
    }
}
