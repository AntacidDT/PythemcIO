package com.pythemcio.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pythemcio.PythemcIO;
import com.pythemcio.trigger.Trigger;
import com.pythemcio.trigger.TriggerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class GameActionHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String handle(String body, String apiKey) {
        try {
            JsonObject request = JsonParser.parseString(body).getAsJsonObject();

            if (request.has("api_key")) {
                String provided = request.get("api_key").getAsString();
                if (!provided.equals(apiKey)) {
                    return error("Invalid API key");
                }
            }

            String eventName = request.has("event") ? request.get("event").getAsString() : null;
            if (eventName == null || eventName.isEmpty()) {
                return error("Missing 'event' field");
            }

            if (!TriggerManager.isEnabledInput()) {
                return error("Input triggers are disabled. Use /pythemcio enable i");
            }

            Trigger trigger = TriggerManager.getInputTrigger(eventName);
            if (trigger == null) {
                return error("No trigger registered for event: " + eventName);
            }

            String action = trigger.getCommands()[0];
            String resolved = resolveVariables(action, eventName, request);

            PythemcIO.LOGGER.info("[PythemcIO] Input event: {} -> executing: {}", eventName, resolved);

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return error("No player in game");
            }

            String actionType = detectActionType(resolved);

            mc.execute(() -> {
                try {
                    executeGameAction(actionType, resolved, mc);
                } catch (Exception e) {
                    PythemcIO.LOGGER.error("[PythemcIO] Failed to execute game action: {}", resolved, e);
                }
            });

            return ok(eventName, actionType);

        } catch (Exception e) {
            return error("Invalid request: " + e.getMessage());
        }
    }

    private static String detectActionType(String action) {
        String lower = action.toLowerCase();
        if (lower.startsWith("chat ")) return "send_chat";
        if (lower.startsWith("command ")) return "run_command";
        if (lower.startsWith("title ")) return "show_title";
        if (lower.startsWith("subtitle ")) return "show_subtitle";
        if (lower.startsWith("actionbar ")) return "action_bar";
        return "command";
    }

    private static void executeGameAction(String type, String action, Minecraft mc) {
        if (mc.player == null) return;

        switch (type) {
            case "send_chat" -> {
                String message = action.substring(5).trim();
                mc.player.connection.sendChat(message);
            }
            case "run_command" -> {
                String command = action.substring(8).trim();
                if (command.startsWith("/")) {
                    mc.player.connection.sendCommand(command.substring(1));
                } else {
                    mc.player.connection.sendCommand(command);
                }
            }
            case "show_title" -> {
                String text = action.substring(6).trim();
                mc.player.connection.sendCommand("title @s title " + text);
            }
            case "show_subtitle" -> {
                String text = action.substring(9).trim();
                mc.player.connection.sendCommand("title @s subtitle " + text);
            }
            case "action_bar" -> {
                String text = action.substring(10).trim();
                mc.player.connection.sendCommand("title @s actionbar " + text);
            }
            default -> {
                if (action.startsWith("/")) {
                    mc.player.connection.sendCommand(action.substring(1));
                } else {
                    mc.player.connection.sendCommand(action);
                }
            }
        }
    }

    private static String resolveVariables(String action, String eventName, JsonObject request) {
        String result = action;
        for (Map.Entry<String, com.google.gson.JsonElement> entry : request.entrySet()) {
            String key = "$" + entry.getKey().toUpperCase();
            String value = entry.getValue().getAsString();
            result = result.replace(key, value);
        }
        result = result.replace("$EVENT", eventName);
        return result;
    }

    private static String ok(String event, String actionType) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "ok");
        response.addProperty("event", event);
        response.addProperty("action", actionType);
        return GSON.toJson(response);
    }

    private static String error(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("message", message);
        return GSON.toJson(response);
    }
}
