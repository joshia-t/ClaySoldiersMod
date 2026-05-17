package io.github.joshiat.claylegion.entity.mount;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Base mount entity class for all 5 mount types (Horse, Pegasus, Turtle, Bunny, Gecko).
 *
 * Architectural constraints:
 *  - Extends Entity, NOT AbstractHorse or LivingEntity, to maintain O(1) performance.
 *  - Movement is "pulled" from the riding ClaySoldierEntity's AI, not generated independently.
 *  - Damage delegation uses chance-based resolution per the combat interaction matrix.
 *  - Mount death cleanly ejects the passenger before discard() to prevent null-reference errors.
 */
public abstract class BaseMountEntity extends Entity {

    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(BaseMountEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> MOUNT_TYPE =
            SynchedEntityData.defineId(BaseMountEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(BaseMountEntity.class, EntityDataSerializers.BYTE);

    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;

    public BaseMountEntity(EntityType<? extends BaseMountEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEALTH, getMaxHealth());
        builder.define(MOUNT_TYPE, getMountTypeId());
        builder.define(VARIANT, (byte) 0);
    }

    public float getHealth() {
        return entityData.get(HEALTH);
    }

    public void setHealth(float health) {
        entityData.set(HEALTH, Math.max(0f, health));
    }

    public float getMaxHealth() {
        return 20.0f;  // Default; override in subclasses for specialized mount resilience.
    }

    public boolean isMountDead() {
        return getHealth() <= 0f;
    }

    public byte getMountTypeId() {
        return 0;  // Override in subclasses
    }

    public byte getVariant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(byte variant) {
        entityData.set(VARIANT, variant);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    public int getMaxPassengers() {
        return 1;
    }

    public boolean canAcceptPassenger(Entity passenger) {
        return passenger instanceof ClaySoldierEntity && this.getPassengers().size() < getMaxPassengers();
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            return;
        }

        // Apply gravity and drag to match standard physics.
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0.0, -GRAVITY, 0.0));
        }

        setDeltaMovement(getDeltaMovement().multiply(DRAG, 1.0, DRAG));
        move(MoverType.SELF, getDeltaMovement());

        serverMountTick();
    }

    protected void serverMountTick() {
        // Pull movement vector from the riding soldier's AI state (if mounted).
        Entity passenger = getPassengers().isEmpty() ? null : getPassengers().get(0);
        if (passenger instanceof ClaySoldierEntity soldier) {
            // The mounted soldier's target-seeking logic drives the mount's velocity.
            // This avoids duplicating pathfinding logic and keeps the system hierarchically flat.
            Vec3 soldierVelocity = soldier.getDeltaMovement();
            applyMountMovement(soldierVelocity, soldier);
        } else {
            // No rider: drop to idle state (zero out horizontal velocity).
            Vec3 v = getDeltaMovement();
            setDeltaMovement(0.0, v.y, 0.0);
        }
    }

    /**
     * Overridden by subclasses to apply mount-specific movement logic.
     * This is where variant behavior (wall-climbing, 3D pathfinding, hopping, etc.) is injected.
     *
     * @param soldierVelocity The velocity vector from the riding soldier's AI.
     * @param soldier The riding soldier entity (used for targeting or state queries).
     */
    protected abstract void applyMountMovement(Vec3 soldierVelocity, ClaySoldierEntity soldier);

    /**
     * Called when the mount takes damage from a soldier attack.
     * Subclasses can override to apply variant-specific effects (e.g., Turtle's 0.5x damage reduction).
     */
    public void applyMountDamage(float amount, ClaySoldierEntity attacker) {
        if (level().isClientSide() || isMountDead() || amount <= 0.0f) {
            return;
        }

        float newHealth = getHealth() - amount;
        setHealth(newHealth);

        if (newHealth <= 0f && level() instanceof ServerLevel serverLevel) {
            onMountKilled(serverLevel);
        }
    }

    private void onMountKilled(ServerLevel serverLevel) {
        // Eject passenger before discard to prevent null-reference errors or entity fallthrough.
        if (!getPassengers().isEmpty()) {
            Entity passenger = getPassengers().get(0);
            passenger.stopRiding();
        }

        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ClaySoldierEntity soldier) {
            applyMountDamage(amount, soldier);
        }
        return !isMountDead();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setHealth(input.getFloatOr("Health", getMaxHealth()));
        setVariant(input.getByteOr("Variant", (byte) 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Health", getHealth());
        output.putByte("Variant", getVariant());
    }
}
