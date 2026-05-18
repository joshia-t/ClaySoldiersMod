package io.github.joshiat.claylegion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Shared mount renderer for all mount entity variants.
 */
public class MountEntityRenderer extends EntityRenderer<BaseMountEntity, MountEntityRenderState> {

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("clay-legion", "textures/entity/clay_mount.png");

    private final MountEntityModel model;

    public MountEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new MountEntityModel(ctx.bakeLayer(ModEntityModelLayers.CLAY_MOUNT));
        this.shadowRadius = 0.24f;
    }

    @Override
    public MountEntityRenderState createRenderState() {
        return new MountEntityRenderState();
    }

    @Override
    public void extractRenderState(BaseMountEntity entity, MountEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.mountType = entity.getMountTypeId();
        state.renderYaw = entity.getYRot();

        Vec3 velocity = entity.getDeltaMovement();
        state.horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        state.animTime = entity.tickCount + partialTick;
    }

    @Override
    public void submit(MountEntityRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        super.submit(state, poseStack, collector, cameraRenderState);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.renderYaw));

        RenderType renderType = model.renderType(TEXTURE);
        collector.submitModel(
            model,
            state,
            poseStack,
            renderType,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0xFFFFFFFF,
            null,
            state.outlineColor,
            null
        );
        poseStack.popPose();
    }
}
