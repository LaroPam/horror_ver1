package com.laropam.dontturnaround.haunt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Every kind of scare, the stage it unlocks at, and how often it is picked. */
public enum HauntEvent {
    DISTANT_SOUND(0, 6, HauntEvents::distantSound),
    FOOTSTEPS(1, 10, HauntEvents::footsteps),
    MINING(1, 7, HauntEvents::mining),
    DOOR(1, 6, HauntEvents::door),
    WATCHER(1, 6, HauntEvents::watcher),
    WHISPER(2, 6, HauntEvents::whisper),
    FAKE_CHAT(2, 4, HauntEvents::fakeChat),
    TORCHES_OUT(2, 5, HauntEvents::torchesOut),
    STRUCTURE(2, 3, HauntEvents::structure),
    BEHIND(2, 5, HauntEvents::behind),
    GLITCH(2, 3, HauntEvents::glitch),
    STALKER(3, 8, HauntEvents::stalker);

    public final int minStage;
    private final int weight;
    private final Predicate<HauntContext> action;

    HauntEvent(int minStage, int weight, Predicate<HauntContext> action) {
        this.minStage = minStage;
        this.weight = weight;
        this.action = action;
    }

    /** @return false if this isn't a good moment for it (no door nearby, already daytime, ...). */
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
            int total = pool.stream().mapToInt(event -> event.weight).sum();
            int roll = context.random().nextInt(total);
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
