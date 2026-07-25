package com.pythemcio.trigger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TriggerTest {

    @Test
    void outputTriggerIsOutput() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o");
        assertTrue(t.isOutput());
        assertFalse(t.isInput());
    }

    @Test
    void inputTriggerIsInput() {
        Trigger t = Trigger.createScriptTrigger(1, "hello", "test.py", "chat hello");
        assertTrue(t.isInput());
        assertFalse(t.isOutput());
    }

    @Test
    void hasArgumentWithNonNull() {
        Trigger t = new Trigger(1, "chat_message", "hello", new String[]{"echo hi"}, "o");
        assertTrue(t.hasArgument());
    }

    @Test
    void hasArgumentWithNull() {
        Trigger t = new Trigger(1, "chat_message", null, new String[]{"echo hi"}, "o");
        assertFalse(t.hasArgument());
    }

    @Test
    void hasArgumentWithEmpty() {
        Trigger t = new Trigger(1, "chat_message", "", new String[]{"echo hi"}, "o");
        assertFalse(t.hasArgument());
    }

    @Test
    void matchesContextNoArgumentAlwaysTrue() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o");
        assertTrue(t.matchesContext("anything"));
        assertTrue(t.matchesContext(null));
        assertTrue(t.matchesContext(""));
    }

    @Test
    void matchesContextCaseInsensitive() {
        Trigger t = new Trigger(1, "chat_message", "hello", new String[]{"echo hi"}, "o");
        assertTrue(t.matchesContext("Hello World"));
        assertTrue(t.matchesContext("HELLO"));
        assertTrue(t.matchesContext("say hello to me"));
    }

    @Test
    void matchesContextDoesNotMatch() {
        Trigger t = new Trigger(1, "chat_message", "hello", new String[]{"echo hi"}, "o");
        assertFalse(t.matchesContext("goodbye"));
        assertFalse(t.matchesContext(null));
    }

    @Test
    void matchesContextSubstring() {
        Trigger t = new Trigger(1, "chat_message", "ell", new String[]{"echo hi"}, "o");
        assertTrue(t.matchesContext("hello"));
    }

    @Test
    void createScriptTriggerSetsAllFields() {
        Trigger t = Trigger.createScriptTrigger(5, "rain", "/path/to/weather.py", "chat rain");
        assertEquals(5, t.getId());
        assertEquals("rain", t.getExpectedOutput());
        assertEquals("/path/to/weather.py", t.getScriptPath());
        assertEquals("chat rain", t.getGameAction());
        assertEquals("i", t.getDirection());
        assertTrue(t.isScriptTrigger());
        assertEquals("script", t.getEvent());
    }

    @Test
    void scriptTriggerHasNoArgument() {
        Trigger t = Trigger.createScriptTrigger(1, "hello", "test.py", "chat hello");
        assertFalse(t.hasArgument());
        assertNull(t.getArgument());
    }

    @Test
    void gettersWorkCorrectly() {
        Trigger t = new Trigger(3, "block_break", "stone", new String[]{"echo stone"}, "o");
        assertEquals(3, t.getId());
        assertEquals("block_break", t.getEvent());
        assertEquals("stone", t.getArgument());
        assertArrayEquals(new String[]{"echo stone"}, t.getCommands());
        assertEquals("o", t.getDirection());
    }

    @Test
    void isScriptTriggerWithNull() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o");
        assertFalse(t.isScriptTrigger());
    }

    @Test
    void isScriptTriggerWithEmpty() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o");
        assertFalse(t.isScriptTrigger());
    }

    @Test
    void toStringOutput() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o");
        String str = t.toString();
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("event='player_join'"));
        assertTrue(str.contains("echo hi"));
    }

    @Test
    void toStringScriptTrigger() {
        Trigger t = Trigger.createScriptTrigger(2, "hello", "test.py", "chat hello");
        String str = t.toString();
        assertTrue(str.contains("id=2"));
        assertTrue(str.contains("expected='hello'"));
        assertTrue(str.contains("script='test.py'"));
    }

    @Test
    void defaultScopeIsLocal() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o");
        assertEquals("local", t.getScope());
        assertTrue(t.isLocal());
        assertFalse(t.isGlobal());
    }

    @Test
    void explicitLocalScope() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o", "local");
        assertEquals("local", t.getScope());
        assertTrue(t.isLocal());
        assertFalse(t.isGlobal());
    }

    @Test
    void globalScope() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o", "global");
        assertEquals("global", t.getScope());
        assertTrue(t.isGlobal());
        assertFalse(t.isLocal());
    }

    @Test
    void nullScopeDefaultsToLocal() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o", null);
        assertEquals("local", t.getScope());
        assertTrue(t.isLocal());
    }

    @Test
    void setScope() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o");
        assertEquals("local", t.getScope());
        t.setScope("global");
        assertEquals("global", t.getScope());
        assertTrue(t.isGlobal());
    }

    @Test
    void setScopeNullDefaultsToLocal() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o", "global");
        t.setScope(null);
        assertEquals("local", t.getScope());
        assertTrue(t.isLocal());
    }

    @Test
    void scriptTriggerDefaultScope() {
        Trigger t = Trigger.createScriptTrigger(1, "hello", "test.py", "chat hello");
        assertEquals("local", t.getScope());
        assertTrue(t.isLocal());
    }

    @Test
    void scriptTriggerGlobalScope() {
        Trigger t = Trigger.createScriptTrigger(1, "hello", "test.py", "chat hello", "global");
        assertEquals("global", t.getScope());
        assertTrue(t.isGlobal());
    }

    @Test
    void toStringGlobalScope() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o", "global");
        String str = t.toString();
        assertTrue(str.contains("[G]"));
    }

    @Test
    void toStringLocalScopeNoTag() {
        Trigger t = new Trigger(1, "player_join", null, new String[]{"echo hi"}, "o", "local");
        String str = t.toString();
        assertFalse(str.contains("[G]"));
    }
}
