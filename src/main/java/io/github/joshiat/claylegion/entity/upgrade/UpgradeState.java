package io.github.joshiat.claylegion.entity.upgrade;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Compact bitfield storage for all active soldier upgrades.
 * All 60 upgrade flags fit in a single long, keeping the entity object header small
 * and allowing O(1) upgrade checks via bitwise AND.
 */
public final class UpgradeState {

    private static final String STORAGE_KEY = "Upgrades";

    private long bits;

    public UpgradeState() {
        this.bits = 0L;
    }

    private UpgradeState(long bits) {
        this.bits = bits;
    }

    /** Returns true if the upgrade flag (or combination of flags) is fully set. */
    public boolean has(long flag) {
        return (bits & flag) == flag;
    }

    /** Sets one or more upgrade flags. */
    public void set(long flag) {
        bits |= flag;
    }

    /** Clears one or more upgrade flags. */
    public void clear(long flag) {
        bits &= ~flag;
    }

    /** Replaces all flags at once (used during NBT deserialization). */
    public void setRaw(long raw) {
        this.bits = raw;
    }

    public long getRaw() {
        return bits;
    }

    public void writeToStorage(ValueOutput out) {
        out.putLong(STORAGE_KEY, bits);
    }

    public void readFromStorage(ValueInput in) {
        bits = in.getLong(STORAGE_KEY).orElse(0L);
    }

    public UpgradeState copy() {
        return new UpgradeState(this.bits);
    }
}
