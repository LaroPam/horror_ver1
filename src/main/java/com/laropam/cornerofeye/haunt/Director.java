package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.HorrorConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Paces the fear for each player the way a horror film is paced, not at random: a stretch of quiet in which nothing
 * happens (and you start to wait for it), then a build-up in which small things start to happen, faint at first, then
 * closer and more often and worse, until it comes itself. After it has been, quiet again, the longer the worse it was.
 * The quiet is not empty: it is when you listen hardest.
 */
public final class Director {
    public enum Mood {
        /** Nothing happens. */
        QUIET,
        /** Small things, more and worse as it goes on. */
        BUILDING,
        /** It may come now; until it does, things close by. */
        READY,
        /** It is here (or you are dreaming of it). */
        ENCOUNTER
    }

    private static final class State {
        Mood mood = Mood.QUIET;
        /** Seconds spent in this mood, counted faster at night and in the dark. */
        double progress;
        /** How long this mood lasts, in the same seconds. */
        double length = 60.0D + Math.random() * 60.0D;
        /** How frightened they have been lately, 0-1; wears off over a few minutes. */
        float tension;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    /** Once a second for every player who is in the overworld. */
    static void tick(ServerPlayer player, int stage) {
        State state = state(player);
        RandomSource random = player.getRandom();
        HauntContext context = HauntContext.of(player, stage, false);
        state.tension = Math.max(0.0F, state.tension - 0.004F);
        // How fast the wave rises: faster at night, in the dark, under a blood moon. The config only thins or
        // thickens the small events along the way, not how often it comes itself.
        double pace = (context.night() ? 1.5D : 1.0D) * (context.dark() ? 1.3D : 1.0D)
                * (HauntData.get(player.server).isBloodMoon() ? 1.5D : 1.0D);
        double events = pace * HorrorConfig.EVENT_FREQUENCY.get();
        if (state.mood == Mood.READY) {
            // Ready, but it isn't free to come yet: the wait stretches (up to about ten minutes).
            state.progress += HauntManager.appearanceDue(player.server) ? 1.0D : 0.25D;
        } else {
            state.progress += state.mood == Mood.ENCOUNTER ? 1.0D : pace;
        }
        switch (state.mood) {
            case QUIET -> {
                if (state.progress >= state.length) {
                    begin(state, Mood.BUILDING, buildLength(stage, random));
                }
            }
            case BUILDING -> {
                float build = (float) Math.min(1.0D, state.progress / state.length);
                // Sparse at first, then more and more often, and worse.
                double chance = events * (0.012D + 0.032D * build) * (stage == HauntData.SILENCE ? 0.4D : 1.0D);
                if (random.nextDouble() < chance) {
                    int tier = stage == HauntData.SILENCE ? 0 : build < 0.35F ? 0 : build < 0.75F ? 1 : 2;
                    HauntEvent event = HauntEvent.runRandom(context, tier);
                    if (event != null) {
                        state.tension = Math.min(1.0F, state.tension + 0.04F + 0.04F * event.tier);
                    }
                }
                if (build >= 1.0F) {
                    // Below WATCHING it never comes itself: round again.
                    begin(state, stage >= HauntData.WATCHING ? Mood.READY : Mood.QUIET,
                            stage >= HauntData.WATCHING ? 150.0D : quietLength(stage, random));
                }
            }
            case READY -> {
                if (random.nextDouble() < events * 0.015D) {
                    HauntEvent.runRandom(context, 2);
                }
                if (state.progress >= state.length) {
                    // It didn't come this time. Let them breathe, a little.
                    begin(state, Mood.QUIET, quietLength(stage, random) * 0.5D);
                }
            }
            case ENCOUNTER -> {
                if (state.progress >= 900.0D) {
                    begin(state, Mood.QUIET, quietLength(stage, random));
                }
            }
        }
    }

    /** True if the build-up has run its course: it may come for this player now. */
    public static boolean wantsEncounter(ServerPlayer player) {
        return state(player).mood == Mood.READY;
    }

    /** It has come for this player (or they are dreaming of it). */
    public static void encounterStarted(ServerPlayer player) {
        State state = state(player);
        state.tension = Math.min(1.0F, state.tension + 0.35F);
        begin(state, Mood.ENCOUNTER, 900.0D);
    }

    /** It is gone. Quiet for a while, the longer the closer it got. */
    public static void encounterEnded(ServerPlayer player, boolean hurt) {
        State state = state(player);
        if (hurt) {
            state.tension = Math.min(1.0F, state.tension + 0.3F);
        }
        int stage = HauntData.get(player.server).getStage();
        begin(state, Mood.QUIET, quietLength(stage, player.getRandom()) * (1.0D + state.tension));
    }

    /** Something frightening happened that the director didn't start itself. */
    public static void scared(ServerPlayer player, float amount) {
        State state = state(player);
        state.tension = Math.min(1.0F, state.tension + amount);
    }

    public static Mood mood(ServerPlayer player) {
        return state(player).mood;
    }

    /** "QUIET 40/120, tension 0.20; learned light 0.30 ...": for the status command. */
    public static String describe(ServerPlayer player) {
        State state = state(player);
        StringBuilder text = new StringBuilder(String.format(java.util.Locale.ROOT, "%s %.0f/%.0f, tension %.2f; learned",
                state.mood, state.progress, state.length, state.tension));
        for (Habits.Habit habit : Habits.Habit.values()) {
            text.append(String.format(java.util.Locale.ROOT, " %s %.2f", habit.name().toLowerCase(java.util.Locale.ROOT), Habits.get(player, habit)));
        }
        return text.toString();
    }

    /** Skips to the next mood, for testing. */
    public static void advance(ServerPlayer player) {
        State state = state(player);
        state.progress = state.length;
    }

    static void reset() {
        STATES.clear();
    }

    private static void begin(State state, Mood mood, double length) {
        state.mood = mood;
        state.progress = 0.0D;
        state.length = length;
    }

    /** Seconds of quiet (at day pace) before the next build-up. */
    private static double quietLength(int stage, RandomSource random) {
        return switch (stage) {
            case HauntData.SILENCE -> 600.0D + random.nextInt(300);
            case HauntData.WATCHING -> 240.0D + random.nextInt(120);
            case HauntData.APPROACHING -> 150.0D + random.nextInt(90);
            default -> 90.0D + random.nextInt(70);
        };
    }

    /** Seconds of build-up (at day pace) before it may come. */
    private static double buildLength(int stage, RandomSource random) {
        return switch (stage) {
            case HauntData.SILENCE -> 300.0D;
            case HauntData.WATCHING -> 240.0D + random.nextInt(90);
            case HauntData.APPROACHING -> 180.0D + random.nextInt(60);
            default -> 120.0D + random.nextInt(60);
        };
    }

    private Director() {
    }
}
