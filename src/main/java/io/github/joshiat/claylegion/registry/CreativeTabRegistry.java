package io.github.joshiat.claylegion.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class CreativeTabRegistry {

    public static final ResourceKey<CreativeModeTab> CLAY_LEGION_TAB_KEY = ResourceKey.create(
        BuiltInRegistries.CREATIVE_MODE_TAB.key(),
        Identifier.fromNamespaceAndPath("clay-legion", "clay_legion")
    );

    public static final CreativeModeTab CLAY_LEGION_TAB = FabricCreativeModeTab.builder()
        .icon(() -> new ItemStack(ItemRegistry.RED_SOLDIER_DOLL))
        .title(net.minecraft.network.chat.Component.translatable("creativeTab.clay-legion.clay_legion"))
        .displayItems((parameters, output) -> {
            output.accept(ItemRegistry.LEXICON);
            output.accept(ItemRegistry.CLAY_NEXUS);
            output.accept(ItemRegistry.SOLDIER_DOLL);
            output.accept(ItemRegistry.RED_SOLDIER_DOLL);
            output.accept(ItemRegistry.BRICK_SOLDIER_DOLL);
            output.accept(ItemRegistry.HORSE_SPAWNER);
            output.accept(ItemRegistry.PEGASUS_SPAWNER);
            output.accept(ItemRegistry.TURTLE_SPAWNER);
            output.accept(ItemRegistry.BUNNY_SPAWNER);
            output.accept(ItemRegistry.GECKO_SPAWNER);

            // Horse variants
            output.accept(ItemRegistry.CLAY_HORSE_SPAWNER);
            output.accept(ItemRegistry.DIRT_HORSE_SPAWNER);
            output.accept(ItemRegistry.SAND_HORSE_SPAWNER);
            output.accept(ItemRegistry.GRAVEL_HORSE_SPAWNER);
            output.accept(ItemRegistry.SNOW_HORSE_SPAWNER);
            output.accept(ItemRegistry.GRASS_HORSE_SPAWNER);
            output.accept(ItemRegistry.LAPIS_HORSE_SPAWNER);
            output.accept(ItemRegistry.CARROT_HORSE_SPAWNER);
            output.accept(ItemRegistry.SOULSAND_HORSE_SPAWNER);
            output.accept(ItemRegistry.CAKE_HORSE_SPAWNER);
            output.accept(ItemRegistry.NIGHTMARE_HORSE_SPAWNER);

            // Pegasus variants (share horse textures)
            output.accept(ItemRegistry.CLAY_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.DIRT_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.SAND_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.GRAVEL_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.SNOW_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.GRASS_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.LAPIS_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.CARROT_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.SOULSAND_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.CAKE_PEGASUS_SPAWNER);
            output.accept(ItemRegistry.NIGHTMARE_PEGASUS_SPAWNER);

            // Turtle variants
            output.accept(ItemRegistry.COBBLE_TURTLE_SPAWNER);
            output.accept(ItemRegistry.MOSSY_TURTLE_SPAWNER);
            output.accept(ItemRegistry.SANDSTONE_TURTLE_SPAWNER);
            output.accept(ItemRegistry.NETHERRACK_TURTLE_SPAWNER);
            output.accept(ItemRegistry.ENDSTONE_TURTLE_SPAWNER);
            output.accept(ItemRegistry.LAPIS_TURTLE_SPAWNER);
            output.accept(ItemRegistry.MELON_TURTLE_SPAWNER);
            output.accept(ItemRegistry.PUMPKIN_TURTLE_SPAWNER);
            output.accept(ItemRegistry.CAKE_TURTLE_SPAWNER);
            output.accept(ItemRegistry.KAWAKO_TURTLE_SPAWNER);

            // Bunny variants
            output.accept(ItemRegistry.WHITE_BUNNY_SPAWNER);
            output.accept(ItemRegistry.LIGHT_GRAY_BUNNY_SPAWNER);
            output.accept(ItemRegistry.GRAY_BUNNY_SPAWNER);
            output.accept(ItemRegistry.BLACK_BUNNY_SPAWNER);
            output.accept(ItemRegistry.BROWN_BUNNY_SPAWNER);
            output.accept(ItemRegistry.RED_BUNNY_SPAWNER);
            output.accept(ItemRegistry.ORANGE_BUNNY_SPAWNER);
            output.accept(ItemRegistry.YELLOW_BUNNY_SPAWNER);
            output.accept(ItemRegistry.LIME_BUNNY_SPAWNER);
            output.accept(ItemRegistry.GREEN_BUNNY_SPAWNER);
            output.accept(ItemRegistry.CYAN_BUNNY_SPAWNER);
            output.accept(ItemRegistry.LIGHT_BLUE_BUNNY_SPAWNER);
            output.accept(ItemRegistry.BLUE_BUNNY_SPAWNER);
            output.accept(ItemRegistry.PURPLE_BUNNY_SPAWNER);
            output.accept(ItemRegistry.MAGENTA_BUNNY_SPAWNER);
            output.accept(ItemRegistry.PINK_BUNNY_SPAWNER);

            // Gecko variants
            output.accept(ItemRegistry.OAK_GECKO_SPAWNER);
            output.accept(ItemRegistry.BIRCH_GECKO_SPAWNER);
            output.accept(ItemRegistry.JUNGLE_GECKO_SPAWNER);
            output.accept(ItemRegistry.ACACIA_GECKO_SPAWNER);
            output.accept(ItemRegistry.DARKOAK_GECKO_SPAWNER);
            output.accept(ItemRegistry.PINE_GECKO_SPAWNER);
        })
        .build();

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CLAY_LEGION_TAB_KEY, CLAY_LEGION_TAB);
    }

    private CreativeTabRegistry() {}
}
