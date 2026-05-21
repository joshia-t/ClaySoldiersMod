package io.github.joshiat.claylegion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.joshiat.claylegion.entity.mount.BaseMountEntity;
import io.github.joshiat.claylegion.entity.mount.HorseVariant;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Reuses the horse texture variants — wings UV is on the same 64x32 sheet.
 */
public class PegasusMountRenderer extends EntityRenderer<BaseMountEntity, MountEntityRenderState> {

    private final PegasusMountModel model;

    public PegasusMountRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new PegasusMountModel(ctx.bakeLayer(ModEntityModelLayers.CLAY_PEGASUS));
        this.shadowRadius = 0.24f;
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
        poseStack.scale(0.5f, 0.5f, 0.5f);

        Identifier texture = HorseVariant.textureFor(state.variant);
        RenderType renderType = model.renderType(texture);
        collector.submitModel(
            model, state, poseStack, renderType,
            state.lightCoords, OverlayTexture.NO_OVERLAY,
            0xFFFFFFFF, null, state.outlineColor, null
        );
        poseStack.popPose();
    }
}
