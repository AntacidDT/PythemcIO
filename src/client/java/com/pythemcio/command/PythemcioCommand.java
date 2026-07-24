package com.pythemcio.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pythemcio.event.EventType;
import com.pythemcio.security.SecurityManager;
import com.pythemcio.trigger.Trigger;
import com.pythemcio.trigger.TriggerManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

public class PythemcioCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("pythemcio")
                .then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.argument("event", StringArgumentType.word())
                        .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                            .executes(PythemcioCommand::executeAdd))))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("event", StringArgumentType.word())
                        .then(ClientCommandManager.argument("id", IntegerArgumentType.integer())
                            .executes(PythemcioCommand::executeRemove))))
                .then(ClientCommandManager.literal("list")
                    .executes(PythemcioCommand::executeList))
                .then(ClientCommandManager.literal("disable")
                    .executes(PythemcioCommand::executeDisable))
                .then(ClientCommandManager.literal("enable")
                    .executes(PythemcioCommand::executeEnable))
            );
        });
    }

    private static int executeAdd(CommandContext<FabricClientCommandSource> context) {
        String event = StringArgumentType.getString(context, "event");
        String command = StringArgumentType.getString(context, "command");

        if (EventType.fromName(event) == null) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Unknown event: " + event + ". Use /pythemcio list to see valid events."
            ));
            return 0;
        }

        SecurityManager.ValidationResult validation = SecurityManager.validate(command);
        if (!validation.isValid()) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Command blocked: " + validation.getMessage()
            ));
            return 0;
        }

        String[] commands = command.split("&&(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (int i = 0; i < commands.length; i++) {
            commands[i] = commands[i].trim().replaceAll("^\"|\"$", "");
        }

        Trigger trigger = TriggerManager.addTrigger(event, commands);

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Added trigger #" + trigger.getId() + ": " + event + " -> " + command
        ));
        return 1;
    }

    private static int executeRemove(CommandContext<FabricClientCommandSource> context) {
        String event = StringArgumentType.getString(context, "event");
        int id = context.getArgument("id", Integer.class);

        if (TriggerManager.removeTrigger(event, id)) {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] Removed trigger #" + id + " from " + event
            ));
        } else {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Trigger #" + id + " not found for event " + event
            ));
            return 0;
        }
        return 1;
    }

    private static int executeList(CommandContext<FabricClientCommandSource> context) {
        Map<String, List<Trigger>> all = TriggerManager.getAllTriggers();

        if (all.isEmpty()) {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] No triggers configured."
            ));
        } else {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] Active triggers:"
            ));
            for (Map.Entry<String, List<Trigger>> entry : all.entrySet()) {
                for (Trigger trigger : entry.getValue()) {
                    context.getSource().sendFeedback(Component.literal(
                        "  #" + trigger.getId() + " [" + trigger.getEvent() + "] -> " + String.join(", ", trigger.getCommands())
                    ));
                }
            }
        }

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Status: " + (TriggerManager.isEnabled() ? "ENABLED" : "DISABLED")
        ));
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Valid events: dimension_change, health_change, food_change, armor_change, xp_change, redstone_signal, player_join, player_leave, item_pickup, item_drop, block_break, block_place, chat_message, time_change, death, respawn, sleep, wake_up"
        ));
        return 1;
    }

    private static int executeDisable(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabled(false);
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Mod disabled. No triggers will fire."
        ));
        return 1;
    }

    private static int executeEnable(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabled(true);
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Mod enabled."
        ));
        return 1;
    }
}
