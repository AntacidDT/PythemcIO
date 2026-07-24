package com.pythemcio.executor;

import com.pythemcio.PythemcIO;
import com.pythemcio.security.SecurityManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CommandExecutor {

    private static final ConcurrentHashMap<String, Long> lastExecutions = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 3000;

    public static void execute(String command) {
        CompletableFuture.runAsync(() -> {
            Long lastTime = lastExecutions.get(command);
            if (lastTime != null && System.currentTimeMillis() - lastTime < COOLDOWN_MS) {
                return;
            }
            lastExecutions.put(command, System.currentTimeMillis());

            SecurityManager.ValidationResult result = SecurityManager.validate(command);
            if (!result.isValid()) {
                PythemcIO.LOGGER.warn("[PythemcIO] Command blocked: {} - {}", command, result.getMessage());
                return;
            }

            try {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;

                if (os.contains("win")) {
                    pb = new ProcessBuilder("cmd", "/c", command);
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("/bin/sh", "-c", command);
                } else {
                    pb = new ProcessBuilder("/bin/sh", "-c", command);
                }

                pb.redirectErrorStream(true);

                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        PythemcIO.LOGGER.info("[PythemcIO] Output: {}", line);
                    }
                }

                int exitCode = process.waitFor();
                PythemcIO.LOGGER.info("[PythemcIO] Command exited with code {}: {}", exitCode, command);

            } catch (Exception e) {
                PythemcIO.LOGGER.error("[PythemcIO] Failed to execute command: {}", command, e);
            }
        });
    }
}
