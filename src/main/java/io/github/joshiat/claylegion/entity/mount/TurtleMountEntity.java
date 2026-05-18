package io.github.joshiat.claylegion.entity.mount;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.RuntimeTelemetry;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Turtle mount: 9 variants with specialized water and defense traits.
 *
 * Characteristics:
 *  - Moves normally on land, but water does NOT slow down movement.
 *  - Takes 0.5x damage from direct attacks (defensive mount).
 *  - Slower than Horse but more durable for sustained battles.
 */
public class TurtleMountEntity extends BaseMountEntity {

    private static final double LAND_GRAVITY = 0.08;
    private static final double LAND_DRAG = 0.98;
    private static final double WATER_DRAG = 0.99;
    private static final double WATER_TARGET_DEPTH = 0.55;
    private static final float DAMAGE_REDUCTION_MULTIPLIER = 0.5f;

    public TurtleMountEntity(EntityType<? extends BaseMountEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public byte getMountTypeId() {
        return 3;  // Unique type identifier for Turtle
    }

    @Override
    public float getMaxHealth() {
        return 28.0f;  // Higher health than Horse for tank role
    }

    @Override
    protected void applyMountMovement(Vec3 soldierVelocity, ClaySoldierEntity soldier) {
        // Turtle applies soldier velocity normally; water resistance is handled in tick logic.
        setDeltaMovement(soldierVelocity);
    }

    @Override
    public void applyMountDamage(float amount, ClaySoldierEntity attacker) {
        if (level().isClientSide() || isMountDead() || amount <= 0.0f) {
            return;
        }

        float reducedDamage = amount;
        if (getFluidHeight(FluidTags.WATER) > 0.0D) {
            reducedDamage *= DAMAGE_REDUCTION_MULTIPLIER;
        }
        float newHealth = getHealth() - reducedDamage;
        setHealth(newHealth);

        if (newHealth <= 0f && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            onTurtleMountKilled(serverLevel);
        }
    }

    private void onTurtleMountKilled(net.minecraft.server.level.ServerLevel serverLevel) {
        // Eject passenger before discard
        if (!getPassengers().isEmpty()) {
            net.minecraft.world.entity.Entity passenger = getPassengers().get(0);
            passenger.stopRiding();
        }

        discard();
    }

    @Override
    public void tick() {
        super.baseTick();

        if (level().isClientSide()) {
            return;
        }

        serverMountTick();

        Vec3 velocity = getDeltaMovement();
        double waterDepth = getFluidHeight(FluidTags.WATER);
        if (waterDepth > 0.0D) {
            RuntimeTelemetry.recordTurtleWaterTick();

            double depthError = WATER_TARGET_DEPTH - waterDepth;
            double newY = velocity.y * 0.55 + Mth.clamp(depthError * 0.08, -0.02, 0.02);

            velocity = new Vec3(velocity.x * WATER_DRAG, newY, velocity.z * WATER_DRAG);
        } else {
            if (!onGround()) {
                velocity = velocity.add(0.0, -LAND_GRAVITY, 0.0);
            }
            velocity = velocity.multiply(LAND_DRAG, 1.0, LAND_DRAG);
        }

        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
    }
}
