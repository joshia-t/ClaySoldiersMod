package io.github.joshiat.claylegion.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Per-frame render state for ClaySoldierEntity.
 */
public class ClaySoldierEntityRenderState extends EntityRenderState {
    /**
     * Packed ARGB from SoldierTeam.dyeColor().
     * Default 0xFFFFFFFF = white = no tint (fallback if team lookup fails).
     */
    public int teamColor = 0xFFFFFFFF;
}
