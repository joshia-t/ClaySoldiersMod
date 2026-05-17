package io.github.joshiat.claylegion.entity.mount;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Horse mount: 10 variants with basic 2D movement.
 *
 * Characteristics:
 *  - Inherits standard mounted combat bonuses (100% damage to unmounted enemies).
 *  - Applies knockback modifier on impact (slightly pushes back hit enemies).
 */
public class HorseMountEntity extends BaseMountEntity {

    public HorseMountEntity(EntityType<? extends BaseMountEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public byte getMountTypeId() {
        return 1;  // Unique type identifier for Horse
    }

    @Override
    public float getMaxHealth() {
        return 20.0f;
    }

    @Override
    protected void applyMountMovement(Vec3 soldierVelocity, ClaySoldierEntity soldier) {
        // Simple 2D movement: directly apply the soldier's horizontal velocity.
        setDeltaMovement(soldierVelocity);
    }

    @Override
    public void applyMountDamage(float amount, ClaySoldierEntity attacker) {
        // Default damage processing for Horse mount.
        super.applyMountDamage(amount, attacker);
    }
}
