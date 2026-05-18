package io.github.joshiat.claylegion.entity.mount;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Bunny mount: 16 variants with hopping/jumping mechanics.
 *
 * Characteristics:
 *  - Fast horizontal movement with cyclic hopping behavior.
 *  - Uses a timer to apply upward Y-velocity spikes (hopping effect).
 *  - Lower health than Horse but higher speed.
 */
public class BunnyMountEntity extends BaseMountEntity {

    private static final int HOP_CYCLE_TICKS = 12;
    private static final double HOP_IMPULSE = 0.72;
    private static final double HOP_FORWARD_BOOST = 1.18;

    private int hopTickCounter;

    public BunnyMountEntity(EntityType<? extends BaseMountEntity> type, Level level) {
        super(type, level);
        hopTickCounter = 0;
    }

    @Override
    public byte getMountTypeId() {
        return 4;  // Unique type identifier for Bunny
    }

    @Override
    public float getMaxHealth() {
        return 12.0f;  // Lower health than Horse; speed trade-off
    }

    @Override
    protected void applyMountMovement(Vec3 soldierVelocity, ClaySoldierEntity soldier) {
        Vec3 horizontalIntent = new Vec3(soldierVelocity.x, 0.0, soldierVelocity.z);
        boolean wantsMove = horizontalIntent.lengthSqr() > 1.0E-5
            && soldier.getAiState() != ClaySoldierEntity.SoldierAiState.ATTACKING;

        if (!wantsMove) {
            hopTickCounter = 0;
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x * 0.2, v.y, v.z * 0.2);
            return;
        }

        if (onGround()) {
            hopTickCounter++;
            if (hopTickCounter >= HOP_CYCLE_TICKS) {
                hopTickCounter = 0;
                setDeltaMovement(
                    horizontalIntent.x * HOP_FORWARD_BOOST,
                    HOP_IMPULSE,
                    horizontalIntent.z * HOP_FORWARD_BOOST
                );
            } else {
                setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
            }
        } else {
            Vec3 airborne = getDeltaMovement();
            setDeltaMovement(airborne.x * 0.985, airborne.y, airborne.z * 0.985);
        }
    }

    @Override
    public void applyMountDamage(float amount, ClaySoldierEntity attacker) {
        // Bunny takes normal damage
        super.applyMountDamage(amount, attacker);
    }
}
