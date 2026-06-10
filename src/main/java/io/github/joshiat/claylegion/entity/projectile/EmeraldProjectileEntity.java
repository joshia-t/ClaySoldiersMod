package io.github.joshiat.claylegion.entity.projectile;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.upgrade.UpgradeFlags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Emerald projectile: High-damage, piercing projectile.
 *
 * Characteristics:
 *  - Highest base damage of all projectile types.
 *  - Applies a high-piercing damage multiplier.
 *  - Can be used for armor-piercing or critical strikes.
 */
public class EmeraldProjectileEntity extends ClayProjectileEntity {

    private static final float DAMAGE = 3.0f;  // Highest damage tier

    public EmeraldProjectileEntity(EntityType<? extends ClayProjectileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public byte getProjectileTypeId() {
        return 4;
    }

    @Override
    public float getDamage() {
        return DAMAGE;
    }

    /** Emeralds punch through the first target and can strike a second (issue #15). */
    @Override
    protected int getMaxPierces() {
        return 1;
    }

    @Override
    protected void onHitSoldier(ClaySoldierEntity target) {
        if (shooter instanceof ClaySoldierEntity shooterSoldier
                && shooterSoldier.hasUpgrade(UpgradeFlags.EMERALD)) {
            target.applyEmeraldPayload(shooterSoldier, getDamage());
            return;
        }

        super.onHitSoldier(target);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        discard();
        return false;
    }
}
