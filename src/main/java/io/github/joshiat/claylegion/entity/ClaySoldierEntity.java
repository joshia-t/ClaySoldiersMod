package io.github.joshiat.claylegion.entity;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Core clay soldier entity.
 *
 * Architectural constraints (see MANIFESTO.md):
 *  - Extends Entity, NOT LivingEntity, to avoid O(n) vanilla update bloat.
 *  - AI runs on a lazy tick (every 8 ticks) to reduce per-entity overhead.
 *  - Upgrades stored as a compact long bitfield in UpgradeState.
 */
public class ClaySoldierEntity extends Entity {

    private static final EntityDataAccessor<Integer> TEAM_ID =
            SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> ENTITY_STATE =
            SynchedEntityData.defineId(ClaySoldierEntity.class, EntityDataSerializers.BYTE);

    public static final float MAX_HEALTH = 4.0f;
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;
    private static final int LAZY_TICK_INTERVAL = 8;

    private final UpgradeState upgradeState = new UpgradeState();
    private int lazyTickCounter = 0;

    public ClaySoldierEntity(EntityType<? extends ClaySoldierEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TEAM_ID, 0);
        builder.define(HEALTH, MAX_HEALTH);
        builder.define(ENTITY_STATE, (byte) 0);
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

    public void hurtSoldier(float amount, DamageSource source) {
        if (level().isClientSide()) {
            return;
        }

        float newHealth = getSoldierHealth() - amount;
        setSoldierHealth(newHealth);
        if (newHealth <= 0f && level() instanceof ServerLevel serverLevel) {
            kill(serverLevel);
        }
    }

    private byte getState() {
        return entityData.get(ENTITY_STATE);
    }

    private void setState(byte state) {
        entityData.set(ENTITY_STATE, state);
    }

    public boolean isBrickSoldier() {
        return (getState() & 0x02) != 0;
    }

    public void setBrickSoldier(boolean brick) {
        byte s = getState();
        setState(brick ? (byte) (s | 0x02) : (byte) (s & ~0x02));
    }

    public UpgradeState getUpgradeState() {
        return upgradeState;
    }

    @Override
    public void tick() {
        super.tick();

        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0.0, -GRAVITY, 0.0));
        }

        setDeltaMovement(getDeltaMovement().multiply(DRAG, 1.0, DRAG));
        move(MoverType.SELF, getDeltaMovement());

        if (++lazyTickCounter >= LAZY_TICK_INTERVAL) {
            lazyTickCounter = 0;
            lazyTick();
        }
    }

    protected void lazyTick() {
        // Stub: no AI yet — Phase 2
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setTeamId(input.getInt("TeamId").orElse(0));
        setSoldierHealth(input.getFloatOr("Health", MAX_HEALTH));
        setState(input.getByteOr("EntityState", (byte) 0));
        upgradeState.readFromStorage(input);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("TeamId", getTeamId());
        output.putFloat("Health", getSoldierHealth());
        output.putByte("EntityState", getState());
        upgradeState.writeToStorage(output);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        hurtSoldier(amount, source);
        return !isSoldierDead();
    }
}
