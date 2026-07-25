package com.pythemcio.trigger;

public class Trigger {
    private int id;
    private String event;
    private String argument;
    private String[] commands;
    private String direction;
    private String scope;
    private String expectedOutput;
    private String scriptPath;
    private String gameAction;

    public Trigger(int id, String event, String argument, String[] commands, String direction) {
        this(id, event, argument, commands, direction, "local");
    }

    public Trigger(int id, String event, String argument, String[] commands, String direction, String scope) {
        this.id = id;
        this.event = event;
        this.argument = argument;
        this.commands = commands;
        this.direction = direction;
        this.scope = scope != null ? scope : "local";
    }

    public static Trigger createScriptTrigger(int id, String expectedOutput, String scriptPath, String gameAction) {
        return createScriptTrigger(id, expectedOutput, scriptPath, gameAction, "local");
    }

    public static Trigger createScriptTrigger(int id, String expectedOutput, String scriptPath, String gameAction, String scope) {
        Trigger t = new Trigger(id, "script", null, new String[]{gameAction}, "i", scope);
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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope != null ? scope : "local";
    }

    public boolean isGlobal() {
        return "global".equals(scope);
    }

    public boolean isLocal() {
        return !"global".equals(scope);
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
        String scopeStr = isGlobal() ? " [G]" : "";
        if (isScriptTrigger()) {
            return "Trigger{id=" + id + scopeStr + ", expected='" + expectedOutput + "', script='" + scriptPath + "', action='" + gameAction + "'}";
        }
        String argStr = hasArgument() ? ", argument='" + argument + "'" : "";
        return "Trigger{id=" + id + scopeStr + ", direction='" + direction + "', event='" + event + "'" + argStr + ", commands=" + String.join(", ", commands) + "}";
    }
}
