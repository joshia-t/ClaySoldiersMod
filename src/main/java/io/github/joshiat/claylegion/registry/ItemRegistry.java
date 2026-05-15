package io.github.joshiat.claylegion.registry;

import io.github.joshiat.claylegion.item.SoldierDollItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ItemRegistry {

    public static final SoldierDollItem SOLDIER_DOLL = new SoldierDollItem(
        new Item.Properties().stacksTo(64)
    );

    public static void init() {
    ResourceKey<Item> key = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "soldier_doll")
    );
    Registry.register(BuiltInRegistries.ITEM, key, SOLDIER_DOLL);
    }

    private ItemRegistry() {}
}
