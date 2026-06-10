package io.github.joshiat.claylegion.entity;

import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import io.github.joshiat.claylegion.entity.possession.SoldierPossessionManager;
import io.github.joshiat.claylegion.entity.projectile.ClayProjectileEntity;
import io.github.joshiat.claylegion.item.SoldierDollItem;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import io.github.joshiat.claylegion.registry.ItemRegistry;
import io.github.joshiat.claylegion.entity.drop.DropStackMetadata;
import io.github.joshiat.claylegion.entity.team.SoldierTeam;
import io.github.joshiat.claylegion.entity.team.TeamRegistry;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeFlags;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeRegistry;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeSlot;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeSpec;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.UUID;

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

    public static final float MAX_HEALTH = 20.0f;
    private static final float ATTACK_DAMAGE = 1.0f;
    // How many times a soldier can be revived from its doll before the doll is
    // spent — prevents infinite battlefield resurrection loops (issue #8).
    public static final int DEFAULT_RESURRECTION_BUDGET = 3;

    // Issue #18 upgrade tuning constants.
    private static final float LOW_HEALTH_FRACTION = 0.25f;
    private static final float MELON_TARGET_HEAL = 15.0f;
    private static final float BROWN_MUSHROOM_HEAL = 10.0f;
    private static final int BURN_TICKS_BLAZE_ROD = 60;
    private static final int BURN_TICKS_BLAZE_ROD_COAL = 120;
    private static final int POISON_DURATION_TICKS = 100;
    private static final float POISON_DAMAGE = 0.5f;
    private static final int POISON_DAMAGE_INTERVAL = 20;
    private static final int BLIND_DURATION_TICKS = 60;
    private static final int ROOT_DURATION_TICKS = 60;
    private static final int MAGMA_BOMB_FUSE_TICKS = 40;
    private static final float MAGMA_BOMB_DAMAGE = 1000.0f;
    private static final int ZOMBIE_DECAY_TICKS = 12000;
    private static final int QUARTZ_HIT_WINDOW_TICKS = 40;
    private static final int QUARTZ_HITS_TO_TRIGGER = 5;
    private static final double QUARTZ_SHOCKWAVE_RADIUS = 2.5;
    private static final double QUARTZ_SHOCKWAVE_STRENGTH = 0.6;
    private static final double FEATHER_MAX_FALL_SPEED = -0.08;
    private static final double FALL_DAMAGE_THRESHOLD = 3.0;
    private static final double KING_FOLLOW_STOP_RANGE_SQ = 2.5 * 2.5;
    private static final int KING_SCAN_INTERVAL = 20;
    private static final int MOB_HUNT_SCAN_INTERVAL = 16;
    private static final double MOB_HUNT_RANGE = 10.0;
    private static final int PRISMARINE_PARTICLE_INTERVAL = 10;
    private static final float ESCAPE_ROCKET_HEALTH_FRACTION = 0.5f;
    private static final double ESCAPE_ROCKET_BOOST = 1.1;
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;

    private static final double MOUNT_BOARD_RANGE = 0.8;
    private static final double MOUNT_BOARD_RANGE_SQ = MOUNT_BOARD_RANGE * MOUNT_BOARD_RANGE;

    private static final double ATTACK_RANGE = 0.8;
    private static final double ATTACK_RANGE_SQ = ATTACK_RANGE * ATTACK_RANGE;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private static final int RANGED_ATTACK_COOLDOWN_TICKS = 18;
    private static final double RANGED_ATTACK_RANGE = 6.0;
    private static final double RANGED_ATTACK_RANGE_SQ = RANGED_ATTACK_RANGE * RANGED_ATTACK_RANGE;
    private static final double RETALIATE_AGGRO_RANGE_SQ = RANGED_ATTACK_RANGE_SQ;
    private static final double PROJECTILE_SPEED = 0.55;
    private static final int ATTACK_SWING_DURATION = 7;
    private static final int HURT_FLASH_DURATION = 8;
    private static final int TARGET_MEMORY_DURATION_TICKS = 90;
    private static final int TARGET_LOS_FRESHNESS_TICKS = 6;
    private static final int ACTIVE_LOS_CHECK_INTERVAL = 4;
    private static final int IDLE_LOS_CHECK_INTERVAL = 10;
    private static final int SHOUT_BROADCAST_INTERVAL = 12;
    private static final float GOSSIP_MIN_CHASE_STRENGTH = 0.18f;
    private static final int IMMEDIATE_THREAT_CHECK_INTERVAL = 2;
    private static final double IMMEDIATE_THREAT_RANGE = 2.0;
    private static final double GOSSIP_MAX_RANGE_BLOCKS = 16.0 * 10.0;
    private static final double TARGET_MEMORY_REACHED_SQ = 1.2 * 1.2;

    private static final double CHASE_ACCEL = 0.05;
    private static final double MAX_CHASE_SPEED = 0.1;
    private static final int JUMP_ASSIST_COOLDOWN_TICKS = 6;
    private static final int SEPARATION_UPDATE_INTERVAL = 4;
    private static final int UPGRADE_PICKUP_UPDATE_INTERVAL = 10;
    private static final double UPGRADE_PICKUP_RANGE = 0.55;

    private static final byte FLAG_BRICK = 0x02;
    private static final byte FLAG_NEXUS_SUMMON = 0x04;
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
    private boolean registeredInSoldierIndex = false;
    private boolean registeredInNexusIndex = false;
    private int attackCooldown;
    private int jumpAssistCooldown;
    private double obstructionBaseY;
    private int obstructionTicks;
    private int slowTicks;
    private int combustionTicks;
    private int combustionDamageCooldown;

    // Per-upgrade remaining uses, indexed by upgrade bit (0 = unlimited or absent).
    private final short[] upgradeUses = new short[64];
    private int poisonTicks;
    private int poisonDamageCooldown;
    private int blindTicks;
    private int rootTicks;
    private int magmaBombTicks = -1;
    private int zombieDecayTicks = -1;
    private byte foodHealNutrition = 4;
    private int lastShearTargetId = -1;
    private long quartzWindowStartTick = Long.MIN_VALUE;
    private int quartzWindowHits;
    private double airborneStartY = Double.NaN;
    private Monster cachedMobTarget;
    private ClaySoldierEntity cachedKing;
    // True while a player remote-controls this soldier; AI is suspended. Transient.
    private boolean possessed;
    // Remaining doll revivals this soldier carries into its next death (issue #8).
    private int resurrectionUsesRemaining = DEFAULT_RESURRECTION_BUDGET;
    private double targetMemoryX;
    private double targetMemoryY;
    private double targetMemoryZ;
    private float targetMemoryConfidence;
    private long targetMemoryExpiryTick = Long.MIN_VALUE;
    private long lastDirectSightTick = Long.MIN_VALUE;
    private long lastShoutTick = Long.MIN_VALUE;
    private Vec3 cachedSeparation = Vec3.ZERO;
    private UUID nexusOriginId;

    // Client interpolation (issue #28): position-sync packets only interpolate
    // for entities exposing an InterpolationHandler via getInterpolation();
    // the default Entity behavior teleports. We own a vanilla handler and tick
    // it in the client tick, then sample xo/x with partialTick for rendering.
    private final InterpolationHandler interpolation = new InterpolationHandler(this);

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
        SoldierCombatDamageHelper.applySoldierDamage(this, amount, attackingTeam, null);
    }

    public void applySoldierDamage(float amount, byte attackingTeam, Entity attackerEntity) {
        SoldierCombatDamageHelper.applySoldierDamage(this, amount, attackingTeam, attackerEntity);
    }

    public void applySoldierDamage(float amount, byte attackingTeam, Entity attackerEntity, SoldierDamageKind kind) {
        SoldierCombatDamageHelper.applySoldierDamage(this, amount, attackingTeam, attackerEntity, kind);
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
        SoldierCombatDamageHelper.applyCombatDamage(this, rawDamage, attacker);
    }

    public void applyCombatDamage(float rawDamage, ClaySoldierEntity attacker,
                                  float riderHitChanceOverride, float rawDamageMultiplier) {
        SoldierCombatDamageHelper.applyCombatDamage(this, rawDamage, attacker, riderHitChanceOverride, rawDamageMultiplier);
    }

    public int getHurtFlashTicks() {
        return entityData.get(HURT_FLASH_TICKS);
    }

    public void setHurtFlashTicks(int ticks) {
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
     * Client-only partial-tick yaw. The InterpolationHandler updates yRot once
     * per client tick; sampling yRotO→yRot with partialTick keeps turning as
     * smooth as the position glide at any framerate.
     */
    public float getRenderYaw(float partialTick) {
        if (level().isClientSide() && isPassenger() && getVehicle() != null) {
            return getVehicle().getYRot();
        }
        return Mth.rotLerp(partialTick, this.yRotO, getYRot());
    }

    /**
     * Client-only interpolated render position for partial-tick smoothing.
     * The InterpolationHandler moves the entity fractionally each client tick
     * (so xo→x is one interpolation step); lerping by partialTick yields the
     * standard vanilla-smooth glide.
     */
    public Vec3 getRenderPosition(float partialTick) {
        if (level().isClientSide() && isPassenger() && getVehicle() != null) {
            Entity vehicle = getVehicle();
            return vehicle.getPosition(partialTick).add(0.0, vehicle.getBbHeight() * 0.55, 0.0);
        }
        return new Vec3(
            Mth.lerp(partialTick, this.xo, getX()),
            Mth.lerp(partialTick, this.yo, getY()),
            Mth.lerp(partialTick, this.zo, getZ())
        );
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
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

    public boolean isNexusSummon() {
        return (getFlags() & FLAG_NEXUS_SUMMON) != 0;
    }

    public void setNexusSummon(boolean summoned) {
        byte flags = getFlags();
        setFlags(summoned ? (byte) (flags | FLAG_NEXUS_SUMMON) : (byte) (flags & ~FLAG_NEXUS_SUMMON));
    }

    public UUID getNexusOriginId() {
        return nexusOriginId;
    }

    public void setNexusOriginId(UUID nexusOriginId) {
        this.nexusOriginId = nexusOriginId;
    }

    public UpgradeState getUpgradeState() {
        return upgradeState;
    }

    public long getActiveUpgrades() {
        return activeUpgrades;
    }

    public int getStickUsesRemaining() {
        return getUpgradeUses(UpgradeFlags.STICK);
    }

    /** Remaining uses for a finite-durability upgrade (0 if absent or unlimited). */
    public int getUpgradeUses(long flag) {
        return hasUpgrade(flag) ? Short.toUnsignedInt(upgradeUses[Long.numberOfTrailingZeros(flag)]) : 0;
    }

    private void setUpgradeUses(long flag, int uses) {
        upgradeUses[Long.numberOfTrailingZeros(flag)] = (short) Mth.clamp(uses, 0, Short.MAX_VALUE);
    }

    /**
     * Consume one use of a finite upgrade; breaks it (with dependent
     * enhancements) when durability runs out. No-op for unlimited upgrades.
     */
    private void consumeUpgradeUse(long flag) {
        UpgradeSpec spec = UpgradeRegistry.getSpec(flag);
        if (spec == null || !spec.hasFiniteUses() || !hasUpgrade(flag)) {
            return;
        }

        int idx = Long.numberOfTrailingZeros(flag);
        if (upgradeUses[idx] > 0) {
            upgradeUses[idx]--;
        }
        if (upgradeUses[idx] <= 0) {
            breakUpgrade(flag);
        }
    }

    /** Removes an upgrade and cascades removal to upgrades whose prerequisites broke. */
    private void breakUpgrade(long flag) {
        long next = activeUpgrades & ~flag;
        setUpgradeUses(flag, 0);

        // Enhancements break when their base upgrade breaks.
        boolean changed = true;
        while (changed) {
            changed = false;
            long remaining = next;
            while (remaining != 0L) {
                long bit = Long.lowestOneBit(remaining);
                remaining &= ~bit;
                UpgradeSpec spec = UpgradeRegistry.getSpec(bit);
                if (spec == null) {
                    continue;
                }
                boolean requirementsHeld = (next & spec.requiresAll()) == spec.requiresAll()
                    && (spec.requiresAny() == 0L || (next & spec.requiresAny()) != 0L);
                if (!requirementsHeld) {
                    next &= ~bit;
                    setUpgradeUses(bit, 0);
                    changed = true;
                }
            }
        }

        setActiveUpgrades(next);
    }

    public float getTargetMemoryConfidence() {
        return targetMemoryConfidence;
    }

    /** Human-readable upgrade list with remaining durability, for debug commands. */
    public String describeUpgrades() {
        if (activeUpgrades == 0L) {
            return "none";
        }

        StringBuilder sb = new StringBuilder();
        long remaining = activeUpgrades;
        while (remaining != 0L) {
            long bit = Long.lowestOneBit(remaining);
            remaining &= ~bit;
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(UpgradeFlags.nameOf(bit));
            UpgradeSpec spec = UpgradeRegistry.getSpec(bit);
            if (spec != null && spec.hasFiniteUses()) {
                sb.append('(').append(getUpgradeUses(bit)).append(')');
            }
        }
        return sb.toString();
    }

    public boolean hasUpgrade(long flag) {
        return (activeUpgrades & flag) == flag;
    }

    /** True while a player is remote-controlling this soldier. */
    public boolean isPossessed() {
        return possessed;
    }

    public void setPossessed(boolean possessed) {
        this.possessed = possessed;
        if (possessed) {
            cachedTarget = null;
            cachedMobTarget = null;
            cachedMountTarget = null;
            clearTargetMemory();
            setAiState(SoldierAiState.IDLE);
        }
    }

    /**
     * Called when the possessing player left-clicks: melee the nearest enemy in
     * reach, otherwise fire the ranged weapon at the nearest visible enemy.
     * Respects the normal attack cooldown so durability/effects behave exactly
     * like AI combat.
     */
    public void possessionTriggerAttack() {
        if (isSoldierDead() || attackCooldown > 0) {
            return;
        }

        ClaySoldierEntity meleeTarget = findNearestEnemyWithin(ATTACK_RANGE);
        if (meleeTarget != null) {
            setAiState(SoldierAiState.ATTACKING);
            faceTarget(meleeTarget);
            performMeleeAttack(meleeTarget);
            return;
        }

        ClaySoldierEntity rangedTarget = findNearestEnemyWithin(RANGED_ATTACK_RANGE);
        if (rangedTarget != null && SoldierTargetingHelper.hasLineOfSight(this, rangedTarget)) {
            faceTarget(rangedTarget);
            if (tryRangedAttack(rangedTarget)) {
                setAiState(SoldierAiState.ATTACKING);
            }
            return;
        }

        // Swing at air so the player gets feedback even on a miss.
        setAttackSwingTicks(ATTACK_SWING_DURATION);
    }

    private ClaySoldierEntity findNearestEnemyWithin(double range) {
        List<ClaySoldierEntity> candidates = level().getEntitiesOfClass(
            ClaySoldierEntity.class,
            getBoundingBox().inflate(range, 1.0, range),
            candidate -> candidate != this
                && candidate.isAlive()
                && !candidate.isRemoved()
                && !candidate.isSoldierDead()
                && candidate.getTeamId() != getTeamId()
        );

        ClaySoldierEntity best = null;
        double bestSq = range * range;
        for (int i = 0, size = candidates.size(); i < size; i++) {
            ClaySoldierEntity candidate = candidates.get(i);
            double distSq = distanceToSqr(candidate);
            if (distSq <= bestSq) {
                best = candidate;
                bestSq = distSq;
            }
        }
        return best;
    }

    /**
     * Reduced server tick while possessed: status effects, pickups, and
     * adjacent mount boarding stay active, but all autonomous AI is suspended —
     * the player's inputs (relayed by SoldierPossessionManager) drive movement.
     */
    private void possessedCombatTick(long gameTime) {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (getHurtFlashTicks() > 0) {
            setHurtFlashTicks(getHurtFlashTicks() - 1);
        }
        if (getAttackSwingTicks() > 0) {
            setAttackSwingTicks(getAttackSwingTicks() - 1);
        }

        tickStatusEffects();
        tryPickupNearbyUpgrade(gameTime);

        // Walking into a mount with a free seat boards it.
        if (!isPassenger() && !hasUpgrade(UpgradeFlags.BONE)) {
            cachedMountTarget = SoldierTargetingHelper.updateMountTargetCache(this, cachedMountTarget);
            if (cachedMountTarget != null
                && distanceToSqr(cachedMountTarget) <= MOUNT_BOARD_RANGE_SQ
                && cachedMountTarget.getMaxPassengers() > cachedMountTarget.getPassengers().size()) {
                startRiding(cachedMountTarget);
            }
        }
    }

    /** Nether wart berserkers attack everything, including their own team. */
    public boolean ignoresTeamLines() {
        return hasUpgrade(UpgradeFlags.NETHER_WART);
    }

    /** Sponge soldiers are completely ignored by AI auto-targeting. */
    public boolean isUntargetable() {
        return hasUpgrade(UpgradeFlags.SPONGE);
    }

    /** Egg stealth hides a soldier from anyone not wearing goggles (glass). */
    public boolean canDetect(ClaySoldierEntity candidate) {
        return !candidate.hasUpgrade(UpgradeFlags.EGG) || hasUpgrade(UpgradeFlags.GLASS);
    }

    /** Target scan range squared; goggles (glass) add +16 blocks of follow range. */
    public double getTargetScanRangeSq() {
        double range = hasUpgrade(UpgradeFlags.GLASS) ? 32.0 : 16.0;
        return range * range;
    }

    private void setActiveUpgrades(long upgrades) {
        this.activeUpgrades = upgrades;
        this.upgradeState.setRaw(upgrades);
        this.entityData.set(ACTIVE_UPGRADES, upgrades);

        for (int i = 0; i < 64; i++) {
            if ((upgrades & (1L << i)) == 0L) {
                upgradeUses[i] = 0;
            }
        }
    }

    /** Max health including diamond (+10), diamond block (+80), and ender pearl (+5) bonuses. */
    public float getSoldierMaxHealth() {
        float max = MAX_HEALTH;
        if (hasUpgrade(UpgradeFlags.DIAMOND)) {
            max += 10.0f;
        }
        if (hasUpgrade(UpgradeFlags.DIAMOND_BLOCK)) {
            max += 80.0f;
        }
        if (hasUpgrade(UpgradeFlags.ENDER_PEARL)) {
            max += 5.0f;
        }
        return max;
    }

    /** Durability multiplier applied to upgrades equipped from now on. */
    private int durabilityMultiplier() {
        int multiplier = 1;
        if (hasUpgrade(UpgradeFlags.GOLD_INGOT)) {
            multiplier *= 2;
        }
        if (hasUpgrade(UpgradeFlags.DIAMOND)) {
            multiplier *= 2;
        }
        if (hasUpgrade(UpgradeFlags.DIAMOND_BLOCK)) {
            multiplier *= 5;
        }
        return multiplier;
    }

    private boolean isSlotOccupied(UpgradeSlot slot) {
        long remaining = activeUpgrades;
        while (remaining != 0L) {
            long bit = Long.lowestOneBit(remaining);
            remaining &= ~bit;
            UpgradeSpec spec = UpgradeRegistry.getSpec(bit);
            if (spec != null && spec.slot() == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempt to equip whatever upgrade an item stack maps to, honoring slot
     * exclusivity, prerequisites, and incompatibilities. Returns true if the
     * stack should shrink by one.
     */
    private boolean tryEquipUpgradeStack(ItemStack stack) {
        // Arrow shortcut: instantly equips Stick (main hand) + Flint (enhancement).
        if (stack.is(Items.ARROW)) {
            boolean equippedStick = equipUpgrade(UpgradeRegistry.getSpec(UpgradeFlags.STICK), 0);
            boolean equippedFlint = equipUpgrade(UpgradeRegistry.getSpec(UpgradeFlags.FLINT), 0);
            return equippedStick || equippedFlint;
        }

        UpgradeSpec spec = UpgradeRegistry.getSpecFor(stack);
        if (spec == null) {
            return false;
        }

        // Dual-wield: a second shear blade goes to the off hand.
        if (spec.flag() == UpgradeFlags.SHEAR_RIGHT && hasUpgrade(UpgradeFlags.SHEAR_RIGHT)) {
            spec = UpgradeRegistry.SHEAR_LEFT_SPEC;
        }

        int carriedUses = DropStackMetadata.getUpgradeUsesOrDefault(stack, 0);
        if (!equipUpgrade(spec, carriedUses)) {
            return false;
        }

        if (spec.flag() == UpgradeFlags.FOOD) {
            FoodProperties food = stack.get(DataComponents.FOOD);
            this.foodHealNutrition = food != null ? (byte) Mth.clamp(food.nutrition(), 1, 127) : 4;
        }
        return true;
    }

    private boolean equipUpgrade(UpgradeSpec spec, int carriedUses) {
        if (spec == null || !spec.canEquipOnto(activeUpgrades)) {
            return false;
        }
        if (spec.slot().isExclusive() && isSlotOccupied(spec.slot())) {
            return false;
        }
        // Bone cannot be equipped while riding a mount.
        if (spec.flag() == UpgradeFlags.BONE && isPassenger()) {
            return false;
        }

        setActiveUpgrades(activeUpgrades | spec.flag());
        if (spec.hasFiniteUses()) {
            int uses = carriedUses > 0 ? carriedUses : spec.maxUses() * durabilityMultiplier();
            setUpgradeUses(spec.flag(), uses);
        }
        onUpgradeEquipped(spec.flag());
        return true;
    }

    private void onUpgradeEquipped(long flag) {
        if (flag == UpgradeFlags.GOLD_INGOT || flag == UpgradeFlags.DIAMOND) {
            multiplyAllUpgradeUses(2);
        } else if (flag == UpgradeFlags.DIAMOND_BLOCK) {
            multiplyAllUpgradeUses(5);
        }

        if (flag == UpgradeFlags.DIAMOND || flag == UpgradeFlags.DIAMOND_BLOCK) {
            setSoldierHealth(getSoldierMaxHealth());
        }
        if (flag == UpgradeFlags.ENDER_PEARL) {
            zombieDecayTicks = ZOMBIE_DECAY_TICKS;
        }
    }

    private void multiplyAllUpgradeUses(int factor) {
        for (int i = 0; i < 64; i++) {
            if (upgradeUses[i] > 0) {
                upgradeUses[i] = (short) Math.min(Short.MAX_VALUE, upgradeUses[i] * factor);
            }
        }
    }

    private boolean tryPickupNearbyUpgrade(long gameTime) {
        if (((gameTime + getId()) % UPGRADE_PICKUP_UPDATE_INTERVAL) != 0) {
            return false;
        }

        AABB pickupBox = getBoundingBox().inflate(UPGRADE_PICKUP_RANGE);
        List<ItemEntity> itemEntities = level().getEntitiesOfClass(
            ItemEntity.class,
            pickupBox,
            item -> item.isAlive() && !item.isRemoved()
        );

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            boolean consumed = tryReviveDollStack(itemEntity, stack) || tryEquipUpgradeStack(stack);
            if (!consumed) {
                continue;
            }

            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }

            return true;
        }

        return false;
    }

    /**
     * Resurrection: clay revives soldier dolls, ghast tears revive brick dolls,
     * and ender pearl carriers zombify dolls onto their own team.
     */
    private boolean tryReviveDollStack(ItemEntity itemEntity, ItemStack stack) {
        boolean isClayDoll = stack.is(ItemRegistry.SOLDIER_DOLL) || stack.is(ItemRegistry.RED_SOLDIER_DOLL);
        boolean isBrickDoll = stack.is(ItemRegistry.BRICK_SOLDIER_DOLL);
        if (!isClayDoll && !isBrickDoll) {
            return false;
        }

        // Doll durability (issue #8): a spent doll can never be revived again.
        int remainingRevivals = DropStackMetadata.getSoldierUsesOrDefault(stack, DEFAULT_RESURRECTION_BUDGET);
        if (remainingRevivals <= 0) {
            return false;
        }

        boolean zombify = hasUpgrade(UpgradeFlags.ENDER_PEARL)
            && !DropStackMetadata.isZombificationBlocked(stack);
        long reviverFlag;
        if (isClayDoll && hasUpgrade(UpgradeFlags.CLAY_BALL)) {
            reviverFlag = UpgradeFlags.CLAY_BALL;
        } else if (isBrickDoll && hasUpgrade(UpgradeFlags.GHAST_TEAR)) {
            reviverFlag = UpgradeFlags.GHAST_TEAR;
        } else if (zombify) {
            reviverFlag = UpgradeFlags.ENDER_PEARL;
        } else {
            return false;
        }

        ClaySoldierEntity revived = EntityRegistry.CLAY_SOLDIER.create(level(), EntitySpawnReason.SPAWN_ITEM_USE);
        if (revived == null) {
            return false;
        }

        if (reviverFlag == UpgradeFlags.ENDER_PEARL) {
            // Zombified soldiers join the reviver's team and inherit the decay timer.
            revived.setTeamId(getTeamId());
            revived.equipUpgrade(UpgradeRegistry.getSpec(UpgradeFlags.ENDER_PEARL), 0);
        } else {
            revived.setTeamId(stack.getItem() instanceof SoldierDollItem doll
                ? doll.getTeamId(stack)
                : getTeamId());
            consumeUpgradeUse(reviverFlag);
        }
        revived.setBrickSoldier(isBrickDoll);
        // Each revival consumes one use of the doll's resurrection budget.
        revived.setResurrectionUsesRemaining(remainingRevivals - 1);
        revived.setPos(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
        revived.setYRot(level().getRandom().nextFloat() * 360f);
        level().addFreshEntity(revived);
        return true;
    }

    /** Remaining doll revivals this soldier will carry onto its death drop. */
    public int getResurrectionUsesRemaining() {
        return resurrectionUsesRemaining;
    }

    public void setResurrectionUsesRemaining(int uses) {
        this.resurrectionUsesRemaining = Math.max(0, uses);
    }

    public ClaySoldierEntity getCachedTarget() {
        return cachedTarget;
    }

    public void noteEnemySighting(ClaySoldierEntity enemy, double x, double y, double z,
                                  float confidence, long gameTime, boolean allowRetarget) {
        if (confidence <= 0.0f) {
            return;
        }

        updateTargetMemory(x, y, z, confidence, gameTime);

        if (enemy == null || enemy == this || enemy.isSoldierDead()
            || (enemy.getTeamId() == getTeamId() && !ignoresTeamLines())) {
            return;
        }

        if (!SoldierTargetingHelper.hasValidTarget(this, enemy)) {
            return;
        }

        if (cachedTarget == null) {
            cachedTarget = enemy;
            cachedMountTarget = null;
            if (getAiState() == SoldierAiState.IDLE) {
                setAiState(SoldierAiState.CHASING);
            }
            return;
        }

        if (!allowRetarget) {
            return;
        }

        if (getAiState() == SoldierAiState.IDLE && !hasRecentLineOfSight(gameTime)) {
            cachedTarget = enemy;
            cachedMountTarget = null;
            setAiState(SoldierAiState.CHASING);
        }
    }

    private void updateTargetMemory(double x, double y, double z, float confidence, long gameTime) {
        float clamped = Mth.clamp(confidence, 0.0f, 1.0f);
        if (targetMemoryConfidence <= 1.0E-6f) {
            targetMemoryX = x;
            targetMemoryY = y;
            targetMemoryZ = z;
        } else {
            float mix = Math.max(0.15f, clamped);
            targetMemoryX = targetMemoryX + (x - targetMemoryX) * mix;
            targetMemoryY = targetMemoryY + (y - targetMemoryY) * mix;
            targetMemoryZ = targetMemoryZ + (z - targetMemoryZ) * mix;
        }

        targetMemoryConfidence = Math.max(targetMemoryConfidence, clamped);
        targetMemoryExpiryTick = gameTime + TARGET_MEMORY_DURATION_TICKS;
    }

    private void clearTargetMemory() {
        targetMemoryConfidence = 0.0f;
        targetMemoryExpiryTick = Long.MIN_VALUE;
    }

    private boolean hasTargetMemory(long gameTime) {
        return targetMemoryConfidence > 0.01f && gameTime <= targetMemoryExpiryTick;
    }

    private boolean hasRecentLineOfSight(long gameTime) {
        return lastDirectSightTick != Long.MIN_VALUE && (gameTime - lastDirectSightTick) <= TARGET_LOS_FRESHNESS_TICKS;
    }

    private boolean shouldCheckLineOfSight(long gameTime) {
        int interval = getAiState() == SoldierAiState.IDLE ? IDLE_LOS_CHECK_INTERVAL : ACTIVE_LOS_CHECK_INTERVAL;
        return Math.floorMod(gameTime + getId(), interval) == 0;
    }

    private void onEnemyDirectlySeen(ClaySoldierEntity enemy, long gameTime) {
        lastDirectSightTick = gameTime;
        noteEnemySighting(enemy, enemy.getX(), enemy.getY(), enemy.getZ(), 1.0f, gameTime, true);
        TeamGossipIndex.get(level()).reportEnemySighting(
            getTeamId(),
            enemy.getTeamId(),
            enemy.getX(),
            enemy.getY(),
            enemy.getZ(),
            1.0f,
            gameTime
        );

        if (gameTime - lastShoutTick >= SHOUT_BROADCAST_INTERVAL) {
            SoldierTargetingHelper.relayEnemySighting(this, enemy, gameTime);
            lastShoutTick = gameTime;
        }
    }

    private boolean chaseTargetMemory(long gameTime) {
        boolean profiling = TargetingProfiler.isEnabled();
        long start = profiling ? System.nanoTime() : 0L;
        if (!hasTargetMemory(gameTime)) {
            if (profiling) {
                TargetingProfiler.recordCombatSample("memoryChaseTime", "memoryChaseCalls", System.nanoTime() - start, gameTime);
            }
            return false;
        }

        Vec3 memoryPos = new Vec3(targetMemoryX, targetMemoryY, targetMemoryZ);
        double distSq = position().distanceToSqr(memoryPos);
        if (distSq <= TARGET_MEMORY_REACHED_SQ) {
            targetMemoryConfidence *= 0.6f;
            if (targetMemoryConfidence < 0.05f) {
                clearTargetMemory();
            }
            if (profiling) {
                TargetingProfiler.recordCombatSample("memoryChaseTime", "memoryChaseCalls", System.nanoTime() - start, gameTime);
            }
            return false;
        }

        chasePosition(memoryPos);
        if (profiling) {
            TargetingProfiler.recordCombatSample("memoryChaseTime", "memoryChaseCalls", System.nanoTime() - start, gameTime);
        }
        return true;
    }

    private boolean tryAcquireImmediateThreat(long gameTime) {
        boolean profiling = TargetingProfiler.isEnabled();
        long start = profiling ? System.nanoTime() : 0L;
        if (Math.floorMod(gameTime + getId(), IMMEDIATE_THREAT_CHECK_INTERVAL) != 0) {
            if (profiling) {
                TargetingProfiler.recordCombatSample("immediateThreatTime", "immediateThreatChecks", System.nanoTime() - start, gameTime);
            }
            return false;
        }

        double rangeSq = IMMEDIATE_THREAT_RANGE * IMMEDIATE_THREAT_RANGE;
        AABB nearbyBox = getBoundingBox().inflate(IMMEDIATE_THREAT_RANGE, 1.25, IMMEDIATE_THREAT_RANGE);
        List<ClaySoldierEntity> nearbyEnemies = level().getEntitiesOfClass(
            ClaySoldierEntity.class,
            nearbyBox,
            candidate -> candidate != this
                && candidate.isAlive()
                && !candidate.isRemoved()
                && !candidate.isSoldierDead()
                && (candidate.getTeamId() != getTeamId() || ignoresTeamLines())
        );

        ClaySoldierEntity best = null;
        double bestSq = rangeSq;
        for (int i = 0, size = nearbyEnemies.size(); i < size; i++) {
            ClaySoldierEntity candidate = nearbyEnemies.get(i);
            double distSq = distanceToSqr(candidate);
            if (distSq > bestSq) {
                continue;
            }
            if (!SoldierTargetingHelper.hasLineOfSight(this, candidate)) {
                continue;
            }

            best = candidate;
            bestSq = distSq;
        }

        if (best == null) {
            if (profiling) {
                TargetingProfiler.recordCombatSample("immediateThreatTime", "immediateThreatChecks", System.nanoTime() - start, gameTime);
            }
            return false;
        }

        noteEnemySighting(best, best.getX(), best.getY(), best.getZ(), 1.0f, gameTime, true);
        if (profiling) {
            TargetingProfiler.recordCombatSample("immediateThreatTime", "immediateThreatChecks", System.nanoTime() - start, gameTime);
        }
        return true;
    }

    public void aggroOnHit(ClaySoldierEntity attacker) {
        if (attacker == null || attacker == this || attacker.isSoldierDead()) {
            return;
        }
        // Pacifists never fight back; sponges ignore everyone.
        if (hasUpgrade(UpgradeFlags.WHEAT) || hasUpgrade(UpgradeFlags.SPONGE) || blindTicks > 0) {
            return;
        }
        // Berserkers (nether wart) retaliate even against teammates.
        if (attacker.getTeamId() == getTeamId() && !hasUpgrade(UpgradeFlags.NETHER_WART)) {
            return;
        }
        if (getAiState() != SoldierAiState.IDLE) {
            return;
        }
        if (distanceToSqr(attacker) > RETALIATE_AGGRO_RANGE_SQ) {
            return;
        }

        // Retaliators bypass noteEnemySighting's team/validity gates: direct aggro.
        if (cachedTarget == null) {
            cachedTarget = attacker;
            cachedMountTarget = null;
            setAiState(SoldierAiState.CHASING);
        }
        updateTargetMemory(attacker.getX(), attacker.getY(), attacker.getZ(), 1.0f, level().getGameTime());
    }

    /**
     * Rotten flesh: hunt nearby hostile mobs when no soldier enemies demand attention.
     */
    private boolean tryHuntHostileMob(long gameTime) {
        if (!hasUpgrade(UpgradeFlags.ROTTEN_FLESH)) {
            return false;
        }

        double huntRangeSq = MOB_HUNT_RANGE * MOB_HUNT_RANGE;
        if (cachedMobTarget != null
            && (!cachedMobTarget.isAlive() || cachedMobTarget.isRemoved()
                || distanceToSqr(cachedMobTarget) > huntRangeSq * 4.0)) {
            cachedMobTarget = null;
        }

        if (cachedMobTarget == null) {
            if (Math.floorMod(gameTime + getId(), MOB_HUNT_SCAN_INTERVAL) != 0) {
                return false;
            }
            List<Monster> mobs = level().getEntitiesOfClass(
                Monster.class,
                getBoundingBox().inflate(MOB_HUNT_RANGE, 2.0, MOB_HUNT_RANGE),
                m -> m.isAlive() && !m.isRemoved()
            );
            Monster best = null;
            double bestSq = huntRangeSq;
            for (int i = 0, size = mobs.size(); i < size; i++) {
                Monster candidate = mobs.get(i);
                double distSq = distanceToSqr(candidate);
                if (distSq < bestSq) {
                    best = candidate;
                    bestSq = distSq;
                }
            }
            cachedMobTarget = best;
            if (cachedMobTarget == null) {
                return false;
            }
        }

        // Mobs are full-size: extend the tiny soldier melee reach to the mob's bounding box.
        double reach = ATTACK_RANGE + cachedMobTarget.getBbWidth() * 0.5;
        if (distanceToSqr(cachedMobTarget) <= reach * reach) {
            setAiState(SoldierAiState.ATTACKING);
            Vec3 v = getDeltaMovement();
            setDeltaMovement(0.0, v.y, 0.0);
            if (attackCooldown <= 0 && level() instanceof ServerLevel serverLevel) {
                setAttackSwingTicks(ATTACK_SWING_DURATION);
                attackCooldown = ATTACK_COOLDOWN_TICKS;
                cachedMobTarget.hurtServer(serverLevel, level().damageSources().generic(), getMeleeAttackDamage());
                consumeMeleeWeaponDurability();
            }
        } else {
            setAiState(SoldierAiState.CHASING);
            chasePosition(cachedMobTarget.position());
        }
        return true;
    }

    /**
     * Gold nugget king: idle teammates regroup around their team's king.
     */
    private boolean tryFollowKing(long gameTime) {
        if (hasUpgrade(UpgradeFlags.GOLD_NUGGET)) {
            return false;
        }

        if (cachedKing != null
            && (cachedKing.isRemoved() || !cachedKing.isAlive() || cachedKing.isSoldierDead()
                || !cachedKing.hasUpgrade(UpgradeFlags.GOLD_NUGGET)
                || cachedKing.getTeamId() != getTeamId())) {
            cachedKing = null;
        }

        if (cachedKing == null) {
            if (Math.floorMod(gameTime + getId(), KING_SCAN_INTERVAL) != 0) {
                return false;
            }
            List<ClaySoldierEntity> teammates = SoldierIndex.get(level()).getTeam(getTeamId());
            for (int i = 0, size = teammates.size(); i < size; i++) {
                ClaySoldierEntity mate = teammates.get(i);
                if (mate != this && mate != null && !mate.isRemoved() && mate.isAlive()
                    && !mate.isSoldierDead() && mate.hasUpgrade(UpgradeFlags.GOLD_NUGGET)) {
                    cachedKing = mate;
                    break;
                }
            }
            if (cachedKing == null) {
                return false;
            }
        }

        if (distanceToSqr(cachedKing) <= KING_FOLLOW_STOP_RANGE_SQ) {
            return false;
        }

        setAiState(SoldierAiState.CHASING);
        chasePosition(cachedKing.position());
        return true;
    }

    @Override
    public boolean isPickable() {
        // Required so players can ray-hit and attack this non-Living entity.
        return true;
    }

    /** Middle-click pick block returns the matching doll, team preserved (issue #12). */
    @Override
    public ItemStack getPickResult() {
        ItemStack stack = new ItemStack(isBrickSoldier()
            ? ItemRegistry.BRICK_SOLDIER_DOLL
            : ItemRegistry.SOLDIER_DOLL);
        if (getTeamId() != 0) {
            SoldierDollItem.setTeamIdOnStack(stack, getTeamId());
        }
        return stack;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
        ItemStack held = player.getItemInHand(hand);

        // Empty hand: send your soul into the soldier (remote control mode).
        if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (!level().isClientSide()) {
                SoldierPossessionManager.getInstance().startPossession(player, this);
            }
            return InteractionResult.SUCCESS;
        }

        if (!UpgradeRegistry.supports(held)) {
            return InteractionResult.PASS;
        }

        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (tryEquipUpgradeStack(held) && !player.getAbilities().instabuild) {
            held.shrink(1);
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
            // Client is presentation-only: advance the server-correction glide and
            // avoid running parallel local physics that causes snap-back stutter.
            interpolation.interpolate();
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

        boolean physProfiling = TargetingProfiler.isEnabled();
        long physicsStart = physProfiling ? System.nanoTime() : 0L;

        if (!onGround()) {
            if (isInWater()) {
                airborneStartY = Double.NaN;
                Vec3 buoyant = getDeltaMovement();
                if (hasUpgrade(UpgradeFlags.LILY_PAD)) {
                    // Lily pad: float up to the surface and tread water.
                    double waterDepth = getFluidHeight(FluidTags.WATER);
                    double depthError = 0.45 - waterDepth;
                    double y = buoyant.y * 0.55 + Mth.clamp(depthError * 0.08, -0.015, 0.015);
                    setDeltaMovement(buoyant.x * 0.94, y, buoyant.z * 0.94);
                } else {
                    // Without a lily pad, clay sinks (slowly, water drag applies).
                    setDeltaMovement(buoyant.x * 0.94, Math.max(buoyant.y - 0.01, -0.06), buoyant.z * 0.94);
                }
            } else {
                setDeltaMovement(getDeltaMovement().add(0.0, -GRAVITY, 0.0));
                if (hasUpgrade(UpgradeFlags.FEATHER)) {
                    // Parachute: cap downward velocity.
                    Vec3 falling = getDeltaMovement();
                    if (falling.y < FEATHER_MAX_FALL_SPEED) {
                        setDeltaMovement(falling.x, FEATHER_MAX_FALL_SPEED, falling.z);
                    }
                }
                if (Double.isNaN(airborneStartY) || getY() > airborneStartY) {
                    airborneStartY = getY();
                }
            }
        } else {
            if (!Double.isNaN(airborneStartY)) {
                double dropHeight = airborneStartY - getY();
                airborneStartY = Double.NaN;
                if (dropHeight > FALL_DAMAGE_THRESHOLD && !hasUpgrade(UpgradeFlags.FEATHER)) {
                    applySoldierDamage((float) (dropHeight - FALL_DAMAGE_THRESHOLD), (byte) -1);
                }
            }
        }

        setDeltaMovement(getDeltaMovement().multiply(DRAG, 1.0, DRAG));
        if (slowTicks > 0) {
            Vec3 slowed = getDeltaMovement();
            setDeltaMovement(slowed.x * SLOW_VELOCITY_SCALAR, slowed.y, slowed.z * SLOW_VELOCITY_SCALAR);
        }
        move(MoverType.SELF, getDeltaMovement());

        if (physProfiling) {
            TargetingProfiler.recordCombatSample("soldierPhysicsTime", "soldierPhysicsTicks", System.nanoTime() - physicsStart, level().getGameTime());
        }

        serverCombatTick();
    }

    /**
     * True while this soldier should not seek out enemies on its own:
     * blinded, pacifist (wheat), ignored-and-ignoring (sponge), or
     * retaliator (fermented spider eye — fights only when attacked).
     */
    private boolean isAutoTargetingSuppressed() {
        return blindTicks > 0
            || hasUpgrade(UpgradeFlags.WHEAT)
            || hasUpgrade(UpgradeFlags.SPONGE)
            || hasUpgrade(UpgradeFlags.FERM_SPIDER_EYE);
    }

    private boolean updateAwarenessAndTarget(long gameTime) {
        if (blindTicks > 0 || hasUpgrade(UpgradeFlags.WHEAT) || hasUpgrade(UpgradeFlags.SPONGE)) {
            cachedTarget = null;
            return false;
        }

        boolean autoAcquire = !hasUpgrade(UpgradeFlags.FERM_SPIDER_EYE);
        if (autoAcquire) {
            tryAcquireImmediateThreat(gameTime);
        }

        if (!SoldierTargetingHelper.hasValidTarget(this, cachedTarget)) {
            cachedTarget = null;
        }

        if (cachedTarget == null) {
            if (!autoAcquire) {
                return false;
            }
            cachedTarget = SoldierTargetingHelper.updateTargetCache(this, null);
            if (cachedTarget != null) {
                onEnemyDirectlySeen(cachedTarget, gameTime);
                return true;
            }
            return false;
        }

        if (shouldCheckLineOfSight(gameTime)) {
            boolean profiling = TargetingProfiler.isEnabled();
            long losStart = profiling ? System.nanoTime() : 0L;
            if (SoldierTargetingHelper.hasLineOfSight(this, cachedTarget)) {
                onEnemyDirectlySeen(cachedTarget, gameTime);
                if (profiling) {
                    TargetingProfiler.recordCombatSample("lineOfSightTime", "lineOfSightChecks", System.nanoTime() - losStart, gameTime);
                }
                return true;
            }

            if (!hasRecentLineOfSight(gameTime)) {
                ClaySoldierEntity reacquired = SoldierTargetingHelper.updateTargetCache(this, null);
                if (reacquired != null && reacquired != cachedTarget) {
                    cachedTarget = reacquired;
                    onEnemyDirectlySeen(cachedTarget, gameTime);
                    if (profiling) {
                        TargetingProfiler.recordCombatSample("lineOfSightTime", "lineOfSightChecks", System.nanoTime() - losStart, gameTime);
                    }
                    return true;
                }
            }
            if (profiling) {
                TargetingProfiler.recordCombatSample("lineOfSightTime", "lineOfSightChecks", System.nanoTime() - losStart, gameTime);
            }
        }

        return hasRecentLineOfSight(gameTime);
    }

    private void serverCombatTick() {
        if (!registeredInSoldierIndex) {
            SoldierIndex.get(level()).register(this);
            registeredInSoldierIndex = true;
        }

        if (isNexusSummon() && nexusOriginId != null) {
            if (!registeredInNexusIndex) {
                NexusSummonIndex.get(level()).register(this);
                registeredInNexusIndex = true;
            }
        } else if (registeredInNexusIndex) {
            NexusSummonIndex.get(level()).unregister(this);
            registeredInNexusIndex = false;
        }

        if (isSoldierDead()) {
            return;
        }

        if (possessed) {
            possessedCombatTick(level().getGameTime());
            return;
        }

        boolean profiling = TargetingProfiler.isEnabled();
        long gameTime = level().getGameTime();
        long combatStart = profiling ? System.nanoTime() : 0L;

        try {

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

            if (profiling) {
                long statusStart = System.nanoTime();
                tickStatusEffects();
                TargetingProfiler.recordCombatSample("statusEffectTime", "statusEffectTicks", System.nanoTime() - statusStart, gameTime);
            } else {
                tickStatusEffects();
            }

            if (hasUpgrade(UpgradeFlags.PRISMARINE_CRYSTALS)
                && ((gameTime + getId()) % PRISMARINE_PARTICLE_INTERVAL) == 0
                && level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    getX(), getY() + 0.3, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            }

            if (tryPickupNearbyUpgrade(gameTime)) {
                return;
            }

            if (((gameTime + getId()) % SEPARATION_UPDATE_INTERVAL) == 0) {
                if (profiling) {
                    long separationStart = System.nanoTime();
                    cachedSeparation = sampleSeparationForce();
                    TargetingProfiler.recordCombatSample("separationSampleTime", "separationSamples", System.nanoTime() - separationStart, gameTime);
                } else {
                    cachedSeparation = sampleSeparationForce();
                }
            }

            boolean targetVisible = false;
            if (profiling) {
                long targetSelectStart = System.nanoTime();
                targetVisible = updateAwarenessAndTarget(gameTime);
                TargetingProfiler.recordCombatSample("targetSelectionTime", "targetSelections", System.nanoTime() - targetSelectStart, gameTime);
            } else {
                targetVisible = updateAwarenessAndTarget(gameTime);
            }

            if (cachedTarget == null) {
                if (!isAutoTargetingSuppressed()) {
                    if (chaseTargetMemory(gameTime)) {
                        setAiState(SoldierAiState.CHASING);
                        return;
                    }

                    TeamGossipIndex.GossipHint hint;
                    if (profiling) {
                        long gossipStart = System.nanoTime();
                        hint = TeamGossipIndex.get(level()).getStrongestEnemyHint(
                            getTeamId(),
                            getX(),
                            getY(),
                            getZ(),
                            GOSSIP_MAX_RANGE_BLOCKS * GOSSIP_MAX_RANGE_BLOCKS,
                            gameTime
                        );
                        TargetingProfiler.recordCombatSample("gossipHintTime", "gossipHintQueries", System.nanoTime() - gossipStart, gameTime);
                    } else {
                        hint = TeamGossipIndex.get(level()).getStrongestEnemyHint(
                            getTeamId(),
                            getX(),
                            getY(),
                            getZ(),
                            GOSSIP_MAX_RANGE_BLOCKS * GOSSIP_MAX_RANGE_BLOCKS,
                            gameTime
                        );
                    }
                    if (hint != null && hint.strength >= GOSSIP_MIN_CHASE_STRENGTH) {
                        noteEnemySighting(null, hint.x, hint.y, hint.z, Math.min(0.45f, hint.strength / 8.0f), gameTime, false);
                        if (chaseTargetMemory(gameTime)) {
                            setAiState(SoldierAiState.CHASING);
                            return;
                        }
                    }

                    if (tryHuntHostileMob(gameTime)) {
                        return;
                    }
                }

                setAiState(SoldierAiState.IDLE);

                boolean acquiredMount;
                if (profiling) {
                    long mountAcquireStart = System.nanoTime();
                    acquiredMount = tryAcquireMount();
                    TargetingProfiler.recordCombatSample("mountAcquireTime", "mountAcquireCalls", System.nanoTime() - mountAcquireStart, gameTime);
                } else {
                    acquiredMount = tryAcquireMount();
                }
                if (acquiredMount) {
                    return;
                }

                if (tryFollowKing(gameTime)) {
                    return;
                }

                if (profiling) {
                    long idleBrakeStart = System.nanoTime();
                    applyIdleBraking();
                    TargetingProfiler.recordCombatSample("idleBrakeTime", "idleBrakeCalls", System.nanoTime() - idleBrakeStart, gameTime);
                } else {
                    applyIdleBraking();
                }
                resetObstructionTracking();
                return;
            }

            if (!targetVisible) {
                setAiState(SoldierAiState.CHASING);
                if (!chaseTargetMemory(gameTime)) {
                    cachedTarget = null;
                    applyIdleBraking();
                }
                return;
            }

            double distSq = distanceToSqr(cachedTarget);
            if (distSq > ATTACK_RANGE_SQ && distSq <= RANGED_ATTACK_RANGE_SQ) {
                boolean firedRanged;
                if (profiling) {
                    long rangedStart = System.nanoTime();
                    firedRanged = tryRangedAttack(cachedTarget);
                    TargetingProfiler.recordCombatSample("rangedDecisionTime", "rangedDecisions", System.nanoTime() - rangedStart, gameTime);
                } else {
                    firedRanged = tryRangedAttack(cachedTarget);
                }
                if (firedRanged) {
                    setAiState(SoldierAiState.ATTACKING);
                    faceTarget(cachedTarget);
                    return;
                }
            }

            if (distSq <= ATTACK_RANGE_SQ) {
                if (profiling) {
                    long meleeStart = System.nanoTime();
                    setAiState(SoldierAiState.ATTACKING);

                    Vec3 v = getDeltaMovement();
                    setDeltaMovement(0.0, v.y, 0.0);
                    faceTarget(cachedTarget);

                    if (attackCooldown <= 0) {
                        performMeleeAttack(cachedTarget);
                    }
                    TargetingProfiler.recordCombatSample("meleeEngagementTime", "meleeEngagements", System.nanoTime() - meleeStart, gameTime);
                } else {
                    setAiState(SoldierAiState.ATTACKING);

                    Vec3 v = getDeltaMovement();
                    setDeltaMovement(0.0, v.y, 0.0);
                    faceTarget(cachedTarget);

                    if (attackCooldown <= 0) {
                        performMeleeAttack(cachedTarget);
                    }
                }
            } else {
                setAiState(SoldierAiState.CHASING);
                if (profiling) {
                    long chaseStart = System.nanoTime();
                    chaseTarget(cachedTarget);
                    TargetingProfiler.recordCombatSample("chaseTargetTime", "chaseTargetCalls", System.nanoTime() - chaseStart, gameTime);
                } else {
                    chaseTarget(cachedTarget);
                }
            }
        } finally {
            if (profiling) {
                TargetingProfiler.recordCombatSample("combatTickTime", "combatTicks", System.nanoTime() - combatStart, gameTime);
            }
        }
    }

    /** Total melee damage from base attack + weapon + enhancement bonuses. */
    private float getMeleeAttackDamage() {
        float damage = ATTACK_DAMAGE;
        if (hasUpgrade(UpgradeFlags.STICK)) {
            damage += 2.0f;
        }
        if (hasUpgrade(UpgradeFlags.BONE)) {
            damage += 3.0f;
        }
        if (hasUpgrade(UpgradeFlags.BLAZEROD)) {
            damage += 1.0f;
        }

        int shearBlades = (hasUpgrade(UpgradeFlags.SHEAR_RIGHT) ? 1 : 0)
            + (hasUpgrade(UpgradeFlags.SHEAR_LEFT) ? 1 : 0);
        damage += shearBlades;
        if (hasUpgrade(UpgradeFlags.PRISMARINE_SHARD)) {
            damage += shearBlades;
        }

        if (hasUpgrade(UpgradeFlags.FLINT)) {
            damage += 2.0f;
        }
        if (hasUpgrade(UpgradeFlags.GOLD_INGOT)) {
            damage += 1.0f;
        }

        // Brawler buttons occupy the main-hand slot, so they never stack with weapons.
        if (hasUpgrade(UpgradeFlags.WOOD_BUTTON)) {
            damage += 1.0f;
        }
        if (hasUpgrade(UpgradeFlags.STONE_BUTTON)) {
            damage += 2.0f;
        }

        return damage;
    }

    private void performMeleeAttack(ClaySoldierEntity target) {
        setAttackSwingTicks(ATTACK_SWING_DURATION);
        attackCooldown = ATTACK_COOLDOWN_TICKS;

        // Speckled melon: pacifist medic strike — heal critical targets and disengage.
        if (hasUpgrade(UpgradeFlags.GOLD_MELON)
            && target.getSoldierHealth() < target.getSoldierMaxHealth() * LOW_HEALTH_FRACTION) {
            target.setSoldierHealth(Math.min(target.getSoldierMaxHealth(),
                target.getSoldierHealth() + MELON_TARGET_HEAL));
            consumeUpgradeUse(UpgradeFlags.GOLD_MELON);
            cachedTarget = null;
            clearTargetMemory();
            setAiState(SoldierAiState.IDLE);
            return;
        }

        float damage = getMeleeAttackDamage();

        // Shear burst: bonus damage on the very first hit against a new enemy.
        boolean hasShears = hasUpgrade(UpgradeFlags.SHEAR_RIGHT) || hasUpgrade(UpgradeFlags.SHEAR_LEFT);
        if (hasShears && target.getId() != lastShearTargetId) {
            damage += 2.0f;
            lastShearTargetId = target.getId();
        }

        target.applyCombatDamage(damage, this);

        if (!target.isSoldierDead()) {
            if (hasUpgrade(UpgradeFlags.BLAZEROD)) {
                target.applyBurn(hasUpgrade(UpgradeFlags.COAL)
                    ? BURN_TICKS_BLAZE_ROD_COAL
                    : BURN_TICKS_BLAZE_ROD);
            }
            if (hasUpgrade(UpgradeFlags.RED_MUSHROOM)) {
                target.applyPoison();
                consumeUpgradeUse(UpgradeFlags.RED_MUSHROOM);
            }
            if (hasUpgrade(UpgradeFlags.BLAZE_POWDER)) {
                consumeUpgradeUse(UpgradeFlags.BLAZE_POWDER);
                target.applyBurn(BURN_TICKS_BLAZE_ROD);
                target.applySoldierDamage(MAGMA_BOMB_DAMAGE, (byte) -1, this);
            }
            if (hasUpgrade(UpgradeFlags.REDSTONE)) {
                target.applyBlindness();
                consumeUpgradeUse(UpgradeFlags.REDSTONE);
            }
            if (hasUpgrade(UpgradeFlags.SLIMEBALL)) {
                target.applyRoot();
                consumeUpgradeUse(UpgradeFlags.SLIMEBALL);
            }
        }

        consumeMeleeWeaponDurability();
    }

    private void consumeMeleeWeaponDurability() {
        if (hasUpgrade(UpgradeFlags.STICK)) {
            consumeUpgradeUse(UpgradeFlags.STICK);
        } else if (hasUpgrade(UpgradeFlags.BONE)) {
            consumeUpgradeUse(UpgradeFlags.BONE);
        } else if (hasUpgrade(UpgradeFlags.BLAZEROD)) {
            consumeUpgradeUse(UpgradeFlags.BLAZEROD);
        } else if (hasUpgrade(UpgradeFlags.SHEAR_RIGHT)) {
            consumeUpgradeUse(UpgradeFlags.SHEAR_RIGHT);
        } else if (hasUpgrade(UpgradeFlags.SHEAR_LEFT)) {
            consumeUpgradeUse(UpgradeFlags.SHEAR_LEFT);
        }
    }

    /** Damage classes used to resolve defensive upgrades. */
    public enum SoldierDamageKind {
        MELEE,
        RANGED,
        FIRE,
        EXPLOSION,
        GENERIC
    }

    /**
     * Run incoming damage through immunities and armor-style reductions,
     * consuming durability where applicable. Returns the final damage.
     */
    public float applyDefensiveUpgrades(float amount, SoldierDamageKind kind, Entity attacker) {
        if (kind == SoldierDamageKind.FIRE && hasUpgrade(UpgradeFlags.CACTUS)) {
            return 0.0f;
        }
        if (kind == SoldierDamageKind.EXPLOSION && hasUpgrade(UpgradeFlags.STRING)) {
            return 0.0f;
        }

        if (kind != SoldierDamageKind.MELEE && kind != SoldierDamageKind.RANGED) {
            return amount;
        }

        if (hasUpgrade(UpgradeFlags.LEATHER)) {
            amount *= 0.5f;
            consumeUpgradeUse(UpgradeFlags.LEATHER);
        }
        if (hasUpgrade(UpgradeFlags.RABBIT_HIDE)) {
            amount *= 0.875f;
            consumeUpgradeUse(UpgradeFlags.RABBIT_HIDE);
        }
        if (hasUpgrade(UpgradeFlags.WOOL)) {
            amount -= 1.0f;
        }
        if (hasUpgrade(UpgradeFlags.BOWL)) {
            amount -= 1.0f;
            consumeUpgradeUse(UpgradeFlags.BOWL);
        }
        if (hasUpgrade(UpgradeFlags.IRON_BLOCK)) {
            amount -= 1.0f;
        }
        if (hasUpgrade(UpgradeFlags.GOLD_INGOT)) {
            amount -= 1.0f;
        }
        amount = Math.max(0.0f, amount);

        registerQuartzHit();
        if (kind == SoldierDamageKind.MELEE
            && hasUpgrade(UpgradeFlags.NETHER_BRICK)
            && attacker instanceof ClaySoldierEntity attackingSoldier) {
            attackingSoldier.applyBurn(BURN_TICKS_BLAZE_ROD);
        }

        return amount;
    }

    /** Quartz shield: five hits inside a two-second window trigger a shockwave. */
    private void registerQuartzHit() {
        if (!hasUpgrade(UpgradeFlags.NETHER_QUARTZ)) {
            return;
        }

        long now = level().getGameTime();
        if (now - quartzWindowStartTick > QUARTZ_HIT_WINDOW_TICKS) {
            quartzWindowStartTick = now;
            quartzWindowHits = 0;
        }

        if (++quartzWindowHits >= QUARTZ_HITS_TO_TRIGGER) {
            quartzWindowHits = 0;
            quartzWindowStartTick = Long.MIN_VALUE;
            triggerQuartzShockwave();
        }
    }

    private void triggerQuartzShockwave() {
        consumeUpgradeUse(UpgradeFlags.NETHER_QUARTZ);

        AABB shockBox = getBoundingBox().inflate(QUARTZ_SHOCKWAVE_RADIUS, 1.0, QUARTZ_SHOCKWAVE_RADIUS);
        List<Entity> nearby = level().getEntities(this, shockBox,
            e -> e.isAlive() && !e.isRemoved() && !e.isPassengerOfSameVehicle(this));
        for (Entity entity : nearby) {
            Vec3 away = entity.position().subtract(position());
            Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
            double lenSq = horizontal.lengthSqr();
            if (lenSq < 1.0E-6) {
                continue;
            }
            Vec3 dir = horizontal.scale(1.0 / Math.sqrt(lenSq));
            entity.setDeltaMovement(entity.getDeltaMovement()
                .add(dir.x * QUARTZ_SHOCKWAVE_STRENGTH, 0.3, dir.z * QUARTZ_SHOCKWAVE_STRENGTH));
        }

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Low-health reactions: self-heal items and the firework escape rocket. */
    public void runPostDamageTriggers() {
        float maxHealth = getSoldierMaxHealth();
        float health = getSoldierHealth();

        if (health > 0.0f && health < maxHealth * LOW_HEALTH_FRACTION) {
            if (hasUpgrade(UpgradeFlags.BROWN_MUSHROOM)) {
                setSoldierHealth(Math.min(maxHealth, health + BROWN_MUSHROOM_HEAL));
                consumeUpgradeUse(UpgradeFlags.BROWN_MUSHROOM);
            } else if (hasUpgrade(UpgradeFlags.FOOD)) {
                setSoldierHealth(Math.min(maxHealth, health + foodHealNutrition * 0.5f));
                consumeUpgradeUse(UpgradeFlags.FOOD);
            }
        }

        if (health > 0.0f && health < maxHealth * ESCAPE_ROCKET_HEALTH_FRACTION
            && hasUpgrade(UpgradeFlags.FIREWORK_ROCKET)) {
            consumeUpgradeUse(UpgradeFlags.FIREWORK_ROCKET);
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x, ESCAPE_ROCKET_BOOST, v.z);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FIREWORK,
                    getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    /** Knockback scale for incoming hits: iron ingot resists 75%, brick is immovable. */
    public double getKnockbackResistFactor() {
        if (hasUpgrade(UpgradeFlags.BRICK)) {
            return 0.0;
        }
        if (hasUpgrade(UpgradeFlags.IRON_INGOT)) {
            return 0.25;
        }
        return 1.0;
    }

    /** Outgoing knockback scale: iron ingot doubles knockback dealt. */
    public double getKnockbackDealtFactor() {
        return hasUpgrade(UpgradeFlags.IRON_INGOT) ? 2.0 : 1.0;
    }

    public void applyBurn(int ticks) {
        if (hasUpgrade(UpgradeFlags.CACTUS)) {
            return;
        }
        combustionTicks = Math.max(combustionTicks, ticks);
    }

    /** True while burn damage-over-time is active. */
    public boolean isCombusting() {
        return combustionTicks > 0;
    }

    /**
     * Directly equip an upgrade by flag, running the normal validation and
     * on-equip effects. Used by debug tooling and game tests.
     */
    public boolean forceEquipUpgrade(long flag) {
        return equipUpgrade(UpgradeRegistry.getSpec(flag), 0);
    }

    public void applyPoison() {
        poisonTicks = Math.max(poisonTicks, POISON_DURATION_TICKS);
    }

    /** Blindness drops the current target and suppresses re-acquisition while active. */
    public void applyBlindness() {
        blindTicks = Math.max(blindTicks, BLIND_DURATION_TICKS);
        cachedTarget = null;
        clearTargetMemory();
    }

    public void applyRoot() {
        rootTicks = Math.max(rootTicks, ROOT_DURATION_TICKS);
    }

    /** Magma cream time bomb: short fuse, then a lethal blast on the carrier. */
    public void applyTimeBomb() {
        if (magmaBombTicks < 0) {
            magmaBombTicks = MAGMA_BOMB_FUSE_TICKS;
        }
    }

    private void tickStatusEffects() {
        if (slowTicks > 0) {
            slowTicks--;
        }
        if (blindTicks > 0) {
            blindTicks--;
        }
        if (rootTicks > 0) {
            rootTicks--;
        }

        if (combustionTicks > 0) {
            if (hasUpgrade(UpgradeFlags.CACTUS)) {
                combustionTicks = 0;
            } else {
                combustionTicks--;
                if (combustionDamageCooldown > 0) {
                    combustionDamageCooldown--;
                }

                if (combustionDamageCooldown <= 0) {
                    combustionDamageCooldown = COMBUSTION_TICK_INTERVAL;
                    RuntimeTelemetry.recordCombustionDamageTick();
                    applySoldierDamage(COMBUSTION_DAMAGE, (byte) -1);
                }
            }
        } else {
            combustionDamageCooldown = 0;
        }

        if (poisonTicks > 0) {
            poisonTicks--;
            if (poisonDamageCooldown > 0) {
                poisonDamageCooldown--;
            }
            if (poisonDamageCooldown <= 0) {
                poisonDamageCooldown = POISON_DAMAGE_INTERVAL;
                // Poison never finishes a soldier off, mirroring vanilla behavior.
                if (getSoldierHealth() > POISON_DAMAGE) {
                    applySoldierDamage(POISON_DAMAGE, (byte) -1);
                }
            }
        } else {
            poisonDamageCooldown = 0;
        }

        if (magmaBombTicks >= 0) {
            magmaBombTicks--;
            if (magmaBombTicks < 0) {
                detonateTimeBomb();
            }
        }

        if (zombieDecayTicks > 0 && hasUpgrade(UpgradeFlags.ENDER_PEARL)) {
            zombieDecayTicks--;
            if (zombieDecayTicks <= 0) {
                applySoldierDamage(MAGMA_BOMB_DAMAGE, (byte) -1);
            }
        }
    }

    private void detonateTimeBomb() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
        applySoldierDamage(MAGMA_BOMB_DAMAGE, (byte) -1);
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
        if (hasUpgrade(UpgradeFlags.GRAVEL)) {
            return EntityRegistry.GRAVEL_PROJECTILE.create(level(), EntitySpawnReason.SPAWN_ITEM_USE);
        }
        return null;
    }

    public void applySnowPayload() {
        slowTicks = Math.max(slowTicks, SLOW_DURATION_TICKS);
        RuntimeTelemetry.recordSlowPayload();
    }

    public void applyFirePayload() {
        applyBurn(COMBUSTION_DURATION_TICKS);
        RuntimeTelemetry.recordCombustionPayload();
    }

    public void applyEmeraldPayload(ClaySoldierEntity attacker, float damage) {
        SoldierCombatDamageHelper.applyCombatDamage(this, damage, attacker, 1.0f,
            EMERALD_RAW_DAMAGE_MULTIPLIER, SoldierDamageKind.RANGED);
    }

    private boolean tryAcquireMount() {
        if (getAiState() != SoldierAiState.IDLE) {
            return false;
        }

        if (getVehicle() != null) {
            return false;
        }

        // Legacy MH_BONE behavior: soldiers with bone upgrade never target mounts.
        if (hasUpgrade(UpgradeFlags.BONE)) {
            return false;
        }

        cachedMountTarget = SoldierTargetingHelper.updateMountTargetCache(this, cachedMountTarget);
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

    /** Speed multiplier from sugar/diamond/diamond block/rabbit hide. Brick roots the soldier. */
    private double getSpeedMultiplier() {
        double bonus = 0.0;
        if (hasUpgrade(UpgradeFlags.SUGAR)) {
            bonus += 0.5;
        }
        if (hasUpgrade(UpgradeFlags.DIAMOND)) {
            bonus += 0.5;
        }
        if (hasUpgrade(UpgradeFlags.DIAMOND_BLOCK)) {
            bonus += 0.5;
        }
        if (hasUpgrade(UpgradeFlags.RABBIT_HIDE)) {
            bonus += 0.25;
        }
        return 1.0 + bonus;
    }

    private boolean isMovementLocked() {
        return rootTicks > 0 || hasUpgrade(UpgradeFlags.BRICK);
    }

    private void chaseMount(BaseMountEntity mount) {
        if (isMovementLocked()) {
            faceMovementOrMount(mount);
            return;
        }

        Vec3 to = mount.position().subtract(position());
        Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
        double lenSq = horizontal.lengthSqr();
        if (lenSq < 1.0E-6) {
            return;
        }

        double speedMult = getSpeedMultiplier();
        Vec3 dir = horizontal.scale(1.0 / Math.sqrt(lenSq));
        Vec3 velocity = getDeltaMovement()
            .add(dir.scale(CHASE_ACCEL * speedMult))
            .add(cachedSeparation.scale(CombatTuning.getSeparationStrength()));

        double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        double maxSpeed = MAX_CHASE_SPEED * speedMult;
        double maxSpeedSq = maxSpeed * maxSpeed;
        if (horizontalSpeedSq > maxSpeedSq) {
            double scale = maxSpeed / Math.sqrt(horizontalSpeedSq);
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

    private void chasePosition(Vec3 targetPos) {
        if (isMovementLocked()) {
            return;
        }

        Vec3 to = targetPos.subtract(position());
        Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
        double lenSq = horizontal.lengthSqr();
        if (lenSq < 1.0E-6) {
            return;
        }

        double speedMult = getSpeedMultiplier();
        Vec3 dir = horizontal.scale(1.0 / Math.sqrt(lenSq));
        Vec3 velocity = getDeltaMovement()
            .add(dir.scale(CHASE_ACCEL * speedMult))
            .add(cachedSeparation.scale(CombatTuning.getSeparationStrength()));

        double horizontalSpeedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        double maxSpeed = MAX_CHASE_SPEED * speedMult;
        double maxSpeedSq = maxSpeed * maxSpeed;
        if (horizontalSpeedSq > maxSpeedSq) {
            double scale = maxSpeed / Math.sqrt(horizontalSpeedSq);
            velocity = new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
        }

        setDeltaMovement(velocity);

        if (horizontalCollision && onGround()) {
            if (obstructionTicks == 0) {
                obstructionBaseY = getY();
            }
            obstructionTicks++;

            double climbedHeight = getY() - obstructionBaseY;
            if (jumpAssistCooldown <= 0 && climbedHeight < CombatTuning.getMaxObstacleClimbHeight()) {
                float jumpVelocity = CombatTuning.getJumpAssistVelocity()
                    * (hasUpgrade(UpgradeFlags.RABBIT_FOOT) ? 1.5f : 1.0f);
                setDeltaMovement(getDeltaMovement().x, jumpVelocity, getDeltaMovement().z);
                jumpAssistCooldown = JUMP_ASSIST_COOLDOWN_TICKS;
            }

            double dirSign = (((level().getGameTime() + getId()) / 10L) & 1L) == 0L ? 1.0 : -1.0;
            Vec3 strafe = new Vec3(-dir.z * CombatTuning.getObstacleStrafeStrength() * dirSign, 0.0,
                    dir.x * CombatTuning.getObstacleStrafeStrength() * dirSign);
            setDeltaMovement(getDeltaMovement().add(strafe));
        } else if (onGround()) {
            resetObstructionTracking();
        }

        Vec3 move = getDeltaMovement();
        double moveSq = move.x * move.x + move.z * move.z;
        if (moveSq > 1.0E-6) {
            float yaw = (float) (Math.atan2(move.z, move.x) * (180.0 / Math.PI)) - 90.0f;
            setYRot(yaw);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
        } else {
            double dx = targetPos.x - getX();
            double dz = targetPos.z - getZ();
            float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            setYRot(yaw);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
        }
    }

    private void chaseTarget(ClaySoldierEntity target) {
        chasePosition(target.position());
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



    void onSoldierKilled(ServerLevel serverLevel) {
        if (isNexusSummon()) {
            discard();
            return;
        }

        // Death explosions.
        if (hasUpgrade(UpgradeFlags.GUNPOWDER)) {
            serverLevel.explode(this, getX(), getY(), getZ(), 1.0f, Level.ExplosionInteraction.NONE);
        } else if (hasUpgrade(UpgradeFlags.FIREWORK_STAR)) {
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                serverLevel, getX(), getY(), getZ(), new ItemStack(Items.FIREWORK_ROCKET));
            serverLevel.addFreshEntity(rocket);
        }

        ItemStack drop = new ItemStack(isBrickSoldier()
            ? ItemRegistry.BRICK_SOLDIER_DOLL
            : ItemRegistry.SOLDIER_DOLL);
        if (getTeamId() != 0) {
            SoldierDollItem.setTeamIdOnStack(drop, getTeamId());
        }
        DropStackMetadata.setSoldierUses(drop, resurrectionUsesRemaining);
        if (hasUpgrade(UpgradeFlags.WHEAT_SEEDS)) {
            // Wheat seeds immunity persists onto the doll: it can't be zombified.
            DropStackMetadata.setZombificationBlocked(drop);
        }

        spawnAtLocation(serverLevel, drop);
        dropActiveUpgrades(serverLevel);
        discard();
    }

    /** Drop every held upgrade as its item, preserving remaining durability. */
    private void dropActiveUpgrades(ServerLevel serverLevel) {
        long remaining = activeUpgrades;
        while (remaining != 0L) {
            long bit = Long.lowestOneBit(remaining);
            remaining &= ~bit;

            Item dropItem = UpgradeRegistry.getDropItem(bit);
            if (dropItem == null) {
                continue;
            }

            ItemStack upgradeDrop = new ItemStack(dropItem);
            UpgradeSpec spec = UpgradeRegistry.getSpec(bit);
            if (spec != null && spec.hasFiniteUses()) {
                DropStackMetadata.setUpgradeData(upgradeDrop, bit, getUpgradeUses(bit));
            }
            spawnAtLocation(serverLevel, upgradeDrop);
        }
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

        for (int i = 0; i < 64; i++) {
            long bit = 1L << i;
            if ((persisted & bit) == 0L) {
                continue;
            }
            short uses = (short) input.getShortOr("UpgradeUses" + i, (short) 0);
            UpgradeSpec spec = UpgradeRegistry.getSpec(bit);
            if (uses <= 0 && spec != null && spec.hasFiniteUses()) {
                // Pre-durability saves (or legacy sticks): start at full durability.
                uses = (short) spec.maxUses();
            }
            upgradeUses[i] = uses;
        }
        // Migrate legacy stick durability saves.
        short legacyStickUses = (short) input.getShortOr("StickUsesRemaining", (short) 0);
        if (legacyStickUses > 0 && hasUpgrade(UpgradeFlags.STICK)) {
            setUpgradeUses(UpgradeFlags.STICK, legacyStickUses);
        }

        poisonTicks = input.getIntOr("PoisonTicks", 0);
        blindTicks = input.getIntOr("BlindTicks", 0);
        rootTicks = input.getIntOr("RootTicks", 0);
        magmaBombTicks = input.getIntOr("MagmaBombTicks", -1);
        zombieDecayTicks = input.getIntOr("ZombieDecayTicks",
            hasUpgrade(UpgradeFlags.ENDER_PEARL) ? ZOMBIE_DECAY_TICKS : -1);
        foodHealNutrition = input.getByteOr("FoodHealNutrition", (byte) 4);
        resurrectionUsesRemaining = input.getIntOr("ResurrectionUses", DEFAULT_RESURRECTION_BUDGET);
        String persistedNexusOriginId = input.getString("NexusOriginId").orElse(null);
        if (persistedNexusOriginId != null && !persistedNexusOriginId.isBlank()) {
            try {
                nexusOriginId = UUID.fromString(persistedNexusOriginId);
            } catch (IllegalArgumentException ignored) {
                nexusOriginId = null;
            }
        } else {
            nexusOriginId = null;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("TeamId", getTeamId());
        output.putFloat("Health", getSoldierHealth());
        output.putByte("EntityFlags", getFlags());
        output.putByte("AiState", getAiState().id);
        output.putLong("ActiveUpgrades", activeUpgrades);
        for (int i = 0; i < 64; i++) {
            if (upgradeUses[i] > 0) {
                output.putShort("UpgradeUses" + i, upgradeUses[i]);
            }
        }
        if (poisonTicks > 0) {
            output.putInt("PoisonTicks", poisonTicks);
        }
        if (blindTicks > 0) {
            output.putInt("BlindTicks", blindTicks);
        }
        if (rootTicks > 0) {
            output.putInt("RootTicks", rootTicks);
        }
        if (magmaBombTicks >= 0) {
            output.putInt("MagmaBombTicks", magmaBombTicks);
        }
        if (zombieDecayTicks >= 0) {
            output.putInt("ZombieDecayTicks", zombieDecayTicks);
        }
        output.putByte("FoodHealNutrition", foodHealNutrition);
        output.putInt("ResurrectionUses", resurrectionUsesRemaining);
        if (nexusOriginId != null) {
            output.putString("NexusOriginId", nexusOriginId.toString());
        }
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
    public void remove(Entity.RemovalReason reason) {
        if (registeredInSoldierIndex && !level().isClientSide()) {
            SoldierIndex.get(level()).unregister(this);
            registeredInSoldierIndex = false;
        }
        if (!level().isClientSide() && (registeredInNexusIndex || (isNexusSummon() && nexusOriginId != null))) {
            NexusSummonIndex.get(level()).unregister(this);
            registeredInNexusIndex = false;
        }
        super.remove(reason);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        float resolvedAmount = amount;
        Entity attacker = source.getEntity();
        if (attacker instanceof Player) {
            resolvedAmount *= CombatTuning.getPlayerDamageMultiplier();
        }

        SoldierDamageKind kind = SoldierDamageKind.GENERIC;
        if (source.is(DamageTypeTags.IS_FIRE)) {
            kind = SoldierDamageKind.FIRE;
        } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            kind = SoldierDamageKind.EXPLOSION;
        } else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            kind = SoldierDamageKind.RANGED;
        } else if (attacker != null) {
            kind = SoldierDamageKind.MELEE;
        }

        Entity knockbackSource = source.getDirectEntity() != null ? source.getDirectEntity() : attacker;
        applySoldierDamage(resolvedAmount, (byte) -1, knockbackSource, kind);

        // Fire damage sets clay alight (unless cactus mitigates it).
        if (kind == SoldierDamageKind.FIRE && !isSoldierDead()) {
            applyBurn(COMBUSTION_DURATION_TICKS);
        }
        // The hit landed — report it as such even when lethal, otherwise the
        // killing blow registers as a miss and the held click falls through
        // to the block behind the soldier (issue #1).
        return true;
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
