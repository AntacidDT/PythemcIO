package com.pythemcio.gui;

import com.pythemcio.server.ScriptManager;
import com.pythemcio.trigger.TriggerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PythemcioConfigScreen extends Screen {

    private final Screen parent;
    private TriggerListWidget triggerList;
    private int selectedTriggerId = -1;
    private boolean selectedIsGlobal = false;
    private Button removeButton;
    private Button enableOutputButton;
    private Button enableInputButton;
    private Button scopeButton;

    public PythemcioConfigScreen(Screen parent) {
        super(Component.literal("PythemcIO Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int listTop = 46;
        int listBottom = height - 50;
        triggerList = new TriggerListWidget(this, width, height, listTop, listBottom, 14);
        addRenderableWidget(triggerList);

        int buttonY = height - 40;
        int buttonWidth = 80;
        int spacing = 4;
        int totalButtons = 6;
        int totalWidth = totalButtons * buttonWidth + (totalButtons - 1) * spacing;
        int startX = (width - totalWidth) / 2;

        boolean outEnabled = TriggerManager.isEnabledOutput();
        boolean inEnabled = TriggerManager.isEnabledInput();

        enableOutputButton = Button.builder(
            Component.literal("Output: " + (outEnabled ? "ON" : "OFF")),
            btn -> {
                TriggerManager.setEnabledOutput(!TriggerManager.isEnabledOutput());
                updateButtons();
            }
        ).bounds(startX, buttonY, buttonWidth, 20).build();
        addRenderableWidget(enableOutputButton);

        enableInputButton = Button.builder(
            Component.literal("Input: " + (inEnabled ? "ON" : "OFF")),
            btn -> {
                boolean current = TriggerManager.isEnabledInput();
                TriggerManager.setEnabledInput(!current);
                if (!current) {
                    ScriptManager.startAll();
                } else {
                    ScriptManager.stopAll();
                }
                updateButtons();
            }
        ).bounds(startX + (buttonWidth + spacing), buttonY, buttonWidth, 20).build();
        addRenderableWidget(enableInputButton);

        addRenderableWidget(Button.builder(
            Component.literal("Add Output"),
            btn -> minecraft.setScreen(new AddOutputTriggerScreen(this))
        ).bounds(startX + 2 * (buttonWidth + spacing), buttonY, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("Add Input"),
            btn -> minecraft.setScreen(new AddInputTriggerScreen(this))
        ).bounds(startX + 3 * (buttonWidth + spacing), buttonY, buttonWidth, 20).build());

        scopeButton = Button.builder(
            Component.literal("Scope"),
            btn -> {
                if (selectedTriggerId != -1) {
                    String newScope = selectedIsGlobal ? "local" : "global";
                    TriggerManager.setScope(selectedTriggerId, newScope);
                    selectedIsGlobal = !selectedIsGlobal;
                    triggerList.refreshEntries();
                    updateButtons();
                }
            }
        ).bounds(startX + 4 * (buttonWidth + spacing), buttonY, buttonWidth, 20).build();
        addRenderableWidget(scopeButton);

        removeButton = Button.builder(
            Component.literal("Remove"),
            btn -> {
                if (selectedTriggerId != -1) {
                    TriggerManager.removeTrigger(null, selectedTriggerId);
                    ScriptManager.stopScript(selectedTriggerId);
                    selectedTriggerId = -1;
                    selectedIsGlobal = false;
                    triggerList.refreshEntries();
                    updateButtons();
                }
            }
        ).bounds(startX + 5 * (buttonWidth + spacing), buttonY, buttonWidth, 20).build();
        addRenderableWidget(removeButton);

        updateButtons();
    }

    private void updateButtons() {
        enableOutputButton.setMessage(Component.literal("Output: " + (TriggerManager.isEnabledOutput() ? "ON" : "OFF")));
        enableInputButton.setMessage(Component.literal("Input: " + (TriggerManager.isEnabledInput() ? "ON" : "OFF")));
        removeButton.active = selectedTriggerId != -1;
        scopeButton.active = selectedTriggerId != -1;
        if (selectedTriggerId != -1) {
            scopeButton.setMessage(Component.literal(selectedIsGlobal ? "Make Local" : "Make Global"));
        } else {
            scopeButton.setMessage(Component.literal("Scope"));
        }
    }

    public void selectTrigger(int id, boolean isOutput, boolean isGlobal, String text) {
        selectedTriggerId = id;
        selectedIsGlobal = isGlobal;
        updateButtons();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);

        String worldName = TriggerManager.getCurrentWorldName();
        if (worldName != null) {
            guiGraphics.drawCenteredString(font, ChatFormatting.AQUA + "World: " + ChatFormatting.WHITE + worldName, width / 2, 22, 0xFFFFFFFF);
        }

        int outCount = 0;
        for (var entry : TriggerManager.getAllTriggers().values()) {
            outCount += entry.size();
        }
        int inCount = TriggerManager.getScriptTriggers().size();
        int statsY = 22 + (worldName != null ? 10 : 0);
        String status = ChatFormatting.GRAY + "Output: " + outCount + " | Input: " + inCount;
        guiGraphics.drawCenteredString(font, status, width / 2, statsY, 0xFF888888);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
