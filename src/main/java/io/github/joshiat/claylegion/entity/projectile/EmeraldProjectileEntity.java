package io.github.joshiat.claylegion.entity.projectile;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
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

    @Override
    protected void onHitSoldier(ClaySoldierEntity target) {
        super.onHitSoldier(target);
        // TODO: Apply piercing damage multiplier or armor-piercing effect
        // Emerald projectiles could bypass certain defenses or deal bonus damage
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        discard();
        return false;
    }
}
