package io.github.joshiat.claylegion.entity.projectile;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Fire Charge projectile: Applies combustion/burn effect on impact.
 *
 * Characteristics:
 *  - Moderate damage with burn effect.
 *  - Sets target's combustion timer, applying DOT (damage over time).
 */
public class FireChargeProjectileEntity extends ClayProjectileEntity {

    private static final float DAMAGE = 1.2f;

    public FireChargeProjectileEntity(EntityType<? extends ClayProjectileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public byte getProjectileTypeId() {
        return 3;
    }

    @Override
    public float getDamage() {
        return DAMAGE;
    }

    @Override
    protected void onHitSoldier(ClaySoldierEntity target) {
        super.onHitSoldier(target);
        // TODO: Apply burn effect via upgrade system or state machine
        // This would create a combustion tick flag and apply periodic damage
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        discard();
        return false;
    }
}
