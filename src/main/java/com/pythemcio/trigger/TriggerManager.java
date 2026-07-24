package com.pythemcio.trigger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.pythemcio.PythemcIO;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TriggerManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, List<Trigger>> TRIGGERS = new ConcurrentHashMap<>();
    private static final List<Trigger> SCRIPT_TRIGGERS = new ArrayList<>();
    private static int nextId = 1;
    private static boolean enabledOutput = true;
    private static boolean enabledInput = false;
    private static Path configDir;

    public static void init(Path gameDir) {
        configDir = gameDir.resolve("config").resolve("pythemcio");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to create config directory", e);
        }
        load();
    }

    public static Trigger addTrigger(String event, String argument, String[] commands, String direction) {
        if ("i".equals(direction)) {
            Trigger trigger = new Trigger(nextId++, event, argument, commands, direction);
            SCRIPT_TRIGGERS.add(trigger);
            save();
            return trigger;
        }
        List<Trigger> eventTriggers = TRIGGERS.computeIfAbsent(event, k -> new ArrayList<>());
        Trigger trigger = new Trigger(nextId++, event, argument, commands, direction);
        eventTriggers.add(trigger);
        save();
        return trigger;
    }

    public static Trigger addScriptTrigger(String expectedOutput, String scriptPath, String gameAction) {
        Trigger trigger = Trigger.createScriptTrigger(nextId++, expectedOutput, scriptPath, gameAction);
        SCRIPT_TRIGGERS.add(trigger);
        save();
        return trigger;
    }

    public static boolean removeTrigger(String event, int id) {
        List<Trigger> eventTriggers = TRIGGERS.get(event);
        if (eventTriggers != null) {
            boolean removed = eventTriggers.removeIf(t -> t.getId() == id);
            if (eventTriggers.isEmpty()) {
                TRIGGERS.remove(event);
            }
            if (removed) {
                save();
                return true;
            }
        }

        boolean removed = SCRIPT_TRIGGERS.removeIf(t -> t.getId() == id);
        if (removed) save();
        return removed;
    }

    public static List<Trigger> getTriggersForEvent(String event) {
        return TRIGGERS.getOrDefault(event, Collections.emptyList());
    }

    public static List<Trigger> getScriptTriggers() {
        return Collections.unmodifiableList(SCRIPT_TRIGGERS);
    }

    public static Map<String, List<Trigger>> getAllTriggers() {
        return Collections.unmodifiableMap(TRIGGERS);
    }

    public static boolean isEnabledOutput() {
        return enabledOutput;
    }

    public static boolean isEnabledInput() {
        return enabledInput;
    }

    public static void setEnabledOutput(boolean state) {
        enabledOutput = state;
        save();
    }

    public static void setEnabledInput(boolean state) {
        enabledInput = state;
        save();
    }

    public static void clearAll() {
        TRIGGERS.clear();
        SCRIPT_TRIGGERS.clear();
        nextId = 1;
        save();
    }

    private static void save() {
        if (configDir == null) return;
        Path file = configDir.resolve("triggers.json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            JsonObject data = new JsonObject();
            data.addProperty("nextId", nextId);
            data.addProperty("enabledOutput", enabledOutput);
            data.addProperty("enabledInput", enabledInput);
            data.add("triggers", GSON.toJsonTree(TRIGGERS));
            data.add("scriptTriggers", GSON.toJsonTree(SCRIPT_TRIGGERS));
            GSON.toJson(data, writer);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to save triggers", e);
        }
    }

    private static void load() {
        if (configDir == null) return;
        Path file = configDir.resolve("triggers.json");
        if (!Files.exists(file)) return;

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            if (data == null) return;

            if (data.has("nextId")) {
                nextId = data.get("nextId").getAsInt();
            }
            if (data.has("enabledOutput")) {
                enabledOutput = data.get("enabledOutput").getAsBoolean();
            }
            if (data.has("enabledInput")) {
                enabledInput = data.get("enabledInput").getAsBoolean();
            }
            if (data.has("triggers")) {
                Map<String, List<Trigger>> loaded = GSON.fromJson(
                    data.get("triggers"),
                    new TypeToken<Map<String, List<Trigger>>>() {}.getType()
                );
                if (loaded != null) {
                    TRIGGERS.putAll(loaded);
                }
            }
            if (data.has("scriptTriggers")) {
                List<Trigger> loaded = GSON.fromJson(
                    data.get("scriptTriggers"),
                    new TypeToken<List<Trigger>>() {}.getType()
                );
                if (loaded != null) {
                    SCRIPT_TRIGGERS.addAll(loaded);
                }
            }
        } catch (Exception e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to load triggers", e);
        }
    }
}
