package com.pythemcio.event;

public enum EventType {
    DIMENSION_CHANGE("dimension_change"),
    HEALTH_CHANGE("health_change"),
    FOOD_CHANGE("food_change"),
    ARMOR_CHANGE("armor_change"),
    XP_CHANGE("xp_change"),
    REDSTONE_SIGNAL("redstone_signal"),
    PLAYER_JOIN("player_join"),
    PLAYER_LEAVE("player_leave"),
    ITEM_PICKUP("item_pickup"),
    ITEM_DROP("item_drop"),
    BLOCK_BREAK("block_break"),
    BLOCK_PLACE("block_place"),
    CHAT_MESSAGE("chat_message"),
    TIME_CHANGE("time_change"),
    DEATH("death"),
    RESPAWN("respawn"),
    SLEEP("sleep"),
    WAKE_UP("wake_up"),
    PLAYER_ATTACK("player_attack"),
    ON_FIRE("on_fire"),
    IN_WATER("in_water"),
    SPRINT("sprint"),
    ELYTRA("elytra"),
    SNEAK("sneak"),
    USING_ITEM("using_item");

    private final String name;

    EventType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static EventType fromName(String name) {
        for (EventType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
