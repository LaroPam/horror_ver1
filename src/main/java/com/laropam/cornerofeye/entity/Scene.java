package com.laropam.cornerofeye.entity;

import java.util.Locale;

/** Scripted scary moments: the times it lets itself be seen. */
public enum Scene {
    NONE(0),
    /** A door creaks open, it crawls through and unfolds to full height. */
    DOOR(2),
    /** Leans out from behind a tree trunk and stares. */
    PEEK(1),
    /** Breathing on the back of your neck. */
    BEHIND(2),
    /** Every light around you dies at once and it walks out of the dark. */
    LIGHTS_OUT(2),
    /** Circles you on all fours, sniffing. */
    STILL(2);

    /** Stage at which the haunt starts using this moment on its own. */
    public final int minStage;

    Scene(int minStage) {
        this.minStage = minStage;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static Scene byId(int id) {
        Scene[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
