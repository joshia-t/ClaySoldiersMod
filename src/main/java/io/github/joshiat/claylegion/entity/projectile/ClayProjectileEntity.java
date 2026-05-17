package io.github.joshiat.claylegion.entity.projectile;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
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
public abstract class ClayProjectileEntity extends Entity {

    private static final EntityDataAccessor<Byte> PROJECTILE_TYPE =
            SynchedEntityData.defineId(ClayProjectileEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(ClayProjectileEntity.class, EntityDataSerializers.BYTE);

    private static final double GRAVITY = 0.05;  // Slightly less than entity gravity for flatter arc

    protected Entity shooter;

    public ClayProjectileEntity(EntityType<? extends ClayProjectileEntity> type, Level level) {
        super(type, level);
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

        // Every tick, perform collision detection with nearby entities
        performEntityCollisionCheck();
    }

    private void performEntityCollisionCheck() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Create a bounding box around current position for entity scanning
        net.minecraft.world.phys.AABB searchBox = getBoundingBox().inflate(1.5);
        var entities = serverLevel.getEntitiesOfClass(Entity.class, searchBox, e -> e != this && e != shooter);

        for (Entity entity : entities) {
            if (entity instanceof ClaySoldierEntity soldier) {
                // Verify the target is of an opposing team
                if (shooter instanceof ClaySoldierEntity shooterSoldier) {
                    if (soldier.getTeamId() != shooterSoldier.getTeamId()) {
                        // Hit detected: apply payload
                        onHitSoldier(soldier);
                        discard();
                        return;
                    }
                } else {
                    // Non-soldier shooter; damage any soldier
                    onHitSoldier(soldier);
                    discard();
                    return;
                }
            }
        }
    }

    protected void onHitSoldier(ClaySoldierEntity target) {
        // Default behavior: apply damage. Subclasses override for variant-specific effects.
        target.applySoldierDamage(getDamage(), (byte) (shooter instanceof ClaySoldierEntity s ? s.getTeamId() : -1));
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
