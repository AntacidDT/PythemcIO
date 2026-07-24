package com.pythemcio.trigger;

public class Trigger {
    private int id;
    private String event;
    private String argument;
    private String[] commands;

    public Trigger(int id, String event, String argument, String[] commands) {
        this.id = id;
        this.event = event;
        this.argument = argument;
        this.commands = commands;
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

    public boolean matchesContext(String context) {
        if (!hasArgument()) return true;
        if (context == null) return false;
        return context.toLowerCase().contains(argument.toLowerCase());
    }

    @Override
    public String toString() {
        String argStr = hasArgument() ? ", argument='" + argument + "'" : "";
        return "Trigger{id=" + id + ", event='" + event + "'" + argStr + ", commands=" + String.join(", ", commands) + "}";
    }
}
