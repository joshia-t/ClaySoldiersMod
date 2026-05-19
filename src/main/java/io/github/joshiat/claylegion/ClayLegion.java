package io.github.joshiat.claylegion;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.joshiat.claylegion.config.ClayLegionConfig;
import io.github.joshiat.claylegion.entity.CombatTuning;
import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.entity.RuntimeTelemetry;
import io.github.joshiat.claylegion.entity.TargetingProfiler;
import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import io.github.joshiat.claylegion.entity.mount.TurtleMountEntity;
import io.github.joshiat.claylegion.registry.BlockEntityRegistry;
import io.github.joshiat.claylegion.registry.BlockRegistry;
import io.github.joshiat.claylegion.registry.CreativeTabRegistry;
import io.github.joshiat.claylegion.registry.EntityRegistry;
import io.github.joshiat.claylegion.registry.ItemRegistry;
import io.github.joshiat.claylegion.render.RenderTuning;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ClayLegion implements ModInitializer {
	public static final String MOD_ID = "clay-legion";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		BlockRegistry.init();
		BlockEntityRegistry.init();
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
				.then(literal("debug")
					.then(literal("inspect")
						.executes(ctx -> debugInspect(ctx.getSource())))
					.then(literal("inspectmount")
						.executes(ctx -> debugInspectMount(ctx.getSource()))))
				.then(literal("profiler")
					.then(literal("enable")
						.executes(ctx -> {
							TargetingProfiler.setEnabled(true);
							ctx.getSource().sendSuccess(() -> Component.literal("TargetingProfiler enabled."), false);
							return 1;
						}))
					.then(literal("disable")
						.executes(ctx -> {
							TargetingProfiler.setEnabled(false);
							ctx.getSource().sendSuccess(() -> Component.literal("TargetingProfiler disabled."), false);
							return 1;
						}))
					.then(literal("report")
						.executes(ctx -> {
							TargetingProfiler.printReport();
							ctx.getSource().sendSuccess(() -> Component.literal("TargetingProfiler report printed to console."), false);
							return 1;
						})))
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
						.then(literal("nexusMaxSpawnLimit")
							.then(argument("value", IntegerArgumentType.integer(0))
								.executes(ctx -> {
									ClayLegionConfig.setNexusMaxSpawnLimit(IntegerArgumentType.getInteger(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
						.then(literal("nexusMaxSpawnCount")
							.then(argument("value", IntegerArgumentType.integer(0))
								.executes(ctx -> {
									ClayLegionConfig.setNexusMaxSpawnCount(IntegerArgumentType.getInteger(ctx, "value"));
									ClayLegionConfig.saveRuntimeToDisk(LOGGER);
									return sendCurrentConfig(ctx.getSource());
								})))
					)
				)
			)
		);
	}

	private static int debugInspect(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception e) {
			source.sendFailure(Component.literal("This command can only be used by a player."));
			return 0;
		}

		ClaySoldierEntity soldier = raycastSoldier(player, 12.0D);
		if (soldier == null) {
			source.sendFailure(Component.literal("No Clay Soldier targeted."));
			return 0;
		}

		String message = "ClaySoldier inspect | activeUpgrades=0x"
			+ Long.toHexString(soldier.getActiveUpgrades()).toUpperCase(Locale.ROOT)
			+ ", health=" + String.format(Locale.ROOT, "%.2f", soldier.getSoldierHealth())
			+ ", combatState=" + soldier.getAiState().name()
			+ ", " + RuntimeTelemetry.snapshot();

		Component inspectMessage = Component.literal(message);
		source.sendSuccess(() -> inspectMessage, false);
		return 1;
	}

	private static ClaySoldierEntity raycastSoldier(ServerPlayer player, double maxDistance) {
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(player.getViewVector(1.0f).scale(maxDistance));

		HitResult blockHit = player.level().clip(new ClipContext(
			start,
			end,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			player
		));
		double bestDistSq = maxDistance * maxDistance;
		if (blockHit.getType() != HitResult.Type.MISS) {
			end = blockHit.getLocation();
			bestDistSq = start.distanceToSqr(end);
		}

		AABB scanBox = new AABB(start, end).inflate(1.0D);
		List<ClaySoldierEntity> soldiers = player.level().getEntitiesOfClass(
			ClaySoldierEntity.class,
			scanBox,
			e -> e.isAlive() && !e.isRemoved()
		);

		ClaySoldierEntity best = null;
		for (ClaySoldierEntity candidate : soldiers) {
			Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.2D).clip(start, end);
			if (hit.isEmpty()) {
				continue;
			}

			double distSq = start.distanceToSqr(hit.get());
			if (distSq <= bestDistSq) {
				bestDistSq = distSq;
				best = candidate;
			}
		}

		return best;
	}

	private static int debugInspectMount(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception e) {
			source.sendFailure(Component.literal("This command can only be used by a player."));
			return 0;
		}

		BaseMountEntity mount = raycastMount(player, 12.0D);
		if (mount == null) {
			source.sendFailure(Component.literal("No mount targeted."));
			return 0;
		}

		Vec3 v = mount.getDeltaMovement();
		double speed = Math.sqrt(v.x * v.x + v.z * v.z);
		double waterDepth = mount.getFluidHeight(FluidTags.WATER);
		boolean turtle = mount instanceof TurtleMountEntity;

		String message = "Mount inspect | type=" + mount.getClass().getSimpleName()
			+ ", health=" + String.format(Locale.ROOT, "%.2f/%.2f", mount.getHealth(), mount.getMaxHealth())
			+ ", waterDepth=" + String.format(Locale.ROOT, "%.2f", waterDepth)
			+ ", speedXZ=" + String.format(Locale.ROOT, "%.3f", speed)
			+ ", passengers=" + mount.getPassengers().size()
			+ ", turtleMitigation=" + (turtle ? "0.5x" : "none")
			+ ", " + RuntimeTelemetry.snapshot();

		source.sendSuccess(() -> Component.literal(message), false);
		return 1;
	}

	private static BaseMountEntity raycastMount(ServerPlayer player, double maxDistance) {
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(player.getViewVector(1.0f).scale(maxDistance));

		HitResult blockHit = player.level().clip(new ClipContext(
			start,
			end,
			ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE,
			player
		));
		double bestDistSq = maxDistance * maxDistance;
		if (blockHit.getType() != HitResult.Type.MISS) {
			end = blockHit.getLocation();
			bestDistSq = start.distanceToSqr(end);
		}

		AABB scanBox = new AABB(start, end).inflate(1.0D);
		List<BaseMountEntity> mounts = player.level().getEntitiesOfClass(
			BaseMountEntity.class,
			scanBox,
			e -> e.isAlive() && !e.isRemoved()
		);

		BaseMountEntity best = null;
		for (BaseMountEntity candidate : mounts) {
			Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.2D).clip(start, end);
			if (hit.isEmpty()) {
				continue;
			}

			double distSq = start.distanceToSqr(hit.get());
			if (distSq <= bestDistSq) {
				bestDistSq = distSq;
				best = candidate;
			}
		}

		return best;
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
				+ ", nexus.maxSpawnLimit=" + ClayLegionConfig.getNexusMaxSpawnLimit()
				+ ", nexus.maxSpawnCount=" + ClayLegionConfig.getNexusMaxSpawnCount()
		), false);
		return 1;
	}
}