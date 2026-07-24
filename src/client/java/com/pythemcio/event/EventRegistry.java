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
            fireEvent(EventType.PLAYER_JOIN);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            fireEvent(EventType.PLAYER_LEAVE);
        });

        ClientSendMessageEvents.CHAT.register((message) -> {
            fireEvent(EventType.CHAT_MESSAGE);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            fireEvent(EventType.BLOCK_BREAK);
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            fireEvent(EventType.BLOCK_PLACE);
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            fireEvent(EventType.PLAYER_ATTACK);
            return InteractionResult.PASS;
        });

        PythemcIO.LOGGER.info("[PythemcIO] Event registry initialized.");
    }

    public static void fireEvent(EventType eventType) {
        if (!TriggerManager.isEnabled()) return;

        List<Trigger> triggers = TriggerManager.getTriggersForEvent(eventType.getName());
        if (triggers.isEmpty()) return;

        PythemcIO.LOGGER.info("[PythemcIO] Event fired: {} ({} trigger(s))", eventType.getName(), triggers.size());

        for (Trigger trigger : triggers) {
            for (String command : trigger.getCommands()) {
                PythemcIO.LOGGER.info("[PythemcIO] Executing: {}", command);
                CommandExecutor.execute(command);
            }
        }
    }
}
