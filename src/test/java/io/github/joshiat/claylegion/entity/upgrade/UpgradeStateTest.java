package io.github.joshiat.claylegion.entity.upgrade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeStateTest {

    @Test
    void startsEmpty() {
        UpgradeState state = new UpgradeState();
        assertEquals(0L, state.getRaw());
        assertFalse(state.has(UpgradeFlags.STICK));
    }

    @Test
    void setAndClearSingleFlags() {
        UpgradeState state = new UpgradeState();
        state.set(UpgradeFlags.STICK);
        assertTrue(state.has(UpgradeFlags.STICK));
        assertFalse(state.has(UpgradeFlags.BONE));

        state.clear(UpgradeFlags.STICK);
        assertFalse(state.has(UpgradeFlags.STICK));
        assertEquals(0L, state.getRaw());
    }

    @Test
    void hasRequiresAllBitsOfACombination() {
        UpgradeState state = new UpgradeState();
        state.set(UpgradeFlags.STICK);
        // STICK alone does not satisfy STICK+FLINT.
        assertFalse(state.has(UpgradeFlags.STICK | UpgradeFlags.FLINT));

        state.set(UpgradeFlags.FLINT);
        assertTrue(state.has(UpgradeFlags.STICK | UpgradeFlags.FLINT));
    }

    @Test
    void clearOnlyRemovesRequestedBits() {
        UpgradeState state = new UpgradeState();
        state.set(UpgradeFlags.LEATHER | UpgradeFlags.WOOL);
        state.clear(UpgradeFlags.LEATHER);
        assertFalse(state.has(UpgradeFlags.LEATHER));
        assertTrue(state.has(UpgradeFlags.WOOL));
    }

    @Test
    void setRawReplacesEverything() {
        UpgradeState state = new UpgradeState();
        state.set(UpgradeFlags.STICK);
        state.setRaw(UpgradeFlags.BONE);
        assertFalse(state.has(UpgradeFlags.STICK));
        assertTrue(state.has(UpgradeFlags.BONE));
    }

    @Test
    void copyIsIndependent() {
        UpgradeState original = new UpgradeState();
        original.set(UpgradeFlags.STICK);

        UpgradeState copy = original.copy();
        assertTrue(copy.has(UpgradeFlags.STICK));

        copy.set(UpgradeFlags.BONE);
        assertFalse(original.has(UpgradeFlags.BONE), "Mutating the copy must not affect the original");
    }

    @Test
    void highestUpgradeBitRoundTrips() {
        UpgradeState state = new UpgradeState();
        state.set(UpgradeFlags.CONCRETE_POWDER); // bit 59, the top of the range
        assertTrue(state.has(UpgradeFlags.CONCRETE_POWDER));
        assertEquals(1L << 59, state.getRaw());
    }
}
