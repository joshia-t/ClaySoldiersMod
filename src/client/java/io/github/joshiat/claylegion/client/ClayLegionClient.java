package io.github.joshiat.claylegion.client;

import io.github.joshiat.claylegion.client.render.ClaySoldierEntityRenderer;
import io.github.joshiat.claylegion.client.render.BunnyMountRenderer;
import io.github.joshiat.claylegion.client.render.GeckoMountRenderer;
import io.github.joshiat.claylegion.client.render.HorseMountRenderer;
import io.github.joshiat.claylegion.client.render.MountEntityRenderer;
import io.github.joshiat.claylegion.client.render.ModEntityModelLayers;
import io.github.joshiat.claylegion.client.render.PegasusMountRenderer;
import io.github.joshiat.claylegion.client.render.TurtleMountRenderer;
import io.github.joshiat.claylegion.client.possession.PossessionClientState;
import io.github.joshiat.claylegion.network.PossessionEndS2CPacket;
import io.github.joshiat.claylegion.network.PossessionStartS2CPacket;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class ClayLegionClient implements ClientModInitializer {
	@Override
	@SuppressWarnings("deprecation")
	public void onInitializeClient() {
		ModEntityModelLayers.registerModelLayers();
		EntityRendererRegistry.register(EntityRegistry.CLAY_SOLDIER, ClaySoldierEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.HORSE_MOUNT, HorseMountRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.PEGASUS_MOUNT, PegasusMountRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.TURTLE_MOUNT, TurtleMountRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.BUNNY_MOUNT, BunnyMountRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.GECKO_MOUNT, GeckoMountRenderer::new);
		// Projectiles render their thrown item (snowball-style) — issue #16.
		EntityRendererRegistry.register(EntityRegistry.GRAVEL_PROJECTILE, ThrownItemRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.SNOW_PROJECTILE, ThrownItemRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.FIRE_CHARGE_PROJECTILE, ThrownItemRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.EMERALD_PROJECTILE, ThrownItemRenderer::new);

		registerPacketReceivers();
	}

	private static void registerPacketReceivers() {
		// Possession start: tell the camera mixin to follow this soldier entity.
		ClientPlayNetworking.registerGlobalReceiver(PossessionStartS2CPacket.TYPE,
			(packet, ctx) -> PossessionClientState.onPossessionStart(packet.soldierEntityId()));

		// Possession end: restore camera to the local player.
		ClientPlayNetworking.registerGlobalReceiver(PossessionEndS2CPacket.TYPE,
			(packet, ctx) -> PossessionClientState.onPossessionEnd());

		// PossessionAttackC2SPacket is serverbound — no client receiver needed.
	}
}