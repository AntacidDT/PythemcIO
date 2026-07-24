package com.pythemcio.security;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SecurityManager {

    private static final List<String> BLOCKED_COMMANDS = Arrays.asList(
        "rm", "rmdir", "del", "erase",
        "touch", "mkdir", "rmdir",
        "nano", "vim", "vi", "emacs", "ed",
        "mv", "rename",
        "cp", "copy", "xcopy",
        "dd", "format", "mkfs",
        "chmod", "chown", "chgrp",
        "sudo", "su", "passwd",
        "apt", "apt-get", "yum", "dnf", "pacman", "brew", "snap",
        "pip", "pip3", "npm", "yarn", "cargo",
        "bash", "sh", "zsh", "fish", "csh", "ksh",
        "powershell", "pwsh", "cmd",
        "eval", "exec",
        "systemctl", "service",
        "mount", "umount", "fdisk", "parted",
        "kill", "killall", "pkill", "halt", "shutdown", "reboot",
        "crontab", "at"
    );

    private static final List<String> BLOCKED_FLAGS = Arrays.asList(
        "--no-preserve-root",
        "-rf", "-fr", "-r", "-R",
        "-f",
        "--force",
        "-recursive",
        "--recursive"
    );

    private static final List<String> BLOCKED_PATTERNS = Arrays.asList(
        "&&", "||", "|", ";", ">", ">>", "<", "<<",
        "`", "$(",
        "rm -", "rmdir -",
        "sudo ", "su -",
        "chmod ", "chown ",
        "dd if=", "dd of=",
        "> /dev/", "< /dev/",
        "/etc/passwd", "/etc/shadow",
        "/bin/", "/sbin/", "/usr/bin/"
    );

    public static ValidationResult validate(String command) {
        if (command == null || command.trim().isEmpty()) {
            return new ValidationResult(false, "Command is empty");
        }

        String lower = command.toLowerCase().trim();

        for (String blocked : BLOCKED_COMMANDS) {
            String[] tokens = lower.split("\\s+");
            for (String token : tokens) {
                if (token.equals(blocked) || token.equals(blocked + ";") || token.equals(blocked + "&")) {
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

        if (lower.matches(".*\\brm\\b.*")) {
            return new ValidationResult(false, "rm command detected");
        }

        if (lower.matches(".*\\bsudo\\b.*")) {
            return new ValidationResult(false, "sudo command detected");
        }

        if (lower.matches(".*--no-preserve-root.*")) {
            return new ValidationResult(false, "--no-preserve-root detected");
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
