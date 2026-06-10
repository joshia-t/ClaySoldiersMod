package io.github.joshiat.claylegion.entity.nav;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamNavMemoryTest {

    private static final long CELL = TeamNavMemory.pack(10, 64, -20);

    @Test
    void unknownByDefault() {
        TeamNavMemory memory = new TeamNavMemory();
        assertEquals(TeamNavMemory.CellState.UNKNOWN, memory.getState(CELL, 0));
        assertEquals(TeamNavMemory.FLOW_NONE, memory.getFlow(CELL, 0));
    }

    @Test
    void visitedAndBlockedStates() {
        TeamNavMemory memory = new TeamNavMemory();
        memory.markVisited(CELL, 100);
        assertTrue(memory.isVisited(CELL, 100));
        assertFalse(memory.isBlocked(CELL, 100));

        memory.markBlocked(CELL, 110);
        assertTrue(memory.isBlocked(CELL, 110));
        // Blocked never downgrades back to visited.
        memory.markVisited(CELL, 120);
        assertTrue(memory.isBlocked(CELL, 120));
    }

    @Test
    void flowVectorsRoundTrip() {
        TeamNavMemory memory = new TeamNavMemory();
        memory.setFlow(CELL, TeamNavMemory.FLOW_NORTH, 50);
        assertEquals(TeamNavMemory.FLOW_NORTH, memory.getFlow(CELL, 60));

        assertEquals(TeamNavMemory.FLOW_SOUTH, TeamNavMemory.oppositeFlow(TeamNavMemory.FLOW_NORTH));
        assertEquals(1, TeamNavMemory.flowDx(TeamNavMemory.FLOW_EAST));
        assertEquals(-1, TeamNavMemory.flowDz(TeamNavMemory.FLOW_NORTH));
        assertEquals(0, TeamNavMemory.flowDz(TeamNavMemory.FLOW_EAST));
    }

    @Test
    void entriesExpireAfterTtl() {
        TeamNavMemory memory = new TeamNavMemory();
        memory.markBlocked(CELL, 0);
        assertTrue(memory.isBlocked(CELL, TeamNavMemory.ENTRY_TTL_TICKS));
        assertFalse(memory.isBlocked(CELL, TeamNavMemory.ENTRY_TTL_TICKS + 1),
            "Stale intel must age out");
    }

    @Test
    void capEvictsOldestEntries() {
        TeamNavMemory memory = new TeamNavMemory();
        for (int i = 0; i < TeamNavMemory.MAX_ENTRIES + 100; i++) {
            memory.markVisited(TeamNavMemory.pack(i, 64, 0), i);
        }
        assertTrue(memory.size() <= TeamNavMemory.MAX_ENTRIES,
            "Map must stay bounded, got " + memory.size());
        // The newest entry survives.
        assertTrue(memory.isVisited(
            TeamNavMemory.pack(TeamNavMemory.MAX_ENTRIES + 99, 64, 0),
            TeamNavMemory.MAX_ENTRIES + 100));
    }

    @Test
    void packsDistinctCoordinatesDistinctly() {
        assertNotEquals(TeamNavMemory.pack(1, 64, 0), TeamNavMemory.pack(0, 64, 1));
        assertNotEquals(TeamNavMemory.pack(1, 64, 0), TeamNavMemory.pack(1, 65, 0));
        assertNotEquals(TeamNavMemory.pack(-1, 64, 0), TeamNavMemory.pack(1, 64, 0));
    }
}
