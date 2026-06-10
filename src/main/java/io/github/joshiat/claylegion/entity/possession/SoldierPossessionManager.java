package io.github.joshiat.claylegion.entity.possession;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.network.PossessionEndS2CPacket;
import io.github.joshiat.claylegion.network.PossessionStartS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side manager for the soldier possession ("remote control") mechanic.
 *
 * <p>While possessing, the player's body is frozen at an anchor position and
 * stays fully vulnerable. Each tick the manager:
 * <ol>
 *   <li>Reads how far the player's position drifted from the anchor (the WASD
 *       movement the client sent this tick) and applies it as soldier velocity.</li>
 *   <li>Translates upward drift (jump input) into a soldier hop.</li>
 *   <li>Mirrors the player's look yaw onto the soldier.</li>
 *   <li>Snaps the player body back to the anchor.</li>
 * </ol>
 *
 * <p>Exit conditions: sneak, the player body taking damage (forced soul
 * return), the soldier dying or despawning, or the player disconnecting.
 * Left-click attacks arrive via {@code PossessionAttackC2SPacket}.
 */
public final class SoldierPossessionManager {

    private static final SoldierPossessionManager INSTANCE = new SoldierPossessionManager();

    // Player walk speed and soldier chase speed are both ~0.1 blocks/tick,
    // so 1:1 forwarding gives natural control feel.
    private static final double MOVEMENT_SCALE = 1.0;
    private static final double MAX_CONTROL_SPEED = 0.12;
    private static final double JUMP_INPUT_THRESHOLD = 0.05;
    private static final double JUMP_VELOCITY = 0.42;

    // Maps possessing player UUID → active session.
    private final Map<UUID, PossessionSession> sessions = new HashMap<>();

    private SoldierPossessionManager() {}

    public static SoldierPossessionManager getInstance() {
        return INSTANCE;
    }

    // ── Session control ────────────────────────────────────────────────────

    /**
     * Starts a possession session: anchors the player's body, suppresses the
     * soldier's AI, and redirects the client camera. Interacting again while
     * already possessing toggles the session off.
     */
    public void startPossession(Player player, ClaySoldierEntity soldier) {
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }

        UUID playerId = player.getUUID();
        if (sessions.containsKey(playerId)) {
            endPossession(sp);
            return;
        }
        if (soldier.isSoldierDead() || soldier.isRemoved() || soldier.isPossessed()) {
            return;
        }

        sessions.put(playerId, new PossessionSession(sp, soldier, sp.position()));
        soldier.setPossessed(true);
        if (sp.connection != null) {
            ServerPlayNetworking.send(sp, new PossessionStartS2CPacket(soldier.getId()));
        }
    }

    /** Ends the active session for {@code player}, restoring body and soldier AI. */
    public void endPossession(ServerPlayer player) {
        PossessionSession session = sessions.remove(player.getUUID());
        if (session != null) {
            releaseSession(session);
        }
    }

    public boolean isPossessing(Player player) {
        return sessions.containsKey(player.getUUID());
    }

    public PossessionSession getSession(Player player) {
        return sessions.get(player.getUUID());
    }

    /** Drops every session without notifying — used when the server stops. */
    public void clearAll() {
        sessions.clear();
    }

    // ── Per-tick logic ─────────────────────────────────────────────────────

    /**
     * Called once per server tick from {@code ServerTickEvents.END_SERVER_TICK}.
     */
    public void tick(MinecraftServer server) {
        if (sessions.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, PossessionSession>> iter = sessions.entrySet().iterator();
        while (iter.hasNext()) {
            PossessionSession session = iter.next().getValue();
            ServerPlayer player = session.player();
            ClaySoldierEntity soldier = session.soldier();

            // ── Exit conditions ────────────────────────────────────────────
            boolean playerGone = player.isRemoved() || player.hasDisconnected();
            boolean soldierGone = soldier.isRemoved() || soldier.isSoldierDead();
            boolean forcedReturn = player.hurtTime > 0; // body took damage → soul snaps back
            if (playerGone || soldierGone || forcedReturn || player.isShiftKeyDown()) {
                iter.remove();
                releaseSession(session);
                continue;
            }

            // ── Relay movement to soldier ──────────────────────────────────
            // The client moved the player away from the anchor; that delta IS
            // this tick's WASD/jump intent. Forward it, then snap the body back.
            Vec3 anchor = session.anchorPosition();
            Vec3 delta = player.position().subtract(anchor);

            if (delta.horizontalDistanceSqr() > 1.0e-6) {
                double sx = Mth.clamp(delta.x * MOVEMENT_SCALE, -MAX_CONTROL_SPEED, MAX_CONTROL_SPEED);
                double sz = Mth.clamp(delta.z * MOVEMENT_SCALE, -MAX_CONTROL_SPEED, MAX_CONTROL_SPEED);
                Vec3 prev = soldier.getDeltaMovement();
                soldier.setDeltaMovement(sx, prev.y, sz);
            }
            if (delta.y > JUMP_INPUT_THRESHOLD && soldier.onGround()) {
                Vec3 prev = soldier.getDeltaMovement();
                soldier.setDeltaMovement(prev.x, JUMP_VELOCITY, prev.z);
            }

            // Face where the player is looking.
            soldier.setYRot(player.getYRot());
            soldier.setYHeadRot(player.getYHeadRot());
            soldier.setYBodyRot(player.getYRot());

            // Snap the body back to the anchor — it stays put (and vulnerable).
            snapToAnchor(player, anchor);
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private void releaseSession(PossessionSession session) {
        session.soldier().setPossessed(false);

        ServerPlayer player = session.player();
        if (!player.isRemoved() && !player.hasDisconnected()) {
            Vec3 anchor = session.anchorPosition();
            snapToAnchor(player, anchor);
            if (player.connection != null) {
                ServerPlayNetworking.send(player, new PossessionEndS2CPacket());
            }
        }
    }

    /** Teleport for connected players; plain position set for headless (test) players. */
    private static void snapToAnchor(ServerPlayer player, Vec3 anchor) {
        if (player.connection != null) {
            player.teleportTo(anchor.x, anchor.y, anchor.z);
        } else {
            player.setPos(anchor.x, anchor.y, anchor.z);
        }
    }
}
