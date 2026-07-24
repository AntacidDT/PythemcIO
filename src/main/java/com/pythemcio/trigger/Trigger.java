package com.pythemcio.trigger;

public class Trigger {
    private int id;
    private String event;
    private String argument;
    private String[] commands;
    private String direction;
    private String expectedOutput;
    private String scriptPath;
    private String gameAction;

    public Trigger(int id, String event, String argument, String[] commands, String direction) {
        this.id = id;
        this.event = event;
        this.argument = argument;
        this.commands = commands;
        this.direction = direction;
    }

    public static Trigger createScriptTrigger(int id, String expectedOutput, String scriptPath, String gameAction) {
        Trigger t = new Trigger(id, "script", null, new String[]{gameAction}, "i");
        t.expectedOutput = expectedOutput;
        t.scriptPath = scriptPath;
        t.gameAction = gameAction;
        return t;
    }

    public int getId() {
        return id;
    }

    public String getEvent() {
        return event;
    }

    public String getArgument() {
        return argument;
    }

    public boolean hasArgument() {
        return argument != null && !argument.isEmpty();
    }

    public String[] getCommands() {
        return commands;
    }

    public String getDirection() {
        return direction;
    }

    public boolean isOutput() {
        return "o".equals(direction);
    }

    public boolean isInput() {
        return "i".equals(direction);
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public String getGameAction() {
        return gameAction;
    }

    public boolean isScriptTrigger() {
        return scriptPath != null && !scriptPath.isEmpty();
    }

    public boolean matchesContext(String context) {
        if (!hasArgument()) return true;
        if (context == null) return false;
        return context.toLowerCase().contains(argument.toLowerCase());
    }

    @Override
    public String toString() {
        if (isScriptTrigger()) {
            return "Trigger{id=" + id + ", expected='" + expectedOutput + "', script='" + scriptPath + "', action='" + gameAction + "'}";
        }
        String argStr = hasArgument() ? ", argument='" + argument + "'" : "";
        return "Trigger{id=" + id + ", direction='" + direction + "', event='" + event + "'" + argStr + ", commands=" + String.join(", ", commands) + "}";
    }
}
