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

    private static final int HOP_CYCLE_TICKS = 12;  // How often to apply hop impulse
    private static final double HOP_IMPULSE = 0.20;  // Y-velocity added per hop

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
        // Track hopping cycle and apply upward impulse on schedule
        hopTickCounter++;
        if (hopTickCounter >= HOP_CYCLE_TICKS) {
            hopTickCounter = 0;
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x, Math.max(v.y, HOP_IMPULSE), v.z);
        } else {
            setDeltaMovement(soldierVelocity);
        }
    }

    @Override
    public void applyMountDamage(float amount, ClaySoldierEntity attacker) {
        // Bunny takes normal damage
        super.applyMountDamage(amount, attacker);
    }
}
