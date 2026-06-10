package io.github.joshiat.claylegion.mixin;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Splash/lingering potion support for clay soldiers (issue #38).
 *
 * <p>Vanilla potion splashes only iterate LivingEntity, so non-living soldiers
 * were unaffected. This maps the sensible effects onto soldiers within the
 * vanilla 4-block splash range, scaled by proximity like vanilla:
 * instant damage/health apply scaled damage/healing, poison and slowness map
 * onto the soldiers' own status-effect system. Other effects have no
 * non-living equivalent and are ignored.
 */
@Mixin(AbstractThrownPotion.class)
public abstract class PotionSplashSoldierMixin {

    @Inject(method = "onHit", at = @At("TAIL"))
    private void claylegion$splashSoldiers(HitResult hitResult, CallbackInfo ci) {
        AbstractThrownPotion self = (AbstractThrownPotion) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        PotionContents contents = self.getItem().get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }

        AABB splashBox = self.getBoundingBox().inflate(4.0, 2.0, 4.0);
        List<ClaySoldierEntity> soldiers = serverLevel.getEntitiesOfClass(
            ClaySoldierEntity.class, splashBox,
            s -> s.isAlive() && !s.isRemoved() && !s.isSoldierDead());
        if (soldiers.isEmpty()) {
            return;
        }

        for (MobEffectInstance effect : contents.getAllEffects()) {
            for (ClaySoldierEntity soldier : soldiers) {
                double proximity = 1.0 - Math.sqrt(self.distanceToSqr(soldier)) / 4.0;
                if (proximity <= 0.0) {
                    continue;
                }

                int strength = effect.getAmplifier() + 1;
                if (effect.is(MobEffects.INSTANT_DAMAGE)) {
                    soldier.applySoldierDamage((float) (6.0 * strength * proximity), (byte) -1,
                        self, ClaySoldierEntity.SoldierDamageKind.GENERIC);
                } else if (effect.is(MobEffects.INSTANT_HEALTH)) {
                    soldier.setSoldierHealth(Math.min(soldier.getSoldierMaxHealth(),
                        soldier.getSoldierHealth() + (float) (4.0 * strength * proximity)));
                } else if (effect.is(MobEffects.POISON)) {
                    soldier.applyPoison();
                } else if (effect.is(MobEffects.SLOWNESS)) {
                    soldier.applySnowPayload();
                }
            }
        }
    }
}
