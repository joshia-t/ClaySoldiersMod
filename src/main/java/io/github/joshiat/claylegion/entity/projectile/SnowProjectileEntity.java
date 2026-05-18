package io.github.joshiat.claylegion.entity.projectile;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeFlags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Snow projectile: Lower damage but applies a slowdown effect.
 *
 * Characteristics:
 *  - Lower base damage than Gravel.
 *  - On hit: applies a brief slowdown debuff to the target.
 */
public class SnowProjectileEntity extends ClayProjectileEntity {

    private static final float DAMAGE = 0.8f;

    public SnowProjectileEntity(EntityType<? extends ClayProjectileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public byte getProjectileTypeId() {
        return 2;
    }

    @Override
    public float getDamage() {
        return DAMAGE;
    }

    @Override
    protected void onHitSoldier(ClaySoldierEntity target) {
        super.onHitSoldier(target);
        if (shooter instanceof ClaySoldierEntity shooterSoldier
                && shooterSoldier.hasUpgrade(UpgradeFlags.SNOW)) {
            target.applySnowPayload();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        discard();
        return false;
    }
}
