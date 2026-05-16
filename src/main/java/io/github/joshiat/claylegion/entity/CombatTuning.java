package io.github.joshiat.claylegion.entity;

/**
 * Runtime tuning for lightweight soldier combat behavior.
 * Exposed through commands for quick in-game balancing.
 */
public final class CombatTuning {

    private static final float DEFAULT_MAX_OBSTACLE_CLIMB_HEIGHT = 1.25f;
    private static final float DEFAULT_JUMP_ASSIST_VELOCITY = 0.42f;
    private static final float DEFAULT_IDLE_HORIZONTAL_BRAKE = 0.72f;
    private static final float DEFAULT_IDLE_STOP_THRESHOLD_SQ = 0.0009f;
    private static final float DEFAULT_PLAYER_DAMAGE_MULTIPLIER = 1.0f;
    private static final boolean DEFAULT_SOLDIER_COLLISION_ENABLED = true;

    private static volatile float maxObstacleClimbHeight = DEFAULT_MAX_OBSTACLE_CLIMB_HEIGHT;
    private static volatile float jumpAssistVelocity = DEFAULT_JUMP_ASSIST_VELOCITY;
    private static volatile float idleHorizontalBrake = DEFAULT_IDLE_HORIZONTAL_BRAKE;
    private static volatile float idleStopThresholdSq = DEFAULT_IDLE_STOP_THRESHOLD_SQ;
    private static volatile float playerDamageMultiplier = DEFAULT_PLAYER_DAMAGE_MULTIPLIER;
    private static volatile boolean soldierCollisionEnabled = DEFAULT_SOLDIER_COLLISION_ENABLED;

    private CombatTuning() {
    }

    public static void reset() {
        maxObstacleClimbHeight = DEFAULT_MAX_OBSTACLE_CLIMB_HEIGHT;
        jumpAssistVelocity = DEFAULT_JUMP_ASSIST_VELOCITY;
        idleHorizontalBrake = DEFAULT_IDLE_HORIZONTAL_BRAKE;
        idleStopThresholdSq = DEFAULT_IDLE_STOP_THRESHOLD_SQ;
        playerDamageMultiplier = DEFAULT_PLAYER_DAMAGE_MULTIPLIER;
        soldierCollisionEnabled = DEFAULT_SOLDIER_COLLISION_ENABLED;
    }

    public static float getMaxObstacleClimbHeight() {
        return maxObstacleClimbHeight;
    }

    public static void setMaxObstacleClimbHeight(float value) {
        maxObstacleClimbHeight = clamp(value, 0.0f, 3.0f);
    }

    public static float getJumpAssistVelocity() {
        return jumpAssistVelocity;
    }

    public static void setJumpAssistVelocity(float value) {
        jumpAssistVelocity = clamp(value, 0.2f, 0.8f);
    }

    public static float getIdleHorizontalBrake() {
        return idleHorizontalBrake;
    }

    public static void setIdleHorizontalBrake(float value) {
        idleHorizontalBrake = clamp(value, 0.1f, 0.98f);
    }

    public static float getIdleStopThresholdSq() {
        return idleStopThresholdSq;
    }

    public static void setIdleStopThresholdSq(float value) {
        idleStopThresholdSq = clamp(value, 0.0001f, 0.05f);
    }

    public static float getPlayerDamageMultiplier() {
        return playerDamageMultiplier;
    }

    public static void setPlayerDamageMultiplier(float value) {
        playerDamageMultiplier = clamp(value, 0.0f, 10.0f);
    }

    public static boolean isSoldierCollisionEnabled() {
        return soldierCollisionEnabled;
    }

    public static void setSoldierCollisionEnabled(boolean value) {
        soldierCollisionEnabled = value;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
