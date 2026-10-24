package com.laropam.cornerofeye.entity;

import java.util.Locale;

/** What the countdown on screen asks you to do to stay alive. */
public enum Challenge {
    /** Get into light in time: place a torch or run to one. */
    LIGHT_RUN,
    /** Your lights just went out: light one again in time. */
    LIGHT_PLACE,
    /** It is right behind you. Don't turn around until the timer runs out. */
    DONT_LOOK,
    /** Crouch and stay perfectly still until the timer runs out. */
    STILL,
    /** Something is coming up from below: get out of the water. */
    LEAVE_WATER;

    public String key() {
        return "cornerofeye.challenge." + this.name().toLowerCase(Locale.ROOT);
    }

    public static Challenge byId(int id) {
        Challenge[] values = values();
        return id >= 0 && id < values.length ? values[id] : LIGHT_RUN;
    }
}
