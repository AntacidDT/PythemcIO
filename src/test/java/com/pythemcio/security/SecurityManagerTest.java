package com.pythemcio.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityManagerTest {

    @Test
    void nullCommandIsBlocked() {
        var result = SecurityManager.validate(null);
        assertFalse(result.isValid());
        assertEquals("Command is empty", result.getMessage());
    }

    @Test
    void emptyCommandIsBlocked() {
        var result = SecurityManager.validate("");
        assertFalse(result.isValid());
    }

    @Test
    void whitespaceOnlyCommandIsBlocked() {
        var result = SecurityManager.validate("   ");
        assertFalse(result.isValid());
    }

    @Test
    void rmIsBlocked() {
        assertFalse(SecurityManager.validate("rm file.txt").isValid());
    }

    @Test
    void sudoIsBlocked() {
        assertFalse(SecurityManager.validate("sudo ls").isValid());
    }

    @Test
    void curlIsBlocked() {
        assertFalse(SecurityManager.validate("curl http://evil.com").isValid());
    }

    @Test
    void wgetIsBlocked() {
        assertFalse(SecurityManager.validate("wget http://evil.com").isValid());
    }

    @Test
    void powershellIsBlocked() {
        assertFalse(SecurityManager.validate("powershell Get-Process").isValid());
    }

    @Test
    void killIsBlocked() {
        assertFalse(SecurityManager.validate("kill 1234").isValid());
    }

    @Test
    void chmodIsBlocked() {
        assertFalse(SecurityManager.validate("chmod 777 file").isValid());
    }

    @Test
    void ddIsBlocked() {
        assertFalse(SecurityManager.validate("dd if=/dev/zero of=disk.img").isValid());
    }

    @Test
    void suIsBlocked() {
        assertFalse(SecurityManager.validate("su - root").isValid());
    }

    @Test
    void shutdownIsBlocked() {
        assertFalse(SecurityManager.validate("shutdown now").isValid());
    }

    @Test
    void rebootIsBlocked() {
        assertFalse(SecurityManager.validate("reboot").isValid());
    }

    @Test
    void systemctlIsBlocked() {
        assertFalse(SecurityManager.validate("systemctl stop nginx").isValid());
    }

    @Test
    void formatIsBlocked() {
        assertFalse(SecurityManager.validate("format /dev/sda").isValid());
    }

    @Test
    void noPreserveRootFlagIsBlocked() {
        assertFalse(SecurityManager.validate("rm --no-preserve-root /").isValid());
    }

    @Test
    void forceFlagIsBlocked() {
        assertFalse(SecurityManager.validate("rm --force file").isValid());
    }

    @Test
    void rfFlagIsBlocked() {
        assertFalse(SecurityManager.validate("rm -rf /tmp/dir").isValid());
    }

    @Test
    void sudoPatternIsBlocked() {
        assertFalse(SecurityManager.validate("sudo apt install").isValid());
    }

    @Test
    void ddIfPatternIsBlocked() {
        assertFalse(SecurityManager.validate("dd if=input of=output").isValid());
    }

    @Test
    void devNullRedirectIsBlocked() {
        assertFalse(SecurityManager.validate("echo x > /dev/null").isValid());
    }

    @Test
    void etcPasswdIsBlocked() {
        assertFalse(SecurityManager.validate("cat /etc/passwd").isValid());
    }

    @Test
    void etcShadowIsBlocked() {
        assertFalse(SecurityManager.validate("cat /etc/shadow").isValid());
    }

    @Test
    void safeCommandIsAllowed() {
        assertTrue(SecurityManager.validate("echo hello world").isValid());
    }

    @Test
    void pythonCommandIsAllowed() {
        assertTrue(SecurityManager.validate("python3 script.py").isValid());
    }

    @Test
    void pipCommandIsAllowed() {
        assertTrue(SecurityManager.validate("pip install requests").isValid());
    }

    @Test
    void npmCommandIsAllowed() {
        assertTrue(SecurityManager.validate("npm start").isValid());
    }

    @Test
    void bashCommandIsAllowed() {
        assertTrue(SecurityManager.validate("bash script.sh").isValid());
    }

    @Test
    void shCommandIsAllowed() {
        assertTrue(SecurityManager.validate("sh script.sh").isValid());
    }

    @Test
    void lsCommandIsAllowed() {
        assertTrue(SecurityManager.validate("ls -la /tmp").isValid());
    }

    @Test
    void quotedCommandStillBlocked() {
        assertFalse(SecurityManager.validate("sh -c \"sudo rm -rf /\"").isValid());
    }

    @Test
    void singleQuotesStillBlocked() {
        assertFalse(SecurityManager.validate("sh -c 'sudo rm -rf /'").isValid());
    }

    @Test
    void backtickBypassStillBlocked() {
        assertFalse(SecurityManager.validate("sh -c `sudo rm`").isValid());
    }

    @Test
    void dollarParenBypassStillBlocked() {
        assertFalse(SecurityManager.validate("sh -c $(sudo rm)").isValid());
    }

    @Test
    void semicolonChainingIsAllowed() {
        assertTrue(SecurityManager.validate("echo a; echo b").isValid());
    }

    @Test
    void pipeIsAllowed() {
        assertTrue(SecurityManager.validate("cat file | grep pattern").isValid());
    }

    @Test
    void gitCommandIsAllowed() {
        assertTrue(SecurityManager.validate("git status").isValid());
    }

    @Test
    void cargoCommandIsAllowed() {
        assertTrue(SecurityManager.validate("cargo build").isValid());
    }

    @Test
    void nodeCommandIsAllowed() {
        assertTrue(SecurityManager.validate("node server.js").isValid());
    }
}
