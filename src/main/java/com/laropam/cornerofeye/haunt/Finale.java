package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;

/** The last night: once it has been caught all but one time, the final catch has to happen on a night of its own. */
public final class Finale {
    /** One wound left: the next night is the last one, under a blood moon. */
    public static boolean isDue(HauntData data) {
        return !data.isBanished() && data.getWounds() >= HorrorConfig.WOUNDS_TO_BANISH.get() - 1 && data.getWounds() > 0;
    }

    private Finale() {
    }
}
