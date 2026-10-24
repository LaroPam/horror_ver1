package com.laropam.cornerofeye.entity;

public enum Phase {
    /** Stands far off in the dark and stares. Walk up to the spot and it is gone, leaving a scrap behind. */
    WATCH,
    /** Circles you at the edge of your vision and puts out the lights you aren't looking at. */
    STALK,
    /** Comes for you through walls. Can't enter light, can't touch you while you stand in it. */
    HUNT,
    /** Caught by the Lantern of Revelation: frozen and fully visible for a moment. */
    REVEALED,
    /** Playing out one of the scripted {@link Scene scary moments}. */
    SCENE;

    public static Phase byId(int id) {
        Phase[] values = values();
        return id >= 0 && id < values.length ? values[id] : WATCH;
    }
}
