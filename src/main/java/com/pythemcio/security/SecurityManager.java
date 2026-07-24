package com.pythemcio.security;

import java.util.Arrays;
import java.util.List;

public class SecurityManager {

    private static final List<String> BLOCKED_COMMANDS = Arrays.asList(
        "rm", "rmdir", "del", "erase",
        "dd", "format", "mkfs",
        "chmod", "chown", "chgrp",
        "sudo", "su", "passwd",
        "kill", "killall", "pkill", "halt", "shutdown", "reboot",
        "systemctl", "service",
        "mount", "umount", "fdisk", "parted",
        "crontab", "at",
        "curl", "wget",
        "powershell", "pwsh"
    );

    private static final List<String> BLOCKED_FLAGS = Arrays.asList(
        "--no-preserve-root",
        "-rf", "-fr",
        "--force",
        "--recursive"
    );

    private static final List<String> BLOCKED_PATTERNS = Arrays.asList(
        "rm -", "rmdir -",
        "sudo ", "su -",
        "chmod ", "chown ",
        "dd if=", "dd of=",
        "> /dev/", "< /dev/",
        "/etc/passwd", "/etc/shadow"
    );

    public static ValidationResult validate(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new ValidationResult(false, "Command is empty");
        }

        String lower = command.toLowerCase().trim();

        String stripped = lower.replaceAll("[^a-z0-9 \\-/.]", " ");

        String[] words = stripped.split("\\s+");
        for (String word : words) {
            if (word.isEmpty()) continue;
            for (String blocked : BLOCKED_COMMANDS) {
                if (word.equals(blocked)) {
                    return new ValidationResult(false, "Blocked command: " + blocked);
                }
            }
        }

        for (String flag : BLOCKED_FLAGS) {
            if (lower.contains(flag)) {
                return new ValidationResult(false, "Blocked flag: " + flag);
            }
        }

        for (String pattern : BLOCKED_PATTERNS) {
            if (lower.contains(pattern)) {
                return new ValidationResult(false, "Blocked pattern: " + pattern);
            }
        }

        return new ValidationResult(true, "Command is safe");
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
