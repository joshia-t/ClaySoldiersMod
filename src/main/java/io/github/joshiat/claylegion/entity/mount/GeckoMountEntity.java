package io.github.joshiat.claylegion.entity.mount;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.RuntimeTelemetry;
import net.minecraft.util.Mth;
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
        // Check if the Gecko is against a horizontal surface (horizontalCollision flag).
        if (horizontalCollision) {
            RuntimeTelemetry.recordGeckoClimbTick();

            double forwardMag = Math.sqrt(soldierVelocity.x * soldierVelocity.x + soldierVelocity.z * soldierVelocity.z);
            double climbTarget = Math.max(CLIMB_SPEED, forwardMag);
            double climbY = Mth.clamp(climbTarget, 0.0, CLIMB_SPEED + CLIMB_ACCEL);

            // Map forward momentum into vertical traversal while preserving slight wall adhesion.
            double wallX = soldierVelocity.x * 0.15;
            double wallZ = soldierVelocity.z * 0.15;
            setDeltaMovement(wallX, climbY, wallZ);

            // Keep rider orientation aligned while climbing.
            setYRot(soldier.getYRot());
            setXRot(-75.0f);
            soldier.setYRot(getYRot());
            soldier.setXRot(getXRot());
        } else {
            // Normal movement: apply soldier's velocity as-is
            setDeltaMovement(soldierVelocity);
            setXRot(0.0f);
        }
    }

    @Override
    public void applyMountDamage(float amount, ClaySoldierEntity attacker) {
        // Gecko takes normal damage
        super.applyMountDamage(amount, attacker);
    }
}
