package io.github.joshiat.claylegion.entity.upgrade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for upgrade equip validation. Registry/item mapping and
 * in-world behavior are covered by the GameTest suite (gradlew runGametest).
 */
class UpgradeSpecTest {

    @Test
    void rejectsDuplicateEquip() {
        UpgradeSpec stick = new UpgradeSpec(UpgradeFlags.STICK, UpgradeSlot.MAIN_HAND, 20, 0L, 0L, 0L);
        assertTrue(stick.canEquipOnto(0L));
        assertFalse(stick.canEquipOnto(UpgradeFlags.STICK));
    }

    @Test
    void enforcesRequiresAll() {
        UpgradeSpec flint = new UpgradeSpec(UpgradeFlags.FLINT, UpgradeSlot.ENHANCEMENT, 0,
            UpgradeFlags.STICK, 0L, 0L);
        assertFalse(flint.canEquipOnto(0L));
        assertTrue(flint.canEquipOnto(UpgradeFlags.STICK));
    }

    @Test
    void enforcesRequiresAny() {
        UpgradeSpec wool = new UpgradeSpec(UpgradeFlags.WOOL, UpgradeSlot.ENHANCEMENT, 0,
            0L, UpgradeFlags.LEATHER | UpgradeFlags.RABBIT_HIDE, 0L);
        assertFalse(wool.canEquipOnto(0L));
        assertTrue(wool.canEquipOnto(UpgradeFlags.LEATHER));
        assertTrue(wool.canEquipOnto(UpgradeFlags.RABBIT_HIDE));
    }

    @Test
    void enforcesIncompatibilities() {
        UpgradeSpec leather = new UpgradeSpec(UpgradeFlags.LEATHER, UpgradeSlot.MISC, 20,
            0L, 0L, UpgradeFlags.RABBIT_HIDE);
        assertTrue(leather.canEquipOnto(0L));
        assertFalse(leather.canEquipOnto(UpgradeFlags.RABBIT_HIDE));
    }

    @Test
    void exclusiveSlotsAreFlagged() {
        assertTrue(UpgradeSlot.MAIN_HAND.isExclusive());
        assertTrue(UpgradeSlot.OFF_HAND.isExclusive());
        assertTrue(UpgradeSlot.CORE.isExclusive());
        assertTrue(UpgradeSlot.BEHAVIOR.isExclusive());
        assertFalse(UpgradeSlot.ENHANCEMENT.isExclusive());
        assertFalse(UpgradeSlot.MISC.isExclusive());
    }

    @Test
    void bitIndexMatchesFlag() {
        UpgradeSpec spec = new UpgradeSpec(UpgradeFlags.CONCRETE_POWDER, UpgradeSlot.MISC, 0, 0L, 0L, 0L);
        assertEquals(59, spec.bitIndex());
    }

    @Test
    void combinedRequirementAndBanBothApply() {
        // Hypothetical upgrade needing STICK while banned by BONE.
        UpgradeSpec spec = new UpgradeSpec(UpgradeFlags.FLINT, UpgradeSlot.ENHANCEMENT, 0,
            UpgradeFlags.STICK, 0L, UpgradeFlags.BONE);

        assertFalse(spec.canEquipOnto(0L), "missing requirement");
        assertTrue(spec.canEquipOnto(UpgradeFlags.STICK));
        assertFalse(spec.canEquipOnto(UpgradeFlags.STICK | UpgradeFlags.BONE), "ban outweighs met requirement");
    }

    @Test
    void finiteUsesFlagging() {
        assertTrue(new UpgradeSpec(UpgradeFlags.STICK, UpgradeSlot.MAIN_HAND, 20, 0L, 0L, 0L).hasFiniteUses());
        assertFalse(new UpgradeSpec(UpgradeFlags.GLOWSTONE, UpgradeSlot.MISC, 0, 0L, 0L, 0L).hasFiniteUses());
    }
}
