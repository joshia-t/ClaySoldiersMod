package io.github.joshiat.claylegion.client.render;

import io.github.joshiat.claylegion.entity.ClaySoldierEntity;
import io.github.joshiat.claylegion.render.RenderTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Renders a ClaySoldierEntity.
 *
 * Performance notes:
 *  - Team colour is a vertex tint on a single shared texture — no texture swaps.
 *  - LOD: arm ModelParts hidden beyond LOD_FINE_SQ (32 blocks²) to cut
 *    geometry submitted per distant soldier.
 */
public class ClaySoldierEntityRenderer
        extends EntityRenderer<ClaySoldierEntity, ClaySoldierEntityRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("clay-legion", "textures/entity/clay_soldier.png");

    /** Squared distance threshold beyond which arm parts are skipped. */
    private static final double LOD_FINE_SQ = 32.0 * 32.0;

    private final ClaySoldierEntityModel model;

    public ClaySoldierEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new ClaySoldierEntityModel(ctx.bakeLayer(ModEntityModelLayers.CLAY_SOLDIER));
        this.shadowRadius = 0.18f;
    }

    @Override
    public ClaySoldierEntityRenderState createRenderState() {
        return new ClaySoldierEntityRenderState();
    }

    @Override
    public void extractRenderState(ClaySoldierEntity entity,
                                   ClaySoldierEntityRenderState state,
                                   float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.teamColor = entity.getSoldierTeam().dyeColor();
    }

    @Override
    public void submit(ClaySoldierEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        super.submit(state, poseStack, collector, cameraRenderState);

        poseStack.pushPose();
        poseStack.translate(0.0f, RenderTuning.getYOffset(), 0.0f);
        float scale = RenderTuning.getScale();
        poseStack.scale(scale, scale, scale);

        // LOD: skip arm geometry for distant soldiers
        model.setArmVisibility(state.distanceToCameraSq < LOD_FINE_SQ);
        model.setupAnim(state);
        RenderType renderType = model.renderType(TEXTURE);
        // Match vanilla submit order: light, overlay, modelTint, sprite, outlineColor, crumbling.
        collector.submitModel(model, state, poseStack, renderType,
            state.lightCoords, OverlayTexture.NO_OVERLAY, state.teamColor, null, state.outlineColor, null);
        poseStack.popPose();
    }
}
