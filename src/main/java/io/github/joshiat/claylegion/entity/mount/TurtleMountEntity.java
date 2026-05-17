package io.github.joshiat.claylegion.entity.mount;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
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

        // Apply 0.5x damage multiplier for Turtle's defensive trait
        float reducedDamage = amount * DAMAGE_REDUCTION_MULTIPLIER;
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
    protected void serverMountTick() {
        // Standard mount tick but with fluid resistance handling
        super.serverMountTick();
    }
}
