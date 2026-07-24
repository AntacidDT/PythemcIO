package com.pythemcio.server;

import com.pythemcio.PythemcIO;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public class FileWatcher {

    private static WatchService watchService;
    private static Thread watchThread;
    private static volatile boolean running = false;
    private static Path inboxDir;

    public static void start(Path configDir) {
        inboxDir = configDir.resolve("inbox");
        try {
            Files.createDirectories(inboxDir);
        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to create inbox directory", e);
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            inboxDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            running = true;

            watchThread = new Thread(() -> {
                PythemcIO.LOGGER.info("[PythemcIO] File watcher started. Watching: {}", inboxDir);
                while (running) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                Path filename = (Path) event.context();
                                processFile(inboxDir.resolve(filename));
                            }
                        }
                        key.reset();
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        PythemcIO.LOGGER.error("[PythemcIO] File watcher error", e);
                    }
                }
                PythemcIO.LOGGER.info("[PythemcIO] File watcher stopped.");
            }, "PythemcIO-FileWatcher");
            watchThread.setDaemon(true);
            watchThread.start();

        } catch (IOException e) {
            PythemcIO.LOGGER.error("[PythemcIO] Failed to start file watcher", e);
        }
    }

    public static void stop() {
        running = false;
        if (watchThread != null) {
            watchThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                PythemcIO.LOGGER.error("[PythemcIO] Failed to close watch service", e);
            }
        }
        PythemcIO.LOGGER.info("[PythemcIO] File watcher stopped.");
    }

    public static boolean isRunning() {
        return running;
    }

    public static Path getInboxDir() {
        return inboxDir;
    }

    private static void processFile(Path filePath) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }

            String fileName = filePath.getFileName().toString();
            String eventName = fileName;

            String payload;
            try {
                payload = Files.readString(filePath).trim();
            } catch (IOException e) {
                PythemcIO.LOGGER.error("[PythemcIO] Failed to read inbox file: {}", filePath, e);
                return;
            }

            PythemcIO.LOGGER.info("[PythemcIO] File trigger: {} -> {}", eventName, payload);

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                PythemcIO.LOGGER.warn("[PythemcIO] No player in game, ignoring file trigger");
                deleteFile(filePath);
                return;
            }

            String actionType = detectActionType(eventName);
            String action = buildAction(actionType, eventName, payload);

            mc.execute(() -> {
                try {
                    GameActionHandler.executeAction(actionType, action, mc);
                    PythemcIO.LOGGER.info("[PythemcIO] File action executed: {} -> {}", eventName, action);
                } catch (Exception e) {
                    PythemcIO.LOGGER.error("[PythemcIO] Failed to execute file action: {}", action, e);
                }
            });

            deleteFile(filePath);
        });
    }

    private static void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            PythemcIO.LOGGER.warn("[PythemcIO] Failed to delete inbox file: {}", filePath);
        }
    }

    private static String detectActionType(String eventName) {
        String lower = eventName.toLowerCase();
        if (lower.contains("chat")) return "send_chat";
        if (lower.contains("command")) return "run_command";
        if (lower.contains("title")) return "show_title";
        if (lower.contains("subtitle")) return "show_subtitle";
        if (lower.contains("actionbar")) return "action_bar";
        return "send_chat";
    }

    private static String buildAction(String type, String eventName, String payload) {
        return switch (type) {
            case "send_chat" -> "chat " + payload;
            case "run_command" -> "command " + payload;
            case "show_title" -> "title " + payload;
            case "show_subtitle" -> "subtitle " + payload;
            case "action_bar" -> "actionbar " + payload;
            default -> "chat " + payload;
        };
    }
}
