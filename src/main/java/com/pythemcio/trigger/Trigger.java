package com.pythemcio.trigger;

public class Trigger {
    private int id;
    private String event;
    private String[] commands;

    public Trigger(int id, String event, String[] commands) {
        this.id = id;
        this.event = event;
        this.commands = commands;
    }

    public int getId() {
        return id;
    }

    public String getEvent() {
        return event;
    }

    public String[] getCommands() {
        return commands;
    }

    @Override
    public String toString() {
        return "Trigger{id=" + id + ", event='" + event + "', commands=" + String.join(", ", commands) + "}";
    }
}
