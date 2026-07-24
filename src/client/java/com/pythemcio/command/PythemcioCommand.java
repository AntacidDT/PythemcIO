package com.pythemcio.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pythemcio.PythemcIO;
import com.pythemcio.event.EventType;
import com.pythemcio.security.SecurityManager;
import com.pythemcio.server.ScriptManager;
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
                    .then(ClientCommandManager.literal("o")
                        .then(ClientCommandManager.argument("event", StringArgumentType.word())
                            .then(ClientCommandManager.literal("filter")
                                .then(ClientCommandManager.argument("argument", StringArgumentType.word())
                                    .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                        .executes(ctx -> executeAddOutput(ctx, true)))))
                            .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> executeAddOutput(ctx, false)))))
                    .then(ClientCommandManager.literal("i")
                        .then(ClientCommandManager.argument("input", StringArgumentType.greedyString())
                            .executes(PythemcioCommand::executeAddInput))))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(1))
                        .executes(PythemcioCommand::executeRemove)))
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
            "  /pythemcio add o <event> <command>               - Game -> OS trigger"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add o <event> filter <arg> <command>   - Game -> OS with filter"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  INPUT (OS -> game):"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add i \"output\" from <script> then <action>"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  Example:"
        ));
        context.getSource().sendFeedback(Component.literal(
            "    /pythemcio add i \"rain\" from /path/to/weather.py then chat \"It's raining!\""
        ));
        context.getSource().sendFeedback(Component.literal(
            "  GENERAL:"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio enable [i|o]     - Enable (start scripts for i)"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio disable [i|o]    - Disable (kill scripts for i)"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio list             - List all triggers + running scripts"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio remove <id>      - Remove trigger by ID"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio clear            - Remove all triggers"
        ));
        return 1;
    }

    private static int executeAddOutput(CommandContext<FabricClientCommandSource> context, boolean hasFilter) {
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

        SecurityManager.ValidationResult validation = SecurityManager.validate(command);
        if (!validation.isValid()) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Command blocked: " + validation.getMessage()
            ));
            PythemcIO.LOGGER.warn("[PythemcIO] Blocked command: {} - {}", command, validation.getMessage());
            return 0;
        }

        Trigger trigger = TriggerManager.addTrigger(event, argument, new String[]{command}, "o");

        String argStr = (argument != null) ? " (filter: " + argument + ")" : "";
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] + #" + trigger.getId() + " [o] [" + event + "]" + argStr + " -> " + command
        ));
        return 1;
    }

    private static int executeAddInput(CommandContext<FabricClientCommandSource> context) {
        String input = StringArgumentType.getString(context, "input").trim();

        int fromIdx = input.indexOf(" from ");
        if (fromIdx == -1) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Invalid syntax. Use: /pythemcio add i \"output\" from <script> then <action>"
            ));
            return 0;
        }

        int thenIdx = input.indexOf(" then ", fromIdx + 6);
        if (thenIdx == -1) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Invalid syntax. Use: /pythemcio add i \"output\" from <script> then <action>"
            ));
            return 0;
        }

        String expectedOutput = input.substring(0, fromIdx).trim();
        String scriptPath = input.substring(fromIdx + 6, thenIdx).trim();
        String gameAction = input.substring(thenIdx + 6).trim();

        if (expectedOutput.isEmpty() || scriptPath.isEmpty() || gameAction.isEmpty()) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] All fields required. Use: /pythemcIO add i \"output\" from <script> then <action>"
            ));
            return 0;
        }

        Trigger trigger = TriggerManager.addScriptTrigger(expectedOutput, scriptPath, gameAction);

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] + #" + trigger.getId() + " [i] \"" + expectedOutput + "\" from " + scriptPath + " then " + gameAction
        ));
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Run /pythemcio enable i to start the script."
        ));
        return 1;
    }

    private static int executeRemove(CommandContext<FabricClientCommandSource> context) {
        int id = context.getArgument("id", Integer.class);

        if (TriggerManager.removeTrigger(null, id)) {
            ScriptManager.stopScript(id);
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] - Removed trigger #" + id
            ));
        } else {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Trigger #" + id + " not found."
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

        List<Trigger> scripts = TriggerManager.getScriptTriggers();
        for (Trigger trigger : scripts) {
            boolean running = ScriptManager.getRunningScripts().containsKey(trigger.getId());
            String status = running ? "RUNNING" : "STOPPED";
            context.getSource().sendFeedback(Component.literal(
                "  #" + trigger.getId() + " [i] [" + status + "] \"" + trigger.getExpectedOutput() + "\" from " + trigger.getScriptPath() + " then " + trigger.getGameAction()
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
        count += TriggerManager.getScriptTriggers().size();

        ScriptManager.stopAll();
        TriggerManager.clearAll();

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Cleared " + count + " trigger(s). Scripts stopped."
        ));
        return 1;
    }

    private static int executeEnableAll(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledOutput(true);
        TriggerManager.setEnabledInput(true);
        ScriptManager.startAll();
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Enabled all. " + ScriptManager.getRunningCount() + " script(s) started."
        ));
        return 1;
    }

    private static int executeEnableInput(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledInput(true);
        ScriptManager.startAll();
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Enabled input. " + ScriptManager.getRunningCount() + " script(s) started."
        ));
        return 1;
    }

    private static int executeEnableOutput(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledOutput(true);
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Enabled output (Game->OS)."
        ));
        return 1;
    }

    private static int executeDisableAll(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledOutput(false);
        TriggerManager.setEnabledInput(false);
        ScriptManager.stopAll();
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Disabled all. Scripts stopped."
        ));
        return 1;
    }

    private static int executeDisableInput(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledInput(false);
        ScriptManager.stopAll();
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Disabled input. Scripts stopped."
        ));
        return 1;
    }

    private static int executeDisableOutput(CommandContext<FabricClientCommandSource> context) {
        TriggerManager.setEnabledOutput(false);
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Disabled output (Game->OS)."
        ));
        return 1;
    }
}
