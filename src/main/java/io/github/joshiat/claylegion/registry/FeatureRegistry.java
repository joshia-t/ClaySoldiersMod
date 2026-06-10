package io.github.joshiat.claylegion.registry;

import io.github.joshiat.claylegion.worldgen.ClayHutFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class FeatureRegistry {

    public static final ClayHutFeature CLAY_HUT = new ClayHutFeature(NoneFeatureConfiguration.CODEC);

    public static final ResourceKey<PlacedFeature> CLAY_HUT_PLACED = ResourceKey.create(
        Registries.PLACED_FEATURE,
        Identifier.fromNamespaceAndPath("clay-legion", "clay_hut"));

    public static void init() {
        Registry.register(BuiltInRegistries.FEATURE,
            Identifier.fromNamespaceAndPath("clay-legion", "clay_hut"), CLAY_HUT);

        // Clay huts surface rarely in open, sunny biomes (issue #27).
        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(
                Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SAVANNA, Biomes.DESERT),
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            CLAY_HUT_PLACED);
    }

    private FeatureRegistry() {
    }
}
