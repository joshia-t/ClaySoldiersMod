package io.github.joshiat.claylegion.registry;

import io.github.joshiat.claylegion.block.ClayNexusBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class BlockRegistry {

    private static final ResourceKey<Block> CLAY_NEXUS_KEY = ResourceKey.create(
        Registries.BLOCK,
        Identifier.fromNamespaceAndPath("clay-legion", "clay_nexus")
    );

    public static final Block CLAY_NEXUS = new ClayNexusBlock(
        BlockBehaviour.Properties.of()
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            // The pedestal model is not a full cube; without this, neighbors
            // cull faces against it and x-ray holes appear (issue #30).
            .noOcclusion()
            .lightLevel(state -> 7)
            .setId(CLAY_NEXUS_KEY)
    );

    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK, CLAY_NEXUS_KEY, CLAY_NEXUS);
    }

    private BlockRegistry() {
    }
}