package io.github.joshiat.claylegion.entity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight performance profiler for soldier targeting operations.
 *
 * Can be toggled on/off via command or config; zero overhead when disabled.
 * Tracks cumulative timing for targeting scans and combat-tick phases to identify bottlenecks.
 */
public class TargetingProfiler {

    private static volatile boolean enabled = false;
    private static final ConcurrentHashMap<String, AtomicLong> metrics = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> sampleCounts = new ConcurrentHashMap<>();
    private static final Object tickLock = new Object();

    private static long currentTick = Long.MIN_VALUE;
    private static long currentTickTargetScans;
    private static long currentTickMountScans;
    private static long currentTickCombatTicks;
    private static long currentTickTargetScanTimeNs;
    private static long currentTickMountScanTimeNs;
    private static long currentTickCombatTimeNs;
    private static long currentTickSeparationTimeNs;
    private static long currentTickSoldierPhysicsTimeNs;
    private static long currentTickMountTimeNs;
    private static long currentTickProjectileTimeNs;
    private static long currentTickNexusTimeNs;

    private static long maxTargetScansPerTick;
    private static long maxMountScansPerTick;
    private static long maxCombinedScansPerTick;
    private static long maxCombatTicksPerTick;
    private static long maxTargetScanTimePerTickNs;
    private static long maxMountScanTimePerTickNs;
    private static long maxTotalTargetingTimePerTickNs;
    private static long maxCombatTimePerTickNs;
    private static long maxSeparationTimePerTickNs;
    private static long maxSoldierPhysicsTimePerTickNs;
    private static long maxMountTimePerTickNs;
    private static long maxProjectileTimePerTickNs;
    private static long maxNexusTimePerTickNs;

