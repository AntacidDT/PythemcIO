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
    private static String lastUsedItem = "";
    private static int prevRedstonePower = -1;
    private static double prevSpeed = -1.0;
    private static boolean wasOnGround = true;
    private static int prevBlockX = Integer.MIN_VALUE;
    private static int prevBlockY = Integer.MIN_VALUE;
    private static int prevBlockZ = Integer.MIN_VALUE;
    private static boolean prevFlying = false;

    private static int tickCounter = 0;
    private static int onFireTicks = 0;
    private static int inWaterTicks = 0;
    private static int sprintTicks = 0;
    private static int elytraTicks = 0;
    private static int sneakTicks = 0;
    private static int usingItemTicks = 0;
    private static int flyingTicks = 0;

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
            checkFly(player);
            checkVelocity(player);
            checkJump(player);
            checkCoordinates(player);

            tickCounter++;
            if (tickCounter % 5 == 0) {
                checkRedstone(client, player);
            }
        });

        PythemcIO.LOGGER.info("[PythemcIO] Player state tracker initialized.");
    }

    private static int ticksToSeconds(int ticks) {
        return ticks / 20;
    }

    private static void checkDimension(LocalPlayer player) {
        ResourceKey<Level> current = player.level().dimension();
        if (prevDimension != null && current != prevDimension) {
            String dimName = current.location().toString();
            String shortName;
            if (dimName.contains("nether")) shortName = "nether";
            else if (dimName.contains("end")) shortName = "end";
            else shortName = "overworld";
            PythemcIO.LOGGER.info("[PythemcIO] Dimension changed: {} -> {}", prevDimension.location(), dimName);
            EventRegistry.fireEvent(EventType.DIMENSION_CHANGE, shortName, 0);
        }
        prevDimension = current;
    }

    private static void checkHealth(LocalPlayer player) {
        float current = player.getHealth();
        if (prevHealth >= 0 && Float.compare(current, prevHealth) != 0) {
            String healthStr = String.format("%.1f", current);
            PythemcIO.LOGGER.info("[PythemcIO] Health changed: {} -> {}", prevHealth, healthStr);
            EventRegistry.fireEvent(EventType.HEALTH_CHANGE, healthStr, 0);
        }
        prevHealth = current;
    }

    private static void checkFood(LocalPlayer player) {
        int current = player.getFoodData().getFoodLevel();
        if (prevFood >= 0 && current != prevFood) {
            PythemcIO.LOGGER.info("[PythemcIO] Food changed: {} -> {}", prevFood, current);
            EventRegistry.fireEvent(EventType.FOOD_CHANGE, null, 0);
        }
        prevFood = current;
    }

    private static void checkArmor(LocalPlayer player) {
        int current = player.getArmorValue();
        if (prevArmor >= 0 && current != prevArmor) {
            PythemcIO.LOGGER.info("[PythemcIO] Armor changed: {} -> {}", prevArmor, current);
            EventRegistry.fireEvent(EventType.ARMOR_CHANGE, null, 0);
        }
        prevArmor = current;
    }

    private static void checkXp(LocalPlayer player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        if (prevXpLevel >= 0 && (level != prevXpLevel || Float.compare(progress, prevXpProgress) != 0)) {
            PythemcIO.LOGGER.info("[PythemcIO] XP changed: level {}->{}", prevXpLevel, level);
            EventRegistry.fireEvent(EventType.XP_CHANGE, null, 0);
        }
        prevXpLevel = level;
        prevXpProgress = progress;
    }

    private static void checkAlive(LocalPlayer player) {
        boolean alive = !player.isDeadOrDying();
        if (prevAlive && !alive) {
            String cause = "unknown";
            if (player.getLastDamageSource() != null) {
                cause = player.getLastDamageSource().getMsgId();
                if (player.getLastDamageSource().getEntity() != null) {
                    cause = player.getLastDamageSource().getEntity().getType().builtInRegistryHolder().key().location().toString();
                }
            }
            PythemcIO.LOGGER.info("[PythemcIO] Player died (cause: {})", cause);
            EventRegistry.fireEvent(EventType.DEATH, cause, 0);
        } else if (!prevAlive && alive) {
            PythemcIO.LOGGER.info("[PythemcIO] Player respawned");
            EventRegistry.fireEvent(EventType.RESPAWN, null, 0);
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
                EventRegistry.fireEvent(EventType.TIME_CHANGE, timeName, 0);
            }
        }
        prevDayTime = currentTime;
    }

    private static void checkSleep(LocalPlayer player) {
        boolean sleeping = player.isSleeping();
        if (!wasSleeping && sleeping) {
            PythemcIO.LOGGER.info("[PythemcIO] Player started sleeping");
            EventRegistry.fireEvent(EventType.SLEEP, null, 0);
        } else if (wasSleeping && !sleeping) {
            PythemcIO.LOGGER.info("[PythemcIO] Player woke up");
            EventRegistry.fireEvent(EventType.WAKE_UP, null, 0);
        }
        wasSleeping = sleeping;
    }

    private static void checkOnFire(LocalPlayer player) {
        boolean current = player.isOnFire();
        if (!prevOnFire && current) {
            onFireTicks = 1;
            PythemcIO.LOGGER.info("[PythemcIO] Player is on fire");
            EventRegistry.fireEvent(EventType.ON_FIRE, null, 0);
        } else if (prevOnFire && current) {
            onFireTicks++;
            EventRegistry.fireEvent(EventType.ON_FIRE, null, ticksToSeconds(onFireTicks));
        } else if (prevOnFire && !current) {
            onFireTicks = 0;
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped being on fire");
        }
        prevOnFire = current;
    }

    private static void checkInWater(LocalPlayer player) {
        boolean current = player.isInWater();
        if (!prevInWater && current) {
            inWaterTicks = 1;
            PythemcIO.LOGGER.info("[PythemcIO] Player entered water");
            EventRegistry.fireEvent(EventType.IN_WATER, null, 0);
        } else if (prevInWater && current) {
            inWaterTicks++;
            EventRegistry.fireEvent(EventType.IN_WATER, null, ticksToSeconds(inWaterTicks));
        } else if (prevInWater && !current) {
            inWaterTicks = 0;
            PythemcIO.LOGGER.info("[PythemcIO] Player left water");
        }
        prevInWater = current;
    }

    private static void checkSprint(LocalPlayer player) {
        boolean current = player.isSprinting();
        if (!prevSprinting && current) {
            sprintTicks = 1;
            PythemcIO.LOGGER.info("[PythemcIO] Player started sprinting");
            EventRegistry.fireEvent(EventType.SPRINT, null, 0);
        } else if (prevSprinting && current) {
            sprintTicks++;
        } else if (prevSprinting && !current) {
            sprintTicks = 0;
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped sprinting");
        }
        prevSprinting = current;
    }

    private static void checkElytra(LocalPlayer player) {
        boolean current = player.isFallFlying();
        if (!prevFallFlying && current) {
            elytraTicks = 1;
            PythemcIO.LOGGER.info("[PythemcIO] Player started elytra flight");
            EventRegistry.fireEvent(EventType.ELYTRA, null, 0);
        } else if (prevFallFlying && current) {
            elytraTicks++;
        } else if (prevFallFlying && !current) {
            elytraTicks = 0;
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped elytra flight");
        }
        prevFallFlying = current;
    }

    private static void checkSneak(LocalPlayer player) {
        boolean current = player.isShiftKeyDown();
        if (!prevSneaking && current) {
            sneakTicks = 1;
            PythemcIO.LOGGER.info("[PythemcIO] Player started sneaking");
            EventRegistry.fireEvent(EventType.SNEAK, null, 0);
        } else if (prevSneaking && current) {
            sneakTicks++;
        } else if (prevSneaking && !current) {
            sneakTicks = 0;
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped sneaking");
        }
        prevSneaking = current;
    }

    private static void checkUsingItem(LocalPlayer player) {
        boolean current = player.isUsingItem();
        if (!prevUsingItem && current) {
            usingItemTicks = 1;
            String itemName = player.getUseItem().getItem().builtInRegistryHolder().key().location().toString();
            lastUsedItem = itemName;
            PythemcIO.LOGGER.info("[PythemcIO] Player started using item: {}", itemName);
            EventRegistry.fireEvent(EventType.USING_ITEM, itemName, 0);
        } else if (prevUsingItem && current) {
            usingItemTicks++;
        } else if (prevUsingItem && !current) {
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped using item");
            EventRegistry.fireEvent(EventType.USING_ITEM, null, 0);
            if (!lastUsedItem.isEmpty() && player.getUseItem().isEmpty()) {
                if (lastUsedItem.contains("potion") || lastUsedItem.contains("splash") || lastUsedItem.contains("lingering")) {
                    String potionType = lastUsedItem.contains("splash") ? "splash" : lastUsedItem.contains("lingering") ? "lingering" : "normal";
                    EventRegistry.fireEvent(EventType.POTION_EFFECT, potionType, 0);
                }
                EventRegistry.fireEvent(EventType.ITEM_CONSUME, lastUsedItem, 0);
                PythemcIO.LOGGER.info("[PythemcIO] Item consumed: {}", lastUsedItem);
            }
            lastUsedItem = "";
            usingItemTicks = 0;
        }
        prevUsingItem = current;
    }

    private static void checkFly(LocalPlayer player) {
        boolean current = player.getAbilities().flying;
        if (!prevFlying && current) {
            flyingTicks = 1;
            PythemcIO.LOGGER.info("[PythemcIO] Player started flying");
            EventRegistry.fireEvent(EventType.FLY, null, 0);
        } else if (prevFlying && current) {
            flyingTicks++;
        } else if (prevFlying && !current) {
            flyingTicks = 0;
            PythemcIO.LOGGER.info("[PythemcIO] Player stopped flying");
        }
        prevFlying = current;
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
            EventRegistry.fireEvent(EventType.REDSTONE_SIGNAL, String.valueOf(maxPower), 0);
        }
        prevRedstonePower = maxPower;
    }

    private static void checkVelocity(LocalPlayer player) {
        double speed = Math.sqrt(
            player.getDeltaMovement().x * player.getDeltaMovement().x +
            player.getDeltaMovement().z * player.getDeltaMovement().z
        );
        if (prevSpeed >= 0 && Math.abs(speed - prevSpeed) > 0.05) {
            String speedStr = String.format("%.2f", speed);
            PythemcIO.LOGGER.info("[PythemcIO] Velocity changed: {} -> {}", String.format("%.2f", prevSpeed), speedStr);
            EventRegistry.fireEvent(EventType.VELOCITY, speedStr, 0);
        }
        prevSpeed = speed;
    }

    private static void checkJump(LocalPlayer player) {
        boolean onGround = player.onGround();
        if (wasOnGround && !onGround && player.getDeltaMovement().y > 0) {
            String yStr = String.valueOf((int) player.getY());
            PythemcIO.LOGGER.info("[PythemcIO] Player jumped at y={}", yStr);
            EventRegistry.fireEvent(EventType.JUMP, yStr, 0);
        }
        wasOnGround = onGround;
    }

    private static void checkCoordinates(LocalPlayer player) {
        int bx = (int) player.getX();
        int by = (int) player.getY();
        int bz = (int) player.getZ();
        if (bx != prevBlockX || by != prevBlockY || bz != prevBlockZ) {
            String coords = bx + "," + by + "," + bz;
            if (prevBlockX != Integer.MIN_VALUE) {
                PythemcIO.LOGGER.info("[PythemcIO] Player moved to {}", coords);
                EventRegistry.fireEvent(EventType.COORDINATES, coords, 0);
            }
            prevBlockX = bx;
            prevBlockY = by;
            prevBlockZ = bz;
        }
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
        lastUsedItem = "";
        prevRedstonePower = -1;
        prevSpeed = -1.0;
        wasOnGround = true;
        prevBlockX = Integer.MIN_VALUE;
        prevBlockY = Integer.MIN_VALUE;
        prevBlockZ = Integer.MIN_VALUE;
        prevFlying = false;
        onFireTicks = 0;
        inWaterTicks = 0;
        sprintTicks = 0;
        elytraTicks = 0;
        sneakTicks = 0;
        usingItemTicks = 0;
        flyingTicks = 0;
        tickCounter = 0;
    }
}
