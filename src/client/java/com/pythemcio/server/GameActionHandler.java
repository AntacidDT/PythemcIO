package com.pythemcio.server;

import com.pythemcio.PythemcIO;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class GameActionHandler {

    public static String detectType(String action) {
        String lower = action.toLowerCase();
        if (lower.startsWith("chat ")) return "send_chat";
        if (lower.startsWith("command ")) return "run_command";
        if (lower.startsWith("title ")) return "show_title";
        if (lower.startsWith("subtitle ")) return "show_subtitle";
        if (lower.startsWith("actionbar ")) return "action_bar";
        return "send_chat";
    }

    public static void executeAction(String type, String action, Minecraft mc) {
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
}
