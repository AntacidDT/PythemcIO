package com.pythemcio.gui;

import com.pythemcio.PythemcIO;
import com.pythemcio.event.EventType;
import com.pythemcio.security.SecurityManager;
import com.pythemcio.trigger.Trigger;
import com.pythemcio.trigger.TriggerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class AddOutputTriggerScreen extends Screen {

    private static final Set<String> FILTERABLE_EVENTS = Set.of(
        "using_item", "item_pickup", "item_drop",
        "block_break", "block_place",
        "player_attack", "chat_message",
        "dimension_change", "death", "time_change",
        "velocity", "jump", "coordinates",
        "item_consume", "block_interact", "entity_interact",
        "potion_effect", "health_change"
    );

    private static final List<String> EVENT_NAMES = Arrays.stream(EventType.values())
        .map(EventType::getName)
        .sorted()
        .toList();

    private final Screen parent;
    private EditBox filterBox;
    private EditBox commandBox;
    private String message = "";
    private boolean messageIsError = false;
    private int eventIndex = 0;
    private boolean globalScope = false;

    public AddOutputTriggerScreen(Screen parent) {
        super(Component.literal("Add Output Trigger"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int fieldWidth = 260;
        int fieldX = centerX - fieldWidth / 2;
        int y = 50;

        addRenderableWidget(Button.builder(
            Component.literal("< " + EVENT_NAMES.get(eventIndex) + " >"),
            btn -> {
                eventIndex = (eventIndex + 1) % EVENT_NAMES.size();
                btn.setMessage(Component.literal("< " + EVENT_NAMES.get(eventIndex) + " >"));
                updateFilterBox();
            }
        ).bounds(fieldX, y, fieldWidth, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(
            Component.literal("Scope: Local"),
            btn -> {
                globalScope = !globalScope;
                btn.setMessage(Component.literal(globalScope ? "Scope: Global" : "Scope: Local"));
            }
        ).bounds(fieldX, y, fieldWidth, 20).build());
        y += 28;

        filterBox = new EditBox(font, fieldX, y, fieldWidth, 20, Component.literal("Filter (optional)"));
        filterBox.setHint(Component.literal("Filter text (optional, case-insensitive)"));
        addRenderableWidget(filterBox);
        y += 28;

        commandBox = new EditBox(font, fieldX, y, fieldWidth, 20, Component.literal("Command"));
        commandBox.setHint(Component.literal("echo $EVENT"));
        addRenderableWidget(commandBox);
        y += 32;

        addRenderableWidget(Button.builder(
            Component.literal("Add Trigger"),
            btn -> addTrigger()
        ).bounds(centerX - 60, y, 120, 20).build());

        y += 28;

        addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> minecraft.setScreen(parent)
        ).bounds(centerX - 40, y, 80, 20).build());

        updateFilterBox();
    }

    private void updateFilterBox() {
        String event = EVENT_NAMES.get(eventIndex);
        boolean filterable = FILTERABLE_EVENTS.contains(event);
        filterBox.visible = filterable;
        filterBox.active = filterable;
        if (!filterable) {
            filterBox.setValue("");
        }
    }

    private void addTrigger() {
        String event = EVENT_NAMES.get(eventIndex);
        String filter = filterBox.getValue().trim();
        String command = commandBox.getValue().trim();
        String scope = globalScope ? "global" : "local";

        if (command.isEmpty()) {
            message = "Command cannot be empty";
            messageIsError = true;
            return;
        }

        SecurityManager.ValidationResult result = SecurityManager.validate(command);
        if (!result.isValid()) {
            message = "Blocked: " + result.getMessage();
            messageIsError = true;
            return;
        }

        String argument = filter.isEmpty() ? null : filter;
        if (argument != null && !FILTERABLE_EVENTS.contains(event)) {
            message = "Event '" + event + "' doesn't support filtering";
            messageIsError = true;
            return;
        }

        Trigger trigger = TriggerManager.addTrigger(event, argument, new String[]{command}, "o", scope);
        PythemcIO.LOGGER.info("[PythemcIO] Added output trigger #{} ({}): [{}] -> {}", trigger.getId(), scope, event, command);
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        int centerX = width / 2;
        int fieldWidth = 260;
        int fieldX = centerX - fieldWidth / 2;

        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
        guiGraphics.drawString(font, "Event:", fieldX, 38, 0xFFAAAAAA);

        if (!FILTERABLE_EVENTS.contains(EVENT_NAMES.get(eventIndex))) {
            guiGraphics.drawString(font, ChatFormatting.GRAY + "(not filterable)", fieldX + fieldWidth - font.width("(not filterable)"), 38, 0xFF888888);
        }

        guiGraphics.drawString(font, "Command (runs on OS):", fieldX, 122, 0xFFAAAAAA);

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
