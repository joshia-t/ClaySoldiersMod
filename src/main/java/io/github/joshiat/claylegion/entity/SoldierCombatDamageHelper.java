package io.github.joshiat.claylegion.entity;

import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import io.github.joshiat.claylegion.entity.projectile.ClayProjectileEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Encapsulates all damage resolution and knockback logic for clay soldiers.
 *
 * This helper allows damage processing to be tested and tuned independently,
 * and reduces merge conflict surface when multiple contributors work on combat.
 */
public class SoldierCombatDamageHelper {

    private static final double DAMAGE_KNOCKBACK_HORIZONTAL = 0.32;
    private static final double DAMAGE_KNOCKBACK_VERTICAL = 0.25;
    private static final double DAMAGE_KNOCKBACK_VERTICAL_CAP = 0.5;
    private static final double DAMAGE_KNOCKBACK_EPSILON = 1.0E-6;

    /**
     * Apply damage to this soldier from a projectile or non-soldier damage source.
     *
     * @param soldier The target soldier entity.
     * @param amount The damage amount.
     * @param attackingTeam The team ID of the attacker (-1 for non-team).
     * @param attackerEntity The entity dealing damage (optional, used for knockback direction).
     */
    public static void applySoldierDamage(ClaySoldierEntity soldier, float amount, byte attackingTeam, Entity attackerEntity) {
        applySoldierDamage(soldier, amount, attackingTeam, attackerEntity, ClaySoldierEntity.SoldierDamageKind.GENERIC);
    }

    public static void applySoldierDamage(ClaySoldierEntity soldier, float amount, byte attackingTeam,
                                          Entity attackerEntity, ClaySoldierEntity.SoldierDamageKind kind) {
        if (soldier.level().isClientSide() || soldier.isSoldierDead() || amount <= 0.0f) {
            return;
        }

        // Friendly-fire suppression for direct soldier-vs-soldier damage calls.
        if (attackingTeam >= 0 && attackingTeam == (byte) soldier.getTeamId()) {
            return;
        }

        applyResolvedDamage(soldier, amount, attackerEntity, kind);
    }

    /**
     * Apply damage via the combat interaction matrix.
     * Handles mounted vs unmounted combat scenarios with chance-based target resolution.
     *
     * Combat Matrix:
     *  - Unmounted vs Unmounted: 100% damage to target
     *  - Unmounted vs Mounted: 80% to mount, 20% to rider
     *  - Mounted vs Unmounted: 100% damage to target + mount modifier
     *  - Mounted vs Mounted: 50/50 split between rider and mount
     */
    public static void applyCombatDamage(ClaySoldierEntity target, float rawDamage, ClaySoldierEntity attacker) {
        applyCombatDamage(target, rawDamage, attacker, -1.0f, 1.0f);
    }

    public static void applyCombatDamage(ClaySoldierEntity target, float rawDamage, ClaySoldierEntity attacker,
                                         float riderHitChanceOverride, float rawDamageMultiplier) {
        applyCombatDamage(target, rawDamage, attacker, riderHitChanceOverride, rawDamageMultiplier,
            ClaySoldierEntity.SoldierDamageKind.MELEE);
    }

    public static void applyCombatDamage(ClaySoldierEntity target, float rawDamage, ClaySoldierEntity attacker,
                                         float riderHitChanceOverride, float rawDamageMultiplier,
                                         ClaySoldierEntity.SoldierDamageKind kind) {
        if (target.level().isClientSide() || target.isSoldierDead() || rawDamage <= 0.0f) {
            return;
        }

        // Friendly-fire suppression — bypassed for nether wart berserkers.
        if (attacker != null && attacker.getTeamId() == target.getTeamId() && !attacker.ignoresTeamLines()) {
            return;
        }

        boolean attackerMounted = attacker != null && attacker.getVehicle() != null;
        boolean targetMounted = target.getVehicle() != null;
        float resolvedDamage = rawDamage * Math.max(0.0f, rawDamageMultiplier);

        if (!targetMounted) {
            // Target is infantry: take full damage
            applyResolvedDamage(target, resolvedDamage, attacker, kind);
            return;
        }

        // Target is mounted: resolve via chance-based rider/mount roll.
        float roll = target.getRandom().nextFloat();
        float riderHitChance = attackerMounted ? 0.50f : 0.20f;
        if (riderHitChanceOverride >= 0.0f) {
            riderHitChance = Mth.clamp(riderHitChanceOverride, 0.0f, 1.0f);
        }

        Entity mount = target.getVehicle();
        if (roll < riderHitChance) {
            // Damage bypasses mount and hits the rider directly.
            applyResolvedDamage(target, resolvedDamage, attacker, kind);
        } else {
            // Delegate damage to the mount.
            if (mount instanceof BaseMountEntity legacyMount) {
                legacyMount.applyMountDamage(resolvedDamage, attacker);
            }
        }
    }