    static {
        metrics.put("targetScanTime", new AtomicLong(0));
        metrics.put("mountScanTime", new AtomicLong(0));
        metrics.put("predicateCheckTime", new AtomicLong(0));
        metrics.put("aabbQueryTime", new AtomicLong(0));
        metrics.put("combatTickTime", new AtomicLong(0));
        metrics.put("statusEffectTime", new AtomicLong(0));
        metrics.put("targetSelectionTime", new AtomicLong(0));
        metrics.put("separationSampleTime", new AtomicLong(0));
        metrics.put("mountAcquireTime", new AtomicLong(0));
        metrics.put("idleBrakeTime", new AtomicLong(0));
        metrics.put("rangedDecisionTime", new AtomicLong(0));
        metrics.put("meleeEngagementTime", new AtomicLong(0));
        metrics.put("chaseTargetTime", new AtomicLong(0));
        metrics.put("immediateThreatTime", new AtomicLong(0));
        metrics.put("lineOfSightTime", new AtomicLong(0));
        metrics.put("memoryChaseTime", new AtomicLong(0));
        metrics.put("gossipHintTime", new AtomicLong(0));
        sampleCounts.put("targetScans", new AtomicLong(0));
        sampleCounts.put("mountScans", new AtomicLong(0));
        sampleCounts.put("predicateChecks", new AtomicLong(0));
        sampleCounts.put("aabbQueries", new AtomicLong(0));
        sampleCounts.put("combatTicks", new AtomicLong(0));
        sampleCounts.put("statusEffectTicks", new AtomicLong(0));
        sampleCounts.put("targetSelections", new AtomicLong(0));
        sampleCounts.put("separationSamples", new AtomicLong(0));
        sampleCounts.put("mountAcquireCalls", new AtomicLong(0));
        sampleCounts.put("idleBrakeCalls", new AtomicLong(0));
        sampleCounts.put("rangedDecisions", new AtomicLong(0));
        sampleCounts.put("meleeEngagements", new AtomicLong(0));
        sampleCounts.put("chaseTargetCalls", new AtomicLong(0));
        sampleCounts.put("immediateThreatChecks", new AtomicLong(0));
        sampleCounts.put("lineOfSightChecks", new AtomicLong(0));
        sampleCounts.put("memoryChaseCalls", new AtomicLong(0));
        sampleCounts.put("gossipHintQueries", new AtomicLong(0));
        // Soldier physics (gravity + drag + move)
        metrics.put("soldierPhysicsTime", new AtomicLong(0));
        sampleCounts.put("soldierPhysicsTicks", new AtomicLong(0));
        // Mount server tick
        metrics.put("mountTickTime", new AtomicLong(0));
        sampleCounts.put("mountTicks", new AtomicLong(0));
        // Projectile collision check
        metrics.put("projectileTickTime", new AtomicLong(0));
        sampleCounts.put("projectileTicks", new AtomicLong(0));
        // Nexus server tick
        metrics.put("nexusTickTime", new AtomicLong(0));
        sampleCounts.put("nexusTicks", new AtomicLong(0));
        // Nexus summon-count world scan (most expensive nexus sub-operation)
        metrics.put("nexusSummonCountTime", new AtomicLong(0));
        sampleCounts.put("nexusSummonCountCalls", new AtomicLong(0));
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            reset();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Record time spent in a specific profiling category.
     * Only records if profiler is enabled; zero-cost call otherwise.
     */
    public static void recordTime(String category, long nanoSeconds) {
        if (!enabled) {
            return;
        }
        AtomicLong metric = metrics.get(category);
        if (metric != null) {
            metric.addAndGet(nanoSeconds);
        }
    }

    /**
     * Increment sample count for a category.
     */
    public static void recordSample(String category) {
        if (!enabled) {
            return;
        }
        AtomicLong count = sampleCounts.get(category);
        if (count != null) {
            count.incrementAndGet();
        }
    }

    /**
     * Record a target or mount scan with game tick context so burst peaks can be tracked.
     */
    public static void recordScan(String timeCategory, String sampleCategory, long nanoSeconds, long gameTime) {
        if (!enabled) {
            return;
        }

        recordTime(timeCategory, nanoSeconds);
        recordSample(sampleCategory);

        synchronized (tickLock) {
            rotateTick(gameTime);

            if ("targetScans".equals(sampleCategory)) {
                currentTickTargetScans++;
                currentTickTargetScanTimeNs += nanoSeconds;
            } else if ("mountScans".equals(sampleCategory)) {
                currentTickMountScans++;
                currentTickMountScanTimeNs += nanoSeconds;
            }

            maxTargetScansPerTick = Math.max(maxTargetScansPerTick, currentTickTargetScans);
            maxMountScansPerTick = Math.max(maxMountScansPerTick, currentTickMountScans);
            maxCombinedScansPerTick = Math.max(maxCombinedScansPerTick, currentTickTargetScans + currentTickMountScans);
            maxTargetScanTimePerTickNs = Math.max(maxTargetScanTimePerTickNs, currentTickTargetScanTimeNs);
            maxMountScanTimePerTickNs = Math.max(maxMountScanTimePerTickNs, currentTickMountScanTimeNs);
            maxTotalTargetingTimePerTickNs = Math.max(maxTotalTargetingTimePerTickNs, currentTickTargetScanTimeNs + currentTickMountScanTimeNs);
        }
    }

    /**
     * Record a combat-phase sample with game tick context so burst peaks can be tracked.
     */
    public static void recordCombatSample(String timeCategory, String sampleCategory, long nanoSeconds, long gameTime) {
        if (!enabled) {
            return;
        }

        recordTime(timeCategory, nanoSeconds);
        recordSample(sampleCategory);

        synchronized (tickLock) {
            rotateTick(gameTime);

            if ("combatTicks".equals(sampleCategory)) {
                currentTickCombatTicks++;
                currentTickCombatTimeNs += nanoSeconds;
                maxCombatTicksPerTick = Math.max(maxCombatTicksPerTick, currentTickCombatTicks);
                maxCombatTimePerTickNs = Math.max(maxCombatTimePerTickNs, currentTickCombatTimeNs);
            } else if ("separationSamples".equals(sampleCategory)) {
                currentTickSeparationTimeNs += nanoSeconds;
                maxSeparationTimePerTickNs = Math.max(maxSeparationTimePerTickNs, currentTickSeparationTimeNs);
            } else if ("soldierPhysicsTicks".equals(sampleCategory)) {
                currentTickSoldierPhysicsTimeNs += nanoSeconds;
                maxSoldierPhysicsTimePerTickNs = Math.max(maxSoldierPhysicsTimePerTickNs, currentTickSoldierPhysicsTimeNs);
            } else if ("mountTicks".equals(sampleCategory)) {
                currentTickMountTimeNs += nanoSeconds;
                maxMountTimePerTickNs = Math.max(maxMountTimePerTickNs, currentTickMountTimeNs);
            } else if ("projectileTicks".equals(sampleCategory)) {
                currentTickProjectileTimeNs += nanoSeconds;
                maxProjectileTimePerTickNs = Math.max(maxProjectileTimePerTickNs, currentTickProjectileTimeNs);
            } else if ("nexusTicks".equals(sampleCategory)) {
                currentTickNexusTimeNs += nanoSeconds;
                maxNexusTimePerTickNs = Math.max(maxNexusTimePerTickNs, currentTickNexusTimeNs);
            }
        }
    }

    private static void rotateTick(long gameTime) {
        if (currentTick == Long.MIN_VALUE) {
            currentTick = gameTime;
            return;
        }
        if (gameTime == currentTick) {
            return;
        }

        currentTick = gameTime;
        currentTickTargetScans = 0;
        currentTickMountScans = 0;
        currentTickCombatTicks = 0;
        currentTickTargetScanTimeNs = 0;
        currentTickMountScanTimeNs = 0;
        currentTickCombatTimeNs = 0;
        currentTickSeparationTimeNs = 0;
        currentTickSoldierPhysicsTimeNs = 0;
        currentTickMountTimeNs = 0;
        currentTickProjectileTimeNs = 0;
        currentTickNexusTimeNs = 0;
    }

    /**
     * Get average time per sample (in microseconds) for a category.
     */
    public static double getAverageTimeUs(String category) {
        AtomicLong time = metrics.get(category);
        AtomicLong count = sampleCounts.get(category);
        if (time == null || count == null || count.get() == 0) {
            return 0.0;
        }
        return time.get() / 1000.0 / Math.max(1, count.get());
    }

    public static double getAverageTimeUs(String timeCategory, String sampleCategory) {
        AtomicLong time = metrics.get(timeCategory);
        AtomicLong count = sampleCounts.get(sampleCategory);
        if (time == null || count == null || count.get() == 0) {
            return 0.0;
        }
        return time.get() / 1000.0 / Math.max(1, count.get());
    }

    /**
     * Get total time spent in category (in milliseconds).
     */
    public static double getTotalTimeMs(String category) {
        AtomicLong time = metrics.get(category);
        if (time == null) {
            return 0.0;
        }
        return time.get() / 1_000_000.0;
    }

    /**
     * Get sample count for a category.
     */
    public static long getSampleCount(String category) {
        AtomicLong count = sampleCounts.get(category);
        return count == null ? 0 : count.get();
    }

    /**
     * Print a summary report to stdout.
     */
    public static void printReport() {
        if (!enabled) {
            System.out.println("[TargetingProfiler] Profiler is disabled.");
            return;
        }

        System.out.println("\n========== TARGETING PROFILER REPORT ==========");
        System.out.println(String.format("Target Scans: %d (avg %.2f µs/scan, total %.3f ms)",
            getSampleCount("targetScans"),
            getAverageTimeUs("targetScanTime", "targetScans"),
            getTotalTimeMs("targetScanTime")
        ));
        System.out.println(String.format("Mount Scans: %d (avg %.2f µs/scan, total %.3f ms)",
            getSampleCount("mountScans"),
            getAverageTimeUs("mountScanTime", "mountScans"),
            getTotalTimeMs("mountScanTime")
        ));
        System.out.println(String.format("AABB Queries: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("aabbQueryTime", "aabbQueries"),
            getTotalTimeMs("aabbQueryTime")
        ));
        System.out.println(String.format("Predicate Checks: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("predicateCheckTime", "predicateChecks"),
            getTotalTimeMs("predicateCheckTime")
        ));
        System.out.println(String.format("Combat Ticks: %d (avg %.2f µs/tick, total %.3f ms)",
            getSampleCount("combatTicks"),
            getAverageTimeUs("combatTickTime", "combatTicks"),
            getTotalTimeMs("combatTickTime")
        ));
        System.out.println(String.format("Status Effects: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("statusEffectTime", "statusEffectTicks"),
            getTotalTimeMs("statusEffectTime")
        ));
        System.out.println(String.format("Target Selection: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("targetSelectionTime", "targetSelections"),
            getTotalTimeMs("targetSelectionTime")
        ));
        System.out.println(String.format("Separation Samples: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("separationSampleTime", "separationSamples"),
            getTotalTimeMs("separationSampleTime")
        ));
        System.out.println(String.format("Mount Acquire: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("mountAcquireTime", "mountAcquireCalls"),
            getTotalTimeMs("mountAcquireTime")
        ));
        System.out.println(String.format("Idle Brake: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("idleBrakeTime", "idleBrakeCalls"),
            getTotalTimeMs("idleBrakeTime")
        ));
        System.out.println(String.format("Ranged Decisions: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("rangedDecisionTime", "rangedDecisions"),
            getTotalTimeMs("rangedDecisionTime")
        ));
        System.out.println(String.format("Melee Engagements: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("meleeEngagementTime", "meleeEngagements"),
            getTotalTimeMs("meleeEngagementTime")
        ));
        System.out.println(String.format("Chase Target: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("chaseTargetTime", "chaseTargetCalls"),
            getTotalTimeMs("chaseTargetTime")
        ));
        System.out.println(String.format("Immediate Threat: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("immediateThreatTime", "immediateThreatChecks"),
            getTotalTimeMs("immediateThreatTime")
        ));
        System.out.println(String.format("Line Of Sight Checks: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("lineOfSightTime", "lineOfSightChecks"),
            getTotalTimeMs("lineOfSightTime")
        ));
        System.out.println(String.format("Memory Chase: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("memoryChaseTime", "memoryChaseCalls"),
            getTotalTimeMs("memoryChaseTime")
        ));
        System.out.println(String.format("Gossip Hint Query: avg %.2f µs, total %.3f ms",
            getAverageTimeUs("gossipHintTime", "gossipHintQueries"),
            getTotalTimeMs("gossipHintTime")
        ));
        System.out.println("--- Other Systems ---");
        System.out.println(String.format("Soldier Physics (move): %d ticks (avg %.2f µs/tick, total %.3f ms)",
            getSampleCount("soldierPhysicsTicks"),
            getAverageTimeUs("soldierPhysicsTime", "soldierPhysicsTicks"),
            getTotalTimeMs("soldierPhysicsTime")
        ));
        System.out.println(String.format("Mount Server Tick: %d ticks (avg %.2f µs/tick, total %.3f ms)",
            getSampleCount("mountTicks"),
            getAverageTimeUs("mountTickTime", "mountTicks"),
            getTotalTimeMs("mountTickTime")
        ));
        System.out.println(String.format("Projectile Collision Check: %d ticks (avg %.2f µs/tick, total %.3f ms)",
            getSampleCount("projectileTicks"),
            getAverageTimeUs("projectileTickTime", "projectileTicks"),
            getTotalTimeMs("projectileTickTime")
        ));
        System.out.println(String.format("Nexus Server Tick: %d ticks (avg %.2f µs/tick, total %.3f ms)",
            getSampleCount("nexusTicks"),
            getAverageTimeUs("nexusTickTime", "nexusTicks"),
            getTotalTimeMs("nexusTickTime")
        ));
        System.out.println(String.format("Nexus Summon Count (world scan): %d calls (avg %.2f µs/call, total %.3f ms)",
            getSampleCount("nexusSummonCountCalls"),
            getAverageTimeUs("nexusSummonCountTime", "nexusSummonCountCalls"),
            getTotalTimeMs("nexusSummonCountTime")
        ));
        synchronized (tickLock) {
            System.out.println(String.format("Per-Tick Peaks: target scans %d, mount scans %d, combined scans %d",
                maxTargetScansPerTick,
                maxMountScansPerTick,
                maxCombinedScansPerTick
            ));
            System.out.println(String.format("Per-Tick Peak Time: target %.3f ms, mount %.3f ms, total targeting %.3f ms",
                maxTargetScanTimePerTickNs / 1_000_000.0,
                maxMountScanTimePerTickNs / 1_000_000.0,
                maxTotalTargetingTimePerTickNs / 1_000_000.0
            ));
            System.out.println(String.format("Combat Tick Peaks: combat ticks %d, separation %.3f ms, total combat %.3f ms",
                maxCombatTicksPerTick,
                maxSeparationTimePerTickNs / 1_000_000.0,
                maxCombatTimePerTickNs / 1_000_000.0
            ));
            System.out.println(String.format("Other System Peaks/tick: soldierPhysics %.3f ms, mount %.3f ms, projectile %.3f ms, nexus %.3f ms",
                maxSoldierPhysicsTimePerTickNs / 1_000_000.0,
                maxMountTimePerTickNs / 1_000_000.0,
                maxProjectileTimePerTickNs / 1_000_000.0,
                maxNexusTimePerTickNs / 1_000_000.0
            ));
        }
        System.out.println("==============================================\n");
    }

    /**
     * Reset all metrics to zero.
     */
    public static void reset() {
        metrics.values().forEach(m -> m.set(0));
        sampleCounts.values().forEach(c -> c.set(0));
        synchronized (tickLock) {
            currentTick = Long.MIN_VALUE;
            currentTickTargetScans = 0;
            currentTickMountScans = 0;
            currentTickCombatTicks = 0;
            currentTickTargetScanTimeNs = 0;
            currentTickMountScanTimeNs = 0;
            currentTickCombatTimeNs = 0;
            currentTickSeparationTimeNs = 0;
            maxTargetScansPerTick = 0;
            maxMountScansPerTick = 0;
            maxCombinedScansPerTick = 0;
            maxCombatTicksPerTick = 0;
            maxTargetScanTimePerTickNs = 0;
            maxMountScanTimePerTickNs = 0;
            maxTotalTargetingTimePerTickNs = 0;
            maxCombatTimePerTickNs = 0;
            maxSeparationTimePerTickNs = 0;
            currentTickSoldierPhysicsTimeNs = 0;
            currentTickMountTimeNs = 0;
            currentTickProjectileTimeNs = 0;
            currentTickNexusTimeNs = 0;
            maxSoldierPhysicsTimePerTickNs = 0;
            maxMountTimePerTickNs = 0;
            maxProjectileTimePerTickNs = 0;
            maxNexusTimePerTickNs = 0;
        }
    }
}
