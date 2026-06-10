package io.github.joshiat.claylegion.entity.upgrade;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeFlagsTest {

    @Test
    void allFlagsAreUniqueSingleBits() throws IllegalAccessException {
        Set<Long> seen = new HashSet<>();
        int count = 0;
        for (Field field : UpgradeFlags.class.getDeclaredFields()) {
            if (field.getType() != long.class || !Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            long flag = field.getLong(null);
            assertEquals(1, Long.bitCount(flag), field.getName() + " must be a single bit");
            assertTrue(seen.add(flag), field.getName() + " reuses a bit");
            count++;
        }
        assertEquals(60, count, "Expected exactly 60 upgrade flags");
    }

    @Test
    void nameOfResolvesFlagNames() {
        assertEquals("STICK", UpgradeFlags.nameOf(UpgradeFlags.STICK));
        assertEquals("CONCRETE_POWDER", UpgradeFlags.nameOf(UpgradeFlags.CONCRETE_POWDER));
        assertEquals("BIT_63", UpgradeFlags.nameOf(1L << 63));
    }
}
