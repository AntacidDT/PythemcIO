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
    private static final Map<String, Trigger> INPUT_TRIGGERS = new ConcurrentHashMap<>();
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
            INPUT_TRIGGERS.put(event, trigger);
            save();
            return trigger;
        }
        List<Trigger> eventTriggers = TRIGGERS.computeIfAbsent(event, k -> new ArrayList<>());
        Trigger trigger = new Trigger(nextId++, event, argument, commands, direction);
        eventTriggers.add(trigger);
        save();
        return trigger;
    }

    public static boolean removeTrigger(String event, int id) {
        List<Trigger> eventTriggers = TRIGGERS.get(event);
        if (eventTriggers == null) return false;

        boolean removed = eventTriggers.removeIf(t -> t.getId() == id);
        if (eventTriggers.isEmpty()) {
            TRIGGERS.remove(event);
        }
        if (removed) save();
        return removed;
    }

    public static List<Trigger> getTriggersForEvent(String event) {
        return TRIGGERS.getOrDefault(event, Collections.emptyList());
    }

    public static Trigger getInputTrigger(String event) {
        return INPUT_TRIGGERS.get(event);
    }

    public static Map<String, Trigger> getAllInputTriggers() {
        return Collections.unmodifiableMap(INPUT_TRIGGERS);
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
        INPUT_TRIGGERS.clear();
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
            data.add("inputTriggers", GSON.toJsonTree(INPUT_TRIGGERS));
            GSON.toJson(data, writer);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to save triggers", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void load() {
        if (configDir == null) return;
        Path file = configDir.resolve("triggers.json");
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file)) {
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
            if (data.has("inputTriggers")) {
                Map<String, Trigger> loaded = GSON.fromJson(
                    data.get("inputTriggers"),
                    new TypeToken<Map<String, Trigger>>() {}.getType()
                );
                if (loaded != null) {
                    INPUT_TRIGGERS.putAll(loaded);
                }
            }
        } catch (Exception e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to load triggers", e);
        }
    }
}
