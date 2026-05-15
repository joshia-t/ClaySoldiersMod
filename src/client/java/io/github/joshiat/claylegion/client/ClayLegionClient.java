package io.github.joshiat.claylegion.client;

import io.github.joshiat.claylegion.client.render.ClaySoldierEntityRenderer;
import io.github.joshiat.claylegion.client.render.ModEntityModelLayers;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ClayLegionClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModEntityModelLayers.registerModelLayers();
		EntityRendererRegistry.register(EntityRegistry.CLAY_SOLDIER, ClaySoldierEntityRenderer::new);
	}
}