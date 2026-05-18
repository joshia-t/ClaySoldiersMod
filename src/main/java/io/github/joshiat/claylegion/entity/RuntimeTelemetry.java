package io.github.joshiat.claylegion.entity;

/**
 * Allocation-free runtime counters for validating hot-path behavior.
 */
public final class RuntimeTelemetry {

    private static long projectileImpacts;
    private static long slowPayloadApplications;
    private static long combustionPayloadApplications;
    private static long combustionDamageTicks;
    private static long geckoClimbTicks;
    private static long pegasusFlightTicks;
    private static long turtleWaterTicks;

    private RuntimeTelemetry() {
    }

    public static void recordProjectileImpact() {
        projectileImpacts++;
    }

    public static void recordSlowPayload() {
        slowPayloadApplications++;
    }

    public static void recordCombustionPayload() {
        combustionPayloadApplications++;
    }

    public static void recordCombustionDamageTick() {
        combustionDamageTicks++;
    }

    public static void recordGeckoClimbTick() {
        geckoClimbTicks++;
    }

    public static void recordPegasusFlightTick() {
        pegasusFlightTicks++;
    }

    public static void recordTurtleWaterTick() {
        turtleWaterTicks++;
    }

    public static String snapshot() {
        return "telemetry{"
            + "projectileImpacts=" + projectileImpacts
            + ", slowPayloads=" + slowPayloadApplications
            + ", combustionPayloads=" + combustionPayloadApplications
            + ", combustionDamageTicks=" + combustionDamageTicks
            + ", geckoClimbTicks=" + geckoClimbTicks
            + ", pegasusFlightTicks=" + pegasusFlightTicks
            + ", turtleWaterTicks=" + turtleWaterTicks
            + '}';
    }
}
