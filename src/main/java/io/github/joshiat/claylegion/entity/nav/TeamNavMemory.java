package io.github.joshiat.claylegion.entity.nav;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Shared "anti-pheromone" navigation memory for one team (swarm AI goal).
 *
 * <p>The world is treated as a sparse grid of block-sized cells. Exploring
 * soldiers stamp cells as VISITED while they walk, BLOCKED when a branch turns
 * out to be a dead end, and leave a cardinal flow vector pointing the way out
 * of dead-end branches so later soldiers skip the mistake entirely. The maze
 * isn't solved by a pathfinder — it is learned by the swarm.
 *
 * <p>Performance constraints (500+ units):
 * <ul>
 *   <li>All operations are O(1) hash lookups on packed long keys.</li>
 *   <li>Entries expire after {@link #ENTRY_TTL_TICKS} so stale layouts
 *       (player rebuilt the fort) age out, and the map is hard-capped at
 *       {@link #MAX_ENTRIES} with oldest-first eviction.</li>
 *   <li>No per-tick maintenance: expiry is checked lazily on read, and
 *       eviction amortizes on write.</li>
 * </ul>
 *
 * <p>Intel isolation: each team owns its own instance — scouting paid for by
 * one team never helps another.
 */
public final class TeamNavMemory {

    /** Cell knowledge states, ordered by how strongly they repel explorers. */
    public enum CellState {
        UNKNOWN,
        VISITED,
        BLOCKED
    }

    /** Cardinal flow directions; NONE means no flow recorded. */
    public static final byte FLOW_NONE = -1;
    public static final byte FLOW_EAST = 0;   // +x
    public static final byte FLOW_WEST = 1;   // -x
    public static final byte FLOW_SOUTH = 2;  // +z
    public static final byte FLOW_NORTH = 3;  // -z

    public static final int ENTRY_TTL_TICKS = 6000; // 5 minutes
    public static final int MAX_ENTRIES = 8192;
    /** How long a discovered route is trusted before the swarm re-probes. */
    public static final int ROUTE_MEMORY_TICKS = 1200; // 1 minute

    private static final class Cell {
        CellState state;
        byte flow = FLOW_NONE;
        long writtenAt;
    }

    private final Map<Long, Cell> cells = new HashMap<>();
    private final ArrayDeque<Long> writeOrder = new ArrayDeque<>();

    // When any soldier last threaded a route toward its goal through here.
    // While stale, "naughty" explorers ignore blocked markers to re-probe.
    private long lastRouteFoundTick = Long.MIN_VALUE;

    public static long pack(int x, int y, int z) {
        // 26/12/26-bit packing, mirroring BlockPos.asLong semantics.
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    // Sign-extending unpack, mirroring BlockPos.getX/getY/getZ.
    public static int unpackX(long key) {
        return (int) (key << 0 >> 38);
    }

    public static int unpackY(long key) {
        return (int) (key << 26 >> 52);
    }

    public static int unpackZ(long key) {
        return (int) (key << 38 >> 38);
    }

    public void markVisited(long key, long gameTime) {
        Cell cell = getOrCreate(key, gameTime);
        // Never downgrade a BLOCKED cell back to merely visited.
        if (cell.state != CellState.BLOCKED) {
            cell.state = CellState.VISITED;
        }
        cell.writtenAt = gameTime;
    }

    public void markBlocked(long key, long gameTime) {
        Cell cell = getOrCreate(key, gameTime);
        cell.state = CellState.BLOCKED;
        cell.writtenAt = gameTime;
    }

    public void setFlow(long key, byte flow, long gameTime) {
        Cell cell = getOrCreate(key, gameTime);
        cell.flow = flow;
        cell.writtenAt = gameTime;
    }

    public CellState getState(long key, long gameTime) {
        Cell cell = getLive(key, gameTime);
        return cell == null ? CellState.UNKNOWN : cell.state;
    }

    public byte getFlow(long key, long gameTime) {
        Cell cell = getLive(key, gameTime);
        return cell == null ? FLOW_NONE : cell.flow;
    }

    public boolean isBlocked(long key, long gameTime) {
        return getState(key, gameTime) == CellState.BLOCKED;
    }

    public boolean isVisited(long key, long gameTime) {
        return getState(key, gameTime) == CellState.VISITED;
    }

    /** Records that a soldier just threaded a route toward its goal. */
    public void recordRouteFound(long gameTime) {
        lastRouteFoundTick = gameTime;
    }

    /** True if the team has a fresh route; while false, ignorers go rogue. */
    public boolean routeFoundRecently(long gameTime) {
        return lastRouteFoundTick != Long.MIN_VALUE
            && gameTime - lastRouteFoundTick <= ROUTE_MEMORY_TICKS;
    }

    public int size() {
        return cells.size();
    }

    public void clear() {
        cells.clear();
        writeOrder.clear();
        lastRouteFoundTick = Long.MIN_VALUE;
    }

    /** Visitor for debug iteration over live cells. */
    public interface CellVisitor {
        void accept(long key, CellState state, byte flow);
    }

    /** Iterates live (non-expired) cells — debug tooling only, O(n). */
    public void forEachLiveCell(long gameTime, CellVisitor visitor) {
        for (Map.Entry<Long, Cell> entry : cells.entrySet()) {
            Cell cell = entry.getValue();
            if (gameTime - cell.writtenAt <= ENTRY_TTL_TICKS) {
                visitor.accept(entry.getKey(), cell.state, cell.flow);
            }
        }
    }

    private Cell getOrCreate(long key, long gameTime) {
        Cell cell = cells.get(key);
        if (cell != null && gameTime - cell.writtenAt > ENTRY_TTL_TICKS) {
            // Lazily refresh an expired cell rather than trusting stale intel.
            cell.state = CellState.UNKNOWN;
            cell.flow = FLOW_NONE;
        }
        if (cell == null) {
            cell = new Cell();
            cell.state = CellState.UNKNOWN;
            cells.put(key, cell);
            writeOrder.addLast(key);
            evictIfOverCap(gameTime);
        }
        return cell;
    }

    private Cell getLive(long key, long gameTime) {
        Cell cell = cells.get(key);
        if (cell == null) {
            return null;
        }
        if (gameTime - cell.writtenAt > ENTRY_TTL_TICKS) {
            cells.remove(key);
            return null;
        }
        return cell;
    }

    private void evictIfOverCap(long gameTime) {
        // Amortized: drop oldest writes (or already-expired ones) until under cap.
        while (cells.size() > MAX_ENTRIES) {
            Long oldest = writeOrder.pollFirst();
            if (oldest == null) {
                return;
            }
            Cell cell = cells.get(oldest);
            if (cell != null && gameTime - cell.writtenAt <= ENTRY_TTL_TICKS && cells.size() <= MAX_ENTRIES) {
                // Still fresh and we're back under cap — keep it, re-queue.
                writeOrder.addLast(oldest);
                return;
            }
            cells.remove(oldest);
        }
    }

    /** Packed offset of one step in a flow direction: {dx, dz}. */
    public static int flowDx(byte flow) {
        return flow == FLOW_EAST ? 1 : flow == FLOW_WEST ? -1 : 0;
    }

    public static int flowDz(byte flow) {
        return flow == FLOW_SOUTH ? 1 : flow == FLOW_NORTH ? -1 : 0;
    }

    /** The flow direction opposite to the given one. */
    public static byte oppositeFlow(byte flow) {
        return switch (flow) {
            case FLOW_EAST -> FLOW_WEST;
            case FLOW_WEST -> FLOW_EAST;
            case FLOW_SOUTH -> FLOW_NORTH;
            case FLOW_NORTH -> FLOW_SOUTH;
            default -> FLOW_NONE;
        };
    }
}
