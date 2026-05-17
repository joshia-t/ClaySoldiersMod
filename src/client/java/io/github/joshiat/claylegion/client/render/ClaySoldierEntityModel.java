package io.github.joshiat.claylegion.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Legacy-inspired clay soldier silhouette (head/body/arms/legs)
 * with compact low-poly geometry.
 */
public class ClaySoldierEntityModel extends EntityModel<ClaySoldierEntityRenderState> {

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

    public ClaySoldierEntityModel(ModelPart root) {
                super(root, RenderTypes::entityCutout);
        this.head     = root.getChild(PartNames.HEAD);
        this.body     = root.getChild(PartNames.BODY);
        this.rightArm = root.getChild(PartNames.RIGHT_ARM);
        this.leftArm  = root.getChild(PartNames.LEFT_ARM);
                this.rightLeg = root.getChild(PartNames.RIGHT_LEG);
                this.leftLeg  = root.getChild(PartNames.LEFT_LEG);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Positive-Y-up convention: EntityRenderer does NOT apply the LivingEntity
        // MODEL_Y_OFFSET flip, so y=0 is feet/ground and positive Y goes upward.
        //
        //  y  0.. 5  legs
        //  y  5.. 9  body
        //  y  5..10  arms (alongside body, splayed outward in PartPose for orientation test)
        //  y  9..12  head

        // Head: 3x3x3
        root.addOrReplaceChild(
                PartNames.HEAD,
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.5f, 9f, -1.5f, 3, 3, 3),
                PartPose.ZERO);

        // Body: 4x4x2
        root.addOrReplaceChild(
                PartNames.BODY,
                CubeListBuilder.create().texOffs(0, 8)
                        .addBox(-2f, 5f, -1f, 4, 4, 2),
                PartPose.ZERO);

        // Arms: pivot at shoulder (y=9), local cube hangs downward from pivot.
        root.addOrReplaceChild(
                PartNames.RIGHT_ARM,
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2f, -5f, -1f, 2, 5, 2),
                PartPose.offset(-2f, 9f, 0f));

        root.addOrReplaceChild(
                PartNames.LEFT_ARM,
                CubeListBuilder.create().texOffs(8, 16)
                        .addBox(0f, -5f, -1f, 2, 5, 2),
                PartPose.offset(2f, 9f, 0f));

        // Legs: pivot at hip (y=5), local cube hangs downward from pivot.
        root.addOrReplaceChild(
                PartNames.RIGHT_LEG,
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-1f, -5f, -1f, 2, 5, 2),
                PartPose.offset(-1f, 5f, 0f));

        root.addOrReplaceChild(
                PartNames.LEFT_LEG,
                CubeListBuilder.create().texOffs(24, 16)
                        .addBox(-1f, -5f, -1f, 2, 5, 2),
                PartPose.offset(1f, 5f, 0f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    /**
     * Toggle arm visibility for LOD. Called by the renderer before submit.
     */
    public void setArmVisibility(boolean visible) {
        rightArm.visible = visible;
        leftArm.visible  = visible;
    }

    @Override
    public void setupAnim(ClaySoldierEntityRenderState state) {
        // Reset per-frame animation transforms.
        head.xRot = 0.0f;
        head.yRot = 0.0f;

        body.xRot = 0.0f;
        body.yRot = 0.0f;

        rightArm.xRot = 0.0f;
        rightArm.yRot = 0.0f;
        leftArm.xRot = 0.0f;
        leftArm.yRot = 0.0f;

        rightLeg.xRot = 0.0f;
        rightLeg.yRot = 0.0f;
        leftLeg.xRot = 0.0f;
        leftLeg.yRot = 0.0f;

        float speedNorm = Mth.clamp(state.horizontalSpeed * 8.0f, 0.0f, 1.0f);
        boolean moving = speedNorm > 0.08f;

        if (moving) {
            float walk = Mth.sin(state.animTime * 0.7f) * 0.8f * speedNorm;
            rightLeg.xRot = walk;
            leftLeg.xRot = -walk;
            rightArm.xRot = -walk * 0.65f;
            leftArm.xRot = walk * 0.65f;
        }

                if (state.attackSwingProgress > 0.0f) {
                        float arc = Mth.sin(state.attackSwingProgress * Mth.PI);

                        // One-shot punch arc: fast extension and retraction per attack event.
                        rightArm.xRot = rightArm.xRot - (0.30f + arc * 1.35f);
                        leftArm.xRot = leftArm.xRot - (0.20f - arc * 0.65f);
                        rightArm.yRot = 0.16f;
                        leftArm.yRot = -0.16f;
        }
    }
}
