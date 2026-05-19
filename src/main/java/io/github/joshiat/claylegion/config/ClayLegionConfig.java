package io.github.joshiat.claylegion.config;

import io.github.joshiat.claylegion.entity.CombatTuning;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * YAML-backed config that can be edited outside game and updated in-game via commands.
 */
public final class ClayLegionConfig {

    private static final String FILE_NAME = "claylegion.yml";
    private static final int DEFAULT_NEXUS_MAX_SPAWN_LIMIT = 10;
    private static final int DEFAULT_NEXUS_MAX_SPAWN_COUNT = 10;

    private static int nexusMaxSpawnLimit = DEFAULT_NEXUS_MAX_SPAWN_LIMIT;
    private static int nexusMaxSpawnCount = DEFAULT_NEXUS_MAX_SPAWN_COUNT;

    private ClayLegionConfig() {
    }

    public static int getNexusMaxSpawnLimit() {
        return nexusMaxSpawnLimit;
    }

    public static void setNexusMaxSpawnLimit(int value) {
        nexusMaxSpawnLimit = Math.max(0, value);
    }

    public static int getNexusMaxSpawnCount() {
        return nexusMaxSpawnCount;
    }

    public static void setNexusMaxSpawnCount(int value) {
        nexusMaxSpawnCount = Math.max(0, value);
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void loadAndApply(Logger logger) {
        Path path = getConfigPath();
        try {
            if (!Files.exists(path)) {
                saveRuntimeToDisk(logger);
                return;
            }

            Yaml yaml = new Yaml();
            try (InputStream in = Files.newInputStream(path)) {
                Object loaded = yaml.load(in);
                if (!(loaded instanceof Map<?, ?> root)) {
                    logger.warn("ClayLegion config is not a mapping. Keeping runtime defaults.");
                    return;
                }

                Object combatObj = root.get("combat");
                if (combatObj instanceof Map<?, ?> combat) {
                    setIfNumber(combat.get("maxObstacleClimbHeight"), CombatTuning::setMaxObstacleClimbHeight);
                    setIfNumber(combat.get("jumpAssistVelocity"), CombatTuning::setJumpAssistVelocity);
                    setIfNumber(combat.get("separationRadius"), CombatTuning::setSeparationRadius);
                    setIfNumber(combat.get("separationStrength"), CombatTuning::setSeparationStrength);
                    setIfNumber(combat.get("obstacleStrafeStrength"), CombatTuning::setObstacleStrafeStrength);
                    setIfNumber(combat.get("idleHorizontalBrake"), CombatTuning::setIdleHorizontalBrake);
                    setIfNumber(combat.get("playerDamageMultiplier"), CombatTuning::setPlayerDamageMultiplier);
                    setIfBoolean(combat.get("soldierCollisionEnabled"), CombatTuning::setSoldierCollisionEnabled);
                }

                Object nexusObj = root.get("nexus");
                if (nexusObj instanceof Map<?, ?> nexus) {
                    setIfInt(nexus.get("maxSpawnLimit"), ClayLegionConfig::setNexusMaxSpawnLimit);
                    setIfInt(nexus.get("maxSpawnCount"), ClayLegionConfig::setNexusMaxSpawnCount);
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to load ClayLegion config from {}", path, ex);
        }
    }

    public static void saveRuntimeToDisk(Logger logger) {
        Path path = getConfigPath();
        String yaml = """
                # Clay Legion runtime config
                # Edited in game via /claylegion config set ... or manually while game is closed.
                combat:
                  maxObstacleClimbHeight: %s
                                    jumpAssistVelocity: %s
                                    separationRadius: %s
                                    separationStrength: %s
                                    obstacleStrafeStrength: %s
                  idleHorizontalBrake: %s
                                    playerDamageMultiplier: %s
                                    soldierCollisionEnabled: %s
                                nexus:
                                    maxSpawnLimit: %s
                                    maxSpawnCount: %s
                """.formatted(
                trimFloat(CombatTuning.getMaxObstacleClimbHeight()),
                                trimFloat(CombatTuning.getJumpAssistVelocity()),
                                trimFloat(CombatTuning.getSeparationRadius()),
                                trimFloat(CombatTuning.getSeparationStrength()),
                                trimFloat(CombatTuning.getObstacleStrafeStrength()),
                trimFloat(CombatTuning.getIdleHorizontalBrake()),
                                trimFloat(CombatTuning.getPlayerDamageMultiplier()),
                                Boolean.toString(CombatTuning.isSoldierCollisionEnabled()),
                                Integer.toString(getNexusMaxSpawnLimit()),
                                Integer.toString(getNexusMaxSpawnCount())
        );

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, yaml, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Failed to save ClayLegion config to {}", path, ex);
        }
    }

    private static void setIfNumber(Object raw, FloatConsumer consumer) {
        if (raw instanceof Number n) {
            consumer.accept(n.floatValue());
        }
    }

    private static void setIfBoolean(Object raw, BooleanConsumer consumer) {
        if (raw instanceof Boolean b) {
            consumer.accept(b);
        }
    }

    private static void setIfInt(Object raw, IntConsumer consumer) {
        if (raw instanceof Number n) {
            consumer.accept(n.intValue());
        }
    }

    private static String trimFloat(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }

    @FunctionalInterface
    private interface FloatConsumer {
        void accept(float value);
    }

    @FunctionalInterface
    private interface BooleanConsumer {
        void accept(boolean value);
    }

    @FunctionalInterface
    private interface IntConsumer {
        void accept(int value);
    }
}
