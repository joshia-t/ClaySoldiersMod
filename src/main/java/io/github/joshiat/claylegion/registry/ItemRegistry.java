package io.github.joshiat.claylegion.registry;

import io.github.joshiat.claylegion.entity.mount.BunnyVariant;
import io.github.joshiat.claylegion.entity.mount.GeckoVariant;
import io.github.joshiat.claylegion.entity.mount.HorseVariant;
import io.github.joshiat.claylegion.entity.mount.TurtleVariant;
import io.github.joshiat.claylegion.item.EntitySpawnerItem;
import io.github.joshiat.claylegion.item.SoldierDollItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ItemRegistry {

    private static final Map<ResourceKey<Item>, Item> REGISTRATIONS = new LinkedHashMap<>();

    private static final ResourceKey<Item> SOLDIER_DOLL_KEY = key("soldier_doll");
    private static final ResourceKey<Item> BRICK_SOLDIER_DOLL_KEY = key("brick_soldier_doll");
    private static final ResourceKey<Item> RED_SOLDIER_DOLL_KEY = key("red_soldier_doll");

    private static final ResourceKey<Item> HORSE_SPAWNER_KEY = key("horse_spawner");
    private static final ResourceKey<Item> PEGASUS_SPAWNER_KEY = key("pegasus_spawner");
    private static final ResourceKey<Item> TURTLE_SPAWNER_KEY = key("turtle_spawner");
    private static final ResourceKey<Item> BUNNY_SPAWNER_KEY = key("bunny_spawner");
    private static final ResourceKey<Item> GECKO_SPAWNER_KEY = key("gecko_spawner");

    public static final SoldierDollItem SOLDIER_DOLL =
        track(SOLDIER_DOLL_KEY, new SoldierDollItem(props(SOLDIER_DOLL_KEY)));
    public static final SoldierDollItem BRICK_SOLDIER_DOLL =
        track(BRICK_SOLDIER_DOLL_KEY, new SoldierDollItem(props(BRICK_SOLDIER_DOLL_KEY)));
    public static final SoldierDollItem RED_SOLDIER_DOLL =
        track(RED_SOLDIER_DOLL_KEY, new SoldierDollItem(props(RED_SOLDIER_DOLL_KEY), 14));

    // Default spawners — pick variant 0 of each animal so they look reasonable.
    public static final EntitySpawnerItem HORSE_SPAWNER = track(HORSE_SPAWNER_KEY,
        new EntitySpawnerItem(props(HORSE_SPAWNER_KEY), EntityRegistry.HORSE_MOUNT,
            r -> HorseVariant.CLAY.pickRandomIndex(r) & 0xff));

    public static final EntitySpawnerItem PEGASUS_SPAWNER = track(PEGASUS_SPAWNER_KEY,
        new EntitySpawnerItem(props(PEGASUS_SPAWNER_KEY), EntityRegistry.PEGASUS_MOUNT,
            r -> HorseVariant.CLAY.pickRandomIndex(r) & 0xff));

    public static final EntitySpawnerItem TURTLE_SPAWNER = track(TURTLE_SPAWNER_KEY,
        new EntitySpawnerItem(props(TURTLE_SPAWNER_KEY), EntityRegistry.TURTLE_MOUNT,
            r -> TurtleVariant.COBBLE.pickRandomIndex(r) & 0xff));

    public static final EntitySpawnerItem BUNNY_SPAWNER = track(BUNNY_SPAWNER_KEY,
        new EntitySpawnerItem(props(BUNNY_SPAWNER_KEY), EntityRegistry.BUNNY_MOUNT,
            r -> BunnyVariant.WHITE.pickRandomIndex(r) & 0xff));

    public static final EntitySpawnerItem GECKO_SPAWNER = track(GECKO_SPAWNER_KEY,
        new EntitySpawnerItem(props(GECKO_SPAWNER_KEY), EntityRegistry.GECKO_MOUNT,
            r -> GeckoVariant.OAK.pickRandomIndex(r) & 0xff));

    private static final ResourceKey<Item> CLAY_NEXUS_KEY = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("clay-legion", "clay_nexus")
    );

    // === Horse variant spawners (11) ===
    public static final EntitySpawnerItem CLAY_HORSE_SPAWNER     = horseSpawner("clay_horse_spawner", HorseVariant.CLAY);
    public static final EntitySpawnerItem DIRT_HORSE_SPAWNER     = horseSpawner("dirt_horse_spawner", HorseVariant.DIRT);
    public static final EntitySpawnerItem SAND_HORSE_SPAWNER     = horseSpawner("sand_horse_spawner", HorseVariant.SAND);
    public static final EntitySpawnerItem GRAVEL_HORSE_SPAWNER   = horseSpawner("gravel_horse_spawner", HorseVariant.GRAVEL);
    public static final EntitySpawnerItem SNOW_HORSE_SPAWNER     = horseSpawner("snow_horse_spawner", HorseVariant.SNOW);
    public static final EntitySpawnerItem GRASS_HORSE_SPAWNER    = horseSpawner("grass_horse_spawner", HorseVariant.GRASS);
    public static final EntitySpawnerItem LAPIS_HORSE_SPAWNER    = horseSpawner("lapis_horse_spawner", HorseVariant.LAPIS);
    public static final EntitySpawnerItem CARROT_HORSE_SPAWNER   = horseSpawner("carrot_horse_spawner", HorseVariant.CARROT);
    public static final EntitySpawnerItem SOULSAND_HORSE_SPAWNER = horseSpawner("soulsand_horse_spawner", HorseVariant.SOULSAND);
    public static final EntitySpawnerItem CAKE_HORSE_SPAWNER     = horseSpawner("cake_horse_spawner", HorseVariant.CAKE);
    public static final EntitySpawnerItem NIGHTMARE_HORSE_SPAWNER = horseSpawner("nightmare_horse_spawner", HorseVariant.NIGHTMARE);

    // === Pegasus variant spawners (same texture set as horse) ===
    public static final EntitySpawnerItem CLAY_PEGASUS_SPAWNER     = pegasusSpawner("clay_pegasus_spawner", HorseVariant.CLAY);
    public static final EntitySpawnerItem DIRT_PEGASUS_SPAWNER     = pegasusSpawner("dirt_pegasus_spawner", HorseVariant.DIRT);
    public static final EntitySpawnerItem SAND_PEGASUS_SPAWNER     = pegasusSpawner("sand_pegasus_spawner", HorseVariant.SAND);
    public static final EntitySpawnerItem GRAVEL_PEGASUS_SPAWNER   = pegasusSpawner("gravel_pegasus_spawner", HorseVariant.GRAVEL);
    public static final EntitySpawnerItem SNOW_PEGASUS_SPAWNER     = pegasusSpawner("snow_pegasus_spawner", HorseVariant.SNOW);
    public static final EntitySpawnerItem GRASS_PEGASUS_SPAWNER    = pegasusSpawner("grass_pegasus_spawner", HorseVariant.GRASS);
    public static final EntitySpawnerItem LAPIS_PEGASUS_SPAWNER    = pegasusSpawner("lapis_pegasus_spawner", HorseVariant.LAPIS);
    public static final EntitySpawnerItem CARROT_PEGASUS_SPAWNER   = pegasusSpawner("carrot_pegasus_spawner", HorseVariant.CARROT);
    public static final EntitySpawnerItem SOULSAND_PEGASUS_SPAWNER = pegasusSpawner("soulsand_pegasus_spawner", HorseVariant.SOULSAND);
    public static final EntitySpawnerItem CAKE_PEGASUS_SPAWNER     = pegasusSpawner("cake_pegasus_spawner", HorseVariant.CAKE);
    public static final EntitySpawnerItem NIGHTMARE_PEGASUS_SPAWNER = pegasusSpawner("nightmare_pegasus_spawner", HorseVariant.NIGHTMARE);

    // === Turtle variant spawners (10) ===
    public static final EntitySpawnerItem COBBLE_TURTLE_SPAWNER     = turtleSpawner("cobble_turtle_spawner", TurtleVariant.COBBLE);
    public static final EntitySpawnerItem MOSSY_TURTLE_SPAWNER      = turtleSpawner("mossy_turtle_spawner", TurtleVariant.MOSSY);
    public static final EntitySpawnerItem SANDSTONE_TURTLE_SPAWNER  = turtleSpawner("sandstone_turtle_spawner", TurtleVariant.SANDSTONE);
    public static final EntitySpawnerItem NETHERRACK_TURTLE_SPAWNER = turtleSpawner("netherrack_turtle_spawner", TurtleVariant.NETHERRACK);
    public static final EntitySpawnerItem ENDSTONE_TURTLE_SPAWNER   = turtleSpawner("endstone_turtle_spawner", TurtleVariant.ENDSTONE);
    public static final EntitySpawnerItem LAPIS_TURTLE_SPAWNER      = turtleSpawner("lapis_turtle_spawner", TurtleVariant.LAPIS);
    public static final EntitySpawnerItem MELON_TURTLE_SPAWNER      = turtleSpawner("melon_turtle_spawner", TurtleVariant.MELON);
    public static final EntitySpawnerItem PUMPKIN_TURTLE_SPAWNER    = turtleSpawner("pumpkin_turtle_spawner", TurtleVariant.PUMPKIN);
    public static final EntitySpawnerItem CAKE_TURTLE_SPAWNER       = turtleSpawner("cake_turtle_spawner", TurtleVariant.CAKE);
    public static final EntitySpawnerItem KAWAKO_TURTLE_SPAWNER     = turtleSpawner("kawako_turtle_spawner", TurtleVariant.KAWAKO);

    // === Bunny variant spawners (16) ===
    public static final EntitySpawnerItem WHITE_BUNNY_SPAWNER      = bunnySpawner("white_bunny_spawner", BunnyVariant.WHITE);
    public static final EntitySpawnerItem LIGHT_GRAY_BUNNY_SPAWNER = bunnySpawner("light_gray_bunny_spawner", BunnyVariant.LIGHT_GRAY);
    public static final EntitySpawnerItem GRAY_BUNNY_SPAWNER       = bunnySpawner("gray_bunny_spawner", BunnyVariant.GRAY);
    public static final EntitySpawnerItem BLACK_BUNNY_SPAWNER      = bunnySpawner("black_bunny_spawner", BunnyVariant.BLACK);
    public static final EntitySpawnerItem BROWN_BUNNY_SPAWNER      = bunnySpawner("brown_bunny_spawner", BunnyVariant.BROWN);
    public static final EntitySpawnerItem RED_BUNNY_SPAWNER        = bunnySpawner("red_bunny_spawner", BunnyVariant.RED);
    public static final EntitySpawnerItem ORANGE_BUNNY_SPAWNER     = bunnySpawner("orange_bunny_spawner", BunnyVariant.ORANGE);
    public static final EntitySpawnerItem YELLOW_BUNNY_SPAWNER     = bunnySpawner("yellow_bunny_spawner", BunnyVariant.YELLOW);
    public static final EntitySpawnerItem LIME_BUNNY_SPAWNER       = bunnySpawner("lime_bunny_spawner", BunnyVariant.LIME);
    public static final EntitySpawnerItem GREEN_BUNNY_SPAWNER      = bunnySpawner("green_bunny_spawner", BunnyVariant.GREEN);
    public static final EntitySpawnerItem CYAN_BUNNY_SPAWNER       = bunnySpawner("cyan_bunny_spawner", BunnyVariant.CYAN);
    public static final EntitySpawnerItem LIGHT_BLUE_BUNNY_SPAWNER = bunnySpawner("light_blue_bunny_spawner", BunnyVariant.LIGHT_BLUE);
    public static final EntitySpawnerItem BLUE_BUNNY_SPAWNER       = bunnySpawner("blue_bunny_spawner", BunnyVariant.BLUE);
    public static final EntitySpawnerItem PURPLE_BUNNY_SPAWNER     = bunnySpawner("purple_bunny_spawner", BunnyVariant.PURPLE);
    public static final EntitySpawnerItem MAGENTA_BUNNY_SPAWNER    = bunnySpawner("magenta_bunny_spawner", BunnyVariant.MAGENTA);
    public static final EntitySpawnerItem PINK_BUNNY_SPAWNER       = bunnySpawner("pink_bunny_spawner", BunnyVariant.PINK);

    // === Gecko variant spawners (6) ===
    public static final EntitySpawnerItem OAK_GECKO_SPAWNER     = geckoSpawner("oak_gecko_spawner", GeckoVariant.OAK);
    public static final EntitySpawnerItem BIRCH_GECKO_SPAWNER   = geckoSpawner("birch_gecko_spawner", GeckoVariant.BIRCH);
    public static final EntitySpawnerItem JUNGLE_GECKO_SPAWNER  = geckoSpawner("jungle_gecko_spawner", GeckoVariant.JUNGLE);
    public static final EntitySpawnerItem ACACIA_GECKO_SPAWNER  = geckoSpawner("acacia_gecko_spawner", GeckoVariant.ACACIA);
    public static final EntitySpawnerItem DARKOAK_GECKO_SPAWNER = geckoSpawner("darkoak_gecko_spawner", GeckoVariant.DARKOAK);
    public static final EntitySpawnerItem PINE_GECKO_SPAWNER    = geckoSpawner("pine_gecko_spawner", GeckoVariant.PINE);

    public static final BlockItem CLAY_NEXUS = track(CLAY_NEXUS_KEY,
        new BlockItem(BlockRegistry.CLAY_NEXUS, props(CLAY_NEXUS_KEY)));

    public static void init() {
        REGISTRATIONS.forEach((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
    }

    // --- helpers ---

    private static EntitySpawnerItem horseSpawner(String name, HorseVariant variant) {
        return makeSpawner(name, EntityRegistry.HORSE_MOUNT, r -> variant.pickRandomIndex(r) & 0xff);
    }

    private static EntitySpawnerItem pegasusSpawner(String name, HorseVariant variant) {
        return makeSpawner(name, EntityRegistry.PEGASUS_MOUNT, r -> variant.pickRandomIndex(r) & 0xff);
    }

    private static EntitySpawnerItem turtleSpawner(String name, TurtleVariant variant) {
        return makeSpawner(name, EntityRegistry.TURTLE_MOUNT, r -> variant.pickRandomIndex(r) & 0xff);
    }

    private static EntitySpawnerItem bunnySpawner(String name, BunnyVariant variant) {
        return makeSpawner(name, EntityRegistry.BUNNY_MOUNT, r -> variant.pickRandomIndex(r) & 0xff);
    }

    private static EntitySpawnerItem geckoSpawner(String name, GeckoVariant variant) {
        return makeSpawner(name, EntityRegistry.GECKO_MOUNT, r -> variant.pickRandomIndex(r) & 0xff);
    }

    private static EntitySpawnerItem makeSpawner(String name,
            EntityType<?> type, ToIntFunction<RandomSource> picker) {
        ResourceKey<Item> k = key(name);
        return track(k, new EntitySpawnerItem(props(k), type, picker));
    }

    private static Item.Properties props(ResourceKey<Item> key) {
        return new Item.Properties().stacksTo(64).setId(key);
    }

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("clay-legion", name));
    }

    private static <T extends Item> T track(ResourceKey<Item> key, T item) {
        REGISTRATIONS.put(key, item);
        return item;
    }

    private ItemRegistry() {}
}
