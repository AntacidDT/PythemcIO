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

    private static Path gameDir;
    private static String currentWorldName;
    private static final Set<Integer> disabledGlobalIds = new HashSet<>();

    public static void init(Path dir) {
        gameDir = dir;
        configDir = dir.resolve("config").resolve("pythemcio");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to create config directory", e);
        }
        loadGlobal();
    }

    public static void setWorld(String worldName) {
        if (worldName != null && worldName.equals(currentWorldName)) return;

        if (currentWorldName != null) {
            saveLocal(currentWorldName);
        }

        currentWorldName = worldName;
        TRIGGERS.clear();
        SCRIPT_TRIGGERS.clear();
        nextId = 1;
        enabledOutput = true;
        enabledInput = false;

        loadGlobal();
        if (worldName != null) {
            loadLocal(worldName);
        }
    }

    public static String getCurrentWorldName() {
        return currentWorldName;
    }

    public static Trigger addTrigger(String event, String argument, String[] commands, String direction) {
        return addTrigger(event, argument, commands, direction, "local");
    }

    public static Trigger addTrigger(String event, String argument, String[] commands, String direction, String scope) {
        if ("i".equals(direction)) {
            Trigger trigger = new Trigger(nextId++, event, argument, commands, direction, scope);
            SCRIPT_TRIGGERS.add(trigger);
            save();
            return trigger;
        }
        List<Trigger> eventTriggers = TRIGGERS.computeIfAbsent(event, k -> new ArrayList<>());
        Trigger trigger = new Trigger(nextId++, event, argument, commands, direction, scope);
        eventTriggers.add(trigger);
        save();
        return trigger;
    }

    public static Trigger addScriptTrigger(String expectedOutput, String scriptPath, String gameAction) {
        return addScriptTrigger(expectedOutput, scriptPath, gameAction, "local");
    }

    public static Trigger addScriptTrigger(String expectedOutput, String scriptPath, String gameAction, String scope) {
        Trigger trigger = Trigger.createScriptTrigger(nextId++, expectedOutput, scriptPath, gameAction, scope);
        SCRIPT_TRIGGERS.add(trigger);
        save();
        return trigger;
    }

    public static boolean removeTrigger(String event, int id) {
        if (event != null) {
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
        } else {
            for (var entry : TRIGGERS.entrySet()) {
                List<Trigger> triggers = entry.getValue();
                boolean removed = triggers.removeIf(t -> t.getId() == id);
                if (triggers.isEmpty()) {
                    TRIGGERS.remove(entry.getKey());
                }
                if (removed) {
                    save();
                    return true;
                }
            }
        }

        boolean removed = SCRIPT_TRIGGERS.removeIf(t -> t.getId() == id);
        if (removed) {
            disabledGlobalIds.remove(id);
            save();
        }
        return removed;
    }

    public static void setScope(int id, String scope) {
        for (List<Trigger> triggers : TRIGGERS.values()) {
            for (Trigger t : triggers) {
                if (t.getId() == id) {
                    t.setScope(scope);
                    save();
                    return;
                }
            }
        }
        for (Trigger t : SCRIPT_TRIGGERS) {
            if (t.getId() == id) {
                t.setScope(scope);
                save();
                return;
            }
        }
    }

    public static void setDuration(int id, int seconds) {
        for (List<Trigger> triggers : TRIGGERS.values()) {
            for (Trigger t : triggers) {
                if (t.getId() == id) {
                    t.setDuration(seconds);
                    save();
                    return;
                }
            }
        }
        for (Trigger t : SCRIPT_TRIGGERS) {
            if (t.getId() == id) {
                t.setDuration(seconds);
                save();
                return;
            }
        }
    }

    public static boolean isGlobalTriggerEnabled(int id) {
        return !disabledGlobalIds.contains(id);
    }

    public static void setGlobalTriggerEnabled(int id, boolean enabled) {
        if (enabled) {
            disabledGlobalIds.remove(id);
        } else {
            disabledGlobalIds.add(id);
        }
        saveWorldState();
    }

    public static List<Trigger> getTriggersForEvent(String event) {
        List<Trigger> result = new ArrayList<>();
        List<Trigger> eventTriggers = TRIGGERS.get(event);
        if (eventTriggers != null) {
            for (Trigger t : eventTriggers) {
                if (t.isGlobal() && !isGlobalTriggerEnabled(t.getId())) continue;
                result.add(t);
            }
        }
        return result;
    }

    public static List<Trigger> getScriptTriggers() {
        List<Trigger> result = new ArrayList<>();
        for (Trigger t : SCRIPT_TRIGGERS) {
            if (t.isGlobal() && !isGlobalTriggerEnabled(t.getId())) continue;
            result.add(t);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Trigger> getAllScriptTriggers() {
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
        disabledGlobalIds.clear();
        nextId = 1;
        save();
    }

    private static void save() {
        saveGlobal();
        if (currentWorldName != null) {
            saveLocal(currentWorldName);
        }
    }

    private static void saveGlobal() {
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
            PythemcIO.LOGGER.error("[PythemcIO] Failed to save global triggers", e);
        }
    }

    private static void saveLocal(String worldName) {
        if (configDir == null) return;
        Path worldDir = configDir.resolve("worlds").resolve(worldName);
        try {
            Files.createDirectories(worldDir);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to create world config directory", e);
            return;
        }

        Map<String, List<Trigger>> localOutput = new HashMap<>();
        List<Trigger> localInput = new ArrayList<>();
        for (Map.Entry<String, List<Trigger>> entry : TRIGGERS.entrySet()) {
            List<Trigger> local = entry.getValue().stream()
                .filter(Trigger::isLocal)
                .toList();
            if (!local.isEmpty()) {
                localOutput.put(entry.getKey(), new ArrayList<>(local));
            }
        }
        for (Trigger t : SCRIPT_TRIGGERS) {
            if (t.isLocal()) localInput.add(t);
        }

        Path triggersFile = worldDir.resolve("triggers.json");
        try (Writer writer = Files.newBufferedWriter(triggersFile)) {
            JsonObject data = new JsonObject();
            data.add("triggers", GSON.toJsonTree(localOutput));
            data.add("scriptTriggers", GSON.toJsonTree(localInput));
            data.addProperty("nextId", nextId);
            GSON.toJson(data, writer);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to save local triggers for world: {}", worldName, e);
        }
    }

    private static void saveWorldState() {
        if (configDir == null || currentWorldName == null) return;
        Path worldDir = configDir.resolve("worlds").resolve(currentWorldName);
        try {
            Files.createDirectories(worldDir);
        } catch (IOException e) {
            return;
        }
        Path stateFile = worldDir.resolve("state.json");
        try (Writer writer = Files.newBufferedWriter(stateFile)) {
            JsonObject data = new JsonObject();
            data.add("disabledGlobalIds", GSON.toJsonTree(new ArrayList<>(disabledGlobalIds)));
            GSON.toJson(data, writer);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to save world state for: {}", currentWorldName, e);
        }
    }

    private static void loadGlobal() {
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
                    for (var entry : loaded.entrySet()) {
                        TRIGGERS.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
                    }
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
            PythemcIO.LOGGER.error("[PythemcIO] Failed to load global triggers", e);
        }
    }

    private static void loadLocal(String worldName) {
        if (configDir == null) return;
        Path worldDir = configDir.resolve("worlds").resolve(worldName);

        Path triggersFile = worldDir.resolve("triggers.json");
        if (Files.exists(triggersFile)) {
            try (BufferedReader reader = Files.newBufferedReader(triggersFile)) {
                JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
                if (data == null) return;

                if (data.has("triggers")) {
                    Map<String, List<Trigger>> loaded = GSON.fromJson(
                        data.get("triggers"),
                        new TypeToken<Map<String, List<Trigger>>>() {}.getType()
                    );
                    if (loaded != null) {
                        for (var entry : loaded.entrySet()) {
                            TRIGGERS.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
                        }
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
                if (data.has("nextId")) {
                    int localNextId = data.get("nextId").getAsInt();
                    if (localNextId >= nextId) nextId = localNextId + 1;
                }
            } catch (Exception e) {
                PythemcIO.LOGGER.error("[PythemcIO] Failed to load local triggers for world: {}", worldName, e);
            }
        }

        Path stateFile = worldDir.resolve("state.json");
        if (Files.exists(stateFile)) {
            try (BufferedReader reader = Files.newBufferedReader(stateFile)) {
                JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
                if (data != null && data.has("disabledGlobalIds")) {
                    List<Integer> disabled = GSON.fromJson(
                        data.get("disabledGlobalIds"),
                        new TypeToken<List<Integer>>() {}.getType()
                    );
                    if (disabled != null) {
                        disabledGlobalIds.addAll(disabled);
                    }
                }
            } catch (Exception e) {
                PythemcIO.LOGGER.error("[PythemcIO] Failed to load world state for: {}", worldName, e);
            }
        }
    }
}
