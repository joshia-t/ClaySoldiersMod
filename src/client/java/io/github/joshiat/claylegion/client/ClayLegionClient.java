package io.github.joshiat.claylegion.client;

import io.github.joshiat.claylegion.client.render.ClaySoldierEntityRenderer;
import io.github.joshiat.claylegion.client.render.DebugPlaceholderEntityRenderer;
import io.github.joshiat.claylegion.client.render.MountEntityRenderer;
import io.github.joshiat.claylegion.client.render.ModEntityModelLayers;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ClayLegionClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModEntityModelLayers.registerModelLayers();
		EntityRendererRegistry.register(EntityRegistry.CLAY_SOLDIER, ClaySoldierEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.HORSE_MOUNT, MountEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.PEGASUS_MOUNT, MountEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.TURTLE_MOUNT, MountEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.BUNNY_MOUNT, MountEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.GECKO_MOUNT, MountEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.GRAVEL_PROJECTILE, DebugPlaceholderEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.SNOW_PROJECTILE, DebugPlaceholderEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.FIRE_CHARGE_PROJECTILE, DebugPlaceholderEntityRenderer::new);
		EntityRendererRegistry.register(EntityRegistry.EMERALD_PROJECTILE, DebugPlaceholderEntityRenderer::new);
	}
}