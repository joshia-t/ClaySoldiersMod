package io.github.joshiat.claylegion;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import io.github.joshiat.claylegion.config.ClayLegionConfig;
import io.github.joshiat.claylegion.entity.CombatTuning;
import io.github.joshiat.claylegion.registry.CreativeTabRegistry;
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
		CreativeTabRegistry.init();
		ClayLegionConfig.loadAndApply(LOGGER);
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

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(literal("claylegion")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(literal("config")
					.then(literal("show")
						.executes(ctx -> sendCurrentConfig(ctx.getSource())))
					.then(literal("reset")
						.executes(ctx -> {
							CombatTuning.reset();
							ClayLegionConfig.saveRuntimeToDisk(LOGGER);
							ctx.getSource().sendSuccess(() -> Component.literal("ClayLegion config reset to defaults."), false);
							return sendCurrentConfig(ctx.getSource());
						}))
					.then(literal("reload")
						.executes(ctx -> {
							ClayLegionConfig.loadAndApply(LOGGER);
							ctx.getSource().sendSuccess(() -> Component.literal("ClayLegion config reloaded from disk."), false);
							return sendCurrentConfig(ctx.getSource());
						}))
					.then(literal("save")
						.executes(ctx -> {
							ClayLegionConfig.saveRuntimeToDisk(LOGGER);
							ctx.getSource().sendSuccess(() -> Component.literal("ClayLegion config saved to " + ClayLegionConfig.getConfigPath()), false);
							return 1;
						}))
					.then(literal("set")
						.then(literal("playerDamageMultiplier")
							.then(argument("value", FloatArgumentType.floatArg(0.0f, 10.0f))
								.executes(ctx -> {
									CombatTuning.setPlayerDamageMultiplier(FloatArgumentType.getFloat(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
						.then(literal("maxObstacleClimbHeight")
							.then(argument("value", FloatArgumentType.floatArg(0.0f, 3.0f))
								.executes(ctx -> {
									CombatTuning.setMaxObstacleClimbHeight(FloatArgumentType.getFloat(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
						.then(literal("jumpAssistVelocity")
							.then(argument("value", FloatArgumentType.floatArg(0.2f, 0.8f))
								.executes(ctx -> {
									CombatTuning.setJumpAssistVelocity(FloatArgumentType.getFloat(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
						.then(literal("separationRadius")
							.then(argument("value", FloatArgumentType.floatArg(0.2f, 1.5f))
								.executes(ctx -> {
									CombatTuning.setSeparationRadius(FloatArgumentType.getFloat(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
						.then(literal("separationStrength")
							.then(argument("value", FloatArgumentType.floatArg(0.0f, 0.2f))
								.executes(ctx -> {
									CombatTuning.setSeparationStrength(FloatArgumentType.getFloat(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
						.then(literal("obstacleStrafeStrength")
							.then(argument("value", FloatArgumentType.floatArg(0.0f, 0.2f))
								.executes(ctx -> {
									CombatTuning.setObstacleStrafeStrength(FloatArgumentType.getFloat(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
						.then(literal("idleHorizontalBrake")
							.then(argument("value", FloatArgumentType.floatArg(0.1f, 0.98f))
								.executes(ctx -> {
									CombatTuning.setIdleHorizontalBrake(FloatArgumentType.getFloat(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
						.then(literal("soldierCollisionEnabled")
							.then(argument("value", BoolArgumentType.bool())
								.executes(ctx -> {
									CombatTuning.setSoldierCollisionEnabled(BoolArgumentType.getBool(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
					)
				)
			)
		);
	}

	private static int sendCurrentTuning(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal(
			"Clay render tuning: scale=" + RenderTuning.getScale() + ", yoffset=" + RenderTuning.getYOffset()
		), false);
		return 1;
	}

	private static int sendCurrentConfig(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal("ClayLegion config: " + ClayLegionConfig.getConfigPath()), false);
		source.sendSuccess(() -> Component.literal(
			"combat.maxObstacleClimbHeight=" + CombatTuning.getMaxObstacleClimbHeight()
				+ ", combat.jumpAssistVelocity=" + CombatTuning.getJumpAssistVelocity()
				+ ", combat.separationRadius=" + CombatTuning.getSeparationRadius()
				+ ", combat.separationStrength=" + CombatTuning.getSeparationStrength()
				+ ", combat.obstacleStrafeStrength=" + CombatTuning.getObstacleStrafeStrength()
				+ ", combat.idleHorizontalBrake=" + CombatTuning.getIdleHorizontalBrake()
				+ ", combat.playerDamageMultiplier=" + CombatTuning.getPlayerDamageMultiplier()
				+ ", combat.soldierCollisionEnabled=" + CombatTuning.isSoldierCollisionEnabled()
		), false);
		return 1;
	}
}