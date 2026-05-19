package io.github.joshiat.claylegion.entity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight performance profiler for soldier targeting operations.
 *
 * Can be toggled on/off via command or config; zero overhead when disabled.
 * Tracks cumulative timing for targeting scans to identify bottlenecks.
 */
public class TargetingProfiler {

    private static volatile boolean enabled = false;
    private static final ConcurrentHashMap<String, AtomicLong> metrics = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> sampleCounts = new ConcurrentHashMap<>();

    static {
        metrics.put("targetScanTime", new AtomicLong(0));
        metrics.put("mountScanTime", new AtomicLong(0));
        metrics.put("predicateCheckTime", new AtomicLong(0));
        metrics.put("aabbQueryTime", new AtomicLong(0));
        sampleCounts.put("targetScans", new AtomicLong(0));
        sampleCounts.put("mountScans", new AtomicLong(0));
        sampleCounts.put("predicateChecks", new AtomicLong(0));
        sampleCounts.put("aabbQueries", new AtomicLong(0));
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
        System.out.println("==============================================\n");
    }

    /**
     * Reset all metrics to zero.
     */
    public static void reset() {
        metrics.values().forEach(m -> m.set(0));
        sampleCounts.values().forEach(c -> c.set(0));
    }
}
