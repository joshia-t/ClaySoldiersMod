package io.github.joshiat.claylegion.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.joshiat.claylegion.block.entity.ClayNexusBlockEntity;
import io.github.joshiat.claylegion.registry.ItemRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the nexus's spawn template — the matching soldier doll spinning
 * above the block — so players can see what it will produce (issue #31).
 * Dormant nexuses (no doll inserted yet) display nothing.
 */
@Environment(EnvType.CLIENT)
public class ClayNexusRenderer
    implements BlockEntityRenderer<ClayNexusBlockEntity, ClayNexusRenderer.NexusRenderState> {

    private static final float SPIN_DEGREES_PER_TICK = 2.0f;
    private static final float BOB_HEIGHT = 0.05f;

    public static class NexusRenderState extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public boolean hasTemplate;
        public float spinDegrees;
        public float bobOffset;
    }

    private final ItemModelResolver itemModelResolver;

    public ClayNexusRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public NexusRenderState createRenderState() {
        return new NexusRenderState();
    }

    @Override
    public void extractRenderState(ClayNexusBlockEntity nexus, NexusRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(nexus, state, crumblingOverlay);

        state.hasTemplate = nexus.hasTemplate();
        if (!state.hasTemplate) {
            return;
        }

        ItemStack displayStack = new ItemStack(nexus.isTemplateBrick()
            ? ItemRegistry.BRICK_SOLDIER_DOLL
            : ItemRegistry.SOLDIER_DOLL);
        itemModelResolver.updateForTopItem(state.item, displayStack,
            ItemDisplayContext.GROUND, nexus.getLevel(), null, 0);

        float time = (nexus.getLevel() != null ? nexus.getLevel().getGameTime() % 360_000L : 0L) + partialTick;
        state.spinDegrees = time * SPIN_DEGREES_PER_TICK;
        state.bobOffset = (float) Math.sin(time * 0.08f) * BOB_HEIGHT;
    }

    @Override
    public void submit(NexusRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        if (!state.hasTemplate) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5f, 1.3f + state.bobOffset, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spinDegrees));
        poseStack.scale(0.75f, 0.75f, 0.75f);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
