package io.github.joshiat.claylegion.client.mixin;

import io.github.joshiat.claylegion.client.possession.PossessionClientState;
import io.github.joshiat.claylegion.network.PossessionAttackC2SPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side possession tick hook — handles both camera redirection and input
 * interception while a possession session is active.
 *
 * <h3>Camera</h3>
 * Calls {@code Minecraft.setCameraEntity(soldier)} each tick so the view follows
 * the controlled soldier naturally (same path as spectator mode).
 *
 * <h3>Attack input</h3>
 * Drains the attack key's buffered clicks <em>before</em> Minecraft's own
 * {@code tick()} loop reads them. This prevents Minecraft from sending a
 * {@code ServerboundInteractPacket} (which would crash because the raycast
 * originates from the soldier's position, not the player's body). For each
 * consumed click we send {@link PossessionAttackC2SPacket} instead, which the
 * server routes to the soldier's attack logic.
 *
 * <p>We inject at {@code HEAD} so our consume-loop runs before any downstream
 * vanilla code in the same tick method.
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public abstract class PossessionCameraMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void claylegion$possessionTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.level == null || mc.player == null) return;

        int possessedId = PossessionClientState.getPossessedEntityId();

        // ── No possession: make sure camera is on the player ──────────────
        if (possessedId == -1) {
            if (mc.getCameraEntity() != mc.player) {
                mc.setCameraEntity(mc.player);
            }
            return;
        }

        // ── Soldier entity lookup ─────────────────────────────────────────
        Entity soldier = mc.level.getEntity(possessedId);
        if (soldier == null || soldier.isRemoved()) {
            // Soldier left client visibility — end gracefully.
            PossessionClientState.onPossessionEnd();
            mc.setCameraEntity(mc.player);
            return;
        }

        // ── Redirect camera to soldier ────────────────────────────────────
        if (mc.getCameraEntity() != soldier) {
            mc.setCameraEntity(soldier);
        }

        // ── Intercept attack input ────────────────────────────────────────
        //
        // Vanilla Minecraft's tick() loop does:
        //   while (options.keyAttack.consumeClick()) { startAttack(); }
        // We consume those clicks HERE (before vanilla's loop) and send our
        // own C2S packet instead, so vanilla never sees the clicks and never
        // sends the problematic ServerboundInteractPacket.
        //
        // No need to guard with mc.screen == null — consumeClick() already
        // returns false when a screen is open.
        while (mc.options.keyAttack.consumeClick()) {
            if (ClientPlayNetworking.canSend(PossessionAttackC2SPacket.TYPE)) {
                ClientPlayNetworking.send(new PossessionAttackC2SPacket());
            }
        }
    }
}
