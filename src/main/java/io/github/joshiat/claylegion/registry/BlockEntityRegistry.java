package io.github.joshiat.claylegion.registry;

import io.github.joshiat.claylegion.block.entity.ClayNexusBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BlockEntityRegistry {

    private static final ResourceKey<BlockEntityType<?>> CLAY_NEXUS_KEY = ResourceKey.create(
        Registries.BLOCK_ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "clay_nexus")
    );

    public static final BlockEntityType<ClayNexusBlockEntity> CLAY_NEXUS = FabricBlockEntityTypeBuilder
        .create(ClayNexusBlockEntity::new, BlockRegistry.CLAY_NEXUS)
        .build();

    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CLAY_NEXUS_KEY, CLAY_NEXUS);
    }

    private BlockEntityRegistry() {
    }
}