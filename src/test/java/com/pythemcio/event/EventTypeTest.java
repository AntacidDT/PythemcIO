package com.pythemcio.event;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventTypeTest {

    @Test
    void fromNameReturnsCorrectType() {
        assertEquals(EventType.PLAYER_JOIN, EventType.fromName("player_join"));
    }

    @Test
    void fromNameIsCaseInsensitive() {
        assertEquals(EventType.PLAYER_JOIN, EventType.fromName("PLAYER_JOIN"));
        assertEquals(EventType.PLAYER_JOIN, EventType.fromName("Player_Join"));
    }

    @Test
    void fromNameReturnsNullForUnknown() {
        assertNull(EventType.fromName("unknown_event"));
    }

    @Test
    void fromNameReturnsNullForEmpty() {
        assertNull(EventType.fromName(""));
    }

    @Test
    void fromNameReturnsNullForNull() {
        assertNull(EventType.fromName(null));
    }

    @Test
    void allEventNamesAreUnique() {
        EventType[] types = EventType.values();
        var names = new java.util.HashSet<String>();
        for (EventType type : types) {
            assertTrue(names.add(type.getName()), "Duplicate name: " + type.getName());
        }
    }

    @Test
    void playerJoinName() {
        assertEquals("player_join", EventType.PLAYER_JOIN.getName());
    }

    @Test
    void playerLeaveName() {
        assertEquals("player_leave", EventType.PLAYER_LEAVE.getName());
    }

    @Test
    void chatMessageName() {
        assertEquals("chat_message", EventType.CHAT_MESSAGE.getName());
    }

    @Test
    void blockBreakName() {
        assertEquals("block_break", EventType.BLOCK_BREAK.getName());
    }

    @Test
    void blockPlaceName() {
        assertEquals("block_place", EventType.BLOCK_PLACE.getName());
    }

    @Test
    void itemPickupName() {
        assertEquals("item_pickup", EventType.ITEM_PICKUP.getName());
    }

    @Test
    void itemDropName() {
        assertEquals("item_drop", EventType.ITEM_DROP.getName());
    }

    @Test
    void deathName() {
        assertEquals("death", EventType.DEATH.getName());
    }

    @Test
    void usingItemName() {
        assertEquals("using_item", EventType.USING_ITEM.getName());
    }

    @Test
    void dimensionChangeName() {
        assertEquals("dimension_change", EventType.DIMENSION_CHANGE.getName());
    }

    @Test
    void velocityName() {
        assertEquals("velocity", EventType.VELOCITY.getName());
    }

    @Test
    void jumpName() {
        assertEquals("jump", EventType.JUMP.getName());
    }

    @Test
    void coordinatesName() {
        assertEquals("coordinates", EventType.COORDINATES.getName());
    }

    @Test
    void totalEventCount() {
        assertEquals(34, EventType.values().length);
    }
}
