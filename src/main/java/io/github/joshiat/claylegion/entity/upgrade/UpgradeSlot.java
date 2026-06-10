package io.github.joshiat.claylegion.entity.upgrade;

/**
 * Equipment slot categories for soldier upgrades.
 *
 * Slots marked exclusive hold at most one upgrade at a time; equipping into an
 * occupied exclusive slot is rejected (the soldier keeps what it has).
 */
public enum UpgradeSlot {
    MAIN_HAND(true),
    OFF_HAND(true),
    CORE(true),
    BEHAVIOR(true),
    ENHANCEMENT(false),
    MISC(false);

    private final boolean exclusive;

    UpgradeSlot(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public boolean isExclusive() {
        return exclusive;
    }
}
