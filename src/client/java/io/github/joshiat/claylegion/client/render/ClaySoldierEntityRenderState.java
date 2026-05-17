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

    /** Horizontal speed in blocks/tick projected on XZ. */
    public float horizontalSpeed = 0.0f;

    /** Continuous local animation clock in ticks. */
    public float animTime = 0.0f;

    /** 0..1 single-shot swing progress triggered by attack events. */
    public float attackSwingProgress = 0.0f;

    /** Damage flash timer used for hurt overlay tint. */
    public int hurtFlashTicks = 0;

    /** Render yaw in degrees sourced from server-authoritative entity yaw. */
    public float renderYaw = 0.0f;

    /** Local translation offset applied in submit for partial-tick correction smoothing. */
    public float renderOffsetX = 0.0f;
    public float renderOffsetY = 0.0f;
    public float renderOffsetZ = 0.0f;
}
