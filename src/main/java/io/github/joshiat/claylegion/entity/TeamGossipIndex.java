package io.github.joshiat.claylegion.entity;

import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Team-level shared enemy-location gossip with time decay.
 *
 * Soldiers report sightings here; teammates can query approximate enemy
 * concentrations even without direct line of sight.
 */
public final class TeamGossipIndex {

    private static final WeakHashMap<Level, TeamGossipIndex> INSTANCES = new WeakHashMap<>();

    private static final float DECAY_PER_TICK = 0.985f;
    private static final float MIN_STRENGTH = 0.08f;
    private static final float MAX_STRENGTH = 24.0f;

    private final Map<Integer, TeamIntel> teamIntelByObserver = new HashMap<>();

    private TeamGossipIndex() {
    }

    public static TeamGossipIndex get(Level level) {
        return INSTANCES.computeIfAbsent(level, ignored -> new TeamGossipIndex());
    }

    public void reportEnemySighting(int observerTeamId, int enemyTeamId,
                                    double x, double y, double z,
                                    float confidence, long gameTime) {
        if (observerTeamId == enemyTeamId || confidence <= 0.0f) {
            return;
        }

        TeamIntel teamIntel = teamIntelByObserver.computeIfAbsent(observerTeamId, ignored -> new TeamIntel());
        GossipPoint point = teamIntel.byEnemyTeam.computeIfAbsent(enemyTeamId, ignored -> new GossipPoint());

        decayPoint(point, gameTime);
        float sample = Math.max(0.05f, confidence);
        float nextStrength = Math.min(MAX_STRENGTH, point.strength + sample);

        if (point.strength <= 1.0E-6f) {
            point.x = x;
            point.y = y;
            point.z = z;
        } else {
            float mix = sample / Math.max(1.0E-6f, nextStrength);
            point.x = point.x + (x - point.x) * mix;
            point.y = point.y + (y - point.y) * mix;
            point.z = point.z + (z - point.z) * mix;
        }

        point.strength = nextStrength;
        point.lastTick = gameTime;
    }

    public GossipHint getStrongestEnemyHint(int observerTeamId,
                                            double observerX,
                                            double observerY,
                                            double observerZ,
                                            double maxDistanceSq,
                                            long gameTime) {
        TeamIntel teamIntel = teamIntelByObserver.get(observerTeamId);
        if (teamIntel == null || teamIntel.byEnemyTeam.isEmpty()) {
            return null;
        }

        int bestEnemyTeamId = -1;
        GossipPoint bestPoint = null;
        for (Map.Entry<Integer, GossipPoint> entry : teamIntel.byEnemyTeam.entrySet()) {
            GossipPoint point = entry.getValue();
            decayPoint(point, gameTime);
            if (point.strength < MIN_STRENGTH) {
                continue;
            }

            double dx = point.x - observerX;
            double dy = point.y - observerY;
            double dz = point.z - observerZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > maxDistanceSq) {
                continue;
            }

            if (bestPoint == null || point.strength > bestPoint.strength) {
                bestPoint = point;
                bestEnemyTeamId = entry.getKey();
            }
        }

        if (bestPoint == null) {
            return null;
        }

        return new GossipHint(bestEnemyTeamId, bestPoint.x, bestPoint.y, bestPoint.z, bestPoint.strength);
    }

    private static void decayPoint(GossipPoint point, long gameTime) {
        if (point.lastTick == Long.MIN_VALUE) {
            point.lastTick = gameTime;
            return;
        }

        long elapsed = gameTime - point.lastTick;
        if (elapsed <= 0) {
            return;
        }

        point.strength = (float) (point.strength * Math.pow(DECAY_PER_TICK, elapsed));
        point.lastTick = gameTime;
    }

    private static final class TeamIntel {
        private final Map<Integer, GossipPoint> byEnemyTeam = new HashMap<>();
    }

    private static final class GossipPoint {
        private double x;
        private double y;
        private double z;
        private float strength;
        private long lastTick = Long.MIN_VALUE;
    }

    public static final class GossipHint {
        public final int enemyTeamId;
        public final double x;
        public final double y;
        public final double z;
        public final float strength;

        private GossipHint(int enemyTeamId, double x, double y, double z, float strength) {
            this.enemyTeamId = enemyTeamId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.strength = strength;
        }
    }
}
