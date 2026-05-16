package io.github.joshiat.claylegion.entity;

import io.github.joshiat.claylegion.item.SoldierDollItem;
import io.github.joshiat.claylegion.registry.ItemRegistry;
import io.github.joshiat.claylegion.entity.team.SoldierTeam;
import io.github.joshiat.claylegion.entity.team.TeamRegistry;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * Core clay soldier entity.
 *
 * Architectural constraints (see MANIFESTO.md):
 *  - Extends Entity, NOT LivingEntity, to avoid O(n) vanilla update bloat.
 *  - Target acquisition is staggered by entity id modulo to reduce O(n^2) pressure.
 *  - Upgrades stored as a compact long bitfield in UpgradeState.
 */
public class ClaySoldierEntity extends Entity {

    private static final EntityDataAccessor<Integer> TEAM_ID =
            SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> ENTITY_FLAGS =
        SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> AI_STATE =
            SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.BYTE);

    public static final float MAX_HEALTH = 4.0f;
    private static final float ATTACK_DAMAGE = 1.0f;
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;

    private static final int TARGET_SCAN_INTERVAL = 12;
    private static final double TARGET_RANGE_XZ = 4.0;
    private static final double TARGET_RANGE_Y = 1.0;
    private static final double TARGET_RANGE_SQ = 8.0 * 8.0;

    private static final double ATTACK_RANGE = 0.8;
    private static final double ATTACK_RANGE_SQ = ATTACK_RANGE * ATTACK_RANGE;
    private static final int ATTACK_COOLDOWN_TICKS = 10;

    private static final double CHASE_ACCEL = 0.06;
    private static final double MAX_CHASE_SPEED = 0.18;
    private static final int JUMP_ASSIST_COOLDOWN_TICKS = 6;

    private static final byte FLAG_BRICK = 0x02;

    private final UpgradeState upgradeState = new UpgradeState();

    private ClaySoldierEntity cachedTarget;
    private int attackCooldown;
    private int jumpAssistCooldown;
    private double obstructionBaseY;
    private int obstructionTicks;

    public ClaySoldierEntity(EntityType<? extends ClaySoldierEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TEAM_ID, 0);
        builder.define(HEALTH, MAX_HEALTH);
        builder.define(ENTITY_FLAGS, (byte) 0);
        builder.define(AI_STATE, (byte) SoldierAiState.IDLE.id);
    }

    public int getTeamId() {
        return entityData.get(TEAM_ID);
    }

    public void setTeamId(int teamId) {
        entityData.set(TEAM_ID, teamId);
    }

    public SoldierTeam getSoldierTeam() {
        return TeamRegistry.getById(getTeamId());
    }

    public float getSoldierHealth() {
        return entityData.get(HEALTH);
    }

    public void setSoldierHealth(float health) {
        entityData.set(HEALTH, Math.max(0f, health));
    }

    public boolean isSoldierDead() {
        return getSoldierHealth() <= 0f;
    }

    public void applySoldierDamage(float amount, byte attackingTeam) {
        if (level().isClientSide() || isSoldierDead() || amount <= 0.0f) {
            return;
        }

        // Friendly-fire suppression for direct soldier-vs-soldier damage calls.
        if (attackingTeam >= 0 && attackingTeam == (byte) getTeamId()) {
            return;
        }

        float newHealth = getSoldierHealth() - amount;
        setSoldierHealth(newHealth);
        if (newHealth <= 0f && level() instanceof ServerLevel serverLevel) {
            onSoldierKilled(serverLevel);
        }
    }

    private byte getFlags() {
        return entityData.get(ENTITY_FLAGS);
    }

    private void setFlags(byte flags) {
        entityData.set(ENTITY_FLAGS, flags);
    }

    public SoldierAiState getAiState() {
        return SoldierAiState.fromId(entityData.get(AI_STATE));
    }

    private void setAiState(SoldierAiState state) {
        entityData.set(AI_STATE, state.id);
    }

    public boolean isBrickSoldier() {
        return (getFlags() & FLAG_BRICK) != 0;
    }

    public void setBrickSoldier(boolean brick) {
        byte flags = getFlags();
        setFlags(brick ? (byte) (flags | FLAG_BRICK) : (byte) (flags & ~FLAG_BRICK));
    }

    public UpgradeState getUpgradeState() {
        return upgradeState;
    }

    @Override
    public boolean isPickable() {
        // Required so players can ray-hit and attack this non-Living entity.
        return true;
    }

    @Override
    public boolean isPushable() {
        return CombatTuning.isSoldierCollisionEnabled();
    }

    @Override
    public void push(Entity entity) {
        if (!CombatTuning.isSoldierCollisionEnabled()) {
            return;
        }
        super.push(entity);
    }

    @Override
    public void tick() {
        super.tick();

        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0.0, -GRAVITY, 0.0));
        }

        setDeltaMovement(getDeltaMovement().multiply(DRAG, 1.0, DRAG));
        move(MoverType.SELF, getDeltaMovement());

        if (!level().isClientSide()) {
            serverCombatTick();
        }
    }

    private void serverCombatTick() {
        if (isSoldierDead()) {
            return;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (jumpAssistCooldown > 0) {
            jumpAssistCooldown--;
        }

        updateTargetCache();

        if (cachedTarget == null) {
            setAiState(SoldierAiState.IDLE);
            applyIdleBraking();
            resetObstructionTracking();
            return;
        }

        double distSq = distanceToSqr(cachedTarget);
        if (distSq <= ATTACK_RANGE_SQ) {
            setAiState(SoldierAiState.ATTACKING);

            Vec3 v = getDeltaMovement();
            setDeltaMovement(0.0, v.y, 0.0);
            faceTarget(cachedTarget);

            if (attackCooldown <= 0) {
                cachedTarget.applySoldierDamage(ATTACK_DAMAGE, (byte) getTeamId());
                attackCooldown = ATTACK_COOLDOWN_TICKS;
            }
        } else {
            setAiState(SoldierAiState.CHASING);
            chaseTarget(cachedTarget);
        }
    }

    private void chaseTarget(ClaySoldierEntity target) {
        Vec3 to = target.position().subtract(position());
        Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
        double lenSq = horizontal.lengthSqr();
        if (lenSq < 1.0E-6) {
            return;
        }

        Vec3 dir = horizontal.scale(1.0 / Math.sqrt(lenSq));
        Vec3 velocity = getDeltaMovement().add(dir.scale(CHASE_ACCEL));

        double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        double maxSpeedSq = MAX_CHASE_SPEED * MAX_CHASE_SPEED;
        if (horizontalSpeedSq > maxSpeedSq) {
            double scale = MAX_CHASE_SPEED / Math.sqrt(horizontalSpeedSq);
            velocity = new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
        }

        setDeltaMovement(velocity);

        if (horizontalCollision && onGround()) {
            if (obstructionTicks == 0) {
                obstructionBaseY = getY();
            }
            obstructionTicks++;

            // Bound repeated hops so soldiers cannot scale tall cliffs.
            double climbedHeight = getY() - obstructionBaseY;
            if (jumpAssistCooldown <= 0 && climbedHeight < CombatTuning.getMaxObstacleClimbHeight()) {
                setDeltaMovement(getDeltaMovement().x, CombatTuning.getJumpAssistVelocity(), getDeltaMovement().z);
                jumpAssistCooldown = JUMP_ASSIST_COOLDOWN_TICKS;
            }

            // Lightweight obstacle avoidance: strafe around blocked faces to reduce wall-sticking.
            double dirSign = (((level().getGameTime() + getId()) / 10L) & 1L) == 0L ? 1.0 : -1.0;
            Vec3 strafe = new Vec3(-dir.z * 0.03 * dirSign, 0.0, dir.x * 0.03 * dirSign);
            setDeltaMovement(getDeltaMovement().add(strafe));
        } else if (onGround()) {
            resetObstructionTracking();
        }

        faceTarget(target);
    }

    private void applyIdleBraking() {
        Vec3 v = getDeltaMovement();
        Vec3 braked = new Vec3(v.x * CombatTuning.getIdleHorizontalBrake(), v.y, v.z * CombatTuning.getIdleHorizontalBrake());

        double horizontalSq = braked.x * braked.x + braked.z * braked.z;
        if (horizontalSq < CombatTuning.getIdleStopThresholdSq()) {
            setDeltaMovement(0.0, braked.y, 0.0);
        } else {
            setDeltaMovement(braked);
        }
    }

    private void resetObstructionTracking() {
        obstructionTicks = 0;
        obstructionBaseY = getY();
    }

    private void faceTarget(ClaySoldierEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
    }

    private void updateTargetCache() {
        if (isValidTarget(cachedTarget)) {
            return;
        }

        cachedTarget = null;
        if (!shouldScanForTarget()) {
            return;
        }

        AABB scanBox = getBoundingBox().inflate(TARGET_RANGE_XZ, TARGET_RANGE_Y, TARGET_RANGE_XZ);
        List<ClaySoldierEntity> candidates = level().getEntitiesOfClass(
                ClaySoldierEntity.class,
                scanBox,
                this::isValidTarget
        );

        double bestDistSq = Double.MAX_VALUE;
        ClaySoldierEntity best = null;
        for (ClaySoldierEntity candidate : candidates) {
            double distSq = distanceToSqr(candidate);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }
        cachedTarget = best;
    }

    private boolean shouldScanForTarget() {
        long gameTime = level().getGameTime();
        return ((gameTime + getId()) % TARGET_SCAN_INTERVAL) == 0;
    }

    private boolean isValidTarget(ClaySoldierEntity candidate) {
        return candidate != null
                && candidate != this
                && candidate.isAlive()
                && !candidate.isRemoved()
                && !candidate.isSoldierDead()
                && candidate.getTeamId() != getTeamId()
                && distanceToSqr(candidate) <= TARGET_RANGE_SQ;
    }

    private void onSoldierKilled(ServerLevel serverLevel) {
        ItemStack drop = new ItemStack(isBrickSoldier()
            ? ItemRegistry.BRICK_SOLDIER_DOLL
            : ItemRegistry.SOLDIER_DOLL);
        SoldierDollItem.setTeamIdOnStack(drop, getTeamId());
        spawnAtLocation(serverLevel, drop);
        discard();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setTeamId(input.getInt("TeamId").orElse(0));
        setSoldierHealth(input.getFloatOr("Health", MAX_HEALTH));
        setFlags(input.getByteOr("EntityFlags", (byte) 0));
        setAiState(SoldierAiState.fromId(input.getByteOr("AiState", (byte) 0)));
        upgradeState.readFromStorage(input);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("TeamId", getTeamId());
        output.putFloat("Health", getSoldierHealth());
        output.putByte("EntityFlags", getFlags());
        output.putByte("AiState", getAiState().id);
        upgradeState.writeToStorage(output);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        float resolvedAmount = amount;
        Entity attacker = source.getEntity();
        if (attacker instanceof Player) {
            resolvedAmount *= CombatTuning.getPlayerDamageMultiplier();
        }
        applySoldierDamage(resolvedAmount, (byte) -1);
        return !isSoldierDead();
    }

    public enum SoldierAiState {
        IDLE((byte) 0),
        CHASING((byte) 1),
        ATTACKING((byte) 2);

        private final byte id;

        SoldierAiState(byte id) {
            this.id = id;
        }

        private static SoldierAiState fromId(byte id) {
            for (SoldierAiState state : values()) {
                if (state.id == id) {
                    return state;
                }
            }
            return IDLE;
        }
    }
}
