package io.github.joshiat.claylegion.entity.nav;

import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-level registry handing out each team's {@link TeamNavMemory}.
 * Mirrors the TeamGossipIndex/SoldierIndex pattern.
 */
public final class TeamNavIndex {

    private static final WeakHashMap<Level, TeamNavIndex> INSTANCES = new WeakHashMap<>();

    private final Map<Integer, TeamNavMemory> byTeam = new HashMap<>();

    private TeamNavIndex() {
    }

    public static TeamNavIndex get(Level level) {
        return INSTANCES.computeIfAbsent(level, ignored -> new TeamNavIndex());
    }

    public TeamNavMemory forTeam(int teamId) {
        return byTeam.computeIfAbsent(teamId, ignored -> new TeamNavMemory());
    }
}
