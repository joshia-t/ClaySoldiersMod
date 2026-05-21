package io.github.joshiat.claylegion.entity;

import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lightweight per-level, per-team registry of active clay soldiers.
 *
 * Replaces per-tick AABB entity queries with direct list iteration over enemy teams,
 * eliminating Minecraft's EntitySection traversal overhead and per-query ArrayList
 * allocation. At 40 soldiers the constant overhead of getEntitiesOfClass dwarfs the
 * actual iteration cost; this removes that overhead entirely.
 *
 * Design:
 *  - Level keys are weak references so unloaded levels are GC-eligible.
 *  - All mutations occur on the server thread (entity tick); no synchronization needed.
 *  - Empty team buckets are removed on unregister to keep iteration lean.
 *  - Naturally supports healer targeting (iterate same-team list) and future
 *    targeting classes (iterate ally/enemy lists filtered by role or health).
 *
 * Usage:
 *  - Register in serverCombatTick() on first run (flag-guarded).
 *  - Unregister by overriding Entity.remove(RemovalReason).
 */
public class SoldierIndex {

    private static final WeakHashMap<Level, SoldierIndex> INSTANCES = new WeakHashMap<>();

    // Team ID → live soldiers on that team in this level.
    // Only teams with at least one active soldier are present.
    private final HashMap<Integer, List<ClaySoldierEntity>> byTeam = new HashMap<>();

    private SoldierIndex() {}

    public static SoldierIndex get(Level level) {
        return INSTANCES.computeIfAbsent(level, k -> new SoldierIndex());
    }

    /**
     * Add a soldier to the index. Call once on first server tick after spawn.
     * Note: if team IDs can change mid-battle in the future, call reregister() instead.
     */
    public void register(ClaySoldierEntity soldier) {
        byTeam.computeIfAbsent(soldier.getTeamId(), k -> new ArrayList<>()).add(soldier);
    }

    /**
     * Remove a soldier from the index. Safe to call if not registered (no-op).
     */
    public void unregister(ClaySoldierEntity soldier) {
        List<ClaySoldierEntity> list = byTeam.get(soldier.getTeamId());
        if (list != null) {
            list.remove(soldier);
            if (list.isEmpty()) {
                byTeam.remove(soldier.getTeamId());
            }
        }
    }

    /**
     * Re-bucket a soldier whose team ID just changed. For future use.
     */
    public void reregister(ClaySoldierEntity soldier, int oldTeamId) {
        List<ClaySoldierEntity> oldList = byTeam.get(oldTeamId);
        if (oldList != null) {
            oldList.remove(soldier);
            if (oldList.isEmpty()) {
                byTeam.remove(oldTeamId);
            }
        }
        byTeam.computeIfAbsent(soldier.getTeamId(), k -> new ArrayList<>()).add(soldier);
    }

    /**
     * Direct read-only view of all team buckets.
     * Callers must not modify the returned map or any list within it.
     */
    public Map<Integer, List<ClaySoldierEntity>> getAllTeams() {
        return byTeam;
    }

    /**
     * Returns the list for a specific team, or empty list if none registered.
     * Intended for healer/support targeting that needs allies, not enemies.
     */
    public List<ClaySoldierEntity> getTeam(int teamId) {
        return byTeam.getOrDefault(teamId, Collections.emptyList());
    }
}
