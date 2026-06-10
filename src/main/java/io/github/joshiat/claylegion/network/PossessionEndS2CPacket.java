package io.github.joshiat.claylegion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent server → client to signal that the active possession session has ended.
 *
 * <p>The client resets {@code Minecraft.cameraEntity} back to the local player
 * and re-enables normal input.
 */
public record PossessionEndS2CPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PossessionEndS2CPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("clay-legion", "possession_end"));

    /** No payload — the packet is just a signal. */
    public static final StreamCodec<FriendlyByteBuf, PossessionEndS2CPacket> CODEC =
        StreamCodec.of(
            (buf, pkt) -> { /* nothing to write */ },
            buf -> new PossessionEndS2CPacket()
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
