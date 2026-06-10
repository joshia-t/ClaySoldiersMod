package io.github.joshiat.claylegion.entity.projectile;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Gravel projectile: Standard ranged attack with moderate damage.
 */
public class GravelProjectileEntity extends ClayProjectileEntity {

    private static final float DAMAGE = 1.5f;

    public GravelProjectileEntity(EntityType<? extends ClayProjectileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public byte getProjectileTypeId() {
        return 1;
    }

    @Override
    public float getDamage() {
        return DAMAGE;
    }

    @Override
    protected void onHitSoldier(ClaySoldierEntity target) {
        super.onHitSoldier(target);
        // Heavy lump of rock: shove the target along the flight path (issue #15).
        applyImpactKnockback(target, 0.3);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        discard();
        return false;
    }
}
