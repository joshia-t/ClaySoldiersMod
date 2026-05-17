package io.github.joshiat.claylegion.registry;

import io.github.joshiat.claylegion.item.EntitySpawnerItem;
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

    private static final ResourceKey<Item> RED_SOLDIER_DOLL_KEY = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "red_soldier_doll")
    );

    private static final ResourceKey<Item> HORSE_SPAWNER_KEY = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "horse_spawner")
    );

    private static final ResourceKey<Item> PEGASUS_SPAWNER_KEY = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "pegasus_spawner")
    );

    private static final ResourceKey<Item> TURTLE_SPAWNER_KEY = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "turtle_spawner")
    );

    private static final ResourceKey<Item> BUNNY_SPAWNER_KEY = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "bunny_spawner")
    );

    private static final ResourceKey<Item> GECKO_SPAWNER_KEY = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "gecko_spawner")
    );

    public static final SoldierDollItem SOLDIER_DOLL = new SoldierDollItem(
        new Item.Properties().stacksTo(64).setId(SOLDIER_DOLL_KEY)
    );

    public static final SoldierDollItem BRICK_SOLDIER_DOLL = new SoldierDollItem(
        new Item.Properties().stacksTo(64).setId(BRICK_SOLDIER_DOLL_KEY)
    );

    public static final SoldierDollItem RED_SOLDIER_DOLL = new SoldierDollItem(
        new Item.Properties().stacksTo(64).setId(RED_SOLDIER_DOLL_KEY),
        14
    );

    public static final EntitySpawnerItem HORSE_SPAWNER = new EntitySpawnerItem(
        new Item.Properties().stacksTo(64).setId(HORSE_SPAWNER_KEY),
        EntityRegistry.HORSE_MOUNT
    );

    public static final EntitySpawnerItem PEGASUS_SPAWNER = new EntitySpawnerItem(
        new Item.Properties().stacksTo(64).setId(PEGASUS_SPAWNER_KEY),
        EntityRegistry.PEGASUS_MOUNT
    );

    public static final EntitySpawnerItem TURTLE_SPAWNER = new EntitySpawnerItem(
        new Item.Properties().stacksTo(64).setId(TURTLE_SPAWNER_KEY),
        EntityRegistry.TURTLE_MOUNT
    );

    public static final EntitySpawnerItem BUNNY_SPAWNER = new EntitySpawnerItem(
        new Item.Properties().stacksTo(64).setId(BUNNY_SPAWNER_KEY),
        EntityRegistry.BUNNY_MOUNT
    );

    public static final EntitySpawnerItem GECKO_SPAWNER = new EntitySpawnerItem(
        new Item.Properties().stacksTo(64).setId(GECKO_SPAWNER_KEY),
        EntityRegistry.GECKO_MOUNT
    );

    public static void init() {
        Registry.register(BuiltInRegistries.ITEM, SOLDIER_DOLL_KEY, SOLDIER_DOLL);
        Registry.register(BuiltInRegistries.ITEM, BRICK_SOLDIER_DOLL_KEY, BRICK_SOLDIER_DOLL);
        Registry.register(BuiltInRegistries.ITEM, RED_SOLDIER_DOLL_KEY, RED_SOLDIER_DOLL);
        Registry.register(BuiltInRegistries.ITEM, HORSE_SPAWNER_KEY, HORSE_SPAWNER);
        Registry.register(BuiltInRegistries.ITEM, PEGASUS_SPAWNER_KEY, PEGASUS_SPAWNER);
        Registry.register(BuiltInRegistries.ITEM, TURTLE_SPAWNER_KEY, TURTLE_SPAWNER);
        Registry.register(BuiltInRegistries.ITEM, BUNNY_SPAWNER_KEY, BUNNY_SPAWNER);
        Registry.register(BuiltInRegistries.ITEM, GECKO_SPAWNER_KEY, GECKO_SPAWNER);
    }

    private ItemRegistry() {}
}
