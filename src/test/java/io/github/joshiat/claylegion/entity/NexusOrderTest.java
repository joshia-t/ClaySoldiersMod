package io.github.joshiat.claylegion.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NexusOrderTest {

    @Test
    void cyclesThroughAllOrdersAndWraps() {
        assertEquals(NexusOrder.GUARD, NexusOrder.MARCH.next());
        assertEquals(NexusOrder.HOLD, NexusOrder.GUARD.next());
        assertEquals(NexusOrder.MARCH, NexusOrder.HOLD.next());
    }

    @Test
    void idsRoundTrip() {
        for (NexusOrder order : NexusOrder.values()) {
            assertEquals(order, NexusOrder.fromId(order.id));
        }
    }

    @Test
    void unknownIdsFallBackToMarch() {
        assertEquals(NexusOrder.MARCH, NexusOrder.fromId((byte) 99));
    }
}
