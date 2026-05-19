package io.github.joshiat.claylegion.entity;

import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Encapsulates target acquisition and cache management for clay soldiers.
 *
 * Separating scanning logic makes it easier to experiment with different targeting
 * strategies (e.g., priority weights, team allegiances) without touching the main entity loop.
 */
public class SoldierTargetingHelper {

    private static final int TARGET_SCAN_INTERVAL = 12;
    private static final int MOUNT_SCAN_INTERVAL = 8;
    private static final double TARGET_RANGE_XZ = 4.0;
    private static final double TARGET_RANGE_Y = 1.8;
    private static final double TARGET_RANGE_SQ = 16.0 * 16.0;
    private static final double MOUNT_SEARCH_RANGE = 8.0;
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

        AABB scanBox = soldier.getBoundingBox().inflate(TARGET_RANGE_XZ, TARGET_RANGE_Y, TARGET_RANGE_XZ);
        List<ClaySoldierEntity> candidates = soldier.level().getEntitiesOfClass(
            ClaySoldierEntity.class,
            scanBox,
            e -> isValidTarget(e, soldier)
        );

        double bestDistSq = Double.MAX_VALUE;
        ClaySoldierEntity best = null;
        for (ClaySoldierEntity candidate : candidates) {
            double distSq = soldier.distanceToSqr(candidate);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
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

        AABB scanBox = soldier.getBoundingBox().inflate(MOUNT_SEARCH_RANGE, 1.2, MOUNT_SEARCH_RANGE);
        List<BaseMountEntity> candidates = soldier.level().getEntitiesOfClass(
            BaseMountEntity.class,
            scanBox,
            e -> isValidMountTarget(e, soldier)
        );

        double bestDistSq = Double.MAX_VALUE;
        BaseMountEntity best = null;
        for (BaseMountEntity candidate : candidates) {
            double distSq = soldier.distanceToSqr(candidate);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Check if a soldier target is still valid for combat.
     */
    private static boolean isValidTarget(ClaySoldierEntity candidate, ClaySoldierEntity searcher) {
        return candidate != null
            && candidate != searcher
            && candidate.isAlive()
            && !candidate.isRemoved()
            && !candidate.isSoldierDead()
            && candidate.getTeamId() != searcher.getTeamId()
            && searcher.distanceToSqr(candidate) <= TARGET_RANGE_SQ;
    }

    /**
     * Check if a mount target is still valid for acquisition.
     */
    private static boolean isValidMountTarget(BaseMountEntity candidate, ClaySoldierEntity searcher) {
        return candidate != null
            && candidate.isAlive()
            && !candidate.isRemoved()
            && candidate.getMaxPassengers() > candidate.getPassengers().size()
            && searcher.distanceToSqr(candidate) <= MOUNT_SEARCH_RANGE_SQ;
    }

    /**
     * Check if this is a tick where a combat target scan should occur.
     */
    private static boolean shouldScanForTarget(ClaySoldierEntity soldier) {
        long gameTime = soldier.level().getGameTime();
        return ((gameTime + soldier.getId()) % TARGET_SCAN_INTERVAL) == 0;
    }

    /**
     * Check if this is a tick where a mount target scan should occur.
     */
    private static boolean shouldScanForMount(ClaySoldierEntity soldier) {
        long gameTime = soldier.level().getGameTime();
        return ((gameTime + soldier.getId()) % MOUNT_SCAN_INTERVAL) == 0;
    }
}
