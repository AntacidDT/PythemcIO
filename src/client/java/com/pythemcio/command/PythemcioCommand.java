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
        "player_attack", "chat_message",
        "dimension_change", "death", "time_change",
        "velocity", "jump", "coordinates",
        "item_consume", "block_interact", "entity_interact",
        "potion_effect", "health_change"
    );

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("pythemcio")
                .then(ClientCommandManager.literal("help")
                    .executes(PythemcioCommand::executeHelp))
                .then(ClientCommandManager.literal("credits")
                    .executes(PythemcioCommand::executeCredits))
                .then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.literal("-output")
                        .then(ClientCommandManager.literal("global")
                            .then(ClientCommandManager.argument("event", StringArgumentType.word())
                                .then(ClientCommandManager.literal("filter")
                                    .then(ClientCommandManager.argument("argument", StringArgumentType.word())
                                        .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                            .executes(ctx -> executeAddOutput(ctx, true, "global")))))
                                .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                    .executes(ctx -> executeAddOutput(ctx, false, "global")))))
                        .then(ClientCommandManager.literal("local")
                            .then(ClientCommandManager.argument("event", StringArgumentType.word())
                                .then(ClientCommandManager.literal("filter")
                                    .then(ClientCommandManager.argument("argument", StringArgumentType.word())
                                        .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                            .executes(ctx -> executeAddOutput(ctx, true, "local")))))
                                .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                    .executes(ctx -> executeAddOutput(ctx, false, "local")))))
                        .then(ClientCommandManager.argument("event", StringArgumentType.word())
                            .then(ClientCommandManager.literal("filter")
                                .then(ClientCommandManager.argument("argument", StringArgumentType.word())
                                    .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                        .executes(ctx -> executeAddOutput(ctx, true, "local")))))
                            .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> executeAddOutput(ctx, false, "local")))))
                    .then(ClientCommandManager.literal("-input")
                        .then(ClientCommandManager.literal("global")
                            .then(ClientCommandManager.argument("input", StringArgumentType.greedyString())
                                .executes(ctx -> executeAddInput(ctx, "global"))))
                        .then(ClientCommandManager.literal("local")
                            .then(ClientCommandManager.argument("input", StringArgumentType.greedyString())
                                .executes(ctx -> executeAddInput(ctx, "local"))))
                        .then(ClientCommandManager.argument("input", StringArgumentType.greedyString())
                            .executes(ctx -> executeAddInput(ctx, "local")))))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(1))
                        .executes(PythemcioCommand::executeRemove)))
                .then(ClientCommandManager.literal("list")
                    .executes(PythemcioCommand::executeList))
                .then(ClientCommandManager.literal("clear")
                    .executes(PythemcioCommand::executeClear))
                .then(ClientCommandManager.literal("scope")
                    .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(1))
                        .then(ClientCommandManager.literal("global")
                            .executes(ctx -> executeSetScope(ctx, "global")))
                        .then(ClientCommandManager.literal("local")
                            .executes(ctx -> executeSetScope(ctx, "local")))))
                .then(ClientCommandManager.literal("duration")
                    .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(1))
                        .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(0))
                            .executes(PythemcioCommand::executeSetDuration))))
                .then(ClientCommandManager.literal("disable")
                    .executes(PythemcioCommand::executeDisableAll)
                    .then(ClientCommandManager.literal("-input")
                        .executes(PythemcioCommand::executeDisableInput))
                    .then(ClientCommandManager.literal("-output")
                        .executes(PythemcioCommand::executeDisableOutput)))
                .then(ClientCommandManager.literal("enable")
                    .executes(PythemcioCommand::executeEnableAll)
                    .then(ClientCommandManager.literal("-input")
                        .executes(PythemcioCommand::executeEnableInput))
                    .then(ClientCommandManager.literal("-output")
                        .executes(PythemcioCommand::executeEnableOutput)))
            );
        });
    }

    private static int executeCredits(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] === Credits ==="
        ));
        context.getSource().sendFeedback(Component.literal(
            "  Author: AntacidDT"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  GitHub: https://github.com/AntacidDT/PythemcIO"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  License: Apache-2.0"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  Released: 24.07.2026"
        ));
        return 1;
    }

    private static int executeHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] === PythemcIO Help ==="
        ));
        context.getSource().sendFeedback(Component.literal(
            "  OUTPUT (game -> OS):"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add -output <event> <command>               - Game -> OS trigger (local)"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add -output global <event> <command>        - Game -> OS trigger (global)"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add -output <event> filter <arg> <command>  - Game -> OS with filter"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  INPUT (OS -> game):"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add -input \"output\" from <script> then <action>"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio add -input global \"output\" from <script> then <action>"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  GENERAL:"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio enable [-input|-output]    - Enable (start scripts for -input)"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio disable [-input|-output]   - Disable (kill scripts for -input)"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio list              - List all triggers + running scripts"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio scope <id> global|local - Change trigger scope"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio duration <id> <sec>    - Set min active time (for continuous events)"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio remove <id>       - Remove trigger by ID"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  /pythemcio clear             - Remove all triggers"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  Filterable: using_item, item_pickup, item_drop,"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  block_break, block_place, player_attack, chat_message,"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  dimension_change, death, time_change, health_change,"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  velocity, jump, coordinates, item_consume,"
        ));
        context.getSource().sendFeedback(Component.literal(
            "  block_interact, entity_interact, potion_effect"
        ));
        return 1;
    }

    private static int executeAddOutput(CommandContext<FabricClientCommandSource> context, boolean hasFilter, String scope) {
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

        Trigger trigger = TriggerManager.addTrigger(event, argument, new String[]{command}, "o", scope);

        String scopeStr = "global".equals(scope) ? " [G]" : "";
        String argStr = (argument != null) ? " (filter: " + argument + ")" : "";
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] + #" + trigger.getId() + " [o]" + scopeStr + " [" + event + "]" + argStr + " -> " + command
        ));
        return 1;
    }

    private static int executeAddInput(CommandContext<FabricClientCommandSource> context, String scope) {
        String input = StringArgumentType.getString(context, "input").trim();

        int fromIdx = input.indexOf(" from ");
        if (fromIdx == -1) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Invalid syntax. Use: /pythemcio add -input [global] \"output\" from <script> then <action>"
            ));
            return 0;
        }

        int thenIdx = input.indexOf(" then ", fromIdx + 6);
        if (thenIdx == -1) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] Invalid syntax. Use: /pythemcio add -input [global] \"output\" from <script> then <action>"
            ));
            return 0;
        }

        String expectedOutput = input.substring(0, fromIdx).trim();
        String scriptPath = input.substring(fromIdx + 6, thenIdx).trim();
        String gameAction = input.substring(thenIdx + 6).trim();

        if (expectedOutput.isEmpty() || scriptPath.isEmpty() || gameAction.isEmpty()) {
            context.getSource().sendError(Component.literal(
                "[PythemcIO] All fields required. Use: /pythemcio add -input [global] \"output\" from <script> then <action>"
            ));
            return 0;
        }

        Trigger trigger = TriggerManager.addScriptTrigger(expectedOutput, scriptPath, gameAction, scope);

        String scopeStr = "global".equals(scope) ? " [G]" : "";
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] + #" + trigger.getId() + " [i]" + scopeStr + " \"" + expectedOutput + "\" from " + scriptPath + " then " + gameAction
        ));
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Run /pythemcio enable -input to start the script."
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

    private static int executeSetScope(CommandContext<FabricClientCommandSource> context, String scope) {
        int id = context.getArgument("id", Integer.class);
        TriggerManager.setScope(id, scope);
        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] Trigger #" + id + " scope set to " + scope
        ));
        return 1;
    }

    private static int executeSetDuration(CommandContext<FabricClientCommandSource> context) {
        int id = context.getArgument("id", Integer.class);
        int seconds = context.getArgument("seconds", Integer.class);
        TriggerManager.setDuration(id, seconds);
        if (seconds > 0) {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] Trigger #" + id + " duration set to " + seconds + "s"
            ));
        } else {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] Trigger #" + id + " duration removed"
            ));
        }
        return 1;
    }

    private static int executeList(CommandContext<FabricClientCommandSource> context) {
        String outStatus = TriggerManager.isEnabledOutput() ? "ENABLED" : "DISABLED";
        String inStatus = TriggerManager.isEnabledInput() ? "ENABLED" : "DISABLED";
        String worldName = TriggerManager.getCurrentWorldName();
        if (worldName == null) worldName = "unknown";

        context.getSource().sendFeedback(Component.literal(
            "[PythemcIO] === Output(o)=" + outStatus + " | Input(i)=" + inStatus + " | World=" + worldName + " ==="
        ));

        int total = 0;

        Map<String, List<Trigger>> all = TriggerManager.getAllTriggers();
        if (!all.isEmpty()) {
            for (Map.Entry<String, List<Trigger>> entry : all.entrySet()) {
                for (Trigger trigger : entry.getValue()) {
                    String scopeStr = trigger.isGlobal() ? " [G]" : "";
                    String enabledStr = trigger.isGlobal() && !TriggerManager.isGlobalTriggerEnabled(trigger.getId()) ? " [OFF]" : "";
                    String durStr = trigger.getDuration() > 0 ? " [dur:" + trigger.getDuration() + "s]" : "";
                    String argStr = trigger.hasArgument() ? " (filter: " + trigger.getArgument() + ")" : "";
                    context.getSource().sendFeedback(Component.literal(
                        "  #" + trigger.getId() + " [o]" + scopeStr + enabledStr + durStr + " [" + trigger.getEvent() + "]" + argStr + " -> " + String.join(", ", trigger.getCommands())
                    ));
                    total++;
                }
            }
        }

        List<Trigger> scripts = TriggerManager.getScriptTriggers();
        for (Trigger trigger : scripts) {
            boolean running = ScriptManager.getRunningScripts().containsKey(trigger.getId());
            String status = running ? "RUNNING" : "STOPPED";
            String scopeStr = trigger.isGlobal() ? " [G]" : "";
            context.getSource().sendFeedback(Component.literal(
                "  #" + trigger.getId() + " [i]" + scopeStr + " [" + status + "] \"" + trigger.getExpectedOutput() + "\" from " + trigger.getScriptPath() + " then " + trigger.getGameAction()
            ));
            total++;
        }

        if (total == 0) {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] No triggers configured."
            ));
        } else {
            context.getSource().sendFeedback(Component.literal(
                "[PythemcIO] Total: " + total + " trigger(s) | [G]=global"
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
