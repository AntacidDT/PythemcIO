package com.pythemcio.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pythemcio.PythemcIO;
import com.pythemcio.event.EventType;
import com.pythemcio.security.SecurityManager;
import com.pythemcio.trigger.Trigger;
import com.pythemcio.trigger.TriggerManager;
import com.pythemcio.server.FileWatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
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
                    .then(ClientCommandManager.literal("o")
                        .then(ClientCommandManager.argument("event", StringArgumentType.word())
                            .then(ClientCommandManager.literal("filter")
                                .then(ClientCommandManager.argument("argument", StringArgumentType.word())
                                    .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                        .executes(ctx -> executeAdd(ctx, "o", true)))))
                            .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> executeAdd(ctx, "o", false)))))
                    .then(ClientCommandManager.literal("i")
                        .then(ClientCommandManager.argument("event", StringArgumentType.word())
                            .then(ClientCommandManager.argument("action", StringArgumentType.greedyString())
                                .executes(ctx -> executeAdd(ctx, "i", false))))))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("event", StringArgumentType.word())
                        .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(1))
                            .executes(PythemcioCommand::executeRemove))))
                .then(ClientCommandManager.literal("list")
                    .executes(PythemcioCommand::executeList))
                .then(ClientCommandManager.literal("clear")
                    .executes(PythemcioCommand::executeClear))
                .then(ClientCommandManager.literal("disable")
                    .executes(PythemcioCommand::executeDisableAll)
                    .then(ClientCommandManager.literal("i")
                        .executes(PythemcioCommand::executeDisableInput))
                    .then(ClientCommandManager.literal("o")
                        .executes(PythemcioCommand::executeDisableOutput)))
                .then(ClientCommandManager.literal("enable")
                    .executes(PythemcioCommand::executeEnableAll)
                    .then(ClientCommandManager.literal("i")
                        .executes(PythemcioCommand::executeEnableInput))
                    .then(ClientCommandManager.literal("o")
                        .executes(PythemcioCommand::executeEnableOutput)))
            );
        });
    }

    private static int executeHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] === PythemcIO Help ==="
        ));
        context.getSource().sendFeedback(Component.literal(
            "  OUTPUT (game -> OS):"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add o <event> <command>              - Add game->OS trigger"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add o <event> filter <arg> <command>  - With filter"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  INPUT (OS -> game):"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio enable i                             - Start file watcher"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  Then write files to the inbox folder:"
        ));
        context.getSource().sendFeedback(Component.literal(
            "    echo 'Hello!' > inbox/chat                    - Send chat"
        ));
        context.getSource().sendFeedback(Component.literal(
            "    echo '/time set night' > inbox/command        - Run command"
        ));
        context.getSource().sendFeedback(Component.literal(
            "    echo 'Boss!' > inbox/title                    - Show title"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  GENERAL:"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio list                                  - List all triggers"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio clear                                 - Remove all triggers"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio enable [i|o]                          - Enable triggers"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio disable [i|o]                         - Disable triggers"
        ));
        return 1;
    }

    private static int executeAdd(CommandContext<FabricClientCommandSource> context, String direction, boolean hasFilter) {
        String event = StringArgumentType.getString(context, "event");
        String command;

        if ("i".equals(direction)) {
            command = StringArgumentType.getString(context, "action").trim();
        } else {
            command = StringArgumentType.getString(context, "command").trim();
        }

        if (EventType.fromName(event) == null && "o".equals(direction)) {
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

        String argument = null;
        if (hasFilter) {
            argument = StringArgumentType.getString(context, "argument").trim();
            if (!FILTERABLE_EVENTS.contains(event)) {
                context.getSource().sendError(Component.literal(
                    "[PythemcIO] Event '" + event + "' does not support filtering."
                ));
                return 0;
            }
        }

        if ("o".equals(direction)) {
            SecurityManager.ValidationResult validation = SecurityManager.validate(command);
            if (!validation.isValid()) {
                context.getSource().sendError(Component.literal(
                    "[PythemcIO] Command blocked: " + validation.getMessage()
                ));
                PythemcIO.LOGGER.warn("[PythemcIO] Blocked command: {} - {}", command, validation.getMessage());
                return 0;
            }
        }

        Trigger trigger = TriggerManager.addTrigger(event, argument, new String[]{command}, direction);

        String argStr = (argument != null) ? " (filter: " + argument + ")" : "";
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] + Trigger #" + trigger.getId() + " [" + direction + "] [" + event + "]" + argStr + " -> " + command
        ));
        return 1;
    }

    private static int executeRemove(CommandContext<FabricClientCommandSource> context) {
        String event = StringArgumentType.getString(context, "event");
        int id = context.getArgument("id", Integer.class);

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
        String outStatus = TriggerManager.isEnabledOutput() ? "ENABLED" : "DISABLED";
        String inStatus = TriggerManager.isEnabledInput() ? "ENABLED" : "DISABLED";

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] === Output(o)=" + outStatus + " | Input(i)=" + inStatus + " ==="
        ));

        int total = 0;

        Map<String, List<Trigger>> all = TriggerManager.getAllTriggers();
        if (!all.isEmpty()) {
            for (Map.Entry<String, List<Trigger>> entry : all.entrySet()) {
                for (Trigger trigger : entry.getValue()) {
                    String argStr = trigger.hasArgument() ? " (filter: " + trigger.getArgument() + ")" : "";
                    context.getSource().sendFeedback(Component.literal(
                        "  #" + trigger.getId() + " [o] [" + trigger.getEvent() + "]" + argStr + " -> " + String.join(", ", trigger.getCommands())
                    ));
                    total++;
                }
            }
        }

        Map<String, Trigger> inputs = TriggerManager.getAllInputTriggers();
        for (Map.Entry<String, Trigger> entry : inputs.entrySet()) {
            Trigger trigger = entry.getValue();
            context.getSource().sendFeedback(Component.literal(
                "  #" + trigger.getId() + " [i] [" + trigger.getEvent() + "] -> " + String.join(", ", trigger.getCommands())
            ));
            total++;
        }

        if (total == 0) {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] No triggers configured."
            ));
        } else {
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
        count += TriggerManager.getAllInputTriggers().size();

        TriggerManager.clearAll();

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Cleared " + count + " trigger(s)."
        ));
        return 1;
    }

    private static int executeEnableAll(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledOutput(true);
        TriggerManager.setEnabledInput(true);
        if (!FileWatcher.isRunning()) {
            FileWatcher.start(FabricLoader.getInstance().getGameDir().resolve("config").resolve("pythemcio"));
        }
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Enabled all triggers. File watcher: " + FileWatcher.getInboxDir()
        ));
        return 1;
    }

    private static int executeEnableInput(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledInput(true);
        if (!FileWatcher.isRunning()) {
            FileWatcher.start(FabricLoader.getInstance().getGameDir().resolve("config").resolve("pythemcio"));
        }
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Enabled input (OS->Game). Write files to: " + FileWatcher.getInboxDir()
        ));
        return 1;
    }

    private static int executeEnableOutput(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledOutput(true);
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Enabled output (Game->OS) triggers."
        ));
        return 1;
    }

    private static int executeDisableAll(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledOutput(false);
        TriggerManager.setEnabledInput(false);
        FileWatcher.stop();
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Disabled all triggers. File watcher stopped."
        ));
        return 1;
    }

    private static int executeDisableInput(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledInput(false);
        FileWatcher.stop();
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Disabled input (OS->Game). File watcher stopped."
        ));
        return 1;
    }

    private static int executeDisableOutput(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledOutput(false);
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Disabled output (Game->OS) triggers."
        ));
        return 1;
    }
}
