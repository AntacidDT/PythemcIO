package com.pythemcio.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pythemcio.PythemcIO;
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
import java.util.Set;

public class PythemcioCommand {

    private static final Set<String> FILTERABLE_EVENTS = Set.of(
        "using_item", "item_pickup", "item_drop",
        "block_break", "block_place",
        "player_attack", "chat_message"
    );

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("pythemcio")
                .then(ClientCommandManager.literal("help")
                    .executes(PythemcioCommand::executeHelp))
                .then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.argument("event", StringArgumentType.word())
                        .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                            .executes(ctx -> executeAdd(ctx, null)))
                        .then(ClientCommandManager.literal("filter")
                            .then(ClientCommandManager.argument("argument", StringArgumentType.word())
                                .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                    .executes(PythemcioCommand::executeAddFiltered))))))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("event", StringArgumentType.word())
                        .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(1))
                            .executes(PythemcioCommand::executeRemove))))
                .then(ClientCommandManager.literal("list")
                    .executes(PythemcioCommand::executeList))
                .then(ClientCommandManager.literal("clear")
                    .executes(PythemcioCommand::executeClear))
                .then(ClientCommandManager.literal("disable")
                    .executes(PythemcioCommand::executeDisable))
                .then(ClientCommandManager.literal("enable")
                    .executes(PythemcioCommand::executeEnable))
            );
        });
    }

    private static int executeHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] === PythemcIO Help ==="
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add <event> <command>             - Add a trigger"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add <event> filter <arg> <command> - Add a filtered trigger"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio remove <event> <id>               - Remove a trigger by ID"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio list                              - List all triggers"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio clear                             - Remove all triggers"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio disable                           - Disable all triggers"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio enable                            - Enable all triggers"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio help                              - Show this help"
        ));
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Filterable events: " + String.join(", ", FILTERABLE_EVENTS)
        ));
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Example: /pythemcio add using_item filter bow python3 /path/to/script.py"
        ));
        return 1;
    }

    private static int executeAdd(CommandContext<FabricClientCommandSource> context, String argument) {
        String event = StringArgumentType.getString(context, "event");
        String command = StringArgumentType.getString(context, "command").trim();

        if (EventType.fromName(event) == null) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Unknown event: '" + event + "'. Use /pythemcio help"
            ));
            return 0;
        }

        if (command.isEmpty()) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Command cannot be empty."
            ));
            return 0;
        }

        SecurityManager.ValidationResult validation = SecurityManager.validate(command);
        if (!validation.isValid()) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Command blocked: " + validation.getMessage()
            ));
            PythemcIO.LOGGER.warn("[PythemcIO] Blocked command: {} - {}", command, validation.getMessage());
            return 0;
        }

        Trigger trigger = TriggerManager.addTrigger(event, argument, new String[]{command});

        String argStr = (argument != null) ? " (filter: " + argument + ")" : "";
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] + Trigger #" + trigger.getId() + ": [" + event + "]" + argStr + " -> " + command
        ));
        PythemcIO.LOGGER.info("[PythemcIO] Trigger added: #{} [{}]{} -> {}", trigger.getId(), event, argStr, command);
        return 1;
    }

    private static int executeAddFiltered(CommandContext<FabricClientCommandSource> context) {
        String event = StringArgumentType.getString(context, "event");
        String argument = StringArgumentType.getString(context, "argument").trim();
        String command = StringArgumentType.getString(context, "command").trim();

        if (EventType.fromName(event) == null) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Unknown event: '" + event + "'. Use /pythemcio help"
            ));
            return 0;
        }

        if (!FILTERABLE_EVENTS.contains(event)) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Event '" + event + "' does not support filtering."
            ));
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Filterable events: " + String.join(", ", FILTERABLE_EVENTS)
            ));
            return 0;
        }

        if (command.isEmpty()) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Command cannot be empty."
            ));
            return 0;
        }

        SecurityManager.ValidationResult validation = SecurityManager.validate(command);
        if (!validation.isValid()) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Command blocked: " + validation.getMessage()
            ));
            PythemcIO.LOGGER.warn("[PythemcIO] Blocked command: {} - {}", command, validation.getMessage());
            return 0;
        }

        Trigger trigger = TriggerManager.addTrigger(event, argument, new String[]{command});

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] + Trigger #" + trigger.getId() + ": [" + event + "] (filter: " + argument + ") -> " + command
        ));
        PythemcIO.LOGGER.info("[PythemcIO] Trigger added: #{} [{}] (filter: {}) -> {}", trigger.getId(), event, argument, command);
        return 1;
    }

    private static int executeRemove(CommandContext<FabricClientCommandSource> context) {
        String event = StringArgumentType.getString(context, "event");
        int id = context.getArgument("id", Integer.class);

        if (EventType.fromName(event) == null) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Unknown event: '" + event + "'. Use /pythemcio help"
            ));
            return 0;
        }

        if (TriggerManager.removeTrigger(event, id)) {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] - Removed trigger #" + id + " from [" + event + "]"
            ));
        } else {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Trigger #" + id + " not found for event [" + event + "]"
            ));
            return 0;
        }
        return 1;
    }

    private static int executeList(CommandContext<FabricClientCommandSource> context) {
        Map<String, List<Trigger>> all = TriggerManager.getAllTriggers();
        String status = TriggerManager.isEnabled() ? "ENABLED" : "DISABLED";

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] === Status: " + status + " ==="
        ));

        if (all.isEmpty()) {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] No triggers configured."
            ));
        } else {
            int total = 0;
            for (Map.Entry<String, List<Trigger>> entry : all.entrySet()) {
                for (Trigger trigger : entry.getValue()) {
                    String argStr = trigger.hasArgument() ? " (filter: " + trigger.getArgument() + ")" : "";
                    context.getSource().sendFeedback(Component.literal(
                        "  #" + trigger.getId() + " [" + trigger.getEvent() + "]" + argStr + " -> " + String.join(", ", trigger.getCommands())
                    ));
                    total++;
                }
            }
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] Total: " + total + " trigger(s)"
            ));
        }
        return 1;
    }

    private static int executeClear(CommandContext<FabricClientCommandSource> context) {
        Map<String, List<Trigger>> all = TriggerManager.getAllTriggers();
        int count = 0;
        for (List<Trigger> triggers : all.values()) {
            count += triggers.size();
        }

        TriggerManager.clearAll();

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Cleared " + count + " trigger(s)."
        ));
        return 1;
    }

    private static int executeDisable(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabled(false);
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Disabled. No triggers will fire."
        ));
        return 1;
    }

    private static int executeEnable(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabled(true);
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Enabled."
        ));
        return 1;
    }
}
