package com.laropam.dontturnaround.entity;

public enum FacelessMode {
    /** Stands far away and stares. Gone once you look straight at it or come close. */
    WATCHER,
    /** Hunts you, but only moves while nobody is looking at it. Afraid of bright light. */
    STALKER,
    /** Stands right behind you. Turning around to look is a bad idea. */
    BEHIND;

    public static FacelessMode byId(int id) {
        FacelessMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : STALKER;
    }
}
