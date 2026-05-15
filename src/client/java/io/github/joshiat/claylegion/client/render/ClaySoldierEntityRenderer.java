package io.github.joshiat.claylegion.client.render;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Renders a ClaySoldierEntity using ClaySoldierEntityModel.
 * Phase 5+ will add team-colour overlay layer and equipment rendering.
 */
public class ClaySoldierEntityRenderer
        extends EntityRenderer<ClaySoldierEntity, ClaySoldierEntityRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("clay-legion", "textures/entity/clay_soldier.png");

    private final ClaySoldierEntityModel model;

    public ClaySoldierEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new ClaySoldierEntityModel(ctx.bakeLayer(ModEntityModelLayers.CLAY_SOLDIER));
        this.shadowRadius = 0.3f;
    }

    @Override
    public ClaySoldierEntityRenderState createRenderState() {
        return new ClaySoldierEntityRenderState();
    }

    @Override
    public void submit(ClaySoldierEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        super.submit(state, poseStack, collector, cameraRenderState);
        model.setupAnim(state);
        collector.submitModel(model, state, poseStack, TEXTURE,
                state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null);
    }
}
