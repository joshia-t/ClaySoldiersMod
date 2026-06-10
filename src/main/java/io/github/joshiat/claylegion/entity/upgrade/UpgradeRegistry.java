package io.github.joshiat.claylegion.entity.upgrade;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.github.joshiat.claylegion.entity.upgrade.UpgradeFlags.*;
import static io.github.joshiat.claylegion.entity.upgrade.UpgradeSlot.*;

/**
 * Complete item-to-upgrade table for all soldier upgrades (issue #18).
 *
 * Lookup order for an ItemStack:
 *  1. exact item match,
 *  2. item tags (wool, wooden buttons),
 *  3. generic food fallback (any edible item except rotten flesh).
 *
 * The Arrow item is intentionally absent: it is a shortcut that equips
 * Stick + Flint and is special-cased in the soldier equip path.
 */
public final class UpgradeRegistry {

    private static final Map<Item, UpgradeSpec> ITEM_TO_SPEC;
    private static final UpgradeSpec[] SPEC_BY_BIT = new UpgradeSpec[64];
    private static final Item[] DROP_ITEM_BY_BIT = new Item[64];

    private static final UpgradeSpec FOOD_SPEC;
    private static final UpgradeSpec WOOL_SPEC;
    private static final UpgradeSpec WOOD_BUTTON_SPEC;
    public static final UpgradeSpec SHEAR_LEFT_SPEC;

