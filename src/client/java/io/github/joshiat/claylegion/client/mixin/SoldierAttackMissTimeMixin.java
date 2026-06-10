package io.github.joshiat.claylegion.client.mixin;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fix for issue #1: the killing blow on a clay soldier frequently destroys the
 * block behind it in creative mode.
 *
 * <p>Soldiers are tiny and die in one or two hits. With the attack button held,
 * the soldier disappears mid-click and {@code continueAttack} immediately
 * re-raycasts, now hitting the block behind — which creative insta-breaks.
 *
 * <p>After any attack that targets a clay soldier we set a short {@code missTime}
 * cooldown (the same mechanism vanilla uses after a swing hits nothing), which
 * suppresses block-breaking from the still-held click for a few ticks.
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public abstract class SoldierAttackMissTimeMixin {

    @Shadow protected int missTime;

    @Inject(method = "startAttack", at = @At("TAIL"))
    private void claylegion$cooldownAfterSoldierAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof ClaySoldierEntity) {
            this.missTime = Math.max(this.missTime, 5);
        }
    }
}
