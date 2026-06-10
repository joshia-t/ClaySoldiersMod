package io.github.joshiat.claylegion.client.possession;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side singleton that tracks whether the local player is currently
 * possessing a clay soldier.
 *
 * <p>The camera mixin reads {@link #getPossessedEntityId()} each frame.
 * When the value is {@code -1} no redirection is active.
 */
@Environment(EnvType.CLIENT)
public final class PossessionClientState {

    private static int possessedEntityId = -1;

    private PossessionClientState() {}

    /**
     * Returns the runtime entity ID of the currently possessed soldier,
     * or {@code -1} if no possession is active.
     */
    public static int getPossessedEntityId() {
        return possessedEntityId;
    }

    /** Called when the server sends {@code PossessionStartS2CPacket}. */
    public static void onPossessionStart(int soldierEntityId) {
        possessedEntityId = soldierEntityId;
    }

    /** Called when the server sends {@code PossessionEndS2CPacket}. */
    public static void onPossessionEnd() {
        possessedEntityId = -1;
    }

    /** Returns {@code true} if possession is currently active. */
    public static boolean isActive() {
        return possessedEntityId != -1;
    }
}
