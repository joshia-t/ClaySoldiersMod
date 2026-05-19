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

    // Flat fully-opaque white texture: every UV samples white, so geometry can
    // be iterated without a UV-matched texture. Per-type solid colour is applied
    // as a vertex tint below so silhouettes stay readable.
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("clay-legion", "textures/entity/clay_mount_flat.png");

    private static int tintFor(int mountType) {
        return switch (mountType) {
            case 1 -> 0xFF8B5A2B; // horse  - brown
            case 2 -> 0xFFB0C4DE; // pegasus- light steel
            case 3 -> 0xFF3E8E3E; // turtle - green
            case 4 -> 0xFFD2B48C; // bunny  - tan
            case 5 -> 0xFF6B8E23; // gecko  - olive
            default -> 0xFFFF00FF; // unknown - magenta
        };
    }

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

        model.setupAnim(state);
        RenderType renderType = model.renderType(TEXTURE);
        collector.submitModel(
            model,
            state,
            poseStack,
            renderType,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            tintFor(state.mountType),
            null,
            state.outlineColor,
            null
        );
        poseStack.popPose();
    }
}
