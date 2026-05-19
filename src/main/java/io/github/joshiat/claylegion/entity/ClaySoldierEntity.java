package io.github.joshiat.claylegion.entity;

import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import io.github.joshiat.claylegion.entity.projectile.ClayProjectileEntity;
import io.github.joshiat.claylegion.item.SoldierDollItem;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import io.github.joshiat.claylegion.registry.ItemRegistry;
import io.github.joshiat.claylegion.entity.team.SoldierTeam;
import io.github.joshiat.claylegion.entity.team.TeamRegistry;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeFlags;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeRegistry;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.Mth;

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
    private static final EntityDataAccessor<Byte> HURT_FLASH_TICKS =
            SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ATTACK_SWING_TICKS =
            SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> ACTIVE_UPGRADES =
            SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.LONG);

    public static final float MAX_HEALTH = 4.0f;
    private static final float ATTACK_DAMAGE = 1.0f;
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;

    private static final int TARGET_SCAN_INTERVAL = 12;
    private static final int MOUNT_SCAN_INTERVAL = 8;
    private static final double TARGET_RANGE_XZ = 4.0;
    private static final double TARGET_RANGE_Y = 1.8;
    private static final double TARGET_RANGE_SQ = 8.0 * 8.0;
    private static final double MOUNT_SEARCH_RANGE = 8.0;
    private static final double MOUNT_SEARCH_RANGE_SQ = MOUNT_SEARCH_RANGE * MOUNT_SEARCH_RANGE;
    private static final double MOUNT_BOARD_RANGE = 0.8;
    private static final double MOUNT_BOARD_RANGE_SQ = MOUNT_BOARD_RANGE * MOUNT_BOARD_RANGE;

    private static final double ATTACK_RANGE = 0.8;
    private static final double ATTACK_RANGE_SQ = ATTACK_RANGE * ATTACK_RANGE;
    private static final int ATTACK_COOLDOWN_TICKS = 10;
    private static final int RANGED_ATTACK_COOLDOWN_TICKS = 18;
    private static final double RANGED_ATTACK_RANGE = 6.0;
    private static final double RANGED_ATTACK_RANGE_SQ = RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE;
    private static final double PROJECTILE_SPEED = 0.55;
    private static final int ATTACK_SWING_DURATION = 7;
    private static final int HURT_FLASH_DURATION = 8;

    private static final double CHASE_ACCEL = 0.06;
    private static final double MAX_CHASE_SPEED = 0.18;
    private static final int JUMP_ASSIST_COOLDOWN_TICKS = 6;
    private static final int SEPARATION_UPDATE_INTERVAL = 4;
    // Sanity bounds on the auto-measured correction interval the glide is
    // spread over. The duration self-tunes to the real packet cadence; these
    // only cap a dropped/bunched packet so it eases to rest at the true
    // position instead of slow-motion drifting or snapping.
    private static final int MIN_INTERP_DURATION_TICKS = 1;
    private static final int MAX_INTERP_DURATION_TICKS = 6;

    private static final byte FLAG_BRICK = 0x02;
    private static final float SLOW_VELOCITY_SCALAR = 0.5f;
    private static final int SLOW_DURATION_TICKS = 40;
    private static final int COMBUSTION_DURATION_TICKS = 80;
    private static final int COMBUSTION_TICK_INTERVAL = 10;
    private static final float COMBUSTION_DAMAGE = 0.25f;
    private static final float EMERALD_RAW_DAMAGE_MULTIPLIER = 1.35f;

    private long activeUpgrades;
    private final UpgradeState upgradeState = new UpgradeState();

    private ClaySoldierEntity cachedTarget;
    private BaseMountEntity cachedMountTarget;
    private int attackCooldown;
    private int jumpAssistCooldown;
    private double obstructionBaseY;
    private int obstructionTicks;
    private int slowTicks;
    private int combustionTicks;
    private int combustionDamageCooldown;
    private Vec3 cachedSeparation = Vec3.ZERO;

    // Client interpolation: glide from the transform we had when the last
    // correction arrived ("from") to the server target ("to") at a constant
    // rate over `clientLerpDurationTicks`. The duration self-tunes (EMA) to
    // the measured packet-arrival interval so the glide bridges exactly
    // packet-to-packet regardless of the server's tracking cadence.
    private boolean clientHasCorrectionTarget;
    private double clientFromX, clientFromY, clientFromZ;
    private double clientToX, clientToY, clientToZ;
    private float clientFromYaw, clientFromPitch;
    private float clientToYaw, clientToPitch;
    private float clientLerpDurationTicks = 1.0f;
    private float clientLerpElapsedTicks;
    // Client ticks since the last correction packet; used to measure the
    // real arrival interval so the glide duration self-tunes to it.
    private int clientTicksSinceCorrection;

    public ClaySoldierEntity(EntityType<? extends ClaySoldierEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TEAM_ID, 0);
        builder.define(HEALTH, MAX_HEALTH);
        builder.define(ENTITY_FLAGS, (byte) 0);
        builder.define(AI_STATE, (byte) SoldierAiState.IDLE.id);
        builder.define(HURT_FLASH_TICKS, (byte) 0);
        builder.define(ATTACK_SWING_TICKS, (byte) 0);
        builder.define(ACTIVE_UPGRADES, 0L);
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

    /**
     * Apply damage to this soldier from a combat attack.
     * This is the legacy method used for projectiles and non-soldier damage sources.
     */
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
        setHurtFlashTicks(HURT_FLASH_DURATION);
        if (newHealth <= 0f && level() instanceof ServerLevel serverLevel) {
            onSoldierKilled(serverLevel);
        }
    }

    /**
     * Apply damage via the combat interaction matrix.
     * Handles mounted vs unmounted combat scenarios with chance-based target resolution.
     *
     * Combat Matrix:
     *  - Unmounted vs Unmounted: 100% damage to target
     *  - Unmounted vs Mounted: 80% to mount, 20% to rider
     *  - Mounted vs Unmounted: 100% damage to target + mount modifier
     *  - Mounted vs Mounted: 50/50 split between rider and mount
     */
    public void applyCombatDamage(float rawDamage, ClaySoldierEntity attacker) {
        applyCombatDamage(rawDamage, attacker, -1.0f, 1.0f);
    }

    public void applyCombatDamage(float rawDamage, ClaySoldierEntity attacker,
                                  float riderHitChanceOverride, float rawDamageMultiplier) {
        if (level().isClientSide() || isSoldierDead() || rawDamage <= 0.0f) {
            return;
        }

        // Friendly-fire suppression
        if (attacker != null && attacker.getTeamId() == getTeamId()) {
            return;
        }

        boolean attackerMounted = attacker != null && attacker.getVehicle() != null;
        boolean targetMounted = this.getVehicle() != null;
        float resolvedDamage = rawDamage * Math.max(0.0f, rawDamageMultiplier);

        if (!targetMounted) {
            // Target is infantry: take full damage
            processDirectDamage(resolvedDamage, attacker);
            return;
        }

        // Target is mounted: resolve via chance-based rider/mount roll.
        float roll = this.random.nextFloat();
        float riderHitChance = attackerMounted ? 0.50f : 0.20f;
        if (riderHitChanceOverride >= 0.0f) {
            riderHitChance = Mth.clamp(riderHitChanceOverride, 0.0f, 1.0f);
        }

        Entity mount = this.getVehicle();
        if (roll < riderHitChance) {
            // Damage bypasses mount and hits the rider directly.
            processDirectDamage(resolvedDamage, attacker);
        } else {
            // Delegate damage to the mount.
            if (mount instanceof io.github.joshiat.claylegion.entity.mount.BaseMountEntity legacyMount) {
                legacyMount.applyMountDamage(resolvedDamage, attacker);
            }
        }
    }

    /**
     * Process direct damage to the soldier (bypasses mount logic).
     */
    private void processDirectDamage(float amount, ClaySoldierEntity attacker) {
        float newHealth = getSoldierHealth() - amount;
        setSoldierHealth(newHealth);
        setHurtFlashTicks(HURT_FLASH_DURATION);
        if (newHealth <= 0f && level() instanceof ServerLevel serverLevel) {
            onSoldierKilled(serverLevel);
        }
    }

    public int getHurtFlashTicks() {
        return entityData.get(HURT_FLASH_TICKS);
    }

    private void setHurtFlashTicks(int ticks) {
        entityData.set(HURT_FLASH_TICKS, (byte) Math.max(0, Math.min(127, ticks)));
    }

    public int getAttackSwingTicks() {
        return entityData.get(ATTACK_SWING_TICKS);
    }

    private void setAttackSwingTicks(int ticks) {
        entityData.set(ATTACK_SWING_TICKS, (byte) Math.max(0, Math.min(127, ticks)));
    }

    public float getAttackSwingProgress(float partialTick) {
        float remaining = Math.max(0.0f, getAttackSwingTicks() - partialTick);
        return Mth.clamp((ATTACK_SWING_DURATION - remaining) / ATTACK_SWING_DURATION, 0.0f, 1.0f);
    }

    /**
     * Returns a client-only partial-tick yaw sampled from the active correction arc.
     *
     * The server remains authoritative; this is only for smoother rendering between
     * correction ticks so turn updates match the position interpolation quality.
     */
    public float getRenderYaw(float partialTick) {
        if (level().isClientSide() && isPassenger() && getVehicle() != null) {
            return getVehicle().getYRot();
        }

        if (!level().isClientSide() || !clientHasCorrectionTarget) {
            return getYRot();
        }

        float t = clientLerpFraction(partialTick);
        float yawDelta = Mth.wrapDegrees(clientToYaw - clientFromYaw);
        return clientFromYaw + yawDelta * t;
    }

    /**
     * Returns the client-only interpolated render position for partial-tick smoothing.
     */
    public Vec3 getRenderPosition(float partialTick) {
        if (level().isClientSide() && isPassenger() && getVehicle() != null) {
            Entity vehicle = getVehicle();
            return vehicle.getPosition(partialTick).add(0.0, vehicle.getBbHeight() * 0.55, 0.0);
        }

        if (!level().isClientSide() || !clientHasCorrectionTarget) {
            return position();
        }

        // Single render source, continuous in partialTick: a constant-rate
        // glide from->to. Vanilla's competing xo->getX lerp is neutralised
        // each tick in applyClientLinearCorrection (xo pinned to the corrected
        // position), so the renderer's (getRenderPosition - position) offset
        // carries all motion cleanly and smoothly at any framerate.
        float t = clientLerpFraction(partialTick);
        return new Vec3(
            Mth.lerp(t, clientFromX, clientToX),
            Mth.lerp(t, clientFromY, clientToY),
            Mth.lerp(t, clientFromZ, clientToZ)
        );
    }

    /**
     * Progress [0,1] of the active correction glide, continuous in
     * partialTick. Driven by elapsed client ticks vs the auto-tuned
     * duration, so it is immune to packet-arrival jitter: a late packet
     * simply eases to rest at t=1 (the true server position) until the
     * next correction restarts the glide.
     */
    private float clientLerpFraction(float partialTick) {
        float elapsed = clientLerpElapsedTicks + Mth.clamp(partialTick, 0.0f, 1.0f);
        return Mth.clamp(elapsed / clientLerpDurationTicks, 0.0f, 1.0f);
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

    public long getActiveUpgrades() {
        return activeUpgrades;
    }

    public boolean hasUpgrade(long flag) {
        return (activeUpgrades & flag) == flag;
    }

    private void setActiveUpgrades(long upgrades) {
        this.activeUpgrades = upgrades;
        this.upgradeState.setRaw(upgrades);
        this.entityData.set(ACTIVE_UPGRADES, upgrades);
    }

    public ClaySoldierEntity getCachedTarget() {
        return cachedTarget;
    }

    @Override
    public boolean isPickable() {
        // Required so players can ray-hit and attack this non-Living entity.
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
        ItemStack held = player.getItemInHand(hand);
        long upgradeBit = UpgradeRegistry.getBitFor(held.getItem());
        if (upgradeBit == 0L) {
            return InteractionResult.PASS;
        }

        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!hasUpgrade(upgradeBit)) {
            setActiveUpgrades(activeUpgrades | upgradeBit);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }

        return InteractionResult.CONSUME;
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

        if (level().isClientSide()) {
            // Client is presentation-only: follow server correction arcs and avoid
            // running parallel local physics that causes snap-back stutter.
            applyClientLinearCorrection();
            return;
        }

        // Passenger soldiers don't run independent movement physics; mounts drive transport.
        if (isPassenger()) {
            setDeltaMovement(Vec3.ZERO);
            if (getVehicle() != null) {
                float yaw = getVehicle().getYRot();
                setYRot(yaw);
                setYHeadRot(yaw);
                setYBodyRot(yaw);
            }
            serverCombatTick();
            return;
        }

        if (!onGround()) {
            if (isInWater()) {
                Vec3 buoyant = getDeltaMovement();
                double waterDepth = getFluidHeight(FluidTags.WATER);
                double depthError = 0.45 - waterDepth;
                double y = buoyant.y * 0.55 + Mth.clamp(depthError * 0.08, -0.015, 0.015);
                setDeltaMovement(buoyant.x * 0.94, y, buoyant.z * 0.94);
            } else {
                setDeltaMovement(getDeltaMovement().add(0.0, -GRAVITY, 0.0));
            }
        }

        setDeltaMovement(getDeltaMovement().multiply(DRAG, 1.0, DRAG));
        if (slowTicks > 0) {
            Vec3 slowed = getDeltaMovement();
            setDeltaMovement(slowed.x * SLOW_VELOCITY_SCALAR, slowed.y, slowed.z * SLOW_VELOCITY_SCALAR);
        }
        move(MoverType.SELF, getDeltaMovement());

        serverCombatTick();
    }

    @Override
    protected void lerpPositionAndRotationStep(int steps, double x, double y, double z, double yRot, double xRot) {
        if (!level().isClientSide()) {
            super.lerpPositionAndRotationStep(steps, x, y, z, yRot, xRot);
            return;
        }

        if (!clientHasCorrectionTarget) {
            // First snapshot: appear at the server transform, no glide.
            clientHasCorrectionTarget = true;
            clientFromX = clientToX = x;
            clientFromY = clientToY = y;
            clientFromZ = clientToZ = z;
            clientFromYaw = clientToYaw = (float) yRot;
            clientFromPitch = clientToPitch = (float) xRot;
            clientLerpDurationTicks = 1.0f;
            clientLerpElapsedTicks = 1.0f;
            clientTicksSinceCorrection = 0;
            return;
        }

        // Restart the glide from wherever we currently are (no jump) toward
        // the new server target. The duration self-tunes to the *measured*
        // arrival interval (smoothed), not the server's `steps` (which
        // overstates the real cadence here): matching duration to the true
        // interval is what makes the hand-off seamless. An EMA keeps a single
        // late/dropped packet from swinging the duration into the stutter
        // regime.
        clientFromX = getX();
        clientFromY = getY();
        clientFromZ = getZ();
        clientFromYaw = getYRot();
        clientFromPitch = getXRot();

        clientToX = x;
        clientToY = y;
        clientToZ = z;
        clientToYaw = (float) yRot;
        clientToPitch = (float) xRot;

        float measured = Mth.clamp(
            Math.max(1, clientTicksSinceCorrection),
            MIN_INTERP_DURATION_TICKS,
            MAX_INTERP_DURATION_TICKS);
        clientTicksSinceCorrection = 0;
        clientLerpDurationTicks = clientLerpDurationTicks * 0.6f + measured * 0.4f;
        clientLerpElapsedTicks = 0.0f;
    }

    private void applyClientLinearCorrection() {
        if (!clientHasCorrectionTarget) {
            return;
        }

        clientTicksSinceCorrection++;
        clientLerpElapsedTicks += 1.0f;
        float t = Mth.clamp(clientLerpElapsedTicks / clientLerpDurationTicks, 0.0f, 1.0f);

        setPos(
            Mth.lerp(t, clientFromX, clientToX),
            Mth.lerp(t, clientFromY, clientToY),
            Mth.lerp(t, clientFromZ, clientToZ)
        );
        float yawDelta = Mth.wrapDegrees(clientToYaw - clientFromYaw);
        setYRot(clientFromYaw + yawDelta * t);
        setXRot(Mth.lerp(t, clientFromPitch, clientToPitch));
        setYHeadRot(getYRot());
        setYBodyRot(getYRot());

        // Pin vanilla's previous-tick render baseline to the corrected
        // transform. Vanilla renders at lerp(partialTick, xo, getX()); with
        // xo == getX() that term is constant, so the buffer sampled in
        // getRenderPosition/getRenderYaw (continuous in partialTick) is the
        // single, framerate-smooth source of motion instead of two competing.
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.xOld = getX();
        this.yOld = getY();
        this.zOld = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();
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
        if (getHurtFlashTicks() > 0) {
            setHurtFlashTicks(getHurtFlashTicks() - 1);
        }
        if (getAttackSwingTicks() > 0) {
            setAttackSwingTicks(getAttackSwingTicks() - 1);
        }
        tickStatusEffects();

        if (((level().getGameTime() + getId()) % SEPARATION_UPDATE_INTERVAL) == 0) {
            cachedSeparation = sampleSeparationForce();
        }

        if (tryAcquireMount()) {
            return;
        }

        updateTargetCache();

        if (cachedTarget == null) {
            setAiState(SoldierAiState.IDLE);
            applyIdleBraking();
            resetObstructionTracking();
            return;
        }

        double distSq = distanceToSqr(cachedTarget);
        if (distSq > ATTACK_RANGE_SQ && distSq <= RANGED_ATTACK_RANGE_SQ && tryRangedAttack(cachedTarget)) {
            setAiState(SoldierAiState.ATTACKING);
            faceTarget(cachedTarget);
            return;
        }

        if (distSq <= ATTACK_RANGE_SQ) {
            setAiState(SoldierAiState.ATTACKING);

            Vec3 v = getDeltaMovement();
            setDeltaMovement(0.0, v.y, 0.0);
            faceTarget(cachedTarget);

            if (attackCooldown <= 0) {
                setAttackSwingTicks(ATTACK_SWING_DURATION);
                cachedTarget.applyCombatDamage(ATTACK_DAMAGE, this);
                attackCooldown = ATTACK_COOLDOWN_TICKS;
            }
        } else {
            setAiState(SoldierAiState.CHASING);
            chaseTarget(cachedTarget);
        }
    }

    private void tickStatusEffects() {
        if (slowTicks > 0) {
            slowTicks--;
        }

        if (combustionTicks > 0) {
            combustionTicks--;
            if (combustionDamageCooldown > 0) {
                combustionDamageCooldown--;
            }

            if (combustionDamageCooldown <= 0) {
                combustionDamageCooldown = COMBUSTION_TICK_INTERVAL;
                RuntimeTelemetry.recordCombustionDamageTick();
                applySoldierDamage(COMBUSTION_DAMAGE, (byte) -1);
            }
        } else {
            combustionDamageCooldown = 0;
        }
    }

    private boolean tryRangedAttack(ClaySoldierEntity target) {
        if (attackCooldown > 0) {
            return false;
        }

        ClayProjectileEntity projectile = createProjectileForUpgrades();
        if (projectile == null) {
            return false;
        }

        Vec3 origin = position().add(0.0, 0.22, 0.0);
        Vec3 direction = target.position().add(0.0, 0.16, 0.0).subtract(origin);
        double length = direction.length();
        if (length < 1.0E-6) {
            return false;
        }

        Vec3 velocity = direction.scale(PROJECTILE_SPEED / length);
        projectile.setPos(origin);
        projectile.setShooter(this);
        projectile.setDeltaMovement(velocity);
        level().addFreshEntity(projectile);
        setAttackSwingTicks(ATTACK_SWING_DURATION);
        attackCooldown = RANGED_ATTACK_COOLDOWN_TICKS;
        return true;
    }

    private ClayProjectileEntity createProjectileForUpgrades() {
        if (hasUpgrade(UpgradeFlags.EMERALD)) {
            return EntityRegistry.EMERALD_PROJECTILE.create(level(), EntitySpawnReason.SPAWN_ITEM_USE);
        }
        if (hasUpgrade(UpgradeFlags.FIRE_CHARGE)) {
            return EntityRegistry.FIRE_CHARGE_PROJECTILE.create(level(), EntitySpawnReason.SPAWN_ITEM_USE);
        }
        if (hasUpgrade(UpgradeFlags.SNOW)) {
            return EntityRegistry.SNOW_PROJECTILE.create(level(), EntitySpawnReason.SPAWN_ITEM_USE);
        }
        if (hasUpgrade(UpgradeFlags.GRAVEL) || hasUpgrade(UpgradeFlags.FLINT)) {
            return EntityRegistry.GRAVEL_PROJECTILE.create(level(), EntitySpawnReason.SPAWN_ITEM_USE);
        }
        return null;
    }

    public void applySnowPayload() {
        slowTicks = Math.max(slowTicks, SLOW_DURATION_TICKS);
        RuntimeTelemetry.recordSlowPayload();
    }

    public void applyFirePayload() {
        combustionTicks = Math.max(combustionTicks, COMBUSTION_DURATION_TICKS);
        RuntimeTelemetry.recordCombustionPayload();
    }

    public void applyEmeraldPayload(ClaySoldierEntity attacker, float damage) {
        applyCombatDamage(damage, attacker, 1.0f, EMERALD_RAW_DAMAGE_MULTIPLIER);
    }

    private boolean tryAcquireMount() {
        if (getVehicle() != null) {
            return false;
        }

        // Legacy MH_BONE behavior: soldiers with bone upgrade never target mounts.
        if (hasUpgrade(UpgradeFlags.BONE)) {
            return false;
        }

        updateMountTargetCache();
        if (cachedMountTarget == null) {
            return false;
        }

        double distSq = distanceToSqr(cachedMountTarget);
        if (distSq <= MOUNT_BOARD_RANGE_SQ) {
            if (cachedMountTarget.getMaxPassengers() > cachedMountTarget.getPassengers().size()) {
                startRiding(cachedMountTarget);
            }
            return true;
        }

        // Mount looting has higher priority than combat pursuit while unmounted.
        setAiState(SoldierAiState.CHASING);
        chaseMount(cachedMountTarget);
        return true;
    }

    private void chaseMount(BaseMountEntity mount) {
        Vec3 to = mount.position().subtract(position());
        Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
        double lenSq = horizontal.lengthSqr();
        if (lenSq < 1.0E-6) {
            return;
        }

        Vec3 dir = horizontal.scale(1.0 / Math.sqrt(lenSq));
        Vec3 velocity = getDeltaMovement()
            .add(dir.scale(CHASE_ACCEL))
            .add(cachedSeparation.scale(CombatTuning.getSeparationStrength()));

        double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        double maxSpeedSq = MAX_CHASE_SPEED * MAX_CHASE_SPEED;
        if (horizontalSpeedSq > maxSpeedSq) {
            double scale = MAX_CHASE_SPEED / Math.sqrt(horizontalSpeedSq);
            velocity = new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
        }

        setDeltaMovement(velocity);
        faceMovementOrMount(mount);
    }

    private void faceMovementOrMount(BaseMountEntity fallbackMount) {
        Vec3 velocity = getDeltaMovement();
        double speedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        if (speedSq > 1.0E-6) {
            float yaw = (float) (Math.atan2(velocity.z, velocity.x) * (180.0 / Math.PI)) - 90.0f;
            setYRot(yaw);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
        } else {
            double dx = fallbackMount.getX() - getX();
            double dz = fallbackMount.getZ() - getZ();
            float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            setYRot(yaw);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
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
        Vec3 velocity = getDeltaMovement()
            .add(dir.scale(CHASE_ACCEL))
            .add(cachedSeparation.scale(CombatTuning.getSeparationStrength()));

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
            Vec3 strafe = new Vec3(-dir.z * CombatTuning.getObstacleStrafeStrength() * dirSign, 0.0,
                    dir.x * CombatTuning.getObstacleStrafeStrength() * dirSign);
            setDeltaMovement(getDeltaMovement().add(strafe));
        } else if (onGround()) {
            resetObstructionTracking();
        }

        faceMovementOrTarget(target);
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

    private Vec3 sampleSeparationForce() {
        if (!CombatTuning.isSoldierCollisionEnabled()) {
            return Vec3.ZERO;
        }

        double radius = CombatTuning.getSeparationRadius();
        double radiusSq = radius * radius;
        AABB localBox = getBoundingBox().inflate(radius, 0.6, radius);
        List<ClaySoldierEntity> neighbors = level().getEntitiesOfClass(
                ClaySoldierEntity.class,
                localBox,
                e -> e != this && e.isAlive() && !e.isRemoved()
        );

        if (neighbors.isEmpty()) {
            return Vec3.ZERO;
        }

        Vec3 accum = Vec3.ZERO;
        int count = 0;
        for (ClaySoldierEntity other : neighbors) {
            Vec3 away = position().subtract(other.position());
            Vec3 horizontalAway = new Vec3(away.x, 0.0, away.z);
            double distSq = horizontalAway.lengthSqr();
            if (distSq < 1.0E-6 || distSq > radiusSq) {
                continue;
            }

            // Inverse distance weighting keeps close-body separation strong and far influence weak.
            accum = accum.add(horizontalAway.scale(1.0 / distSq));
            count++;
        }

        if (count == 0) {
            return Vec3.ZERO;
        }

        Vec3 avg = accum.scale(1.0 / count);
        double len = Math.sqrt(avg.x * avg.x + avg.z * avg.z);
        if (len < 1.0E-6) {
            return Vec3.ZERO;
        }

        return new Vec3(avg.x / len, 0.0, avg.z / len);
    }

    private void faceTarget(ClaySoldierEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
    }

    private void faceMovementOrTarget(ClaySoldierEntity fallbackTarget) {
        Vec3 velocity = getDeltaMovement();
        double speedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        if (speedSq > 1.0E-6) {
            float yaw = (float) (Math.atan2(velocity.z, velocity.x) * (180.0 / Math.PI)) - 90.0f;
            setYRot(yaw);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
        } else {
            faceTarget(fallbackTarget);
        }
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

    private void updateMountTargetCache() {
        if (isValidMountTarget(cachedMountTarget)) {
            return;
        }

        cachedMountTarget = null;
        if (!shouldScanForMount()) {
            return;
        }

        AABB scanBox = getBoundingBox().inflate(MOUNT_SEARCH_RANGE, 1.2, MOUNT_SEARCH_RANGE);
        List<BaseMountEntity> candidates = level().getEntitiesOfClass(
            BaseMountEntity.class,
            scanBox,
            this::isValidMountTarget
        );

        double bestDistSq = Double.MAX_VALUE;
        BaseMountEntity best = null;
        for (BaseMountEntity candidate : candidates) {
            double distSq = distanceToSqr(candidate);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }
        cachedMountTarget = best;
    }

    private boolean shouldScanForMount() {
        long gameTime = level().getGameTime();
        return ((gameTime + getId()) % MOUNT_SCAN_INTERVAL) == 0;
    }

    private boolean isValidMountTarget(BaseMountEntity candidate) {
        return candidate != null
            && candidate.isAlive()
            && !candidate.isRemoved()
            && candidate.getMaxPassengers() > candidate.getPassengers().size()
            && distanceToSqr(candidate) <= MOUNT_SEARCH_RANGE_SQ;
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
        long persisted = input.getLong("ActiveUpgrades").orElse(upgradeState.getRaw());
        setActiveUpgrades(persisted);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("TeamId", getTeamId());
        output.putFloat("Health", getSoldierHealth());
        output.putByte("EntityFlags", getFlags());
        output.putByte("AiState", getAiState().id);
        output.putLong("ActiveUpgrades", activeUpgrades);
        upgradeState.writeToStorage(output);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ACTIVE_UPGRADES.equals(key)) {
            this.activeUpgrades = entityData.get(ACTIVE_UPGRADES);
            this.upgradeState.setRaw(this.activeUpgrades);
        }
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
