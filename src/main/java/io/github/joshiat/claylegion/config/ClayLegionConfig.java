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

    private ClayLegionConfig() {
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
                    setIfNumber(combat.get("idleHorizontalBrake"), CombatTuning::setIdleHorizontalBrake);
                    setIfNumber(combat.get("playerDamageMultiplier"), CombatTuning::setPlayerDamageMultiplier);
                    setIfBoolean(combat.get("soldierCollisionEnabled"), CombatTuning::setSoldierCollisionEnabled);
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
                  idleHorizontalBrake: %s
                  playerDamageMultiplier: %s
                                    soldierCollisionEnabled: %s
                """.formatted(
                trimFloat(CombatTuning.getMaxObstacleClimbHeight()),
                                trimFloat(CombatTuning.getJumpAssistVelocity()),
                trimFloat(CombatTuning.getIdleHorizontalBrake()),
                                trimFloat(CombatTuning.getPlayerDamageMultiplier()),
                                Boolean.toString(CombatTuning.isSoldierCollisionEnabled())
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
}
