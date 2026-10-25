package com.laropam.cornerofeye.command;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.Scene;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.HauntEvent;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.laropam.cornerofeye.haunt.Director;
import com.laropam.cornerofeye.haunt.Gnawing;
import com.laropam.cornerofeye.haunt.Habits;
import com.laropam.cornerofeye.haunt.HomeVisit;
import com.laropam.cornerofeye.haunt.Nightmares;
import com.laropam.cornerofeye.haunt.Journal;
import com.laropam.cornerofeye.haunt.Moments;
import com.laropam.cornerofeye.haunt.Nights;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Locale;

/** /cornerofeye: status, stage control and on-demand scares for testing and recording. */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID)
public final class HauntCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(CornerOfTheEye.MODID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(HauntCommand::status))
                .then(Commands.literal("stage")
                        .then(Commands.literal("auto").executes(ctx -> setStage(ctx, -1)))
                        .then(Commands.argument("stage", IntegerArgumentType.integer(0, HauntData.HUNTING))
                                .executes(ctx -> setStage(ctx, IntegerArgumentType.getInteger(ctx, "stage")))))
                .then(Commands.literal("appear")
                        .then(Commands.argument("phase", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[]{"watch", "stalk", "hunt"}, builder))
                                .executes(ctx -> appear(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> appear(ctx, EntityArgument.getPlayer(ctx, "target"))))))
                .then(Commands.literal("moment")
                        .then(Commands.argument("moment", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Scene.values()).filter(scene -> scene != Scene.NONE).map(Scene::id), builder))
                                .executes(ctx -> moment(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> moment(ctx, EntityArgument.getPlayer(ctx, "target"))))))
                .then(Commands.literal("event")
                        .then(Commands.argument("event", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(HauntEvent.values()).map(HauntEvent::id), builder))
                                .executes(ctx -> event(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> event(ctx, EntityArgument.getPlayer(ctx, "target"))))))
                .then(Commands.literal("wounds")
                        .then(Commands.argument("wounds", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> setWounds(ctx, IntegerArgumentType.getInteger(ctx, "wounds")))))
                .then(Commands.literal("dream")
                        .executes(ctx -> dream(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> dream(ctx, EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("visit")
                        .executes(ctx -> visit(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> visit(ctx, EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("director")
                        .executes(ctx -> director(ctx, ctx.getSource().getPlayerOrException(), false))
                        .then(Commands.literal("next").executes(ctx -> director(ctx, ctx.getSource().getPlayerOrException(), true)))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> director(ctx, EntityArgument.getPlayer(ctx, "target"), false))
                                .then(Commands.literal("next").executes(ctx -> director(ctx, EntityArgument.getPlayer(ctx, "target"), true)))))
                .then(Commands.literal("habit")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("habit", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Habits.Habit.values()).map(habit -> habit.name().toLowerCase(Locale.ROOT)), builder))
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                .executes(HauntCommand::habit)))))
                .then(Commands.literal("morning")
                        .executes(ctx -> morning(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> morning(ctx, EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("bloodmoon")
                        .then(Commands.literal("on").executes(ctx -> bloodMoon(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> bloodMoon(ctx, false))))
                .then(Commands.literal("reset").executes(HauntCommand::reset)));
    }

    private static HauntData data(CommandContext<CommandSourceStack> ctx) {
        return HauntData.get(ctx.getSource().getServer());
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        HauntData data = data(ctx);
        boolean present = HauntManager.findTheOne(ctx.getSource().getServer().overworld()) != null;
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.status",
                Component.translatable("cornerofeye.stage." + data.getStage()), data.getDays(),
                data.getWounds(), data.isBanished() ? "✔" : "✘", present ? "✔" : "✘"), false);
        return data.getStage();
    }

    private static int setStage(CommandContext<CommandSourceStack> ctx, int stage) {
        HauntData data = data(ctx);
        data.setStageOverride(stage);
        ctx.getSource().sendSuccess(() -> stage < 0
                ? Component.translatable("commands.cornerofeye.stage.auto", Component.translatable("cornerofeye.stage." + data.getStage()))
                : Component.translatable("commands.cornerofeye.stage.set", Component.translatable("cornerofeye.stage." + stage)), true);
        return 1;
    }

    private static int appear(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        Phase phase;
        try {
            phase = Phase.valueOf(StringArgumentType.getString(ctx, "phase").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.unknown", StringArgumentType.getString(ctx, "phase")));
            return 0;
        }
        TheOneEntity existing = HauntManager.findTheOne(target.serverLevel());
        if (existing != null) {
            existing.discard();
        }
        int stage = Math.max(HauntData.WATCHING, data(ctx).getStage());
        if (phase == Phase.HUNT) {
            stage = HauntData.HUNTING;
        }
        TheOneEntity one = HauntManager.appear(target, stage, phase);
        if (one == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.appear.failed"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.appear", target.getDisplayName()), true);
        return 1;
    }

    private static int moment(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        String name = StringArgumentType.getString(ctx, "moment");
        Scene scene;
        try {
            scene = Scene.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            scene = Scene.NONE;
        }
        if (scene == Scene.NONE) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.unknown", name));
            return 0;
        }
        TheOneEntity existing = HauntManager.findTheOne(target.serverLevel());
        if (existing != null) {
            existing.discard();
        }
        int stage = Math.max(scene.minStage, data(ctx).getStage());
        if (Moments.start(target, stage, scene, true) == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.moment.failed", name));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.moment", name, target.getDisplayName()), true);
        return 1;
    }

    private static int event(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        String name = StringArgumentType.getString(ctx, "event");
        HauntEvent event;
        try {
            event = HauntEvent.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.unknown", name));
            return 0;
        }
        if (!HauntManager.forceEvent(target, event, Math.max(event.minStage, data(ctx).getStage()))) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.event.failed", name));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.event", name, target.getDisplayName()), true);
        return 1;
    }

    private static int setWounds(CommandContext<CommandSourceStack> ctx, int wounds) {
        data(ctx).setWounds(wounds);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.wounds", wounds), true);
        return 1;
    }

    private static int dream(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        if (!Nightmares.begin(target, Math.max(HauntData.APPROACHING, data(ctx).getStage()))) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.dream.failed"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.dream", target.getDisplayName()), true);
        return 1;
    }

    /** It goes through the target's home right now (as long as nobody is in it). */
    private static int visit(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        BlockPos bed = HomeVisit.bedOf(target);
        if (bed == null || !HomeVisit.visit(target.server.overworld(), bed)) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.visit.failed"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.visit", target.getDisplayName()), true);
        return 1;
    }

    /** Where the pacing is for you: quiet, building up, ready for it, or it is here. Optionally skips ahead. */
    private static int director(CommandContext<CommandSourceStack> ctx, ServerPlayer target, boolean next) {
        if (next) {
            Director.advance(target);
        }
        String state = Director.describe(target);
        ctx.getSource().sendSuccess(() -> Component.literal(state), false);
        return 1;
    }

    /** Teaches it a habit of the target's, for testing what it does with it. */
    private static int habit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        String name = StringArgumentType.getString(ctx, "habit");
        Habits.Habit habit;
        try {
            habit = Habits.Habit.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.unknown", name));
            return 0;
        }
        Habits.set(target, habit, FloatArgumentType.getFloat(ctx, "value"));
        String state = Director.describe(target);
        ctx.getSource().sendSuccess(() -> Component.literal(state), false);
        return 1;
    }

    /** What you may wake up to: cracks and claw marks on the wall by your bed. */
    private static int morning(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        BlockPos bed = HomeVisit.bedOf(target);
        if (bed == null || !Gnawing.byTheBed(target.server.overworld(), bed)) {
            ctx.getSource().sendFailure(Component.translatable("commands.cornerofeye.visit.failed"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.morning", target.getDisplayName()), true);
        return 1;
    }

    private static int bloodMoon(CommandContext<CommandSourceStack> ctx, boolean on) {
        HauntData data = data(ctx);
        data.setBloodMoon(on);
        ctx.getSource().getServer().getPlayerList().getPlayers().forEach(player -> Nights.sync(player, data));
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.bloodmoon", on ? "✔" : "✘"), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        data(ctx).reset();
        ctx.getSource().getServer().getPlayerList().getPlayers().forEach(Journal::reset);
        TheOneEntity existing = HauntManager.findTheOne(ctx.getSource().getServer().overworld());
        if (existing != null) {
            existing.discard();
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cornerofeye.reset"), true);
        return 1;
    }

    private HauntCommand() {
    }
}
