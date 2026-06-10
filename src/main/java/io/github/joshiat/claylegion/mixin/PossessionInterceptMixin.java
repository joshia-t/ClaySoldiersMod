package io.github.joshiat.claylegion.mixin;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.possession.SoldierPossessionManager;
import io.github.joshiat.claylegion.entity.possession.PossessionSession;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels all entity-interact packets from a possessing player.
 *
 * <p>When the camera is redirected to the clay soldier via {@code setCameraEntity},
 * the client raycasts from the soldier's position and may find entities that are
 * too far from the player's actual body. The server's validation then rejects the
 * attack, producing "attempting to attack invalid entity" and potentially crashing.
 *
 * <p>We cancel the packet entirely — the client-side {@link PossessionCameraMixin}
 * already drains the attack input and sends a clean {@link
 * io.github.joshiat.claylegion.network.PossessionAttackC2SPacket} instead.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PossessionInterceptMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void claylegion$blockPossessionInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (!SoldierPossessionManager.getInstance().isPossessing(player)) return;
        // Cancel the vanilla attack/interact — the C2S possession-attack packet
        // (sent by the client mixin) handles the intentional attack path separately.
        ci.cancel();
    }
}
