package io.github.joshiat.claylegion.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Per-level index of nexus-owned summons.
 *
 * Tracks live soldiers by Nexus UUID so nexus spawn limits can be calculated
 * without world-wide entity scans, and linked summons can be removed instantly
 * when a nexus is broken.
 */
public final class NexusSummonIndex {

    private static final WeakHashMap<Level, NexusSummonIndex> INSTANCES = new WeakHashMap<>();

    private final Map<UUID, HashSet<ClaySoldierEntity>> byNexus = new HashMap<>();

    private NexusSummonIndex() {
    }

    public static NexusSummonIndex get(Level level) {
        return INSTANCES.computeIfAbsent(level, ignored -> new NexusSummonIndex());
    }

    public void register(ClaySoldierEntity soldier) {
        if (!soldier.isNexusSummon()) {
            return;
        }
        UUID nexusId = soldier.getNexusOriginId();
        if (nexusId == null) {
            return;
        }
        byNexus.computeIfAbsent(nexusId, ignored -> new HashSet<>()).add(soldier);
    }

    public void unregister(ClaySoldierEntity soldier) {
        UUID nexusId = soldier.getNexusOriginId();
        if (nexusId == null) {
            return;
        }
        HashSet<ClaySoldierEntity> summons = byNexus.get(nexusId);
        if (summons == null) {
            return;
        }
        summons.remove(soldier);
        if (summons.isEmpty()) {
            byNexus.remove(nexusId);
        }
    }

    public int countActive(UUID nexusId) {
        HashSet<ClaySoldierEntity> summons = byNexus.get(nexusId);
        if (summons == null || summons.isEmpty()) {
            return 0;
        }

        int count = 0;
        Iterator<ClaySoldierEntity> iterator = summons.iterator();
        while (iterator.hasNext()) {
            ClaySoldierEntity soldier = iterator.next();
            if (soldier == null
                || !soldier.isAlive()
                || soldier.isRemoved()
                || !soldier.isNexusSummon()
                || !nexusId.equals(soldier.getNexusOriginId())) {
                iterator.remove();
                continue;
            }
            count++;
        }

        if (summons.isEmpty()) {
            byNexus.remove(nexusId);
        }
        return count;
    }

    /** Runs an action on every live summon of a nexus (order changes, issue #32). */
    public void forEachActive(UUID nexusId, java.util.function.Consumer<ClaySoldierEntity> action) {
        HashSet<ClaySoldierEntity> summons = byNexus.get(nexusId);
        if (summons == null || summons.isEmpty()) {
            return;
        }
        for (ClaySoldierEntity soldier : new ArrayList<>(summons)) {
            if (soldier != null && soldier.isAlive() && !soldier.isRemoved()
                && soldier.isNexusSummon() && nexusId.equals(soldier.getNexusOriginId())) {
                action.accept(soldier);
            }
        }
    }

    public int removeAllForNexus(ServerLevel level, UUID nexusId) {
        HashSet<ClaySoldierEntity> summons = byNexus.remove(nexusId);
        if (summons == null || summons.isEmpty()) {
            return 0;
        }

        int removed = 0;
        for (ClaySoldierEntity soldier : new ArrayList<>(summons)) {
            if (soldier == null || !soldier.isAlive() || soldier.isRemoved()) {
                continue;
            }
            if (!level.equals(soldier.level())) {
                continue;
            }
            if (!soldier.isNexusSummon() || !nexusId.equals(soldier.getNexusOriginId())) {
                continue;
            }
            soldier.discard();
            removed++;
        }

        return removed;
    }
}