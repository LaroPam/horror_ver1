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
    STILL(2),
    /** In a cave: lowers itself head first out of the ceiling above you. */
    CEILING(1),
    /** Rises out of the deep water under you, arms up. */
    UNDERWATER(2),
    /** Goes through a village in the dark while you aren't looking. Never seen. Then comes for you. */
    MASSACRE(2),
    /** Comes for you on all fours out of the dark, fast. */
    CHASE(3),
    /** Your room is lit, so it bends down to the window and looks in, palms on the glass. */
    WINDOW(2),
    /** You wake up and can't move. It is standing at the foot of the bed, leaning closer. Only on waking. */
    PARALYSIS(2),
    /** The doll you hung up in a frame draws it: it crouches under it, sniffing, before it comes for you. */
    LURED(2);

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
