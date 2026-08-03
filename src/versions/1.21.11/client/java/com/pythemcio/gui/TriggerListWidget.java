package com.pythemcio.gui;

import com.pythemcio.server.ScriptManager;
import com.pythemcio.trigger.Trigger;
import com.pythemcio.trigger.TriggerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

public class TriggerListWidget extends net.minecraft.client.gui.components.ObjectSelectionList<TriggerListWidget.TriggerEntry> {

    private final PythemcioConfigScreen parent;
    private final int listBottom;

    public TriggerListWidget(PythemcioConfigScreen parent, int width, int height, int listTop, int listBottom, int itemHeight) {
        super(Minecraft.getInstance(), width, height, listTop, itemHeight);
        this.parent = parent;
        this.listBottom = listBottom;
        updateSizeAndPosition(width, listBottom - listTop, listTop);
        refreshEntries();
    }

    public void refreshEntries() {
        clearEntries();

        Map<String, List<Trigger>> outputTriggers = TriggerManager.getAllTriggers();
        for (Map.Entry<String, List<Trigger>> entry : outputTriggers.entrySet()) {
            for (Trigger trigger : entry.getValue()) {
                String scopeTag = trigger.isGlobal() ? " [G]" : "";
                boolean disabled = trigger.isGlobal() && !TriggerManager.isGlobalTriggerEnabled(trigger.getId());
                String disabledTag = disabled ? " [OFF]" : "";
                String argStr = trigger.hasArgument() ? " (filter: " + trigger.getArgument() + ")" : "";
                String text = "#" + trigger.getId() + " [o]" + scopeTag + disabledTag + " [" + trigger.getEvent() + "]" + argStr + " -> " + String.join(", ", trigger.getCommands());
                addEntry(new TriggerEntry(text, trigger.getId(), true, trigger.isGlobal(), disabled));
            }
        }

        List<Trigger> scriptTriggers = TriggerManager.getScriptTriggers();
        for (Trigger trigger : scriptTriggers) {
            boolean running = ScriptManager.getRunningScripts().containsKey(trigger.getId());
            String status = running ? "RUNNING" : "STOPPED";
            String scopeTag = trigger.isGlobal() ? " [G]" : "";
            String text = "#" + trigger.getId() + " [i]" + scopeTag + " [" + status + "] \"" + trigger.getExpectedOutput() + "\" from " + trigger.getScriptPath() + " then " + trigger.getGameAction();
            addEntry(new TriggerEntry(text, trigger.getId(), false, trigger.isGlobal(), false));
        }
    }

    @Override
    public int getRowWidth() {
        return width - 20;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public class TriggerEntry extends Entry<TriggerEntry> {
        private final String text;
        private final int triggerId;
        private final boolean isOutput;
        private final boolean isGlobal;
        private final boolean disabled;

        public TriggerEntry(String text, int triggerId, boolean isOutput, boolean isGlobal, boolean disabled) {
            this.text = text;
            this.triggerId = triggerId;
            this.isOutput = isOutput;
            this.isGlobal = isGlobal;
            this.disabled = disabled;
        }

        @Override
        public Component getNarration() {
            return Component.literal(text);
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int index, int top, boolean hovered, float delta) {
            int color;
            if (disabled) {
                color = hovered ? 0xFFAAAA00 : 0xFF888888;
            } else if (isGlobal) {
                color = hovered ? 0xFFFFFF55 : 0xFF55FF55;
            } else {
                color = hovered ? 0xFFFFFF00 : 0xFFFFFFFF;
            }
            guiGraphics.drawString(Minecraft.getInstance().font, text, getX() + 4, top + (getHeight() - 8) / 2, color);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            parent.selectTrigger(triggerId, isOutput, isGlobal, text);
            return true;
        }
    }
}
