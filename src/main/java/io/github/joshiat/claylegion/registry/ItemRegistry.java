package io.github.joshiat.claylegion.registry;

import io.github.joshiat.claylegion.item.SoldierDollItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ItemRegistry {

    private static final ResourceKey<Item> SOLDIER_DOLL_KEY = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "soldier_doll")
    );

    private static final ResourceKey<Item> BRICK_SOLDIER_DOLL_KEY = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "brick_soldier_doll")
    );

    public static final SoldierDollItem SOLDIER_DOLL = new SoldierDollItem(
        new Item.Properties().stacksTo(64).setId(SOLDIER_DOLL_KEY)
    );

    public static final SoldierDollItem BRICK_SOLDIER_DOLL = new SoldierDollItem(
        new Item.Properties().stacksTo(64).setId(BRICK_SOLDIER_DOLL_KEY)
    );

    public static void init() {
        Registry.register(BuiltInRegistries.ITEM, SOLDIER_DOLL_KEY, SOLDIER_DOLL);
        Registry.register(BuiltInRegistries.ITEM, BRICK_SOLDIER_DOLL_KEY, BRICK_SOLDIER_DOLL);
    }

    private ItemRegistry() {}
}
