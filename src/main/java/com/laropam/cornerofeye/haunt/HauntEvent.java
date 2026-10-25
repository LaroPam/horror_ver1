package com.laropam.cornerofeye.haunt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * The small things that happen between its appearances: the stage each unlocks at, how often each is picked, and how
 * strong it is (0 faint and far off, 1 close, 2 right next to you), which the Director uses to build things up.
 */
public enum HauntEvent {
    DISTANT_SOUND(0, 6, 0, HauntEvents::distantSound),
    FAR_LIGHT_OUT(0, 3, 0, HauntEvents::farLightOut),
    FOOTSTEPS(1, 10, 1, HauntEvents::footsteps),
    MINING(1, 6, 0, HauntEvents::mining),
    DOOR(1, 5, 1, HauntEvents::door),
    WHISPER(2, 6, 1, HauntEvents::whisper),
    LIGHTS_OUT(2, 6, 2, HauntEvents::lightsOut),
    FAKE_CHAT(2, 4, 1, HauntEvents::fakeChat),
    SIGN(2, 3, 1, HauntEvents::sign),
    SCRATCHES(1, 4, 1, c -> Marks.scratches(c.level(), c.player().blockPosition())),
    HANDPRINTS(2, 3, 2, c -> Marks.handprintTrail(c.level(), c.player().position())),
    WRITING(2, 3, 2, c -> Marks.writing(c.level(), c.player().blockPosition(), 5, 14)),
    STARE(2, 3, 0, Animals::stareAtNothing),
    MIMIC(2, 3, 2, Mimics::appear),
    GNAW(2, 5, 2, c -> Gnawing.nearby(c.player()));

    public final int minStage;
    private final int weight;
    public final int tier;
    private final Predicate<HauntContext> action;

    HauntEvent(int minStage, int weight, int tier, Predicate<HauntContext> action) {
        this.minStage = minStage;
        this.weight = weight;
        this.tier = tier;
        this.action = action;
    }

    /** @return false if this isn't a good moment for it (no door nearby, not underground, ...). */
    public boolean run(HauntContext context) {
        return this.action.test(context);
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Picks weighted random events unlocked at this stage, no stronger than {@code maxTier} and mostly as strong as
     * that, until one of them works. @return the one that happened, or null
     */
    @Nullable
    public static HauntEvent runRandom(HauntContext context, int maxTier) {
        List<HauntEvent> pool = new ArrayList<>();
        for (HauntEvent event : values()) {
            if (context.stage() >= event.minStage && event.tier <= maxTier) {
                pool.add(event);
            }
        }
        while (!pool.isEmpty()) {
            int roll = context.random().nextInt(pool.stream().mapToInt(event -> event.weight(maxTier)).sum());
            HauntEvent picked = pool.get(pool.size() - 1);
            for (HauntEvent event : pool) {
                roll -= event.weight(maxTier);
                if (roll < 0) {
                    picked = event;
                    break;
                }
            }
            if (picked.run(context)) {
                return picked;
            }
            pool.remove(picked);
        }
        return null;
    }

    private int weight(int maxTier) {
        return this.tier == maxTier ? this.weight * 3 : this.weight;
    }
}
