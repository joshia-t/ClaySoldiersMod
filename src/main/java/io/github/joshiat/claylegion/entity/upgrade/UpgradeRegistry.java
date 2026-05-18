package io.github.joshiat.claylegion.entity.upgrade;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal item-to-upgrade mapping for the early Phase 3 ingestion scaffold.
 *
 * This intentionally includes only the subset needed to unblock Phase 2
 * projectile and mount-trait validation.
 */
public final class UpgradeRegistry {

    private static final Map<Item, Long> ITEM_TO_BIT;

    static {
        Map<Item, Long> map = new HashMap<>();

        // Projectile-path upgrades
        map.put(Items.FLINT, UpgradeFlags.FLINT);
        map.put(Items.GRAVEL, UpgradeFlags.GRAVEL);
        map.put(Items.SNOWBALL, UpgradeFlags.SNOW);
        map.put(Items.FIRE_CHARGE, UpgradeFlags.FIRE_CHARGE);
        map.put(Items.EMERALD, UpgradeFlags.EMERALD);

        // Core/mount-trait checkpoints
        map.put(Items.LEATHER, UpgradeFlags.LEATHER);
        map.put(Items.BONE, UpgradeFlags.BONE);

        ITEM_TO_BIT = Collections.unmodifiableMap(map);
    }

    private UpgradeRegistry() {
    }

    public static long getBitFor(Item item) {
        return ITEM_TO_BIT.getOrDefault(item, 0L);
    }

    public static boolean supports(Item item) {
        return ITEM_TO_BIT.containsKey(item);
    }
}