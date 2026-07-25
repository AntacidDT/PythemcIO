package com.pythemcio.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameActionHandlerTest {

    @Test
    void slashCommandDetected() {
        assertEquals("run_command", GameActionHandler.detectType("/tp @s 0 0 0"));
    }

    @Test
    void chatPrefixDetected() {
        assertEquals("send_chat", GameActionHandler.detectType("chat hello world"));
    }

    @Test
    void commandPrefixDetected() {
        assertEquals("run_command", GameActionHandler.detectType("command tp @s 0 0 0"));
    }

    @Test
    void titlePrefixDetected() {
        assertEquals("show_title", GameActionHandler.detectType("title Hello!"));
    }

    @Test
    void subtitlePrefixDetected() {
        assertEquals("show_subtitle", GameActionHandler.detectType("subtitle Sub text"));
    }

    @Test
    void actionbarPrefixDetected() {
        assertEquals("action_bar", GameActionHandler.detectType("actionbar Bar text"));
    }

    @Test
    void plainTextDefaultsToChat() {
        assertEquals("send_chat", GameActionHandler.detectType("hello world"));
    }

    @Test
    void chatPrefixIsCaseInsensitive() {
        assertEquals("send_chat", GameActionHandler.detectType("Chat hello"));
        assertEquals("send_chat", GameActionHandler.detectType("CHAT hello"));
    }

    @Test
    void commandPrefixIsCaseInsensitive() {
        assertEquals("run_command", GameActionHandler.detectType("Command help"));
    }

    @Test
    void titlePrefixIsCaseInsensitive() {
        assertEquals("show_title", GameActionHandler.detectType("Title Hello"));
    }
}
