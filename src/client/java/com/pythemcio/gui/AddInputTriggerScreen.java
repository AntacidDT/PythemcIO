package com.pythemcio.gui;

import com.pythemcio.PythemcIO;
import com.pythemcio.trigger.Trigger;
import com.pythemcio.trigger.TriggerManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AddInputTriggerScreen extends Screen {

    private final Screen parent;
    private EditBox expectedOutputBox;
    private EditBox scriptPathBox;
    private EditBox actionBox;
    private String message = "";
    private boolean messageIsError = false;
    private boolean globalScope = false;

    public AddInputTriggerScreen(Screen parent) {
        super(Component.literal("Add Input Trigger"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int fieldWidth = 260;
        int fieldX = centerX - fieldWidth / 2;
        int y = 50;

        expectedOutputBox = new EditBox(font, fieldX, y, fieldWidth, 20, Component.literal("Expected Output"));
        expectedOutputBox.setHint(Component.literal("rain"));
        addRenderableWidget(expectedOutputBox);
        y += 28;

        scriptPathBox = new EditBox(font, fieldX, y, fieldWidth, 20, Component.literal("Script Path"));
        scriptPathBox.setHint(Component.literal("/home/user/weather.py"));
        addRenderableWidget(scriptPathBox);
        y += 28;

        actionBox = new EditBox(font, fieldX, y, fieldWidth, 20, Component.literal("Game Action"));
        actionBox.setHint(Component.literal("chat It's raining! or /tp @s 0 0 0"));
        addRenderableWidget(actionBox);
        y += 28;

        addRenderableWidget(Button.builder(
            Component.literal("Scope: Local"),
            btn -> {
                globalScope = !globalScope;
                btn.setMessage(Component.literal(globalScope ? "Scope: Global" : "Scope: Local"));
            }
        ).bounds(fieldX, y, fieldWidth, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(
            Component.literal("Add Trigger"),
            btn -> addTrigger()
        ).bounds(centerX - 60, y, 120, 20).build());

        y += 28;

        addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> minecraft.setScreen(parent)
        ).bounds(centerX - 40, y, 80, 20).build());
    }

    private void addTrigger() {
        String expectedOutput = expectedOutputBox.getValue().trim();
        String scriptPath = scriptPathBox.getValue().trim();
        String action = actionBox.getValue().trim();
        String scope = globalScope ? "global" : "local";

        if (expectedOutput.isEmpty() || scriptPath.isEmpty() || action.isEmpty()) {
            message = "All fields are required";
            messageIsError = true;
            return;
        }

        Trigger trigger = TriggerManager.addScriptTrigger(expectedOutput, scriptPath, action, scope);
        PythemcIO.LOGGER.info("[PythemcIO] Added input trigger #{} ({}): \"{}\" from {} then {}", trigger.getId(), scope, expectedOutput, scriptPath, action);
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        int centerX = width / 2;
        int fieldWidth = 260;
        int fieldX = centerX - fieldWidth / 2;

        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);

        guiGraphics.drawString(font, "Expected stdout output (exact match):", fieldX, 38, 0xFFAAAAAA);
        guiGraphics.drawString(font, "Script path:", fieldX, 38 + 28 + 12, 0xFFAAAAAA);
        guiGraphics.drawString(font, "Game action (auto-detected: / = command, chat = chat, title = title):", fieldX, 38 + 28 + 28 + 12, 0xFFAAAAAA);

        if (!message.isEmpty()) {
            int color = messageIsError ? 0xFFFF5555 : 0xFF55FF55;
            guiGraphics.drawCenteredString(font, message, centerX, height - 70, color);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
