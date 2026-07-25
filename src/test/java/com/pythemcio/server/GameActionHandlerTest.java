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

    @Test
    void subtitlePrefixIsCaseInsensitive() {
        assertEquals("show_subtitle", GameActionHandler.detectType("Subtitle World"));
    }

    @Test
    void actionbarPrefixIsCaseInsensitive() {
        assertEquals("action_bar", GameActionHandler.detectType("Actionbar Test"));
    }

    @Test
    void leadingWhitespaceStillDetectsSlash() {
        assertEquals("run_command", GameActionHandler.detectType("  /tp @s 0 0 0"));
    }

    @Test
    void emptyStringDefaultsToChat() {
        assertEquals("send_chat", GameActionHandler.detectType(""));
    }

    @Test
    void onlySlashDefaultsToCommand() {
        assertEquals("run_command", GameActionHandler.detectType("/"));
    }

    @Test
    void chatPrefixWithExtraSpaces() {
        assertEquals("send_chat", GameActionHandler.detectType("chat   hello"));
    }

    @Test
    void commandPrefixWithExtraSpaces() {
        assertEquals("run_command", GameActionHandler.detectType("command   tp @s"));
    }

    @Test
    void singleWordDefaultsToChat() {
        assertEquals("send_chat", GameActionHandler.detectType("hello"));
    }

    @Test
    void numericStringDefaultsToChat() {
        assertEquals("send_chat", GameActionHandler.detectType("12345"));
    }
}
