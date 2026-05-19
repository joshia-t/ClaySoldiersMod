package io.github.joshiat.claylegion.entity;

import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates target acquisition and cache management for clay soldiers.
 *
 * Separating scanning logic makes it easier to experiment with different targeting
 * strategies (e.g., priority weights, team allegiances) without touching the main entity loop.
 */
public class SoldierTargetingHelper {

    // Keep base intervals but add deterministic per-entity jitter to de-align scan bursts.
    private static final int TARGET_SCAN_BASE_INTERVAL = 16;
    private static final int TARGET_SCAN_JITTER = 4; // Effective interval: 16..20
    private static final int MOUNT_SCAN_BASE_INTERVAL = 12;
    private static final int MOUNT_SCAN_JITTER = 6; // Effective interval: 12..18
    private static final double TARGET_RANGE_XZ = 16.0;
    private static final double TARGET_RANGE_Y = 1.8;
    private static final double TARGET_RANGE_SQ = TARGET_RANGE_XZ * TARGET_RANGE_XZ;
    private static final double MOUNT_SEARCH_RANGE = 6.0;
    private static final double MOUNT_SEARCH_RANGE_SQ = MOUNT_SEARCH_RANGE * MOUNT_SEARCH_RANGE;

    /**
     * Update the cached combat target for this soldier.
     * Returns early if the current target is still valid; otherwise scans for a new one.
     *
     * @param soldier The soldier performing the scan.
     * @param currentTarget The currently cached target (may be null or invalid).
     * @return The best valid target found, or null if none.
     */
    public static ClaySoldierEntity updateTargetCache(ClaySoldierEntity soldier, ClaySoldierEntity currentTarget) {
        if (isValidTarget(currentTarget, soldier)) {
            return currentTarget;
        }

        if (!shouldScanForTarget(soldier)) {
            return null;
        }

        boolean profiling = TargetingProfiler.isEnabled();
        long startNanos = profiling ? System.nanoTime() : 0L;

        SoldierIndex index = SoldierIndex.get(soldier.level());
        Map<Integer, List<ClaySoldierEntity>> allTeams = index.getAllTeams();
        int myTeam = soldier.getTeamId();
        double myX = soldier.getX();
        double myY = soldier.getY();
        double myZ = soldier.getZ();
        double bestDistSq = TARGET_RANGE_SQ;
        ClaySoldierEntity best = null;

        for (Map.Entry<Integer, List<ClaySoldierEntity>> entry : allTeams.entrySet()) {
            if (entry.getKey() == myTeam) continue;
            List<ClaySoldierEntity> enemies = entry.getValue();
            for (int i = 0, size = enemies.size(); i < size; i++) {
                ClaySoldierEntity candidate = enemies.get(i);
                if (!candidate.isAlive() || candidate.isRemoved() || candidate.isSoldierDead()) continue;
                double dy = Math.abs(candidate.getY() - myY);
                if (dy > TARGET_RANGE_Y) continue;
                double dx = candidate.getX() - myX;
                double dz = candidate.getZ() - myZ;
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq < bestDistSq) {
                    best = candidate;
                    bestDistSq = distSq;
                }
            }
        }

