package com.pythemcio.trigger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private static int nextId = 1;
    private static boolean enabled = true;
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

    public static Trigger addTrigger(String event, String[] commands) {
        List<Trigger> eventTriggers = TRIGGERS.computeIfAbsent(event, k -> new ArrayList<>());
        Trigger trigger = new Trigger(nextId++, event, commands);
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

    public static Map<String, List<Trigger>> getAllTriggers() {
        return Collections.unmodifiableMap(TRIGGERS);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    private static void save() {
        if (configDir == null) return;
        Path file = configDir.resolve("triggers.json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("nextId", nextId);
            data.put("enabled", enabled);
            data.put("triggers", TRIGGERS);
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
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> data = GSON.fromJson(reader, mapType);
            if (data == null) return;

            if (data.containsKey("nextId")) {
                nextId = ((Number) data.get("nextId")).intValue();
            }
            if (data.containsKey("enabled")) {
                enabled = (Boolean) data.get("enabled");
            }
            if (data.containsKey("triggers")) {
                Map<String, List<Trigger>> loaded = GSON.fromJson(
                    GSON.toJson(data.get("triggers")),
                    new TypeToken<Map<String, List<Trigger>>>() {}.getType()
                );
                if (loaded != null) {
                    TRIGGERS.putAll(loaded);
                }
            }
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to load triggers", e);
        }
    }
}
