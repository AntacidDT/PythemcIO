package com.pythemcio.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScriptManagerTest {

    @AfterEach
    void cleanup() {
        ScriptManager.stopAll();
    }

    @Test
    void initialStateNotRunning() {
        assertFalse(ScriptManager.isRunning());
        assertEquals(0, ScriptManager.getRunningCount());
        assertTrue(ScriptManager.getRunningScripts().isEmpty());
    }

    @Test
    void startAllSetsRunning() {
        ScriptManager.startAll();
        assertTrue(ScriptManager.isRunning());
    }

    @Test
    void stopAllClearsRunning() {
        ScriptManager.startAll();
        ScriptManager.stopAll();
        assertFalse(ScriptManager.isRunning());
        assertEquals(0, ScriptManager.getRunningCount());
        assertTrue(ScriptManager.getRunningScripts().isEmpty());
    }

    @Test
    void startAllIdempotent() {
        ScriptManager.startAll();
        ScriptManager.startAll();
        assertTrue(ScriptManager.isRunning());
    }

    @Test
    void stopAllIdempotent() {
        ScriptManager.startAll();
        ScriptManager.stopAll();
        ScriptManager.stopAll();
        assertFalse(ScriptManager.isRunning());
    }

    @Test
    void getRunningScriptsReturnsMap() {
        assertNotNull(ScriptManager.getRunningScripts());
        assertTrue(ScriptManager.getRunningScripts() instanceof java.util.concurrent.ConcurrentHashMap);
    }

    @Test
    void stopNonexistentScriptDoesNotThrow() {
        assertDoesNotThrow(() -> ScriptManager.stopScript(999));
    }

    @Test
    void restartNonexistentScriptDoesNotThrow() {
        assertDoesNotThrow(() -> ScriptManager.restartScript(999));
    }

    @Test
    void startScriptWithoutActiveFlagDoesNothing() {
        ScriptManager.stopAll();
        com.pythemcio.trigger.Trigger t = com.pythemcio.trigger.Trigger.createScriptTrigger(
            1, "hello", "/bin/echo hello", "chat hello"
        );
        ScriptManager.startScript(t);
        assertEquals(0, ScriptManager.getRunningCount());
    }
}
