package io.github.joshiat.claylegion.entity.mount;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Gecko mount: 36 variants with wall-climbing/vertical traversal.
 *
 * Characteristics:
 *  - Detects horizontal block adjacency (via horizontalCollision flag).
 *  - When climbing: nullifies gravity and applies upward Y-translation.
 *  - Enables soldiers to scale vertical walls and reach elevated terrain.
 *  - Slower movement than Horse but unique vertical mobility.
 */
public class GeckoMountEntity extends BaseMountEntity {

    private static final double CLIMB_SPEED = 0.12;  // Vertical velocity when climbing
    private static final double CLIMB_ACCEL = 0.03;  // Acceleration toward target climb speed

    public GeckoMountEntity(EntityType<? extends BaseMountEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public byte getMountTypeId() {
        return 5;  // Unique type identifier for Gecko
    }

    @Override
    public float getMaxHealth() {
        return 14.0f;  // Lower than Horse; specialized movement role
    }

    @Override
    protected void applyMountMovement(Vec3 soldierVelocity, ClaySoldierEntity soldier) {
        Vec3 currentVelocity = getDeltaMovement();

        // Check if the Gecko is against a horizontal surface (horizontalCollision flag)
        if (horizontalCollision && !onGround()) {
            // Wall-climbing: nullify gravity and translate vertically along the wall
            double targetClimbVelocity = CLIMB_SPEED;
            double newYVelocity = currentVelocity.y + Math.signum(targetClimbVelocity - currentVelocity.y) * CLIMB_ACCEL;
            newYVelocity = Math.max(-CLIMB_SPEED, Math.min(CLIMB_SPEED, newYVelocity));

            // Apply horizontal velocity from soldier + climbing velocity
            setDeltaMovement(soldierVelocity.x, newYVelocity, soldierVelocity.z);
        } else {
            // Normal movement: apply soldier's velocity as-is
            setDeltaMovement(soldierVelocity);
        }
    }

    @Override
    public void applyMountDamage(float amount, ClaySoldierEntity attacker) {
        // Gecko takes normal damage
        super.applyMountDamage(amount, attacker);
    }
}
