package io.github.joshiat.claylegion;

import com.mojang.brigadier.arguments.FloatArgumentType;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import io.github.joshiat.claylegion.registry.ItemRegistry;
import io.github.joshiat.claylegion.render.RenderTuning;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ClayLegion implements ModInitializer {
	public static final String MOD_ID = "clay-legion";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		EntityRegistry.init();
		ItemRegistry.init();
		registerCommands();
		LOGGER.info("Clay Legion initialized.");
	}

	private static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("clayrender")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(literal("show")
					.executes(ctx -> sendCurrentTuning(ctx.getSource())))
				.then(literal("scale")
					.then(argument("value", FloatArgumentType.floatArg(0.05f, 3.0f))
						.executes(ctx -> {
							RenderTuning.setScale(FloatArgumentType.getFloat(ctx, "value"));
							return sendCurrentTuning(ctx.getSource());
						})))
				.then(literal("yoffset")
					.then(argument("value", FloatArgumentType.floatArg(-2.0f, 2.0f))
						.executes(ctx -> {
							RenderTuning.setYOffset(FloatArgumentType.getFloat(ctx, "value"));
							return sendCurrentTuning(ctx.getSource());
						})))
				.then(literal("reset")
					.executes(ctx -> {
						RenderTuning.reset();
						return sendCurrentTuning(ctx.getSource());
					}))
			)
		);
	}

	private static int sendCurrentTuning(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal(
			"Clay render tuning: scale=" + RenderTuning.getScale() + ", yoffset=" + RenderTuning.getYOffset()
		), false);
		return 1;
	}
}