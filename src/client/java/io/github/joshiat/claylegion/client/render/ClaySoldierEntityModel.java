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
                super(root, RenderTypes::entityCutoutCull);
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

        // Feet-at-origin baseline (y=0 at ground contact) for predictable tuning.
        // Head: old clayman feel (3x3x3), y -12..-9
        root.addOrReplaceChild(
                PartNames.HEAD,
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.5f, -12f, -1.5f, 3, 3, 3),
                PartPose.ZERO);

        // Body: 4x4x2, y -9..-5
        root.addOrReplaceChild(
                PartNames.BODY,
                CubeListBuilder.create().texOffs(0, 8)
                        .addBox(-2f, -9f, -1f, 4, 4, 2),
                PartPose.ZERO);

        // Arms: 2x6x2, y -8..-2
        root.addOrReplaceChild(
                PartNames.RIGHT_ARM,
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-4f, -8f, -1f, 2, 6, 2),
                PartPose.ZERO);

        root.addOrReplaceChild(
                PartNames.LEFT_ARM,
                CubeListBuilder.create().texOffs(8, 16)
                        .addBox(2f, -8f, -1f, 2, 6, 2),
                PartPose.ZERO);

        // Legs: 2x5x2, y -5..0
        root.addOrReplaceChild(
                PartNames.RIGHT_LEG,
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-2f, -5f, -1f, 2, 5, 2),
                PartPose.ZERO);

        root.addOrReplaceChild(
                PartNames.LEFT_LEG,
                CubeListBuilder.create().texOffs(24, 16)
                        .addBox(0f, -5f, -1f, 2, 5, 2),
                PartPose.ZERO);

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
        // Future: walk cycle, attack swing, etc.
    }
}
