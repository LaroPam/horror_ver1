package com.laropam.cornerofeye.command;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.HauntEvent;
import com.laropam.cornerofeye.haunt.HauntManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
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
                .then(Commands.literal("event")
                        .then(Commands.argument("event", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(HauntEvent.values()).map(HauntEvent::id), builder))
                                .executes(ctx -> event(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> event(ctx, EntityArgument.getPlayer(ctx, "target"))))))
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

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        data(ctx).reset();
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
