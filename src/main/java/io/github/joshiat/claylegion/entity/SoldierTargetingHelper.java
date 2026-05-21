package io.github.joshiat.claylegion.entity;

import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import io.github.joshiat.claylegion.entity.mount.MountIndex;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

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
    private static final int TARGET_SCAN_ACTIVE_BASE_INTERVAL = 8;
    private static final int TARGET_SCAN_IDLE_BASE_INTERVAL = 18;
    private static final int MOUNT_SCAN_BASE_INTERVAL = 12;
    private static final int MOUNT_SCAN_JITTER = 6; // Effective interval: 12..18
    private static final double TARGET_RANGE_XZ = 16.0;
    private static final double TARGET_RANGE_Y = 1.8;
    private static final double TARGET_RANGE_SQ = TARGET_RANGE_XZ * TARGET_RANGE_XZ;
    private static final double MOUNT_SEARCH_RANGE = 6.0;
    private static final double MOUNT_SEARCH_RANGE_SQ = MOUNT_SEARCH_RANGE * MOUNT_SEARCH_RANGE;
    private static final int SHOUT_MAX_HOPS = 3;
    private static final float SHOUT_STRENGTH_DECAY = 0.72f;
    private static final int SHOUT_MAX_RECIPIENTS = 64;

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
                if (distSq < bestDistSq && hasLineOfSight(soldier, candidate)) {
                    best = candidate;
                    bestDistSq = distSq;
                }
            }
        }

        if (profiling) {
            TargetingProfiler.recordScan(
                "targetScanTime",
                "targetScans",
                System.nanoTime() - startNanos,
                soldier.level().getGameTime()
            );
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
        BaseMountEntity best = MountIndex.get(soldier.level()).findNearestAvailable(
            soldier.getX(),
            soldier.getY(),
            soldier.getZ(),
            MOUNT_SEARCH_RANGE_SQ
        );
        if (profiling) {
            TargetingProfiler.recordScan(
                "mountScanTime",
                "mountScans",
                System.nanoTime() - startNanos,
                soldier.level().getGameTime()
            );
        }
        return best;
    }

    /**
     * Validate an existing combat target without triggering a new scan.
     */
    public static boolean hasValidTarget(ClaySoldierEntity soldier, ClaySoldierEntity target) {
        return isValidTarget(target, soldier);
    }

    public static boolean hasLineOfSight(ClaySoldierEntity observer, ClaySoldierEntity candidate) {
        if (!isValidTarget(candidate, observer)) {
            return false;
        }

        Vec3 start = new Vec3(observer.getX(), observer.getEyeY(), observer.getZ());
        Vec3 end = new Vec3(candidate.getX(), candidate.getEyeY(), candidate.getZ());
        HitResult hit = observer.level().clip(new ClipContext(
            start,
            end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            observer
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    public static void relayEnemySighting(ClaySoldierEntity source, ClaySoldierEntity enemy, long gameTime) {
        if (source == null || enemy == null || source.level().isClientSide()) {
            return;
        }
        if (!isValidTarget(enemy, source)) {
            return;
        }

        SoldierIndex index = SoldierIndex.get(source.level());
        List<ClaySoldierEntity> teammates = index.getTeam(source.getTeamId());
        if (teammates.isEmpty()) {
            return;
        }

        ArrayDeque<RelayNode> queue = new ArrayDeque<>();
        Set<ClaySoldierEntity> visited = new HashSet<>();
        queue.add(new RelayNode(source, 0, 1.0f));
        visited.add(source);

        int delivered = 0;
        while (!queue.isEmpty() && delivered < SHOUT_MAX_RECIPIENTS) {
            RelayNode node = queue.poll();
            if (node.hop >= SHOUT_MAX_HOPS || node.strength <= 0.01f) {
                continue;
            }

            double radius = Math.max(1.0, 6.0 - node.hop);
            double radiusSq = radius * radius;
            for (int i = 0, size = teammates.size(); i < size && delivered < SHOUT_MAX_RECIPIENTS; i++) {
                ClaySoldierEntity teammate = teammates.get(i);
                if (teammate == null || teammate.isRemoved() || !teammate.isAlive() || teammate.isSoldierDead()) {
                    continue;
                }
                if (visited.contains(teammate)) {
                    continue;
                }
                if (node.sender.distanceToSqr(teammate) > radiusSq) {
                    continue;
                }

                visited.add(teammate);
                float nextStrength = node.strength * SHOUT_STRENGTH_DECAY;
                teammate.noteEnemySighting(enemy, enemy.getX(), enemy.getY(), enemy.getZ(), nextStrength, gameTime, false);
                queue.add(new RelayNode(teammate, node.hop + 1, nextStrength));
                delivered++;
            }
        }
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
        return candidate != null
            && candidate.isAlive()
            && !candidate.isRemoved()
            && candidate.getMaxPassengers() > candidate.getPassengers().size();
    }

    /**
     * Check if this is a tick where a combat target scan should occur.
     */
    private static boolean shouldScanForTarget(ClaySoldierEntity soldier) {
        long gameTime = soldier.level().getGameTime();
        int entityId = soldier.getId();
        boolean activelyLooking = soldier.getAiState() != ClaySoldierEntity.SoldierAiState.IDLE
            || soldier.getDeltaMovement().horizontalDistanceSqr() > 0.0009;
        int base = activelyLooking ? TARGET_SCAN_ACTIVE_BASE_INTERVAL : TARGET_SCAN_IDLE_BASE_INTERVAL;
        int interval = base + Math.floorMod(entityId * 31 + 7, TARGET_SCAN_JITTER + 1);
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

    private static final class RelayNode {
        private final ClaySoldierEntity sender;
        private final int hop;
        private final float strength;

        private RelayNode(ClaySoldierEntity sender, int hop, float strength) {
            this.sender = sender;
            this.hop = hop;
            this.strength = strength;
        }
    }
}