    static {
        Map<Item, UpgradeSpec> map = new HashMap<>();

        // ── Main hand (melee weapons) ──────────────────────────────────────
        define(map, spec(STICK, MAIN_HAND, 20), Items.STICK);
        define(map, spec(BONE, MAIN_HAND, 30), Items.BONE);
        define(map, spec(BLAZEROD, MAIN_HAND, 20), Items.BLAZE_ROD);
        define(map, spec(GOLD_MELON, MAIN_HAND, 20), Items.GLISTERING_MELON_SLICE);
        define(map, spec(SHEAR_RIGHT, MAIN_HAND, 25), Items.SHEARS);
        // Brawler buttons live in the main-hand slot: incompatible with weapons by exclusivity.
        WOOD_BUTTON_SPEC = spec(WOOD_BUTTON, MAIN_HAND, 0);
        define(map, WOOD_BUTTON_SPEC, Items.OAK_BUTTON);
        define(map, spec(STONE_BUTTON, MAIN_HAND, 0), Items.STONE_BUTTON);

        // ── Off hand (shields / ranged / second shear) ─────────────────────
        define(map, spec(BOWL, OFF_HAND, 16), Items.BOWL);
        define(map, spec(NETHER_QUARTZ, OFF_HAND, 4), Items.QUARTZ);
        define(map, spec(GRAVEL, OFF_HAND, 0), Items.GRAVEL);
        define(map, spec(SNOW, OFF_HAND, 0), Items.SNOWBALL);
        define(map, spec(FIRE_CHARGE, OFF_HAND, 0), Items.FIRE_CHARGE);
        define(map, spec(EMERALD, OFF_HAND, 0), Items.EMERALD);
        // Second shear blade: no direct item mapping; equipping a second
        // Items.SHEARS while SHEAR_RIGHT is held routes here (see soldier equip path).
        SHEAR_LEFT_SPEC = register(new UpgradeSpec(SHEAR_LEFT, OFF_HAND, 25, SHEAR_RIGHT, 0L, 0L));

        // ── Core (structural) ──────────────────────────────────────────────
        define(map, new UpgradeSpec(IRON_INGOT, CORE, 0, 0L, 0L, FEATHER | LILY_PAD), Items.IRON_INGOT);
        define(map, spec(BRICK, CORE, 0), Items.BRICK);
        define(map, spec(STRING, CORE, 0), Items.STRING);
        define(map, spec(CACTUS, CORE, 0), Items.CACTUS);
        define(map, spec(NETHER_BRICK, CORE, 0), Items.NETHER_BRICK);

        // ── Enhancements (weapon synergies) ────────────────────────────────
        define(map, new UpgradeSpec(FLINT, ENHANCEMENT, 0, STICK, 0L, 0L), Items.FLINT);
        define(map, new UpgradeSpec(GOLD_INGOT, ENHANCEMENT, 0, GOLD_NUGGET, 0L, 0L), Items.GOLD_INGOT);
        define(map, new UpgradeSpec(IRON_BLOCK, ENHANCEMENT, 0, BOWL, 0L, 0L), Items.IRON_BLOCK);
        define(map, new UpgradeSpec(PRISMARINE_SHARD, ENHANCEMENT, 0, 0L, SHEAR_RIGHT | SHEAR_LEFT, 0L), Items.PRISMARINE_SHARD);
        WOOL_SPEC = register(new UpgradeSpec(WOOL, ENHANCEMENT, 0, 0L, LEATHER | RABBIT_HIDE, 0L));
        define(map, new UpgradeSpec(COAL, ENHANCEMENT, 0, BLAZEROD, 0L, 0L), Items.COAL, Items.CHARCOAL);

        // ── Behavior (AI modifiers, mutually exclusive by slot) ────────────
        define(map, spec(WHEAT, BEHAVIOR, 0), Items.WHEAT);
        define(map, spec(ROTTEN_FLESH, BEHAVIOR, 0), Items.ROTTEN_FLESH);
        define(map, spec(FERM_SPIDER_EYE, BEHAVIOR, 0), Items.FERMENTED_SPIDER_EYE);
        define(map, spec(NETHER_WART, BEHAVIOR, 0), Items.NETHER_WART);
        define(map, spec(SPONGE, BEHAVIOR, 0), Items.SPONGE, Items.WET_SPONGE);

        // ── Misc: armor & defenses ─────────────────────────────────────────
        define(map, new UpgradeSpec(LEATHER, MISC, 20, 0L, 0L, RABBIT_HIDE), Items.LEATHER);
        define(map, new UpgradeSpec(RABBIT_HIDE, MISC, 20, 0L, 0L, LEATHER), Items.RABBIT_HIDE);
        define(map, new UpgradeSpec(DIAMOND, MISC, 0, 0L, 0L, DIAMOND_BLOCK), Items.DIAMOND);
        define(map, new UpgradeSpec(DIAMOND_BLOCK, MISC, 0, 0L, 0L, DIAMOND), Items.DIAMOND_BLOCK);

        // ── Misc: utility & movement ───────────────────────────────────────
        define(map, spec(GLASS, MISC, 0), Items.GLASS, Items.GLASS_BOTTLE, Items.GLASS_PANE);
        define(map, new UpgradeSpec(SUGAR, MISC, 0, 0L, 0L, DIAMOND | DIAMOND_BLOCK), Items.SUGAR);
        define(map, new UpgradeSpec(FEATHER, MISC, 0, 0L, 0L, IRON_INGOT), Items.FEATHER);
        define(map, new UpgradeSpec(LILY_PAD, MISC, 0, 0L, 0L, IRON_INGOT), Items.LILY_PAD);
        define(map, spec(RABBIT_FOOT, MISC, 0), Items.RABBIT_FOOT);

        // ── Misc: combat tools ─────────────────────────────────────────────
        define(map, spec(RED_MUSHROOM, MISC, 1), Items.RED_MUSHROOM);
        define(map, spec(BLAZE_POWDER, MISC, 1), Items.BLAZE_POWDER);
        define(map, spec(REDSTONE, MISC, 4), Items.REDSTONE);
        define(map, spec(SLIMEBALL, MISC, 5), Items.SLIME_BALL);
        define(map, spec(BROWN_MUSHROOM, MISC, 2), Items.BROWN_MUSHROOM);
        FOOD_SPEC = register(new UpgradeSpec(FOOD, MISC, 4, 0L, 0L, 0L));

        // ── Misc: resurrection & infection ─────────────────────────────────
        define(map, spec(CLAY_BALL, MISC, 4), Items.CLAY_BALL);
        define(map, spec(GHAST_TEAR, MISC, 2), Items.GHAST_TEAR);
        define(map, new UpgradeSpec(ENDER_PEARL, MISC, 0, 0L, 0L, WHEAT_SEEDS), Items.ENDER_PEARL);
        define(map, new UpgradeSpec(WHEAT_SEEDS, MISC, 0, 0L, 0L, ENDER_PEARL), Items.WHEAT_SEEDS);

        // ── Misc: death & escape ───────────────────────────────────────────
        define(map, new UpgradeSpec(GUNPOWDER, MISC, 0, 0L, 0L, FIREWORK_STAR | MAGMA_CREAM), Items.GUNPOWDER);
        define(map, new UpgradeSpec(MAGMA_CREAM, MISC, 0, 0L, 0L, GUNPOWDER), Items.MAGMA_CREAM);
        define(map, new UpgradeSpec(FIREWORK_STAR, MISC, 0, 0L, 0L, GUNPOWDER), Items.FIREWORK_STAR);
        define(map, spec(FIREWORK_ROCKET, MISC, 1), Items.FIREWORK_ROCKET);

        // ── Misc: cosmetics & roles ────────────────────────────────────────
        define(map, spec(GOLD_NUGGET, MISC, 0), Items.GOLD_NUGGET);
        define(map, spec(EGG, MISC, 0), Items.EGG);
        define(map, spec(GLOWSTONE, MISC, 0), Items.GLOWSTONE_DUST);
        define(map, spec(PAPER, MISC, 0), Items.PAPER);
        define(map, new UpgradeSpec(CONCRETE_POWDER, MISC, 0, PAPER, 0L, 0L),
            Items.WHITE_CONCRETE_POWDER, Items.ORANGE_CONCRETE_POWDER, Items.MAGENTA_CONCRETE_POWDER,
            Items.LIGHT_BLUE_CONCRETE_POWDER, Items.YELLOW_CONCRETE_POWDER, Items.LIME_CONCRETE_POWDER,
            Items.PINK_CONCRETE_POWDER, Items.GRAY_CONCRETE_POWDER, Items.LIGHT_GRAY_CONCRETE_POWDER,
            Items.CYAN_CONCRETE_POWDER, Items.PURPLE_CONCRETE_POWDER, Items.BLUE_CONCRETE_POWDER,
            Items.BROWN_CONCRETE_POWDER, Items.GREEN_CONCRETE_POWDER, Items.RED_CONCRETE_POWDER,
            Items.BLACK_CONCRETE_POWDER);
        define(map, spec(MOB_HEAD, MISC, 0),
            Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.ZOMBIE_HEAD,
            Items.PLAYER_HEAD, Items.CREEPER_HEAD, Items.DRAGON_HEAD, Items.PIGLIN_HEAD);
        define(map, spec(PRISMARINE_CRYSTALS, MISC, 0), Items.PRISMARINE_CRYSTALS);

        // Canonical drop items for specs without a 1:1 item mapping.
        DROP_ITEM_BY_BIT[Long.numberOfTrailingZeros(WOOL)] = Items.WHITE_WOOL;
        DROP_ITEM_BY_BIT[Long.numberOfTrailingZeros(FOOD)] = Items.BREAD;
        DROP_ITEM_BY_BIT[Long.numberOfTrailingZeros(SHEAR_LEFT)] = Items.SHEARS;

        ITEM_TO_SPEC = Collections.unmodifiableMap(map);
    }