        if (profiling) {
            TargetingProfiler.recordTime("targetScanTime", System.nanoTime() - startNanos);
            TargetingProfiler.recordSample("targetScans");
        }
        return best;
    }

    /**
     * Update the cached mount target for this soldier.
     * Returns early if the current target is still valid; otherwise scans for a new one.
     *
     * @param soldier The soldier performing the scan.
     * @param currentTarget The currently cached mount target (may be null or invalid).
     * @return The best valid mount found, or null if none.
     */
    public static BaseMountEntity updateMountTargetCache(ClaySoldierEntity soldier, BaseMountEntity currentTarget) {
        if (isValidMountTarget(currentTarget, soldier)) {
            return currentTarget;
        }

        if (!shouldScanForMount(soldier)) {
            return null;
        }

        boolean profiling = TargetingProfiler.isEnabled();
        long startNanos = profiling ? System.nanoTime() : 0L;
        AABB scanBox = soldier.getBoundingBox().inflate(MOUNT_SEARCH_RANGE, 1.2, MOUNT_SEARCH_RANGE);
        long aabbStart = profiling ? System.nanoTime() : 0L;
        List<BaseMountEntity> candidates = soldier.level().getEntitiesOfClass(
            BaseMountEntity.class,
            scanBox,
            e -> isMountCandidate(e)
        );
        if (profiling) {
            TargetingProfiler.recordTime("aabbQueryTime", System.nanoTime() - aabbStart);
            TargetingProfiler.recordSample("aabbQueries");
        }

        double bestDistSq = Double.MAX_VALUE;
        BaseMountEntity best = null;
        for (BaseMountEntity candidate : candidates) {
            double distSq = soldier.distanceToSqr(candidate);
            if (distSq <= MOUNT_SEARCH_RANGE_SQ && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }
        if (profiling) {
            TargetingProfiler.recordTime("mountScanTime", System.nanoTime() - startNanos);
            TargetingProfiler.recordSample("mountScans");
        }
        return best;
    }

    /**
     * Validate an existing combat target without triggering a new scan.
     */
    public static boolean hasValidTarget(ClaySoldierEntity soldier, ClaySoldierEntity target) {
        return isValidTarget(target, soldier);
    }

    /**
     * Check if a soldier target is still valid for combat.
     */
    private static boolean isValidTarget(ClaySoldierEntity candidate, ClaySoldierEntity searcher) {
        if (candidate == null || candidate == searcher) return false;
        if (!candidate.isAlive() || candidate.isRemoved() || candidate.isSoldierDead()) return false;
        if (candidate.getTeamId() == searcher.getTeamId()) return false;
        return searcher.distanceToSqr(candidate) <= TARGET_RANGE_SQ;
    }

    /**
     * Check if a mount target is still valid for acquisition.
     */
    private static boolean isValidMountTarget(BaseMountEntity candidate, ClaySoldierEntity searcher) {
        if (!isMountCandidate(candidate)) {
            return false;
        }
        return searcher.distanceToSqr(candidate) <= MOUNT_SEARCH_RANGE_SQ;
    }

    private static boolean isMountCandidate(BaseMountEntity candidate) {
        boolean profiling = TargetingProfiler.isEnabled();
        long checkStart = profiling ? System.nanoTime() : 0L;
        boolean result = candidate != null
            && candidate.isAlive()
            && !candidate.isRemoved()
            && candidate.getMaxPassengers() > candidate.getPassengers().size();
        if (profiling) {
            TargetingProfiler.recordTime("predicateCheckTime", System.nanoTime() - checkStart);
            TargetingProfiler.recordSample("predicateChecks");
        }
        return result;
    }

    /**
     * Check if this is a tick where a combat target scan should occur.
     */
    private static boolean shouldScanForTarget(ClaySoldierEntity soldier) {
        long gameTime = soldier.level().getGameTime();
        int entityId = soldier.getId();
        int interval = TARGET_SCAN_BASE_INTERVAL + Math.floorMod(entityId * 31 + 7, TARGET_SCAN_JITTER + 1);
        int phase = Math.floorMod(entityId * 17 + 3, interval);
        return Math.floorMod(gameTime, interval) == phase;
    }

    /**
     * Check if this is a tick where a mount target scan should occur.
     */
    private static boolean shouldScanForMount(ClaySoldierEntity soldier) {
        long gameTime = soldier.level().getGameTime();
        int entityId = soldier.getId();
        int interval = MOUNT_SCAN_BASE_INTERVAL + Math.floorMod(entityId * 29 + 11, MOUNT_SCAN_JITTER + 1);
        int phase = Math.floorMod(entityId * 13 + 5, interval);
        return Math.floorMod(gameTime, interval) == phase;
    }
}
