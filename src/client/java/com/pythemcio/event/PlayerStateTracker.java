package com.pythemcio.event;

import com.pythemcio.PythemcIO;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class PlayerStateTracker {

    private static float prevHealth = -1f;
    private static int prevFood = -1;
    private static int prevArmor = -1;
    private static int prevXpLevel = -1;
    private static float prevXpProgress = -1f;
    private static ResourceKey<Level> prevDimension = null;
    private static boolean prevAlive = true;
    private static long prevDayTime = -1;
    private static boolean wasSleeping = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) return;

            checkDimension(player);
            checkHealth(player);
            checkFood(player);
            checkArmor(player);
            checkXp(player);
            checkAlive(player);
            checkTime(client);
            checkSleep(player);
        });

        PythemcIO.LOGGER.info("[PythemcIO] Player state tracker initialized.");
    }

    private static void checkDimension(LocalPlayer player) {
        ResourceKey<Level> current = player.level().dimension();
        if (prevDimension != null && current != prevDimension) {
            PythemcIO.LOGGER.info("[PythemcIO] Dimension changed: {} -> {}", prevDimension.location(), current.location());
            EventRegistry.fireEvent(EventType.DIMENSION_CHANGE);
        }
        prevDimension = current;
    }

    private static void checkHealth(LocalPlayer player) {
        float current = player.getHealth();
        if (prevHealth >= 0 && Float.compare(current, prevHealth) != 0) {
            PythemcIO.LOGGER.info("[PythemcIO] Health changed: {} -> {}", prevHealth, current);
            EventRegistry.fireEvent(EventType.HEALTH_CHANGE);
        }
        prevHealth = current;
    }

    private static void checkFood(LocalPlayer player) {
        int current = player.getFoodData().getFoodLevel();
        if (prevFood >= 0 && current != prevFood) {
            PythemcIO.LOGGER.info("[PythemcIO] Food changed: {} -> {}", prevFood, current);
            EventRegistry.fireEvent(EventType.FOOD_CHANGE);
        }
        prevFood = current;
    }

    private static void checkArmor(LocalPlayer player) {
        int current = player.getArmorValue();
        if (prevArmor >= 0 && current != prevArmor) {
            PythemcIO.LOGGER.info("[PythemcIO] Armor changed: {} -> {}", prevArmor, current);
            EventRegistry.fireEvent(EventType.ARMOR_CHANGE);
        }
        prevArmor = current;
    }

    private static void checkXp(LocalPlayer player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        if (prevXpLevel >= 0 && (level != prevXpLevel || Float.compare(progress, prevXpProgress) != 0)) {
            PythemcIO.LOGGER.info("[PythemcIO] XP changed: level {}->{}, progress {}->{}", prevXpLevel, level, prevXpProgress, progress);
            EventRegistry.fireEvent(EventType.XP_CHANGE);
        }
        prevXpLevel = level;
        prevXpProgress = progress;
    }

    private static void checkAlive(LocalPlayer player) {
        boolean alive = !player.isDeadOrDying();
        if (prevAlive && !alive) {
            PythemcIO.LOGGER.info("[PythemcIO] Player died");
            EventRegistry.fireEvent(EventType.DEATH);
        } else if (!prevAlive && alive) {
            PythemcIO.LOGGER.info("[PythemcIO] Player respawned");
            EventRegistry.fireEvent(EventType.RESPAWN);
        }
        prevAlive = alive;
    }

    private static void checkTime(Minecraft client) {
        if (client.level == null) return;
        long currentTime = client.level.getDayTime();
        if (prevDayTime >= 0 && currentTime != prevDayTime) {
            long prevPhase = prevDayTime % 24000;
            long currPhase = currentTime % 24000;
            boolean wasDay = prevPhase < 12000;
            boolean isDay = currPhase < 12000;
            if (wasDay != isDay) {
                PythemcIO.LOGGER.info("[PythemcIO] Time changed: {} -> {}", wasDay ? "day" : "night", isDay ? "day" : "night");
                EventRegistry.fireEvent(EventType.TIME_CHANGE);
            }
        }
        prevDayTime = currentTime;
    }

    private static void checkSleep(LocalPlayer player) {
        boolean sleeping = player.isSleeping();
        if (!wasSleeping && sleeping) {
            PythemcIO.LOGGER.info("[PythemcIO] Player started sleeping");
            EventRegistry.fireEvent(EventType.SLEEP);
        } else if (wasSleeping && !sleeping) {
            PythemcIO.LOGGER.info("[PythemcIO] Player woke up");
            EventRegistry.fireEvent(EventType.WAKE_UP);
        }
        wasSleeping = sleeping;
    }

    public static void reset() {
        prevHealth = -1f;
        prevFood = -1;
        prevArmor = -1;
        prevXpLevel = -1;
        prevXpProgress = -1f;
        prevDimension = null;
        prevAlive = true;
        prevDayTime = -1;
        wasSleeping = false;
    }
}
