package io.github.joshiat.claylegion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent client → server when the possessing player left-clicks (attack input).
 *
 * <p>The client mixin consumes the vanilla attack key-click before Minecraft can
 * route it through {@code handleInteract}, then sends this packet instead.
 * The server routes it to {@link io.github.joshiat.claylegion.entity.ClaySoldierEntity
 * #possessionTriggerAttack()}.
 *
 * <p>No payload — it is purely a signal.
 */
public record PossessionAttackC2SPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PossessionAttackC2SPacket> TYPE =
        new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath("clay-legion", "possession_attack"));

    public static final StreamCodec<FriendlyByteBuf, PossessionAttackC2SPacket> CODEC =
        StreamCodec.of(
            (buf, pkt) -> { /* no data */ },
            buf -> new PossessionAttackC2SPacket()
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
