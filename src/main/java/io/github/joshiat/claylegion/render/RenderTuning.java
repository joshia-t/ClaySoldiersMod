package io.github.joshiat.claylegion.render;

/**
 * Runtime render tuning for in-game visual iteration.
 * Shared between command handlers and client renderer.
 */
public final class RenderTuning {

    public static final float DEFAULT_SCALE = 0.55f;
    public static final float DEFAULT_Y_OFFSET = 0.0f;

    private static volatile float scale = DEFAULT_SCALE;
    private static volatile float yOffset = DEFAULT_Y_OFFSET;

    private RenderTuning() {}

    public static float getScale() {
        return scale;
    }

    public static void setScale(float value) {
        scale = clamp(value, 0.05f, 3.0f);
    }

    public static float getYOffset() {
        return yOffset;
    }

    public static void setYOffset(float value) {
        yOffset = clamp(value, -2.0f, 2.0f);
    }

    public static void reset() {
        scale = DEFAULT_SCALE;
        yOffset = DEFAULT_Y_OFFSET;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
