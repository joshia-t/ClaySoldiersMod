package io.github.joshiat.claylegion.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Per-frame render state for lightweight mount entities.
 */
public class MountEntityRenderState extends EntityRenderState {
    public byte mountType = 0;
    public float renderYaw = 0.0f;
    public float horizontalSpeed = 0.0f;
    public float animTime = 0.0f;
}
