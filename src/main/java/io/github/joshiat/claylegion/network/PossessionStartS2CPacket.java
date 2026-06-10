package io.github.joshiat.claylegion.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent server → client when a player begins possessing a clay soldier.
 *
 * <p>The client uses the {@code soldierEntityId} to locate the entity in the
 * level and redirect {@code Minecraft.cameraEntity} to it.
 */
public record PossessionStartS2CPacket(int soldierEntityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PossessionStartS2CPacket> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("clay-legion", "possession_start"));

    public static final StreamCodec<FriendlyByteBuf, PossessionStartS2CPacket> CODEC =
        StreamCodec.of(
            (buf, pkt) -> buf.writeVarInt(pkt.soldierEntityId),
            buf -> new PossessionStartS2CPacket(buf.readVarInt())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