    private UpgradeRegistry() {
    }

    /** Spec lookup by upgrade flag (single bit). Null for unknown bits. */
    public static UpgradeSpec getSpec(long flag) {
        int idx = Long.numberOfTrailingZeros(flag);
        return idx >= 0 && idx < 64 ? SPEC_BY_BIT[idx] : null;
    }

    /**
     * Resolve the upgrade spec an item stack maps to, or null if the item
     * is not an upgrade. Tag-based and food fallbacks are applied after the
     * exact item table.
     */
    public static UpgradeSpec getSpecFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        UpgradeSpec exact = ITEM_TO_SPEC.get(stack.getItem());
        if (exact != null) {
            return exact;
        }

        if (stack.is(ItemTags.WOOL)) {
            return WOOL_SPEC;
        }
        if (stack.is(ItemTags.WOODEN_BUTTONS)) {
            return WOOD_BUTTON_SPEC;
        }
        if (stack.has(DataComponents.FOOD) && !stack.is(Items.ROTTEN_FLESH)) {
            return FOOD_SPEC;
        }

        return null;
    }

    /** Item dropped when a soldier dies holding this upgrade, or null for none. */
    public static Item getDropItem(long flag) {
        int idx = Long.numberOfTrailingZeros(flag);
        return idx >= 0 && idx < 64 ? DROP_ITEM_BY_BIT[idx] : null;
    }

    /** Legacy bit lookup used by older call sites. Prefer {@link #getSpecFor(ItemStack)}. */
    public static long getBitFor(Item item) {
        UpgradeSpec spec = ITEM_TO_SPEC.get(item);
        return spec == null ? 0L : spec.flag();
    }

    public static boolean supports(ItemStack stack) {
        return getSpecFor(stack) != null || stack.is(Items.ARROW);
    }

    // --- helpers ---

    private static UpgradeSpec spec(long flag, UpgradeSlot slot, int maxUses) {
        return new UpgradeSpec(flag, slot, maxUses, 0L, 0L, 0L);
    }

    private static UpgradeSpec register(UpgradeSpec spec) {
        SPEC_BY_BIT[spec.bitIndex()] = spec;
        return spec;
    }

    private static void define(Map<Item, UpgradeSpec> map, UpgradeSpec spec, Item... items) {
        register(spec);
        if (items.length > 0) {
            DROP_ITEM_BY_BIT[spec.bitIndex()] = items[0];
        }
        for (Item item : items) {
            map.put(item, spec);
        }
    }
}
