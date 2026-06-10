package io.github.joshiat.claylegion.entity.projectile;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.RuntimeTelemetry;
import io.github.joshiat.claylegion.entity.TargetingProfiler;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Unified lightweight projectile entity for all ranged combat.
 *
 * Architectural constraints:
 *  - Extends Entity, NOT Projectile or AbstractArrow, to avoid heavy vanilla registry queries.
 *  - Uses simple parabolic arc physics in tick(): velocity += (0, -gravityAccel, 0)
 *  - Collision detection via raycast every tick: world.raycast(new RaycastContext(...))
 *  - Payload resolution: On EntityHitResult, verify target is a ClaySoldierEntity of opposing team.
 *  - Variants (Gravel, Snow, Fire Charge, Emerald) handled via enum/DataComponent, not separate classes.
 */
public abstract class ClayProjectileEntity extends Entity
    implements net.minecraft.world.entity.projectile.ItemSupplier {

    private static final EntityDataAccessor<Byte> PROJECTILE_TYPE =
            SynchedEntityData.defineId(ClayProjectileEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(ClayProjectileEntity.class, EntityDataSerializers.BYTE);

    private static final double GRAVITY = 0.05;  // Slightly less than entity gravity for flatter arc

    protected Entity shooter;
    // Piercing payload (issue #15): how many further targets this projectile
    // may pass through after a hit, and which entities were already struck.
    private int piercesRemaining;
    private final java.util.Set<Integer> hitEntityIds = new java.util.HashSet<>();

    public ClayProjectileEntity(EntityType<? extends ClayProjectileEntity> type, Level level) {
        super(type, level);
        this.piercesRemaining = getMaxPierces();
    }

    /** Number of extra targets the projectile passes through (0 = stops on first hit). */
    protected int getMaxPierces() {
        return 0;
    }

    /** Item rendered in flight by the vanilla ThrownItemRenderer (issue #16). */
    protected net.minecraft.world.item.Item getRenderItem() {
        return net.minecraft.world.item.Items.SNOWBALL;
    }

    private net.minecraft.world.item.ItemStack cachedRenderStack;

    @Override
    public net.minecraft.world.item.ItemStack getItem() {
        if (cachedRenderStack == null) {
            cachedRenderStack = new net.minecraft.world.item.ItemStack(getRenderItem());
        }
        return cachedRenderStack;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PROJECTILE_TYPE, getProjectileTypeId());
        builder.define(VARIANT, (byte) 0);
    }

    public byte getProjectileTypeId() {
        return 0;  // Override in subclasses
    }

    public byte getVariant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(byte variant) {
        entityData.set(VARIANT, variant);
    }

    public void setShooter(Entity shooter) {
        this.shooter = shooter;
    }

    public Entity getShooter() {
        return shooter;
    }

    public abstract float getDamage();

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            return;
        }

        // Apply simple parabolic physics
        Vec3 velocity = getDeltaMovement();
        velocity = velocity.add(0.0, -GRAVITY, 0.0);
        setDeltaMovement(velocity);

        // Move the projectile
        move(MoverType.SELF, getDeltaMovement());

        // Despawn on world contact instead of sliding along the ground forever.
        if (onGround() || horizontalCollision) {
            discard();
            return;
        }

        // Every tick, perform collision detection with nearby entities
        boolean projProfiling = TargetingProfiler.isEnabled();
        long projStart = projProfiling ? System.nanoTime() : 0L;
        performEntityCollisionCheck();
        if (projProfiling) {
            TargetingProfiler.recordCombatSample("projectileTickTime", "projectileTicks", System.nanoTime() - projStart, level().getGameTime());
        }
    }

    private void performEntityCollisionCheck() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Create a bounding box around current position for entity scanning
        net.minecraft.world.phys.AABB searchBox = getBoundingBox().inflate(1.5);
        var entities = serverLevel.getEntitiesOfClass(Entity.class, searchBox, e -> e != this && e != shooter);

        for (Entity entity : entities) {
            if (!(entity instanceof ClaySoldierEntity soldier) || hitEntityIds.contains(soldier.getId())) {
                continue;
            }
            // Verify the target is of an opposing team (non-soldier shooters hit anyone).
            if (shooter instanceof ClaySoldierEntity shooterSoldier
                && soldier.getTeamId() == shooterSoldier.getTeamId()) {
                continue;
            }

            RuntimeTelemetry.recordProjectileImpact();
            hitEntityIds.add(soldier.getId());
            onHitSoldier(soldier);

            // Piercing projectiles continue through the target (issue #15).
            if (piercesRemaining-- <= 0) {
                discard();
                return;
            }
        }
    }

    /** Push the target along the projectile's flight direction. */
    protected void applyImpactKnockback(ClaySoldierEntity target, double strength) {
        Vec3 velocity = getDeltaMovement();
        double lenSq = velocity.x * velocity.x + velocity.z * velocity.z;
        if (lenSq < 1.0E-6) {
            return;
        }
        double invLen = 1.0 / Math.sqrt(lenSq);
        double scale = strength * target.getKnockbackResistFactor();
        target.setDeltaMovement(target.getDeltaMovement().add(
            velocity.x * invLen * scale,
            0.1 * (scale > 0 ? 1.0 : 0.0),
            velocity.z * invLen * scale
        ));
    }

    protected void onHitSoldier(ClaySoldierEntity target) {
        // Default behavior: apply damage. Subclasses override for variant-specific effects.
        target.applySoldierDamage(getDamage(), (byte) (shooter instanceof ClaySoldierEntity s ? s.getTeamId() : -1),
            shooter, ClaySoldierEntity.SoldierDamageKind.RANGED);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        discard();
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setVariant(input.getByteOr("Variant", (byte) 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putByte("Variant", getVariant());
    }
}
