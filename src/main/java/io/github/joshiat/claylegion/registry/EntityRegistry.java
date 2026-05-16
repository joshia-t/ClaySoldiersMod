package io.github.joshiat.claylegion.registry;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class EntityRegistry {

    private static final ResourceKey<EntityType<?>> CLAY_SOLDIER_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "clay_soldier")
    );

    public static final EntityType<ClaySoldierEntity> CLAY_SOLDIER = EntityType.Builder
        .<ClaySoldierEntity>of(ClaySoldierEntity::new, MobCategory.CREATURE)
        .sized(0.24f, 0.4f)
        .build(CLAY_SOLDIER_KEY);

    public static void init() {
    Registry.register(BuiltInRegistries.ENTITY_TYPE, CLAY_SOLDIER_KEY, CLAY_SOLDIER);
    }

    private EntityRegistry() {}
}
