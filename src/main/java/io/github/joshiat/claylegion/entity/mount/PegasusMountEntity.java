package io.github.joshiat.claylegion.entity.mount;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Pegasus mount: 10 variants with 3D flight pathfinding.
 *
 * Characteristics:
 *  - Adjusts Y-component of velocity to match target's height + altitude offset.
 *  - Enables mounted soldiers to pursue flying or elevated enemies.
 *  - Can hover and maneuver in 3D space.
 */
public class PegasusMountEntity extends BaseMountEntity {

    private static final double ALTITUDE_OFFSET = 1.5;  // How far above target the Pegasus tries to fly
    private static final double VERTICAL_ACCEL = 0.04;  // Y-velocity adjustment per tick
    private static final double MAX_VERTICAL_SPEED = 0.16;

    public PegasusMountEntity(EntityType<? extends BaseMountEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public byte getMountTypeId() {
        return 2;  // Unique type identifier for Pegasus
    }

    @Override
    public float getMaxHealth() {
        return 16.0f;  // Slightly lower health than Horse due to flight vulnerability
    }

    @Override
    protected void applyMountMovement(Vec3 soldierVelocity, ClaySoldierEntity soldier) {
        // Extract horizontal movement from soldier's velocity
        Vec3 currentVelocity = getDeltaMovement();
        Vec3 horizontalMovement = new Vec3(soldierVelocity.x, 0.0, soldierVelocity.z);

        // Determine target height: if the soldier has a target, fly toward it at altitude_offset
        double targetY = getY();
        ClaySoldierEntity target = soldier.getCachedTarget();
        if (target != null) {
            targetY = target.getY() + ALTITUDE_OFFSET;
        }

        // Adjust vertical velocity toward target height
        double yDifference = targetY - getY();
        double newYVelocity = currentVelocity.y;

        if (Math.abs(yDifference) > 0.05) {
            newYVelocity += Math.signum(yDifference) * VERTICAL_ACCEL;
            newYVelocity = Math.max(-MAX_VERTICAL_SPEED, Math.min(MAX_VERTICAL_SPEED, newYVelocity));
        } else {
            // Hover stabilization: reduce vertical velocity when at target height
            newYVelocity *= 0.9;
        }

        setDeltaMovement(horizontalMovement.x, newYVelocity, horizontalMovement.z);
    }

    @Override
    public void applyMountDamage(float amount, ClaySoldierEntity attacker) {
        // Pegasi take full damage (no special reduction)
        super.applyMountDamage(amount, attacker);
    }
}
