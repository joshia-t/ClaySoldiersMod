package io.github.joshiat.claylegion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import io.github.joshiat.claylegion.entity.mount.TurtleVariant;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public class TurtleMountRenderer extends EntityRenderer<BaseMountEntity, MountEntityRenderState> {

    private final TurtleMountModel model;

    public TurtleMountRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new TurtleMountModel(ctx.bakeLayer(ModEntityModelLayers.CLAY_TURTLE));
        this.shadowRadius = 0.22f;
    }

    @Override
    public MountEntityRenderState createRenderState() {
        return new MountEntityRenderState();
    }

    @Override
    public void extractRenderState(BaseMountEntity entity, MountEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.renderYaw = entity.getYRot();
        state.variant = entity.getVariant();
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
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.scale(1.0f, 1.0f, 1.0f);

        Identifier texture = TurtleVariant.textureFor(state.variant);
        RenderType renderType = model.renderType(texture);
        collector.submitModel(
            model, state, poseStack, renderType,
            state.lightCoords, OverlayTexture.NO_OVERLAY,
            0xFFFFFFFF, null, state.outlineColor, null
        );
        poseStack.popPose();
    }
}
