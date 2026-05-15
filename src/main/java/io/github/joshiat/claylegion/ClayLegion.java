package io.github.joshiat.claylegion;

import io.github.joshiat.claylegion.registry.EntityRegistry;
import io.github.joshiat.claylegion.registry.ItemRegistry;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClayLegion implements ModInitializer {
	public static final String MOD_ID = "clay-legion";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		EntityRegistry.init();
		ItemRegistry.init();
		LOGGER.info("Clay Legion initialized.");
	}
}