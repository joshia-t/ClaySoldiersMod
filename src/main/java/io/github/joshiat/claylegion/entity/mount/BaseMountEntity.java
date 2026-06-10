package io.github.joshiat.claylegion.entity.mount;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.TargetingProfiler;
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
import net.minecraft.util.Mth;

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
    private static final double MOUNT_CHASE_SPEED = 0.24;
    private static final float MOUNT_CHASE_BLEND = 0.35f;
    private static final double MOUNT_JUMP_VELOCITY = 0.46;

    private boolean registeredInMountIndex = false;

    public BaseMountEntity(EntityType<? extends BaseMountEntity> type, Level level) {
        super(type, level);
        // Blocks cannot be placed inside mounts, matching vanilla mobs (issue #34).
        this.blocksBuilding = true;
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
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!getPassengers().contains(passenger)) {
            return;
        }

        Vec3 riderPos = getPassengerRidingPosition(passenger);
        moveFunction.accept(passenger, riderPos.x, riderPos.y, riderPos.z);
        float yaw = getYRot();
        passenger.setYRot(yaw);
        passenger.setYHeadRot(yaw);
        passenger.setYBodyRot(yaw);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            // Client-side local integration smooths mount visuals between server updates.
            if (!onGround()) {
                setDeltaMovement(getDeltaMovement().add(0.0, -GRAVITY, 0.0));
            }

            setDeltaMovement(getDeltaMovement().multiply(DRAG, 1.0, DRAG));
            move(MoverType.SELF, getDeltaMovement());
            return;
        }

        if (!registeredInMountIndex) {
            MountIndex.get(level()).register(this);
            registeredInMountIndex = true;
        } else {
            MountIndex.get(level()).refreshAvailability(this);
        }

        boolean profiling = TargetingProfiler.isEnabled();
        long mountStart = profiling ? System.nanoTime() : 0L;
        serverMountTick();
        if (profiling) {
            TargetingProfiler.recordCombatSample("mountTickTime", "mountTicks", System.nanoTime() - mountStart, level().getGameTime());
        }

        // Apply gravity and drag to match standard physics.
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0.0, -GRAVITY, 0.0));
        }

        setDeltaMovement(getDeltaMovement().multiply(DRAG, 1.0, DRAG));
        move(MoverType.SELF, getDeltaMovement());

        // Vanilla only runs inside-block effects (lava, fire blocks) for
        // LivingEntity; plain entities must opt in (issue #38).
        applyEffectsFromBlocks();
    }

    protected void serverMountTick() {
        // Pull movement vector from the riding soldier's AI state (if mounted).
        Entity passenger = getPassengers().isEmpty() ? null : getPassengers().get(0);
        if (passenger instanceof ClaySoldierEntity soldier) {
            Vec3 current = getDeltaMovement();
            Vec3 desiredHorizontal = Vec3.ZERO;
            ClaySoldierEntity target = soldier.getCachedTarget();
            if (target != null && soldier.getAiState() != ClaySoldierEntity.SoldierAiState.ATTACKING) {
                Vec3 to = target.position().subtract(position());
                Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
                double lenSq = horizontal.lengthSqr();
                if (lenSq > 1.0E-6) {
                    double inv = 1.0 / Math.sqrt(lenSq);
                    desiredHorizontal = horizontal.scale(inv * MOUNT_CHASE_SPEED);
                }
            }

            Vec3 blended = new Vec3(
                Mth.lerp(MOUNT_CHASE_BLEND, current.x, desiredHorizontal.x),
                current.y,
                Mth.lerp(MOUNT_CHASE_BLEND, current.z, desiredHorizontal.z)
            );

            if (soldier.getAiState() == ClaySoldierEntity.SoldierAiState.ATTACKING) {
                blended = new Vec3(blended.x * 0.25, blended.y, blended.z * 0.25);
            }

            if (horizontalCollision && onGround()) {
                blended = new Vec3(blended.x, Math.max(blended.y, MOUNT_JUMP_VELOCITY), blended.z);
            }

            applyMountMovement(blended, soldier);

            Vec3 motion = getDeltaMovement();
            double speedSq = motion.x * motion.x + motion.z * motion.z;
            if (speedSq > 1.0E-6) {
                float yaw = (float) (Math.atan2(motion.z, motion.x) * (180.0 / Math.PI)) - 90.0f;
                setYRot(yaw);
                setYHeadRot(yaw);
                setYBodyRot(yaw);
            }
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
        ClaySoldierEntity soldierAttacker = attacker instanceof ClaySoldierEntity s ? s : null;
        applyMountDamage(amount, soldierAttacker);
        return !isMountDead();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (registeredInMountIndex && !level().isClientSide()) {
            MountIndex.get(level()).unregister(this);
            registeredInMountIndex = false;
        }
        super.remove(reason);
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
