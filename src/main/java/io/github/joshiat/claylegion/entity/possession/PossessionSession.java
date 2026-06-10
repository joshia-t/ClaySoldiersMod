package io.github.joshiat.claylegion.entity.possession;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * State of an active soldier possession ("remote control") session.
 *
 * <p>Holds direct references rather than UUIDs so the manager (and game tests
 * using mock players) can tick sessions without a player-list lookup. Sessions
 * are transient — never persisted across saves or reconnects.
 *
 * @param player  the possessing player, whose body stays frozen at the anchor
 * @param soldier the controlled soldier
 * @param anchorPosition world position where the player's body is frozen
 */
public record PossessionSession(
    ServerPlayer player,
    ClaySoldierEntity soldier,
    Vec3 anchorPosition
) {}
