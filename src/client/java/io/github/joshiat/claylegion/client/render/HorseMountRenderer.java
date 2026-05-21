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

public class HorseMountRenderer extends EntityRenderer<BaseMountEntity, MountEntityRenderState> {

    private final HorseMountModel model;

    public HorseMountRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new HorseMountModel(ctx.bakeLayer(ModEntityModelLayers.CLAY_HORSE));
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
        // OLD models faced +Z; modern entities face -Z, so flip 180 deg around Y.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        // OLD preRenderCallback: scale 0.5 to clay-soldier size.
        poseStack.scale(0.5f, 0.5f, 0.5f);

        Identifier texture = HorseVariant.textureFor(state.variant);
        RenderType renderType = model.renderType(texture);
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
