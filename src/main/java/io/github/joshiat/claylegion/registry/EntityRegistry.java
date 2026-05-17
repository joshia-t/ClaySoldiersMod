package io.github.joshiat.claylegion.registry;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.mount.HorseMountEntity;
import io.github.joshiat.claylegion.entity.mount.PegasusMountEntity;
import io.github.joshiat.claylegion.entity.mount.TurtleMountEntity;
import io.github.joshiat.claylegion.entity.mount.BunnyMountEntity;
import io.github.joshiat.claylegion.entity.mount.GeckoMountEntity;
import io.github.joshiat.claylegion.entity.projectile.GravelProjectileEntity;
import io.github.joshiat.claylegion.entity.projectile.SnowProjectileEntity;
import io.github.joshiat.claylegion.entity.projectile.FireChargeProjectileEntity;
import io.github.joshiat.claylegion.entity.projectile.EmeraldProjectileEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class EntityRegistry {

    // Soldier
    private static final ResourceKey<EntityType<?>> CLAY_SOLDIER_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "clay_soldier")
    );

    public static final EntityType<ClaySoldierEntity> CLAY_SOLDIER = EntityType.Builder
        .<ClaySoldierEntity>of(ClaySoldierEntity::new, MobCategory.CREATURE)
        .sized(0.34f, 0.4f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(CLAY_SOLDIER_KEY);

    // Mounts
    private static final ResourceKey<EntityType<?>> HORSE_MOUNT_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "horse_mount")
    );

    public static final EntityType<HorseMountEntity> HORSE_MOUNT = EntityType.Builder
        .<HorseMountEntity>of(HorseMountEntity::new, MobCategory.CREATURE)
        .sized(0.55f, 0.6f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(HORSE_MOUNT_KEY);

    private static final ResourceKey<EntityType<?>> PEGASUS_MOUNT_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "pegasus_mount")
    );

    public static final EntityType<PegasusMountEntity> PEGASUS_MOUNT = EntityType.Builder
        .<PegasusMountEntity>of(PegasusMountEntity::new, MobCategory.CREATURE)
        .sized(0.55f, 0.7f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(PEGASUS_MOUNT_KEY);

    private static final ResourceKey<EntityType<?>> TURTLE_MOUNT_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "turtle_mount")
    );

    public static final EntityType<TurtleMountEntity> TURTLE_MOUNT = EntityType.Builder
        .<TurtleMountEntity>of(TurtleMountEntity::new, MobCategory.CREATURE)
        .sized(0.62f, 0.32f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(TURTLE_MOUNT_KEY);

    private static final ResourceKey<EntityType<?>> BUNNY_MOUNT_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "bunny_mount")
    );

    public static final EntityType<BunnyMountEntity> BUNNY_MOUNT = EntityType.Builder
        .<BunnyMountEntity>of(BunnyMountEntity::new, MobCategory.CREATURE)
        .sized(0.42f, 0.42f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(BUNNY_MOUNT_KEY);

    private static final ResourceKey<EntityType<?>> GECKO_MOUNT_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "gecko_mount")
    );

    public static final EntityType<GeckoMountEntity> GECKO_MOUNT = EntityType.Builder
        .<GeckoMountEntity>of(GeckoMountEntity::new, MobCategory.CREATURE)
        .sized(0.45f, 0.26f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(GECKO_MOUNT_KEY);

    // Projectiles
    private static final ResourceKey<EntityType<?>> GRAVEL_PROJECTILE_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "gravel_projectile")
    );

    public static final EntityType<GravelProjectileEntity> GRAVEL_PROJECTILE = EntityType.Builder
        .<GravelProjectileEntity>of(GravelProjectileEntity::new, MobCategory.MISC)
        .sized(0.25f, 0.25f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(GRAVEL_PROJECTILE_KEY);

    private static final ResourceKey<EntityType<?>> SNOW_PROJECTILE_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "snow_projectile")
    );

    public static final EntityType<SnowProjectileEntity> SNOW_PROJECTILE = EntityType.Builder
        .<SnowProjectileEntity>of(SnowProjectileEntity::new, MobCategory.MISC)
        .sized(0.25f, 0.25f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(SNOW_PROJECTILE_KEY);

    private static final ResourceKey<EntityType<?>> FIRE_CHARGE_PROJECTILE_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "fire_charge_projectile")
    );

    public static final EntityType<FireChargeProjectileEntity> FIRE_CHARGE_PROJECTILE = EntityType.Builder
        .<FireChargeProjectileEntity>of(FireChargeProjectileEntity::new, MobCategory.MISC)
        .sized(0.25f, 0.25f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(FIRE_CHARGE_PROJECTILE_KEY);

    private static final ResourceKey<EntityType<?>> EMERALD_PROJECTILE_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath("clay-legion", "emerald_projectile")
    );

    public static final EntityType<EmeraldProjectileEntity> EMERALD_PROJECTILE = EntityType.Builder
        .<EmeraldProjectileEntity>of(EmeraldProjectileEntity::new, MobCategory.MISC)
        .sized(0.25f, 0.25f)
        .clientTrackingRange(10)
        .updateInterval(1)
        .build(EMERALD_PROJECTILE_KEY);

    public static void init() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, CLAY_SOLDIER_KEY, CLAY_SOLDIER);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, HORSE_MOUNT_KEY, HORSE_MOUNT);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, PEGASUS_MOUNT_KEY, PEGASUS_MOUNT);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, TURTLE_MOUNT_KEY, TURTLE_MOUNT);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, BUNNY_MOUNT_KEY, BUNNY_MOUNT);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GECKO_MOUNT_KEY, GECKO_MOUNT);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GRAVEL_PROJECTILE_KEY, GRAVEL_PROJECTILE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, SNOW_PROJECTILE_KEY, SNOW_PROJECTILE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, FIRE_CHARGE_PROJECTILE_KEY, FIRE_CHARGE_PROJECTILE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, EMERALD_PROJECTILE_KEY, EMERALD_PROJECTILE);
    }

    private EntityRegistry() {}
}
