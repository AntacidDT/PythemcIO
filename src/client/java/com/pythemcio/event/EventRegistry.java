package com.pythemcio.event;

import com.pythemcio.PythemcIO;
import com.pythemcio.executor.CommandExecutor;
import com.pythemcio.trigger.Trigger;
import com.pythemcio.trigger.TriggerManager;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

import java.util.List;

public class EventRegistry {

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            fireEvent(EventType.PLAYER_JOIN, null);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            fireEvent(EventType.PLAYER_LEAVE, null);
        });

        ClientSendMessageEvents.CHAT.register((message) -> {
            fireEvent(EventType.CHAT_MESSAGE, message);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            String blockName = world.getBlockState(pos).getBlock().builtInRegistryHolder().key().location().toString();
            fireEvent(EventType.BLOCK_BREAK, blockName);
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            String blockName = world.getBlockState(hitResult.getBlockPos()).getBlock().builtInRegistryHolder().key().location().toString();
            fireEvent(EventType.BLOCK_PLACE, blockName);
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            String entityName = entity.getType().builtInRegistryHolder().key().location().toString();
            fireEvent(EventType.PLAYER_ATTACK, entityName);
            return InteractionResult.PASS;
        });

        PythemcIO.LOGGER.info("[PythemcIO] Event registry initialized.");
    }

    public static void fireEvent(EventType eventType, String context) {
        fireEvent(eventType.getName(), context);
    }

    public static void fireEvent(String eventName, String context) {
        if (!TriggerManager.isEnabledOutput()) return;

        List<Trigger> triggers = TriggerManager.getTriggersForEvent(eventName);
        if (triggers.isEmpty()) return;

        int matched = 0;
        for (Trigger trigger : triggers) {
            if (!trigger.matchesContext(context)) continue;
            matched++;
            for (String command : trigger.getCommands()) {
                String resolved = resolveVariables(command, eventName, context);
                PythemcIO.LOGGER.info("[PythemcIO] Executing: {}", resolved);
                CommandExecutor.execute(resolved);
            }
        }

        if (matched > 0) {
            PythemcIO.LOGGER.info("[PythemcIO] Event fired: {} ({} trigger(s) matched, context={})", eventName, matched, context);
        }
    }

    private static String resolveVariables(String command, String eventName, String context) {
        if (context == null) context = "";
        return command
            .replace("$CONTEXT", context)
            .replace("$EVENT", eventName)
            .replace("$ITEM", context)
            .replace("$BLOCK", context)
            .replace("$ENTITY", context);
    }
}
