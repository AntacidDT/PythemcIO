package com.pythemcio.event;

import com.pythemcio.PythemcIO;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

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
    private static boolean prevOnFire = false;
    private static boolean prevInWater = false;
    private static boolean prevSprinting = false;
    private static boolean prevFallFlying = false;
    private static boolean prevSneaking = false;
    private static boolean prevUsingItem = false;
    private static int prevRedstonePower = -1;

    private static int tickCounter = 0;

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
            checkOnFire(player);
            checkInWater(player);
            checkSprint(player);
            checkElytra(player);
            checkSneak(player);
            checkUsingItem(player);

            tickCounter++;
            if (tickCounter % 5 == 0) {
                checkRedstone(client, player);
            }
        });

        PythemcIO.LOGGER.info("[PythemcIO] Player state tracker initialized.");
    }

    private static void checkDimension(LocalPlayer player) {
        ResourceKey<Level> current = player.level().dimension();
        if (prevDimension != null && current != prevDimension) {
            String dimName = current.location().toString();
            PythemcIO.LOGGER.info("[PythemcIO] Dimension changed: {} -> {}", prevDimension.location(), dimName);
            EventRegistry.fireEvent(EventType.DIMENSION_CHANGE, dimName);
        }
        prevDimension = current;
    }

    private static void checkHealth(LocalPlayer player) {
        float current = player.getHealth();
        if (prevHealth >= 0 && Float.compare(current, prevHealth) != 0) {
            PythemcIO.LOGGER.info("[PythemcIO] Health changed: {} -> {}", prevHealth, current);
            EventRegistry.fireEvent(EventType.HEALTH_CHANGE, null);
        }
        prevHealth = current;
    }

    private static void checkFood(LocalPlayer player) {
        int current = player.getFoodData().getFoodLevel();
        if (prevFood >= 0 && current != prevFood) {
            PythemcIO.LOGGER.info("[PythemcIO] Food changed: {} -> {}", prevFood, current);
            EventRegistry.fireEvent(EventType.FOOD_CHANGE, null);
        }
        prevFood = current;
    }

    private static void checkArmor(LocalPlayer player) {
        int current = player.getArmorValue();
        if (prevArmor >= 0 && current != prevArmor) {
            PythemcIO.LOGGER.info("[PythemcIO] Armor changed: {} -> {}", prevArmor, current);
            EventRegistry.fireEvent(EventType.ARMOR_CHANGE, null);
        }
        prevArmor = current;
    }

    private static void checkXp(LocalPlayer player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        if (prevXpLevel >= 0 && (level != prevXpLevel || Float.compare(progress, prevXpProgress) != 0)) {
            PythemcIO.LOGGER.info("[PythemcIO] XP changed: level {}->{}", prevXpLevel, level);
            EventRegistry.fireEvent(EventType.XP_CHANGE, null);
        }
        prevXpLevel = level;
        prevXpProgress = progress;
    }

    private static void checkAlive(LocalPlayer player) {
        boolean alive = !player.isDeadOrDying();
        if (prevAlive && !alive) {
            PythemcIO.LOGGER.info("[PythemcIO] Player died");
            EventRegistry.fireEvent(EventType.DEATH, null);
        } else if (!prevAlive && alive) {
            PythemcIO.LOGGER.info("[PythemcIO] Player respawned");
            EventRegistry.fireEvent(EventType.RESPAWN, null);
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
                String timeName = isDay ? "day" : "night";
                PythemcIO.LOGGER.info("[PythemcIO] Time changed: {} -> {}", wasDay ? "day" : "night", timeName);
                EventRegistry.fireEvent(EventType.TIME_CHANGE, timeName);
            }
        }
        prevDayTime = currentTime;
    }

    private static void checkSleep(LocalPlayer player) {
        boolean sleeping = player.isSleeping();
        if (!wasSleeping && sleeping) {
            PythemcIO.LOGGER.info("[PythemcIO] Player started sleeping");
            EventRegistry.fireEvent(EventType.SLEEP, null);
        } else if (wasSleeping && !sleeping) {
            PythemcIO.LOGGER.info("[PythemcIO] Player woke up");
            EventRegistry.fireEvent(EventType.WAKE_UP, null);
        }
        wasSleeping = sleeping;
    }

    private static void checkOnFire(LocalPlayer player) {
        boolean current = player.isOnFire();
        if (!prevOnFire && current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player is on fire");
            EventRegistry.fireEvent(EventType.ON_FIRE, null);
        }
        prevOnFire = current;
    }

    private static void checkInWater(LocalPlayer player) {
        boolean current = player.isInWater();
        if (!prevInWater && current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player entered water");
            EventRegistry.fireEvent(EventType.IN_WATER, null);
        } else if (prevInWater && !current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player left water");
            EventRegistry.fireEvent(EventType.IN_WATER, null);
        }
        prevInWater = current;
    }

    private static void checkSprint(LocalPlayer player) {
        boolean current = player.isSprinting();
        if (!prevSprinting && current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player started sprinting");
            EventRegistry.fireEvent(EventType.SPRINT, null);
        } else if (prevSprinting && !current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped sprinting");
            EventRegistry.fireEvent(EventType.SPRINT, null);
        }
        prevSprinting = current;
    }

    private static void checkElytra(LocalPlayer player) {
        boolean current = player.isFallFlying();
        if (!prevFallFlying && current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player started elytra flight");
            EventRegistry.fireEvent(EventType.ELYTRA, null);
        } else if (prevFallFlying && !current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped elytra flight");
            EventRegistry.fireEvent(EventType.ELYTRA, null);
        }
        prevFallFlying = current;
    }

    private static void checkSneak(LocalPlayer player) {
        boolean current = player.isShiftKeyDown();
        if (!prevSneaking && current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player started sneaking");
            EventRegistry.fireEvent(EventType.SNEAK, null);
        } else if (prevSneaking && !current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped sneaking");
            EventRegistry.fireEvent(EventType.SNEAK, null);
        }
        prevSneaking = current;
    }

    private static void checkUsingItem(LocalPlayer player) {
        boolean current = player.isUsingItem();
        if (!prevUsingItem && current) {
            String itemName = player.getUseItem().getItem().builtInRegistryHolder().key().location().toString();
            PythemcIO.LOGGER.info("[PythemcIO] Player started using item: {}", itemName);
            EventRegistry.fireEvent(EventType.USING_ITEM, itemName);
        } else if (prevUsingItem && !current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped using item");
            EventRegistry.fireEvent(EventType.USING_ITEM, null);
        }
        prevUsingItem = current;
    }

    private static void checkRedstone(Minecraft client, LocalPlayer player) {
        if (client.level == null) return;
        BlockPos playerPos = player.blockPosition();
        int maxPower = 0;

        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState state = client.level.getBlockState(checkPos);
                    if (state.hasProperty(BlockStateProperties.POWER)) {
                        int power = state.getValue(BlockStateProperties.POWER);
                        if (power > maxPower) {
                            maxPower = power;
                        }
                    }
                }
            }
        }

        if (prevRedstonePower >= 0 && maxPower != prevRedstonePower && maxPower > 0) {
            PythemcIO.LOGGER.info("[PythemcIO] Redstone signal detected: power {}", maxPower);
            EventRegistry.fireEvent(EventType.REDSTONE_SIGNAL, String.valueOf(maxPower));
        }
        prevRedstonePower = maxPower;
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
        prevOnFire = false;
        prevInWater = false;
        prevSprinting = false;
        prevFallFlying = false;
        prevSneaking = false;
        prevUsingItem = false;
        prevRedstonePower = -1;
        tickCounter = 0;
    }
}
