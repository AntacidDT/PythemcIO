package com.pythemcio.event;

import com.pythemcio.PythemcIO;
import com.pythemcio.compat.MCCompat;
import com.pythemcio.executor.CommandExecutor;
import com.pythemcio.trigger.Trigger;
import com.pythemcio.trigger.TriggerManager;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;

import java.util.List;

public class EventRegistry {

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String worldName;
            if (client.getCurrentServer() != null) {
                worldName = "mp_" + client.getCurrentServer().name;
            } else if (client.hasSingleplayerServer()) {
                worldName = client.getSingleplayerServer().getWorldData().getLevelName();
            } else {
                worldName = "local";
            }
            TriggerManager.setWorld(worldName);
            fireEvent(EventType.PLAYER_JOIN, null, 0);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            fireEvent(EventType.PLAYER_LEAVE, null, 0);
        });

        ClientSendMessageEvents.CHAT.register((message) -> {
            fireEvent(EventType.CHAT_MESSAGE, message, 0);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            String blockName = MCCompat.blockName(world.getBlockState(pos));
            fireEvent(EventType.BLOCK_BREAK, blockName, 0);
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            String blockName = MCCompat.blockName(world.getBlockState(hitResult.getBlockPos()));
            fireEvent(EventType.BLOCK_PLACE, blockName, 0);
            fireEvent(EventType.BLOCK_INTERACT, blockName, 0);
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            String entityName = MCCompat.entityTypeName(entity.getType());
            fireEvent(EventType.PLAYER_ATTACK, entityName, 0);
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            String entityName = MCCompat.entityTypeName(entity.getType());
            fireEvent(EventType.ENTITY_INTERACT, entityName, 0);
            return InteractionResult.PASS;
        });

        PythemcIO.LOGGER.info("[PythemcIO] Event registry initialized.");
    }

    public static void fireEvent(EventType eventType, String context, int duration) {
        fireEvent(eventType.getName(), context, duration);
    }

    public static void fireEvent(EventType eventType, String context) {
        fireEvent(eventType.getName(), context, 0);
    }

    public static void fireEvent(String eventName, String context, int duration) {
        if (!TriggerManager.isEnabledOutput()) return;

        List<Trigger> triggers = TriggerManager.getTriggersForEvent(eventName);
        if (triggers.isEmpty()) return;

        int matched = 0;
        for (Trigger trigger : triggers) {
            if (!trigger.matchesContext(context)) continue;
            if (trigger.getDuration() > 0 && duration < trigger.getDuration()) continue;
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
