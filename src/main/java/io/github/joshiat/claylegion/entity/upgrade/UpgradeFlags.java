package io.github.joshiat.claylegion.entity.upgrade;

/**
 * Bit-constant definitions for all 60 soldier upgrades, packed into a single long.
 * Bits 0-59 are used; bits 60-63 are reserved.
 *
 * Usage:
 *   state.has(UpgradeFlags.STICK)  → true if stick upgrade is active
 *   state.set(UpgradeFlags.GRAVEL) → adds gravel upgrade
 */
public final class UpgradeFlags {

    // ── Right hand (melee weapons) ─────────────────────────────────────────
    public static final long STICK          = 1L << 0;
    public static final long BLAZEROD       = 1L << 1;
    public static final long IRON_INGOT     = 1L << 2;
    public static final long STONE_BUTTON   = 1L << 3;
    public static final long WOOD_BUTTON    = 1L << 4;
    public static final long SHEAR_RIGHT    = 1L << 5;
    public static final long IRON_BLOCK     = 1L << 6;
    public static final long NETHER_QUARTZ  = 1L << 7;

    // ── Left hand (ranged / shield) ────────────────────────────────────────
    public static final long GRAVEL         = 1L << 8;
    public static final long SNOW           = 1L << 9;
    public static final long FIRE_CHARGE    = 1L << 10;
    public static final long EMERALD        = 1L << 11;
    public static final long BOWL           = 1L << 12;
    public static final long SHEAR_LEFT     = 1L << 13;

    // ── Core (armor / body) ────────────────────────────────────────────────
    public static final long LEATHER        = 1L << 14;
    public static final long BRICK          = 1L << 15;
    public static final long CACTUS         = 1L << 16;
    public static final long NETHER_BRICK   = 1L << 17;
    public static final long STRING         = 1L << 18;
    public static final long GLASS          = 1L << 19;
    public static final long WOOL           = 1L << 20;

    // ── Behavior ──────────────────────────────────────────────────────────
    public static final long WHEAT          = 1L << 21;
    public static final long NETHER_WART    = 1L << 22;
    public static final long FERM_SPIDER_EYE = 1L << 23;

    // ── Misc (utility, passive) ────────────────────────────────────────────
    public static final long FEATHER        = 1L << 24;
    public static final long LILY_PAD       = 1L << 25;
    public static final long SLIMEBALL      = 1L << 26;
    public static final long REDSTONE       = 1L << 27;
    public static final long GLOWSTONE      = 1L << 28;
    public static final long GUNPOWDER      = 1L << 29;
    public static final long MAGMA_CREAM    = 1L << 30;
    public static final long FIREWORK_STAR  = 1L << 31;
    public static final long EGG            = 1L << 32;
    public static final long ENDER_PEARL    = 1L << 33;
    public static final long GOLD_NUGGET    = 1L << 34;
    public static final long CLAY_BALL      = 1L << 35;
    public static final long GHAST_TEAR     = 1L << 36;
    public static final long MOB_HEAD       = 1L << 37;
    public static final long PAPER          = 1L << 38;
    public static final long RED_MUSHROOM   = 1L << 39;
    public static final long BROWN_MUSHROOM = 1L << 40;
    public static final long FOOD           = 1L << 41;

    // ── Enhancement (enchant-style stacking) ──────────────────────────────
    public static final long FLINT          = 1L << 42;
    public static final long GOLD_INGOT     = 1L << 43;
    public static final long DIAMOND        = 1L << 44;
    public static final long DIAMOND_BLOCK  = 1L << 45;
    public static final long COAL           = 1L << 46;
    public static final long BLAZE_POWDER   = 1L << 47;
    public static final long SUGAR          = 1L << 48;
    public static final long RABBIT_HIDE    = 1L << 49;
    public static final long RABBIT_FOOT    = 1L << 50;
    public static final long WHEAT_SEEDS    = 1L << 51;
    public static final long BONE           = 1L << 52;
    public static final long GOLD_MELON     = 1L << 53;
    public static final long PRISMARINE_SHARD = 1L << 54;
    public static final long PRISMARINE_CRYSTALS = 1L << 55;
    public static final long SPONGE         = 1L << 56;
    public static final long ROTTEN_FLESH   = 1L << 57;
    public static final long FIREWORK_ROCKET = 1L << 58;
    public static final long CONCRETE_POWDER = 1L << 59;

    private static final String[] NAMES = new String[64];

    static {
        for (java.lang.reflect.Field field : UpgradeFlags.class.getDeclaredFields()) {
            if (field.getType() == long.class
                && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    NAMES[Long.numberOfTrailingZeros(field.getLong(null))] = field.getName();
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    /** Debug name for a single upgrade bit (e.g. "STICK"), or "BIT_n" if unknown. */
    public static String nameOf(long flag) {
        int idx = Long.numberOfTrailingZeros(flag);
        String name = idx >= 0 && idx < 64 ? NAMES[idx] : null;
        return name != null ? name : "BIT_" + idx;
    }

    private UpgradeFlags() {}
}