    /**
     * Internal: process direct damage and knockback.
     */
    private static void applyResolvedDamage(ClaySoldierEntity soldier, float amount, Entity attackerEntity,
                                            ClaySoldierEntity.SoldierDamageKind kind) {
        amount = soldier.applyDefensiveUpgrades(amount, kind, attackerEntity);
        if (amount <= 0.0f) {
            return;
        }

        float newHealth = soldier.getSoldierHealth() - amount;
        soldier.setSoldierHealth(newHealth);
        soldier.setHurtFlashTicks(8);  // HURT_FLASH_DURATION
        ClaySoldierEntity attackingSoldier = resolveAttackingSoldier(attackerEntity);
        soldier.aggroOnHit(attackingSoldier);
        applyDamageKnockback(soldier, attackerEntity);

        if (newHealth <= 0f && soldier.level() instanceof ServerLevel serverLevel) {
            // Magma cream: a melee killer is handed a ticking time bomb.
            if (kind == ClaySoldierEntity.SoldierDamageKind.MELEE
                && attackingSoldier != null
                && soldier.hasUpgrade(io.github.joshiat.claylegion.entity.upgrade.UpgradeFlags.MAGMA_CREAM)) {
                attackingSoldier.applyTimeBomb();
            }
            soldier.onSoldierKilled(serverLevel);
        } else {
            soldier.runPostDamageTriggers();
        }
    }

    private static ClaySoldierEntity resolveAttackingSoldier(Entity attackerEntity) {
        if (attackerEntity instanceof ClaySoldierEntity attackerSoldier) {
            return attackerSoldier;
        }
        if (attackerEntity instanceof ClayProjectileEntity projectile
            && projectile.getShooter() instanceof ClaySoldierEntity shooterSoldier) {
            return shooterSoldier;
        }
        return null;
    }

    /**
     * Apply directional knockback impulse away from attacker.
     */
    private static void applyDamageKnockback(ClaySoldierEntity soldier, Entity attackerEntity) {
        if (attackerEntity == null || soldier.isPassenger()) {
            return;
        }

        // Iron ingot doubles knockback dealt; iron ingot/brick on the target resist it.
        double knockbackScale = soldier.getKnockbackResistFactor();
        if (attackerEntity instanceof ClaySoldierEntity attackingSoldier) {
            knockbackScale *= attackingSoldier.getKnockbackDealtFactor();
        }
        if (knockbackScale <= 0.0) {
            return;
        }

        double dx = soldier.getX() - attackerEntity.getX();
        double dz = soldier.getZ() - attackerEntity.getZ();
        double lenSq = dx * dx + dz * dz;

        if (lenSq < DAMAGE_KNOCKBACK_EPSILON) {
            dx = (soldier.getRandom().nextDouble() - 0.5) * 0.02;
            dz = (soldier.getRandom().nextDouble() - 0.5) * 0.02;
            lenSq = dx * dx + dz * dz;
            if (lenSq < DAMAGE_KNOCKBACK_EPSILON) {
                return;
            }
        }

        double invLen = 1.0 / Math.sqrt(lenSq);
        dx *= invLen;
        dz *= invLen;

        Vec3 velocity = soldier.getDeltaMovement();
        double nextY = Math.min(DAMAGE_KNOCKBACK_VERTICAL_CAP,
            velocity.y + DAMAGE_KNOCKBACK_VERTICAL * knockbackScale);
        soldier.setDeltaMovement(
            velocity.x + dx * DAMAGE_KNOCKBACK_HORIZONTAL * knockbackScale,
            nextY,
            velocity.z + dz * DAMAGE_KNOCKBACK_HORIZONTAL * knockbackScale
        );
    }
}
