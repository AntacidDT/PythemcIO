package com.pythemcio.server;

import com.pythemcio.PythemcIO;
import com.pythemcio.trigger.TriggerManager;
import net.minecraft.client.Minecraft;

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
                PythemcIO.LOGGER.info("[PythemcIO] File watcher started. Inbox: {}", inboxDir);
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

            if (!TriggerManager.isEnabledInput()) return;

            String fileName = filePath.getFileName().toString();
            String payload;
            try {
                payload = Files.readString(filePath).trim();
            } catch (IOException e) {
                PythemcIO.LOGGER.error("[PythemcIO] Failed to read inbox file: {}", filePath, e);
                return;
            }

            PythemcIO.LOGGER.info("[PythemcIO] File trigger: {} -> {}", fileName, payload);

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                PythemcIO.LOGGER.warn("[PythemcIO] No player in game, ignoring file trigger");
                deleteFile(filePath);
                return;
            }

            String action = mapFileToAction(fileName, payload);

            mc.execute(() -> {
                try {
                    GameActionHandler.executeAction(detectType(fileName), action, mc);
                    PythemcIO.LOGGER.info("[PythemcIO] Executed: {}", action);
                } catch (Exception e) {
                    PythemcIO.LOGGER.error("[PythemcIO] Failed to execute: {}", action, e);
                }
            });

            deleteFile(filePath);
        });
    }

    private static String mapFileToAction(String fileName, String payload) {
        return switch (fileName.toLowerCase()) {
            case "chat" -> "chat " + payload;
            case "command" -> "command " + payload;
            case "title" -> "title " + payload;
            case "subtitle" -> "subtitle " + payload;
            case "actionbar" -> "actionbar " + payload;
            default -> "chat " + payload;
        };
    }

    private static String detectType(String fileName) {
        return switch (fileName.toLowerCase()) {
            case "chat" -> "send_chat";
            case "command" -> "run_command";
            case "title" -> "show_title";
            case "subtitle" -> "show_subtitle";
            case "actionbar" -> "action_bar";
            default -> "send_chat";
        };
    }

    private static void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            PythemcIO.LOGGER.warn("[PythemcIO] Failed to delete inbox file: {}", filePath);
        }
    }
}
