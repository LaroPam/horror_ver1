package com.laropam.cornerofeye.haunt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** The small things that happen between its appearances, the stage each unlocks at, and how often each is picked. */
public enum HauntEvent {
    DISTANT_SOUND(0, 6, HauntEvents::distantSound),
    FAR_LIGHT_OUT(0, 3, HauntEvents::farLightOut),
    FOOTSTEPS(1, 10, HauntEvents::footsteps),
    MINING(1, 6, HauntEvents::mining),
    DOOR(1, 5, HauntEvents::door),
    WHISPER(2, 6, HauntEvents::whisper),
    LIGHTS_OUT(2, 6, HauntEvents::lightsOut),
    FAKE_CHAT(2, 4, HauntEvents::fakeChat),
    SIGN(2, 3, HauntEvents::sign),
    SCRATCHES(1, 4, c -> Marks.scratches(c.level(), c.player().blockPosition())),
    HANDPRINTS(2, 3, c -> Marks.handprintTrail(c.level(), c.player().position())),
    WRITING(2, 3, c -> Marks.writing(c.level(), c.player().blockPosition(), 5, 14));

    public final int minStage;
    private final int weight;
    private final Predicate<HauntContext> action;

    HauntEvent(int minStage, int weight, Predicate<HauntContext> action) {
        this.minStage = minStage;
        this.weight = weight;
        this.action = action;
    }

    /** @return false if this isn't a good moment for it (no door nearby, not underground, ...). */
    public boolean run(HauntContext context) {
        return this.action.test(context);
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /** Picks weighted random events unlocked at this stage until one of them works. */
    public static boolean runRandom(HauntContext context) {
        List<HauntEvent> pool = new ArrayList<>();
        for (HauntEvent event : values()) {
            if (context.stage() >= event.minStage) {
                pool.add(event);
            }
        }
        while (!pool.isEmpty()) {
            int roll = context.random().nextInt(pool.stream().mapToInt(event -> event.weight).sum());
            HauntEvent picked = pool.get(pool.size() - 1);
            for (HauntEvent event : pool) {
                roll -= event.weight;
                if (roll < 0) {
                    picked = event;
                    break;
                }
            }
            if (picked.run(context)) {
                return true;
            }
            pool.remove(picked);
        }
        return false;
    }
}
