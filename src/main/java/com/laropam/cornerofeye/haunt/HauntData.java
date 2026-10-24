package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-wide haunt progress, shared by everyone on the server. Counts real ticks spent in the world
 * instead of the day/night clock, so /time set and sleeping don't reset or skip it.
 */
public class HauntData extends SavedData {
    private static final String NAME = "cornerofeye_haunt";

    public static final int SILENCE = 0;
    public static final int WATCHING = 1;
    public static final int APPROACHING = 2;
    public static final int HUNTING = 3;

    private long ticks;
    private int stageOverride = -1;
    private int wounds;
    private boolean banished;
    private long nextAppearance;
    private long nextMoment = -1;
    private long nextMassacre;
    private int lastCardNight;
    private boolean bloodMoon;

    public static HauntData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(HauntData::load, HauntData::new, NAME);
    }

    private static HauntData load(CompoundTag tag) {
        HauntData data = new HauntData();
        data.ticks = tag.getLong("Ticks");
        data.stageOverride = tag.contains("StageOverride") ? tag.getInt("StageOverride") : -1;
        data.wounds = tag.getInt("Wounds");
        data.banished = tag.getBoolean("Banished");
        data.nextAppearance = tag.getLong("NextAppearance");
        data.nextMoment = tag.contains("NextMoment") ? tag.getLong("NextMoment") : -1;
        data.nextMassacre = tag.getLong("NextMassacre");
        data.lastCardNight = tag.getInt("LastCardNight");
        data.bloodMoon = tag.getBoolean("BloodMoon");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("Ticks", this.ticks);
        tag.putInt("StageOverride", this.stageOverride);
        tag.putInt("Wounds", this.wounds);
        tag.putBoolean("Banished", this.banished);
        tag.putLong("NextAppearance", this.nextAppearance);
        tag.putLong("NextMoment", this.nextMoment);
        tag.putLong("NextMassacre", this.nextMassacre);
        tag.putInt("LastCardNight", this.lastCardNight);
        tag.putBoolean("BloodMoon", this.bloodMoon);
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
        if (days >= HorrorConfig.DAYS_UNTIL_HUNTING.get()) {
            return HUNTING;
        }
        if (days >= HorrorConfig.DAYS_UNTIL_APPROACHING.get()) {
            return APPROACHING;
        }
        if (days >= HorrorConfig.DAYS_UNTIL_WATCHING.get()) {
            return WATCHING;
        }
        return SILENCE;
    }

    public boolean isStageOverridden() {
        return this.stageOverride >= 0;
    }

    /** -1 returns to automatic progression. */
    public void setStageOverride(int stage) {
        this.stageOverride = stage;
        this.setDirty();
    }

    public int getWounds() {
        return this.wounds;
    }

    /** @return true if this wound banished it. */
    public boolean addWound() {
        this.wounds++;
        if (this.wounds >= HorrorConfig.WOUNDS_TO_BANISH.get()) {
            this.banished = true;
        }
        this.setDirty();
        return this.banished;
    }

    public boolean isBanished() {
        return this.banished;
    }

    public long getNextAppearance() {
        return this.nextAppearance;
    }

    /** Earliest haunt tick it may show up again. */
    public void setNextAppearance(long tick) {
        this.nextAppearance = tick;
        this.setDirty();
    }

    /** Earliest haunt tick for the next scripted moment; -1 until the first one is scheduled. */
    public long getNextMoment() {
        return this.nextMoment;
    }

    public void setNextMoment(long tick) {
        this.nextMoment = tick;
        this.setDirty();
    }

    public long getNextMassacre() {
        return this.nextMassacre;
    }

    public void setNextMassacre(long tick) {
        this.nextMassacre = tick;
        this.setDirty();
    }

    /** Last night number announced on screen, so sleeping through a night doesn't repeat a card. */
    public int getLastCardNight() {
        return this.lastCardNight;
    }

    public void setLastCardNight(int night) {
        this.lastCardNight = night;
        this.setDirty();
    }

    public boolean isBloodMoon() {
        return this.bloodMoon;
    }

    public void setBloodMoon(boolean bloodMoon) {
        this.bloodMoon = bloodMoon;
        this.setDirty();
    }

    public void reset() {
        this.ticks = 0;
        this.stageOverride = -1;
        this.wounds = 0;
        this.banished = false;
        this.nextAppearance = 0;
        this.nextMoment = -1;
        this.nextMassacre = 0;
        this.lastCardNight = 0;
        this.bloodMoon = false;
        this.setDirty();
    }
}
