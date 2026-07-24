package com.pythemcio.server;

import com.pythemcio.PythemcIO;
import com.pythemcio.trigger.Trigger;
import com.pythemcio.trigger.TriggerManager;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptManager {

    private static final ConcurrentHashMap<Integer, ScriptProcess> running = new ConcurrentHashMap<>();
    private static volatile boolean active = false;

    public static void startAll() {
        if (active) return;
        active = true;

        List<Trigger> triggers = TriggerManager.getScriptTriggers();
        for (Trigger trigger : triggers) {
            startScript(trigger);
        }

        PythemcIO.LOGGER.info("[PythemcIO] Script manager started. {} script(s) registered.", triggers.size());
    }

    public static void stopAll() {
        active = false;
        for (var entry : running.entrySet()) {
            entry.getValue().kill();
        }
        running.clear();
        PythemcIO.LOGGER.info("[PythemcIO] Script manager stopped.");
    }

    public static boolean isRunning() {
        return active;
    }

    public static int getRunningCount() {
        return running.size();
    }

    public static ConcurrentHashMap<Integer, ScriptProcess> getRunningScripts() {
        return running;
    }

    public static void startScript(Trigger trigger) {
        if (!active) return;
        if (running.containsKey(trigger.getId())) return;

        ScriptProcess sp = new ScriptProcess(trigger);
        running.put(trigger.getId(), sp);
        sp.start();
    }

    public static void stopScript(int triggerId) {
        ScriptProcess sp = running.remove(triggerId);
        if (sp != null) {
            sp.kill();
        }
    }

    public static void restartScript(int triggerId) {
        stopScript(triggerId);
        if (!active) return;
        Trigger trigger = TriggerManager.getScriptTriggers().stream()
            .filter(t -> t.getId() == triggerId)
            .findFirst()
            .orElse(null);
        if (trigger != null) {
            startScript(trigger);
        }
    }

    public static class ScriptProcess {
        private final Trigger trigger;
        private Process process;
        private Thread readerThread;
        private volatile boolean alive = false;

        public ScriptProcess(Trigger trigger) {
            this.trigger = trigger;
        }

        public void start() {
            try {
                String scriptPath = trigger.getScriptPath();
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;

                if (os.contains("win")) {
                    if (scriptPath.endsWith(".py")) {
                        pb = new ProcessBuilder("python", scriptPath);
                    } else if (scriptPath.endsWith(".js")) {
                        pb = new ProcessBuilder("node", scriptPath);
                    } else {
                        pb = new ProcessBuilder("cmd", "/c", scriptPath);
                    }
                } else {
                    if (scriptPath.endsWith(".py")) {
                        pb = new ProcessBuilder("python3", scriptPath);
                    } else if (scriptPath.endsWith(".js")) {
                        pb = new ProcessBuilder("node", scriptPath);
                    } else if (scriptPath.endsWith(".sh")) {
                        pb = new ProcessBuilder("/bin/bash", scriptPath);
                    } else {
                        pb = new ProcessBuilder("/bin/bash", scriptPath);
                    }
                }

                pb.redirectErrorStream(false);
                process = pb.start();
                alive = true;

                PythemcIO.LOGGER.info("[PythemcIO] Script started: {} (PID: {})", scriptPath, process.pid());

                readerThread = new Thread(() -> readOutput(), "ScriptReader-" + trigger.getId());
                readerThread.setDaemon(true);
                readerThread.start();

                Thread monitorThread = new Thread(() -> monitor(), "ScriptMonitor-" + trigger.getId());
                monitorThread.setDaemon(true);
                monitorThread.start();

            } catch (Exception e) {
                PythemcIO.LOGGER.error("[PythemcIO] Failed to start script: {}", trigger.getScriptPath(), e);
                alive = false;
            }
        }

        private void readOutput() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while (alive && (line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.equals(trigger.getExpectedOutput())) {
                        PythemcIO.LOGGER.info("[PythemcIO] Script output matched: '{}' -> '{}'",
                            trigger.getExpectedOutput(), trigger.getGameAction());

                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.execute(() -> {
                                try {
                                    String action = trigger.getGameAction();
                                    String type = detectType(action);
                                    GameActionHandler.executeAction(type, action, mc);
                                    PythemcIO.LOGGER.info("[PythemcIO] Executed: {}", action);
                                } catch (Exception e) {
                                    PythemcIO.LOGGER.error("[PythemcIO] Failed to execute action", e);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                if (alive) {
                    PythemcIO.LOGGER.error("[PythemcIO] Script read error: {}", trigger.getScriptPath(), e);
                }
            }
        }

        private void monitor() {
            try {
                int exitCode = process.waitFor();
                alive = false;
                PythemcIO.LOGGER.warn("[PythemcIO] Script exited with code {}: {}", exitCode, trigger.getScriptPath());

                if (active) {
                    PythemcIO.LOGGER.info("[PythemcIO] Restarting script in 5 seconds: {}", trigger.getScriptPath());
                    Thread.sleep(5000);
                    if (active) {
                        running.remove(trigger.getId());
                        startScript(trigger);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void kill() {
            alive = false;
            if (process != null) {
                process.destroyForcibly();
            }
            if (readerThread != null) {
                readerThread.interrupt();
            }
        }

        public boolean isAlive() {
            return alive;
        }

        public Trigger getTrigger() {
            return trigger;
        }

        private String detectType(String action) {
            String lower = action.toLowerCase();
            if (lower.startsWith("chat ")) return "send_chat";
            if (lower.startsWith("command ")) return "run_command";
            if (lower.startsWith("title ")) return "show_title";
            if (lower.startsWith("subtitle ")) return "show_subtitle";
            if (lower.startsWith("actionbar ")) return "action_bar";
            return "send_chat";
        }
    }
}
